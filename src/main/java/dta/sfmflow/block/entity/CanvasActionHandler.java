package dta.sfmflow.block.entity;

import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.common.network.SculkEventListener;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Common, stateless helper consolidating flowchart canvas modification commands.
 */
public final class CanvasActionHandler {

	private CanvasActionHandler() {
	}

	public static void execute(ManagerBlockEntity manager, CanvasAction action, UUID componentId) {
		switch (action) {
			case DELETE -> handleDelete(manager, componentId);
			case COPY -> handleCopy(manager, componentId);
		}
	}

	private static void handleDelete(ManagerBlockEntity manager, UUID componentId) {
		manager.getFlowComponents().remove(componentId);
		manager.getFlowConnections().removeIf(wire -> wire.getSourceComponentId().equals(componentId)
				|| wire.getTargetComponentId().equals(componentId));
		manager.setDataDirty(true);
		manager.setChanged();

		manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), componentId,
				SyncComponentDeltaPacket.DeltaType.REMOVE, new CompoundTag()));
		
		manager.rebuildListeners();
	}

	private static void handleCopy(ManagerBlockEntity manager, UUID componentId) {
		AbstractFlowComponent original = manager.getFlowComponents().get(componentId);
		if (original != null && manager.getFlowComponents().size() < ServerConfig.MAX_COMPONENT_AMOUNT.get()) {
			UUID newId = UUID.randomUUID();
			AbstractFlowComponent copy = original.getType().createComponent(newId);
			CompoundTag settings = new CompoundTag();
			original.saveData(settings);
			copy.loadData(settings);

			copy.setBaseProperties(new AbstractFlowComponent.BaseProperties(newId, copy.getX(), copy.getY(),
					copy.getZ(), copy.getCustomName(), copy.getColorMask()));

			int h = copy.getVisualHeight();
			int nextX = original.getX() + 10;
			int nextY = original.getY() + 10;

			if (nextX + copy.getVisualWidth() > AbstractFlowComponent.CANVAS_MAX_X) {
				nextX = original.getX() - 10;
			}
			nextX = Math.max(22, Math.min(nextX, AbstractFlowComponent.CANVAS_MAX_X - copy.getVisualWidth()));

			if (nextY + h > AbstractFlowComponent.CANVAS_MAX_Y) {
				nextY = original.getY() - 10;
			}

			int minY = copy.hasInputNodes() ? 10 : 4;
			nextY = Math.max(minY, Math.min(nextY, AbstractFlowComponent.CANVAS_MAX_Y - h));

			copy.setX(nextX);
			copy.setY(nextY);
			copy.setZ(original.getZ() + 1);
			manager.getFlowComponents().put(newId, copy);
			manager.setDataDirty(true);
			manager.setChanged();

			CompoundTag tag = new CompoundTag();
			copy.saveData(tag);
			manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), newId,
					SyncComponentDeltaPacket.DeltaType.ADD, tag));
		}
	}

	public static void move(ManagerBlockEntity manager, ComponentMoved pData, IPayloadContext context) {
		for (ComponentMoved.Entry entry : pData.entries()) {
			AbstractFlowComponent component = manager.getFlowComponents().get(entry.id());
			if (component != null) {
				component.setX(entry.x());
				component.setY(entry.y());
				component.setZ(entry.z());
				manager.setDataDirty(true);

				CompoundTag dataTag = new CompoundTag();
				dataTag.putInt("x", entry.x());
				dataTag.putInt("y", entry.y());
				dataTag.putInt("z", entry.z());
				manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), entry.id(),
						SyncComponentDeltaPacket.DeltaType.MOVE, dataTag));
			}
		}
	}
}