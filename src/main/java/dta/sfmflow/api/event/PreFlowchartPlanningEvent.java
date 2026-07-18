package dta.sfmflow.api.event;

import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.Event;
import java.util.List;
import java.util.UUID;

/**
 * Fired on the main server thread right before a background planning task is submitted.
 * Allows addons to append custom data to the snapshot or flowchart NBT before it is cloned.
 */
public class PreFlowchartPlanningEvent extends Event {
	private final ManagerBlockEntity manager;
	private final ThreadSafeInventorySnapshot snapshot;
	private final CompoundTag flowchartNbt;
	private final List<UUID> activeTriggers;

	public PreFlowchartPlanningEvent(ManagerBlockEntity manager, ThreadSafeInventorySnapshot snapshot, CompoundTag flowchartNbt, List<UUID> activeTriggers) {
		this.manager = manager;
		this.snapshot = snapshot;
		this.flowchartNbt = flowchartNbt;
		this.activeTriggers = activeTriggers;
	}

	public ManagerBlockEntity getManager() { return manager; }
	public ThreadSafeInventorySnapshot getSnapshot() { return snapshot; }
	public CompoundTag getFlowchartNbt() { return flowchartNbt; }
	public List<UUID> getActiveTriggers() { return activeTriggers; }
}