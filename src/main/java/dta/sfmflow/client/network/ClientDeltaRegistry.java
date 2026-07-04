package dta.sfmflow.client.network;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.FlowWidgetContainer;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket.DeltaType;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only strategy registry managing delta types to their modular UI
 * handlers. Bulletproofed against container, packet payload, and registry null
 * pointer exceptions.
 */
@OnlyIn(Dist.CLIENT)
public class ClientDeltaRegistry {
	private static final Map<DeltaType, IDeltaStrategy> STRATEGIES = new EnumMap<>(DeltaType.class);

	static {
		STRATEGIES.put(DeltaType.MOVE, (screen, packet, localComponent) -> {
			if (localComponent != null && packet.data() != null && packet.componentId() != null) {
				localComponent.setX(packet.data().getInt("x"));
				localComponent.setY(packet.data().getInt("y"));
				localComponent.setZ(packet.data().getInt("z"));

				if (screen.renderables != null) {
					for (Renderable r : screen.renderables) {
						if (r instanceof FlowWidgetContainer container && container.getComponent() != null) {
							UUID componentId = container.getComponent().getId();
							if (componentId != null && componentId.equals(packet.componentId())) {
								int screenX = (screen.width - screen.getImageWidth()) / 2 + localComponent.getX();
								int screenY = (screen.height - screen.getImageHeight()) / 2 + localComponent.getY();
								container.setX(screenX);
								container.setY(screenY);
								break;
							}
						}
					}
				}
			}
		});

		STRATEGIES.put(DeltaType.SETTINGS, (screen, packet, localComponent) -> {
			if (localComponent != null && packet.data() != null) {
				localComponent.loadData(packet.data());
				screen.refreshWidgetLayout(); // Rebuild widgets to instantly refresh names/colors! [3]
			}
		});

		STRATEGIES.put(DeltaType.ADD, (screen, packet, localComponent) -> {
			if (packet.data() != null && packet.componentId() != null) {
				if (screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
					var blockEntity = screen.getMenu().getManagerBlockEntity();
					CompoundTag nbtData = packet.data();

					if (nbtData.contains("type")) {
						String typeStr = nbtData.getString("type");
						ResourceLocation typeLoc = ResourceLocation.tryParse(typeStr);
						FlowComponentType componentType = FlowComponentType.REGISTRY.get(typeLoc);

						if (componentType != null) {
							try {
								componentType.codec().codec().parse(NbtOps.INSTANCE, nbtData).resultOrPartial(
										err -> SFMFlow.LOGGER.error("Failed to parse component fields: {}", err))
										.ifPresent(decodedComponent -> {
											var components = blockEntity.getFlowComponents();
											if (components != null) {
												components.put(packet.componentId(), decodedComponent);
												screen.refreshWidgetLayout();
											}
										});
							} catch (Exception e) {
								SFMFlow.LOGGER.error(
										"CRITICAL: Caught unhandled exception inside concrete component codec fields loop for '{}'!",
										typeStr, e);
							}
						} else {
							SFMFlow.LOGGER.error(
									"Client cannot execute ADD packet: component identifier '{}' is completely unrecognized by the synchronized registry map!",
									typeStr);
						}
					}
				}
			}
		});

		STRATEGIES.put(DeltaType.REMOVE, (screen, packet, localComponent) -> {
			if (packet.componentId() != null && screen.getMenu() != null
					&& screen.getMenu().getManagerBlockEntity() != null) {
				var be = screen.getMenu().getManagerBlockEntity();

				if (be.getFlowComponents() != null) {
					be.getFlowComponents().remove(packet.componentId());
				}

				if (be.getFlowConnections() != null) {
					be.getFlowConnections().removeIf(wire -> {
						if (wire == null)
							return false;
						UUID srcId = wire.getSourceComponentId();
						UUID tgtId = wire.getTargetComponentId();
						return (srcId != null && srcId.equals(packet.componentId()))
								|| (tgtId != null && tgtId.equals(packet.componentId()));
					});
				}
				screen.refreshWidgetLayout();
			}
		});
	}

	public static void handle(ManagerScreen screen, SyncComponentDeltaPacket packet) {
		if (screen == null || packet == null || packet.deltaType() == null) {
			return;
		}

		IDeltaStrategy strategy = STRATEGIES.get(packet.deltaType());
		if (strategy != null) {
			if (screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
				var components = screen.getMenu().getManagerBlockEntity().getFlowComponents();
				if (components != null && packet.componentId() != null) {
					AbstractFlowComponent localComponent = components.get(packet.componentId());
					strategy.execute(screen, packet, localComponent);
				} else if (packet.deltaType() == DeltaType.ADD) {
					strategy.execute(screen, packet, null);
				}
			}
		}
	}
}