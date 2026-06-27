package dta.sfmflow.networking;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.block.entity.CableClusterBlockEntity;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.serverbound.CanvasActionPacket;
import dta.sfmflow.networking.packets.serverbound.CreateNodePacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SyncClusterSlotDirectionPacket;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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
	 */
	public static void handleSaveComponentSettings(final SaveComponentSettings data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				dta.sfmflow.api.component.AbstractFlowComponent component = manager.getFlowComponents()
						.get(data.componentId());
				if (component != null) {
					component.loadData(data.settings());
					manager.setChanged();

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
	public static void handleCreateConnection(
			final dta.sfmflow.networking.packets.serverbound.CreateConnectionPacket data,
			final IPayloadContext context) {
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
				connections.add(new dta.sfmflow.flowcomponents.FlowComponentConnections(data.sourceId(),
						data.outputIdx(), data.targetId(), data.inputIdx()));
				manager.setChanged();

				// Serialize updated connections list to NBT Tag [3]
				net.minecraft.nbt.CompoundTag dataTag = new net.minecraft.nbt.CompoundTag();
				net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
				for (var conn : connections) {
					net.minecraft.nbt.CompoundTag connTag = new net.minecraft.nbt.CompoundTag();
					conn.save(connTag);
					listTag.add(connTag);
				}
				dataTag.put("connections", listTag);

				// Broadcast synchronized connections update to clients
				manager.broadcastConnectionsUpdate(new dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket(
						manager.getBlockPos(), dataTag));
			}
		});
	}

	/**
	 * Processes an incoming connection removal request on the server main thread.
	 * Cleans up the flowchart's wire array and broadcasts synchronization packets
	 * [3].
	 */
	public static void handleRemoveConnection(
			final dta.sfmflow.networking.packets.serverbound.RemoveConnectionPacket data,
			final IPayloadContext context) {
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
				net.minecraft.nbt.CompoundTag dataTag = new net.minecraft.nbt.CompoundTag();
				net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
				for (var conn : connections) {
					net.minecraft.nbt.CompoundTag connTag = new net.minecraft.nbt.CompoundTag();
					conn.save(connTag);
					listTag.add(connTag);
				}
				dataTag.put("connections", listTag);

				// Broadcast update instantly to observing players
				manager.broadcastConnectionsUpdate(new dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket(
						manager.getBlockPos(), dataTag));
			}
		});
	}

}