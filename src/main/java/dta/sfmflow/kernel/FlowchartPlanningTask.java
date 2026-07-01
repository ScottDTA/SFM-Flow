package dta.sfmflow.kernel;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Cooperative state-machine planning task executing flowchart logic off-thread
 * [3]. Implements a 1000-node traversal limit to prevent stack overflows and
 * cyclic loop lockups [3]. Dynamically delegates planning actions
 * polymorphically to AbstractFlowComponent.plan [3].
 */
public class FlowchartPlanningTask {
	private final ManagerBlockEntity manager;
	private final ThreadSafeInventorySnapshot snapshot;
	private final List<FlowComponentConnections> connections;
	private final Map<UUID, AbstractFlowComponent> components;

	private final Queue<UUID> evaluationQueue = new ArrayDeque<>();
	private int nodesTraversed = 0;
	private boolean completed = false;

	// Dynamic multi-capability pipeline buffer registry mapping components and
	// their active capability streams [3]
	private final Map<UUID, Map<ResourceLocation, Object>> pipelineBuffers = new HashMap<>();

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
		public boolean tryWriteTask(ResourceLocation capabilityId, BlockPos src, int srcSlot,
				@Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide,
				Object taskParams) {
			return manager.getExecutionBuffer().tryWrite(capabilityId, src, srcSlot, srcSide, dest, destSlot, destSide,
					taskParams);
		}

		@Override
		public @Nullable Object getPipelineBuffer(UUID componentId, ResourceLocation capabilityId) {
			Map<ResourceLocation, Object> compMap = pipelineBuffers.get(componentId);
			return compMap != null ? compMap.get(capabilityId) : null;
		}

		@Override
		public void setPipelineBuffer(UUID componentId, ResourceLocation capabilityId, Object buffer) {
			pipelineBuffers.computeIfAbsent(componentId, k -> new HashMap<>()).put(capabilityId, buffer);
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
				manager.incrementBreakerTrips();
				SFMFlow.LOGGER.error(
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
