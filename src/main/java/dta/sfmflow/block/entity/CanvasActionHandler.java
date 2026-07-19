package dta.sfmflow.block.entity;

import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.flowcomponents.GroupComponent;
import dta.sfmflow.flowcomponents.GroupInputComponent;
import dta.sfmflow.flowcomponents.GroupOutputComponent;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
		AbstractFlowComponent deleted = manager.getFlowComponents().get(componentId);
		if (deleted == null) {
			return;
		}

		UUID parentGroupId = deleted.getParentGroupId();

		// 1. Recursively collect all nested descendants of this folder to prevent leaks
		Set<UUID> toDelete = new HashSet<>();
		toDelete.add(componentId);
		if (deleted instanceof GroupComponent) {
			collectDescendants(manager, componentId, toDelete);
		}

		// 2. Safely remove all collected elements and their wire connections
		for (UUID id : toDelete) {
			manager.getFlowComponents().remove(id);
			manager.getFlowConnections().removeIf(wire -> wire.getSourceComponentId().equals(id)
					|| wire.getTargetComponentId().equals(id));

			// Broadcast individual removal updates to keep clients in perfect sync
			manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), id,
					SyncComponentDeltaPacket.DeltaType.REMOVE, new CompoundTag()));
		}

		manager.setDataDirty(true);
		manager.setChanged();

		// Recount pins if we deleted a terminal
		if (parentGroupId != null && (deleted instanceof GroupInputComponent 
				|| deleted instanceof GroupOutputComponent)) {
			manager.updateGroupPinCounts(parentGroupId);
		}

		manager.rebuildListeners();
	}

	/**
	 * Recursively traverses our flat map to collect all nested folder descendants.
	 */
	private static void collectDescendants(ManagerBlockEntity manager, UUID parentGroupId, Set<UUID> collected) {
		for (AbstractFlowComponent comp : manager.getFlowComponents().values()) {
			if (parentGroupId.equals(comp.getParentGroupId())) {
				collected.add(comp.getId());
				if (comp instanceof GroupComponent) {
					collectDescendants(manager, comp.getId(), collected); // Recurse deeply
				}
			}
		}
	}

	private static void handleCopy(ManagerBlockEntity manager, UUID componentId) {
		AbstractFlowComponent original = manager.getFlowComponents().get(componentId);
		if (original != null && manager.getFlowComponents().size() < ServerConfig.MAX_COMPONENT_AMOUNT.get()) {
			
			// Enforce maximum limit of 5 inputs/outputs per group on duplication (copy)
			if (original.getParentGroupId() != null) {
				if (original instanceof GroupInputComponent && manager.countGroupTerminals(original.getParentGroupId(), true) >= 5) {
					return;
				}
				if (original instanceof GroupOutputComponent && manager.countGroupTerminals(original.getParentGroupId(), false) >= 5) {
					return;
				}
			}

			UUID newId = UUID.randomUUID();
			AbstractFlowComponent copy = original.getType().createComponent(newId);
			CompoundTag settings = new CompoundTag();
			original.saveData(settings);
			copy.loadData(settings);

			copy.setBaseProperties(new AbstractFlowComponent.BaseProperties(
					newId, 
					copy.getX(), 
					copy.getY(),
					copy.getZ(), 
					copy.getCustomName(), 
					copy.getColorMask(),
					Optional.ofNullable(original.getParentGroupId())
			));

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

			// Recount parent Group node pins on copy
			if (copy.getParentGroupId() != null && (copy instanceof GroupInputComponent || copy instanceof GroupOutputComponent)) {
				manager.updateGroupPinCounts(copy.getParentGroupId());
			}
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