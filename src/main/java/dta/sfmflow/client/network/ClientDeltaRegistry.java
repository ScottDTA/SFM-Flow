package dta.sfmflow.client.network;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.FlowWidgetContainer;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket.DeltaType;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only strategy registry mapping delta types to their modular UI handlers.
 * Bulletproofed against container, packet payload, and registry null pointer exceptions.
 */
@OnlyIn(Dist.CLIENT)
public class ClientDeltaRegistry {
  private static final Map<DeltaType, IDeltaStrategy> STRATEGIES = new EnumMap<>(DeltaType.class);

  static {
    // Register standard MOVE delta strategy using public getters [3]
    STRATEGIES.put(DeltaType.MOVE, (screen, packet, localComponent) -> {
        if (localComponent != null && packet.data() != null && packet.componentId() != null) {
          localComponent.setX(packet.data().getInt("x"));
          localComponent.setY(packet.data().getInt("y"));
          localComponent.setZ(packet.data().getInt("z"));
          
          if (screen.renderables != null) {
              // Shift the container's visual coordinate offset directly
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

    // Register standard SETTINGS delta strategy
    STRATEGIES.put(DeltaType.SETTINGS, (screen, packet, localComponent) -> {
        if (localComponent != null && packet.data() != null) {
          localComponent.loadData(packet.data());
          screen.refreshWidgetLayout();
        }
    });

    // Update the ADD strategy block inside ClientDeltaRegistry.java exactly like this:
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
                        // 🔥 THE FIREWALL: Wrap the concrete record parsing step to isolate field validation drops safely
                        try {
                            componentType.codec().codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, nbtData)
                                .resultOrPartial(err -> dta.sfmflow.SFMFlow.LOGGER.error("Failed to parse component fields: {}", err))
                                .ifPresent(decodedComponent -> {
                                    var components = blockEntity.getFlowComponents();
                                    if (components != null) {
                                        components.put(packet.componentId(), decodedComponent);
                                        screen.refreshWidgetLayout();
                                    }
                                });
                        } catch (Exception e) {
                            dta.sfmflow.SFMFlow.LOGGER.error("CRITICAL: Caught unhandled exception inside concrete component codec fields loop for '{}'!", typeStr, e);
                        }
                    } else {
                        dta.sfmflow.SFMFlow.LOGGER.error("Client cannot execute ADD packet: component identifier '{}' is completely unrecognized by the synchronized registry map!", typeStr);
                    }
                }
            }
        }
    });



    // Register standard REMOVE delta strategy
    STRATEGIES.put(DeltaType.REMOVE, (screen, packet, localComponent) -> {
        if (packet.componentId() != null && screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
            var be = screen.getMenu().getManagerBlockEntity();
            
            if (be.getFlowComponents() != null) {
                be.getFlowComponents().remove(packet.componentId());
            }
            
            if (be.getFlowConnections() != null) {
                be.getFlowConnections().removeIf(wire -> {
                    if (wire == null) return false;
                    UUID srcId = wire.getSourceComponentId();
                    UUID tgtId = wire.getTargetComponentId();
                    // FIXED: Protected predicate bounds against null pointer checks if wiring fields are empty
                    return (srcId != null && srcId.equals(packet.componentId())) || 
                           (tgtId != null && tgtId.equals(packet.componentId()));
                });
            }
            screen.refreshWidgetLayout();
        }
    });
  }

  /**
   * Evaluates and routes the delta packet to its corresponding client strategy safely.
   *
   * @param screen the active manager screen interface [3]
   * @param packet the received delta sync packet [3]
   */
  public static void handle(ManagerScreen screen, SyncComponentDeltaPacket packet) {
    if (screen == null || packet == null || packet.deltaType() == null) {
        return;
    }

    IDeltaStrategy strategy = STRATEGIES.get(packet.deltaType());
    if (strategy != null) {
      // FIXED: Insulated container chain fetching from crashing the loop if block entity is temporarily null
      if (screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
          var components = screen.getMenu().getManagerBlockEntity().getFlowComponents();
          if (components != null && packet.componentId() != null) {
              AbstractFlowComponent localComponent = components.get(packet.componentId());
              strategy.execute(screen, packet, localComponent);
          } else if (packet.deltaType() == DeltaType.ADD) {
              // ADD strategy doesn't require a local component to exist yet, execute it safely
              strategy.execute(screen, packet, null);
          }
      }
    }
  }
}
