package dta.sfmflow.networking;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.block.entity.CableClusterBlockEntity;
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
import dta.sfmflow.networking.packets.serverbound.SyncClusterSlotDirectionPacket;
import dta.sfmflow.networking.packets.serverbound.RequestInventorySlotsPacket;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles custom serverbound packet payloads, enqueuing coordinate updates,
 * click tasks, and workspace state updates onto the main server thread
 * sequential queue.
 */
public class ServerPayloadHandler {
	private ServerPayloadHandler() {
	}

	/**
	 * Processes a client request to perform a canvas node action on the server
	 * thread [3].
	 *
	 * @param data    the canvas action payload [3]
	 * @param context the packet execution context [3]
	 */
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

	/**
	 * Processes a request to dynamically spawn a new node on the server thread,
	 * with safety checks [3].
	 *
	 * @param data    the node creation payload [3]
	 * @param context the packet execution context [3]
	 */
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

	/**
	 * Processes a client drag layout modification payload on the main server thread
	 * [3].
	 */
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

	/**
	 * Processes a client settings update payload on the main server thread [3].
	 * Returns any cursor-carried item stack safely back to the player inventory on
	 * completion [3].
	 */
	public static void handleSaveComponentSettings(final SaveComponentSettings data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				AbstractFlowComponent component = manager.getFlowComponents().get(data.componentId());
				if (component != null) {
					component.loadData(data.settings());
					manager.setChanged();

					// Symmetrical carried item snap-back: safely return any held item to the
					// inventory slots [3]
					ServerPlayer player = (ServerPlayer) context.player();
					ItemStack carried = player.containerMenu.getCarried();
					if (carried != null && !carried.isEmpty()) {
						player.getInventory().placeItemBackInInventory(carried);
						player.containerMenu.setCarried(ItemStack.EMPTY);
						player.containerMenu.broadcastChanges(); // Synchronize changes immediately to the client
					}

					manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), data.componentId(),
							SyncComponentDeltaPacket.DeltaType.SETTINGS, data.settings()));
				}
			}
		});
	}

	/**
	 * Syncs and registers the updated face configuration on the server thread [3].
	 */
	public static void handleSyncClusterSlotDirection(final SyncClusterSlotDirectionPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof CableClusterBlockEntity be) {
				be.setSlotDirection(data.slotIndex(), data.directionOrdinal());
			}
		});
	}

	/**
	 * Processes an incoming connection request on the server main thread. Enforces
	 * 1-wire limits on both pins and broadcasts connections sync packet [3].
	 */
	public static void handleCreateConnection(final CreateConnectionPacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				var connections = manager.getFlowConnections();

				// 1-wire limit check: Remove any old connection using either the same output or
				// same input [3]
				connections.removeIf(conn -> (conn.getSourceComponentId().equals(data.sourceId())
						&& conn.getOutputNodeIndex() == data.outputIdx())
						|| (conn.getTargetComponentId().equals(data.targetId())
								&& conn.getInputNodeIndex() == data.inputIdx()));

				// Register new connection wire
				connections.add(new FlowComponentConnections(data.sourceId(), data.outputIdx(), data.targetId(),
						data.inputIdx()));
				manager.setChanged();

				// Serialize updated connections list to NBT Tag [3]
				CompoundTag dataTag = new CompoundTag();
				ListTag listTag = new ListTag();
				for (var conn : connections) {
					CompoundTag connTag = new CompoundTag();
					conn.save(connTag);
					listTag.add(connTag);
				}
				dataTag.put("connections", listTag);

				// Broadcast synchronized connections update to clients
				manager.broadcastConnectionsUpdate(new SyncConnectionsPacket(manager.getBlockPos(), dataTag));
			}
		});
	}

	/**
	 * Processes an incoming connection removal request on the server main thread.
	 * Cleans up the flowchart's wire array and broadcasts synchronization packets
	 * [3].
	 */
	public static void handleRemoveConnection(final RemoveConnectionPacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				var connections = manager.getFlowConnections();

				// Find and extract matching connection [3]
				connections.removeIf(conn -> conn.getSourceComponentId().equals(data.sourceId())
						&& conn.getOutputNodeIndex() == data.outputIdx()
						&& conn.getTargetComponentId().equals(data.targetId())
						&& conn.getInputNodeIndex() == data.inputIdx());
				manager.setChanged();

				// Re-serialize connections NBT package [3]
				CompoundTag dataTag = new CompoundTag();
				ListTag listTag = new ListTag();
				for (var conn : connections) {
					CompoundTag connTag = new CompoundTag();
					conn.save(connTag);
					listTag.add(connTag);
				}
				dataTag.put("connections", listTag);

				// Broadcast update instantly to observing players
				manager.broadcastConnectionsUpdate(new SyncConnectionsPacket(manager.getBlockPos(), dataTag));
			}
		});
	}

	/**
	 * Processes an incoming variable binding request on the server main thread.
	 * Maps variables to target components and broadcasts setting changes [3].
	 */
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
					manager.setChanged();

					// Broadcast setting updates cleanly to other observing menus
					CompoundTag settingsTag = new CompoundTag();
					transfer.saveData(settingsTag);
					manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), transfer.getId(),
							SyncComponentDeltaPacket.DeltaType.SETTINGS, settingsTag));
				}
			}
		});
	}

	/**
	 * Processes an incoming slot item layout sync request on the server thread [3].
	 */
	public static void handleRequestInventorySlots(final RequestInventorySlotsPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer player = (ServerPlayer) context.player();
			Level level = player.level();
			if (level.hasChunkAt(data.pos())) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, data.pos(), null);
				if (handler != null) {
					CompoundTag dataTag = new CompoundTag();
					ListTag list = new ListTag();
					for (int i = 0; i < handler.getSlots(); i++) {
						ItemStack stack = handler.getStackInSlot(i);
						if (!stack.isEmpty()) {
							CompoundTag slotTag = new CompoundTag();
							slotTag.putInt("slot", i);
							slotTag.put("item", stack.save(level.registryAccess()));
							list.add(slotTag);
						}
					}
					dataTag.put("items", list);
					dataTag.putInt("totalSlots", handler.getSlots());

					PacketDistributor.sendToPlayer(player, new SyncInventorySlotsPacket(data.pos(), dataTag));
				}
			}
		});
	}

	public static void handleSetActiveFilterComponent(final SetActiveFilterComponentPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					if (data.componentId() == null) {
						menu.setActiveFilterComponent(null);
					} else {
						var comp = manager.getFlowComponents().get(data.componentId());
						if (comp instanceof ItemTransferComponent transfer) {
							menu.setActiveFilterComponent(transfer);
						}
					}
				}
			}
		});
	}

}