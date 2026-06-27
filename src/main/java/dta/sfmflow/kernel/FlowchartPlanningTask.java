package dta.sfmflow.kernel;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Cooperative state-machine planning task executing flowchart logic off-thread
 * [3]. Implements a 1000-node traversal limit to prevent stack overflows and
 * cyclic loop lockups [3].
 */
public class FlowchartPlanningTask {
	private final ManagerBlockEntity manager;
	private final ThreadSafeInventorySnapshot snapshot;
	private final List<FlowComponentConnections> connections;
	private final Map<UUID, AbstractFlowComponent> components;

	private final Queue<UUID> evaluationQueue = new ArrayDeque<>();
	private int nodesTraversed = 0;
	private boolean completed = false;

	/**
	 * Initializes the planning task and seeds the starting trigger nodes [3].
	 *
	 * @param manager  the managing block entity [3]
	 * @param snapshot the immutable deep-copied inventory snapshot [3]
	 */
	public FlowchartPlanningTask(ManagerBlockEntity manager, ThreadSafeInventorySnapshot snapshot) {
		this.manager = manager;
		this.snapshot = snapshot;
		this.connections = new ArrayList<>(manager.getFlowConnections());
		this.components = new HashMap<>(manager.getFlowComponents());

		// Seed trigger components
		for (AbstractFlowComponent component : this.components.values()) {
			if (component instanceof IntervalTriggerComponent) {
				evaluationQueue.add(component.getId());
			}
		}
	}

	/**
	 * Executes a cooperative slice of evaluation logic, yielding if it exceeds
	 * standard bounds [3].
	 *
	 * @param timeBudgetNs execution time limit in nanoseconds (e.g. 1ms =
	 *                     1,000,000) [3]
	 * @return true if the planning task is fully completed, false otherwise [3]
	 */
	public boolean evaluateSlice(long timeBudgetNs) {
		if (completed) {
			return true;
		}

		long startTime = System.nanoTime();

		while (!evaluationQueue.isEmpty()) {
			if (System.nanoTime() - startTime >= timeBudgetNs) {
				return false; // Yield and wait for next planning cycle [3]
			}

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
				evaluateNode(current);
			}
		}

		completed = true;
		return true;
	}

	private void evaluateNode(AbstractFlowComponent current) {
		if (current instanceof IntervalTriggerComponent trigger) {
			List<FlowComponentConnections> outputs = findOutputConnections(trigger.getId());
			for (FlowComponentConnections conn : outputs) {
				evaluationQueue.add(conn.getTargetComponentId());
			}
		} else if (current instanceof ItemTransferComponent transfer) {
			if (transfer.isInput()) {
				List<FlowComponentConnections> outputs = findOutputConnections(transfer.getId());
				for (FlowComponentConnections conn : outputs) {
					AbstractFlowComponent targetComponent = components.get(conn.getTargetComponentId());
					if (targetComponent instanceof ItemTransferComponent targetOutput && !targetOutput.isInput()) {
						planItemTransfer(transfer, targetOutput);
						evaluationQueue.add(targetOutput.getId());
					}
				}
			}
		}
	}

	private List<FlowComponentConnections> findOutputConnections(UUID sourceId) {
		List<FlowComponentConnections> list = new ArrayList<>();
		for (FlowComponentConnections conn : connections) {
			if (conn.getSourceComponentId().equals(sourceId)) {
				list.add(conn);
			}
		}
		return list;
	}

	private void planItemTransfer(ItemTransferComponent source, ItemTransferComponent target) {
		var inventories = manager.getInventories();
		BlockPos srcInventoryPos = null;
		BlockPos tgtInventoryPos = null;

		for (var block : inventories) {
			if (block.getId() == source.getInventoryId() || source.isUseAll()) {
				srcInventoryPos = block.getBlockPos();
			}
			if (block.getId() == target.getInventoryId() || target.isUseAll()) {
				tgtInventoryPos = block.getBlockPos();
			}
		}

		if (srcInventoryPos == null || tgtInventoryPos == null) {
			return;
		}

		var srcInv = snapshot.getInventory(srcInventoryPos);
		var tgtInv = snapshot.getInventory(tgtInventoryPos);

		if (srcInv == null || tgtInv == null) {
			return;
		}

		for (Map.Entry<Integer, ThreadSafeInventorySnapshot.SlotSnapshot> srcEntry : srcInv.slots().entrySet()) {
			int srcSlot = srcEntry.getKey();
			ItemStack srcStack = srcEntry.getValue().stack();
			if (srcStack.isEmpty()) {
				continue;
			}

			if (source.getTargetSlot() != -1 && source.getTargetSlot() != srcSlot) {
				continue;
			}

			for (Map.Entry<Integer, ThreadSafeInventorySnapshot.SlotSnapshot> tgtEntry : tgtInv.slots().entrySet()) {
				int tgtSlot = tgtEntry.getKey();
				ItemStack tgtStack = tgtEntry.getValue().stack();

				if (target.getTargetSlot() != -1 && target.getTargetSlot() != tgtSlot) {
					continue;
				}

				boolean canInsert = false;
				if (tgtStack.isEmpty()) {
					canInsert = true;
				} else if (ItemStack.isSameItemSameComponents(srcStack, tgtStack)) {
					canInsert = tgtStack.getCount() < tgtEntry.getValue().slotLimit();
				}

				if (canInsert) {
					int amountToTransfer = Math.min(srcStack.getCount(), source.getItemCount());
					if (amountToTransfer > 0) {
						// Symmetrically write to the main-thread execution ring buffer [3]
						manager.getExecutionBuffer().tryWrite(srcInventoryPos, srcSlot, tgtInventoryPos, tgtSlot,
								srcStack, amountToTransfer);
						return;
					}
				}
			}
		}
	}
}