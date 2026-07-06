package dta.sfmflow.networking;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.block.entity.CableClusterBlockEntity;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket;
import dta.sfmflow.networking.packets.clientbound.SyncInventorySlotsPacket;
import dta.sfmflow.networking.packets.serverbound.BindVariablePacket;
import dta.sfmflow.networking.packets.serverbound.CanvasActionPacket;
import dta.sfmflow.networking.packets.serverbound.CreateNodePacket;
import dta.sfmflow.networking.packets.serverbound.RemoveConnectionPacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import dta.sfmflow.networking.packets.serverbound.CreateConnectionPacket;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.networking.packets.serverbound.SyncCarriedItemPacket;
import dta.sfmflow.networking.packets.serverbound.SyncClusterSlotDirectionPacket;
import dta.sfmflow.networking.packets.serverbound.RequestInventorySlotsPacket;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles serverbound network payloads, registering coordinates updates, click
 * tasks, and workspace state updates safely on the server thread [3].
 */
public class ServerPayloadHandler {
	private ServerPayloadHandler() {
	}

	public static void handleCanvasAction(final CanvasActionPacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					manager.executeCanvasAction(data.action(), data.componentId());
					manager.setChanged();
				}
			}
		});
	}

	public static void handleCreateNode(final CreateNodePacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					ResourceLocation typeLoc = data.componentTypeLoc();
					FlowComponentType type = FlowComponentType.REGISTRY.get(typeLoc);

					if (type != null && manager.getFlowComponents().size() < ServerConfig.MAX_COMPONENT_AMOUNT.get()) {
						manager.addFlowComponent(type, context.player());
						manager.setChanged();
					} else {
						SFMFlow.LOGGER.warn("Attempted to spawn invalid or blocked node: {}", typeLoc);
					}
				}
			}
		});
	}

	public static void handleComponentMoved(final ComponentMoved data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					manager.componentMoved(data, context);
					manager.setChanged();
				}
			}
		});
	}

	public static void handleSaveComponentSettings(final SaveComponentSettings data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				AbstractFlowComponent component = manager.getFlowComponents().get(data.componentId());
				if (component != null) {
					component.loadData(data.settings());
					manager.setDataDirty(true); // Flag dirty to trigger saving [3]
					manager.setChanged();

					ServerPlayer player = (ServerPlayer) context.player();
					ItemStack carried = player.containerMenu.getCarried();
					if (carried != null && !carried.isEmpty()) {
						player.getInventory().placeItemBackInInventory(carried);
						player.containerMenu.setCarried(ItemStack.EMPTY);
						player.containerMenu.broadcastChanges();
					}

					manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), data.componentId(),
							SyncComponentDeltaPacket.DeltaType.SETTINGS, data.settings()));
				}
			}
		});
	}

	public static void handleSyncClusterSlotDirection(final SyncClusterSlotDirectionPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof CableClusterBlockEntity be) {
				be.setSlotDirection(data.slotIndex(), data.directionOrdinal());
			}
		});
	}

	public static void handleCreateConnection(final CreateConnectionPacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				var connections = manager.getFlowConnections();

				connections.removeIf(conn -> (conn.getSourceComponentId().equals(data.sourceId())
						&& conn.getOutputNodeIndex() == data.outputIdx())
						|| (conn.getTargetComponentId().equals(data.targetId())
								&& conn.getInputNodeIndex() == data.inputIdx()));

				connections.add(new FlowComponentConnections(data.sourceId(), data.outputIdx(), data.targetId(),
						data.inputIdx()));
				manager.setDataDirty(true); // Flag dirty to trigger saving [3]
				manager.setChanged();

				CompoundTag dataTag = new CompoundTag();
				ListTag listTag = new ListTag();
				for (var conn : connections) {
					CompoundTag connTag = new CompoundTag();
					conn.save(connTag);
					listTag.add(connTag);
				}
				dataTag.put("connections", listTag);

				manager.broadcastConnectionsUpdate(new SyncConnectionsPacket(manager.getBlockPos(), dataTag));
			}
		});
	}

	public static void handleRemoveConnection(final RemoveConnectionPacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				var connections = manager.getFlowConnections();

				connections.removeIf(conn -> conn.getSourceComponentId().equals(data.sourceId())
						&& conn.getOutputNodeIndex() == data.outputIdx()
						&& conn.getTargetComponentId().equals(data.targetId())
						&& conn.getInputNodeIndex() == data.inputIdx());
				manager.setDataDirty(true); // Flag dirty to trigger saving [3]
				manager.setChanged();

				CompoundTag dataTag = new CompoundTag();
				ListTag listTag = new ListTag();
				for (var conn : connections) {
					CompoundTag connTag = new CompoundTag();
					conn.save(connTag);
					listTag.add(connTag);
				}
				dataTag.put("connections", listTag);

				manager.broadcastConnectionsUpdate(new SyncConnectionsPacket(manager.getBlockPos(), dataTag));
			}
		});
	}

	public static void handleBindVariable(final BindVariablePacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				var component = manager.getFlowComponents().get(data.componentId());
				if (component instanceof ItemTransferComponent transfer) {
					if (data.isGroupVariable()) {
						transfer.setBoundGroupVariableId(data.variableId());
					} else {
						transfer.setBoundFilterVariableId(data.variableId());
					}
					manager.setDataDirty(true); // Flag dirty to trigger saving [3]
					manager.setChanged();

					CompoundTag settingsTag = new CompoundTag();
					transfer.saveData(settingsTag);
					manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), transfer.getId(),
							SyncComponentDeltaPacket.DeltaType.SETTINGS, settingsTag));
				}
			}
		});
	}

	public static void handleRequestInventorySlots(final RequestInventorySlotsPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer player = (ServerPlayer) context.player();
			Level level = player.level();
			if (level.hasChunkAt(data.pos())) {
				IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, data.pos(), null);
				if (itemHandler != null) {
					CompoundTag dataTag = new CompoundTag();
					ListTag list = new ListTag();
					for (int i = 0; i < itemHandler.getSlots(); i++) {
						ItemStack stack = itemHandler.getStackInSlot(i);
						if (!stack.isEmpty()) {
							CompoundTag slotTag = new CompoundTag();
							slotTag.putInt("slot", i);
							slotTag.put("item", stack.save(level.registryAccess()));
							list.add(slotTag);
						}
					}
					dataTag.put("items", list);
					dataTag.putInt("totalSlots", itemHandler.getSlots());

					PacketDistributor.sendToPlayer(player, new SyncInventorySlotsPacket(data.pos(), dataTag));
				} else {
					// Fallback to fluid handler capability query for fluid layouts (such as
					// Cauldrons)
					IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, data.pos(), null);
					if (fluidHandler == null) {
						fluidHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.FluidHandler.BLOCK,
								level, data.pos(), level.getBlockState(data.pos()), null);
					}

					if (fluidHandler != null) {
						CompoundTag dataTag = new CompoundTag();
						ListTag list = new ListTag();
						for (int i = 0; i < fluidHandler.getTanks(); i++) {
							FluidStack fluid = fluidHandler.getFluidInTank(i);
							if (!fluid.isEmpty()) {
								Item bucket = fluid.getFluid().getBucket();
								if (bucket != null && bucket != Items.AIR) {
									ItemStack bucketStack = new ItemStack(bucket);
									CompoundTag slotTag = new CompoundTag();
									slotTag.putInt("slot", i);
									slotTag.put("item", bucketStack.save(level.registryAccess()));
									list.add(slotTag);
								}
							}
						}
						dataTag.put("items", list);
						dataTag.putInt("totalSlots", fluidHandler.getTanks());

						PacketDistributor.sendToPlayer(player, new SyncInventorySlotsPacket(data.pos(), dataTag));
					}
				}
			}
		});
	}

	/**
	 * Activates the targeted component safely on both the clientbound and
	 * serverbound container menus [3].
	 */
	public static void handleSetActiveFilterComponent(final SetActiveFilterComponentPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					if (data.componentId() == null) {
						menu.setActiveComponent(null); // Deactivate ghost slots
					} else {
						var comp = manager.getFlowComponents().get(data.componentId());
						if (comp != null) {
							// Generalization fix: set the generic component on the server-side menu [3]
							menu.setActiveComponent(comp);
						}
					}
				}
			}
		});
	}

	/**
	 * Synchronizes the visual card stack safely on the server menu container [3].
	 */
	public static void handleSyncCarriedItem(final SyncCarriedItemPacket payload, final IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer player = (ServerPlayer) context.player();
			ItemStack stack = payload.carried();

			// EXPLOIT FIREWALL: Only allow setting the carried item if it is a
			// VARIABLE_CARD or empty [3]
			if (stack.isEmpty() || stack.is(ModItems.VARIABLE_CARD.get())) {
				player.containerMenu.setCarried(stack);
				player.containerMenu.broadcastChanges();
			}
		});
	}
}