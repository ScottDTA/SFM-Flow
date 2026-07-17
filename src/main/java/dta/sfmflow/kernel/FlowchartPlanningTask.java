package dta.sfmflow.kernel;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.api.flowchart.Flowchart;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Cooperative state-machine planning task executing flowchart logic off-thread [3].
 * Operates purely on thread-safe isolated snapshots and copies [3].
 */
public class FlowchartPlanningTask {
	private final ExecutionRingBuffer executionBuffer;
	private final ThreadSafeInventorySnapshot snapshot;
	private final List<FlowComponentConnections> connections;
	private final Map<UUID, AbstractFlowComponent> components;
	private final Runnable onCircuitBreakerTripped;

	private final Queue<UUID> evaluationQueue = new ArrayDeque<>();
	private int nodesTraversed = 0;
	private boolean completed = false;
	private UUID activePlanningId = null;

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
			return snapshot.getCapturedInventories();
		}

		@Override
		public void enqueue(UUID componentId) {
			if (componentId != null) {
				// Automated Splitter Chain Resetter [3]
				if (activePlanningId != null) {
					AbstractFlowComponent current = components.get(activePlanningId);
					if (!(current instanceof dta.sfmflow.flowcomponents.SplitterComponent)) {
						// Reset depth of target node to 0 if the source is not a splitter
						ResourceLocation chainDepthKey = ResourceLocation.fromNamespaceAndPath("sfmflow", "splitter_chain_depth");
						setPipelineBuffer(componentId, chainDepthKey, 0);
					}
				}
				evaluationQueue.add(componentId);
			}
		}

		@Override
		public boolean tryWriteTask(ResourceLocation capabilityId, BlockPos src, int srcSlot,
				@Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide,
				Object taskParams) {
			return executionBuffer.tryWrite(capabilityId, src, srcSlot, srcSide, dest, destSlot, destSide,
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

	public FlowchartPlanningTask(
			ExecutionRingBuffer executionBuffer,
			Runnable onCircuitBreakerTripped,
			ThreadSafeInventorySnapshot snapshot,
			Flowchart flowchart,
			List<UUID> activeTriggers
	) {
		this.executionBuffer = executionBuffer;
		this.onCircuitBreakerTripped = onCircuitBreakerTripped;
		this.snapshot = snapshot;
		this.connections = flowchart.connections(); // Isolated connection list [3]
		this.components = flowchart.components();     // Isolated components map [3]

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
				onCircuitBreakerTripped.run(); // Concurrency-safe atomic callback [3]
				SFMFlow.LOGGER.error(
						"[SFM-Flow] Circuit breaker tripped! Flowchart exceeded the 1000-node traversal limit, canceling planning task [3].");
				completed = true;
				return true;
			}

			UUID currentId = evaluationQueue.poll();
			AbstractFlowComponent current = components.get(currentId);
			if (current != null) {
				nodesTraversed++;
				this.activePlanningId = currentId; // Set active planning ID [3]
				current.plan(this.planningContext);
				this.activePlanningId = null; // Clear active planning ID after planning [3]
			}
		}
		if (evaluationQueue.isEmpty()) {
			this.completed = true;
			return true;
		}
		return false;
	}
}