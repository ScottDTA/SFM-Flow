package dta.sfmflow.kernel;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Cooperative state-machine planning task executing flowchart logic off-thread [3].
 * Implements a 1000-node traversal limit to prevent stack overflows and cyclic loop lockups [3].
 * Dynamically delegates planning actions polymorphically to AbstractFlowComponent.plan [3].
 */
public class FlowchartPlanningTask {
	private final ManagerBlockEntity manager;
	private final ThreadSafeInventorySnapshot snapshot;
	private final List<FlowComponentConnections> connections;
	private final Map<UUID, AbstractFlowComponent> components;

	private final Queue<UUID> evaluationQueue = new ArrayDeque<>();
	private int nodesTraversed = 0;
	private boolean completed = false;

	private final FlowchartPlanningContext planningContext = new FlowchartPlanningContext() {
		@Override
		public ThreadSafeInventorySnapshot getSnapshot() {
			return snapshot;
		}

		@Override
		public Map<UUID, AbstractFlowComponent> getComponents() {
			return Collections.unmodifiableMap(components);
		}

		@Override
		public List<FlowComponentConnections> getConnections() {
			return Collections.unmodifiableList(connections);
		}

		@Override
		public List<ConnectionBlock> getConnectedInventories() {
			return manager.getInventories();
		}

		@Override
		public void enqueue(UUID componentId) {
			if (componentId != null) {
				evaluationQueue.add(componentId);
			}
		}

		@Override
		public boolean tryWriteTask(BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, ItemStack stack, int amount) {
			return manager.getExecutionBuffer().tryWrite(src, srcSlot, srcSide, dest, destSlot, destSide, stack, amount);
		}
	};

	public FlowchartPlanningTask(ManagerBlockEntity manager, ThreadSafeInventorySnapshot snapshot,
			List<UUID> activeTriggers) {
		this.manager = manager;
		this.snapshot = snapshot;
		this.connections = new ArrayList<>(manager.getFlowConnections());
		this.components = new HashMap<>(manager.getFlowComponents());

		for (UUID id : activeTriggers) {
			if (id != null && this.components.containsKey(id)) {
				this.evaluationQueue.add(id);
			}
		}
	}

	public boolean evaluateSlice(long budgetNs) {
		long start = System.nanoTime();
		while (!evaluationQueue.isEmpty() && (System.nanoTime() - start < budgetNs)) {
			if (nodesTraversed >= 1000) {
				dta.sfmflow.SFMFlow.LOGGER.error(
						"[SFM-Flow] Circuit breaker tripped! Flowchart exceeded the 1000-node traversal limit, canceling planning task [3].");
				completed = true;
				return true;
			}

			UUID currentId = evaluationQueue.poll();
			AbstractFlowComponent current = components.get(currentId);
			if (current != null) {
				nodesTraversed++;
				current.plan(this.planningContext); // Polymorphic delegation [3]
			}
		}
		if (evaluationQueue.isEmpty()) {
			this.completed = true;
			return true;
		}
		return false;
	}
}