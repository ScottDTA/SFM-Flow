package dta.sfmflow.kernel;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Cooperative state-machine planning task executing flowchart logic off-thread
 * [3]. Implements a 1000-node traversal limit to prevent stack overflows and
 * cyclic loop lockups [3]. Upgraded with sorted slot indices, slot-enabled
 * configurations, and copy-before-shrink safety validations [3].
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
	 * Initializes the planning task and seeds only the currently elapsed triggers
	 * [3].
	 *
	 * @param manager        the managing block entity [3]
	 * @param snapshot       the immutable deep-copied inventory snapshot [3]
	 * @param activeTriggers list of UUIDs representing elapsed interval triggers
	 *                       [3]
	 */
	public FlowchartPlanningTask(ManagerBlockEntity manager, ThreadSafeInventorySnapshot snapshot,
			List<UUID> activeTriggers) {
		this.manager = manager;
		this.snapshot = snapshot;
		this.connections = new ArrayList<>(manager.getFlowConnections());
		this.components = new HashMap<>(manager.getFlowComponents());

		// Locate triggers and queue evaluation routes safely without modifying input
		// collections [3]
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
				this.evaluateNode(current);
			}
		}
		if (evaluationQueue.isEmpty()) {
			this.completed = true;
			return true;
		}
		return false;
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

	private boolean matchesFilter(ItemTransferComponent component, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		boolean found = false;
		for (ItemStack filter : component.getFilterItems()) {
			if (filter != null && !filter.isEmpty() && ItemStack.isSameItemSameComponents(stack, filter)) {
				found = true;
				break;
			}
		}

		if (component.isWhitelist()) {
			return found;
		} else {
			return !found;
		}
	}

	/**
	 * Executes multi-slot transaction planning, updating virtual snapshot states in
	 * real-time [3]. Upgraded to sort keys sequentially, perform slot configuration
	 * validations, and copy stacks before shrinking them [3].
	 */
	private void planItemTransfer(ItemTransferComponent source, ItemTransferComponent target) {
		var inventories = manager.getInventories();
		BlockPos srcInventoryPos = null;
		BlockPos tgtInventoryPos = null;

		for (var block : inventories) {
			if (block.getId() == source.getInventoryId()) {
				srcInventoryPos = block.getBlockPos();
			}
			if (block.getId() == target.getInventoryId()) {
				tgtInventoryPos = block.getBlockPos();
			}
		}

		if (srcInventoryPos == null || tgtInventoryPos == null) {
			return;
		}

		// Collect the user's active directions for the source container [3]
		List<Direction> activeSrcSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (source.isSideActive(dir)) {
				activeSrcSides.add(dir);
			}
		}
		if (activeSrcSides.isEmpty()) {
			activeSrcSides.add(null);
		}

		// Collect the user's active directions for the target container [3]
		List<Direction> activeTgtSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (target.isSideActive(dir)) {
				activeTgtSides.add(dir);
			}
		}
		if (activeTgtSides.isEmpty()) {
			activeTgtSides.add(null);
		}

		for (Direction srcSide : activeSrcSides) {
			var srcInv = snapshot.getInventory(srcInventoryPos, srcSide);
			if (srcInv == null)
				continue;

			for (Direction tgtSide : activeTgtSides) {
				var tgtInv = snapshot.getInventory(tgtInventoryPos, tgtSide);
				if (tgtInv == null)
					continue;

				// Sort slot keys sequentially from 0 to N to guarantee orderly evaluations [3]
				List<Integer> sortedSrcSlots = new ArrayList<>(srcInv.slots().keySet());
				Collections.sort(sortedSrcSlots);

				List<Integer> sortedTgtSlots = new ArrayList<>(tgtInv.slots().keySet());
				Collections.sort(sortedTgtSlots);

				for (int srcSlot : sortedSrcSlots) {
					ThreadSafeInventorySnapshot.SlotSnapshot srcEntry = srcInv.slots().get(srcSlot);
					ItemStack srcStack = srcEntry.stack();
					if (srcStack.isEmpty()) {
						continue;
					}

					if (source.getTargetSlot() != -1 && source.getTargetSlot() != srcSlot) {
						continue;
					}

					// Check if slot has been disabled in the slot layout popup
					int mainSrcSlot = srcEntry.mainSlotIndex();
					if (!source.isSlotEnabled(srcSide, mainSrcSlot)) {
						continue;
					}

					if (!matchesFilter(source, srcStack)) {
						continue;
					}

					int srcRemaining = srcStack.getCount();

					for (int tgtSlot : sortedTgtSlots) {
						if (srcRemaining <= 0) {
							break;
						}

						ThreadSafeInventorySnapshot.SlotSnapshot tgtEntry = tgtInv.slots().get(tgtSlot);
						ItemStack tgtStack = tgtEntry.stack();

						if (target.getTargetSlot() != -1 && target.getTargetSlot() != tgtSlot) {
							continue;
						}

						// Check if slot has been disabled in the slot layout popup
						int mainTgtSlot = tgtEntry.mainSlotIndex();
						if (!target.isSlotEnabled(tgtSide, mainTgtSlot)) {
							continue;
						}

						if (!matchesFilter(target, srcStack)) {
							continue;
						}

						boolean canInsert = false;
						int maxInsertable = 0;

						// Resolve the item's maximum stack size limit for the target slot capability
						// wrapper [3]
						int actualLimit = Math.min(tgtEntry.slotLimit(), srcStack.getMaxStackSize());

						if (tgtStack.isEmpty()) {
							canInsert = true;
							maxInsertable = actualLimit;
						} else if (ItemStack.isSameItemSameComponents(srcStack, tgtStack)) {
							int remainingSpace = actualLimit - tgtStack.getCount();
							if (remainingSpace > 0) {
								canInsert = true;
								maxInsertable = remainingSpace;
							}
						}

						if (canInsert) {
							int amountToTransfer = Math.min(srcRemaining, maxInsertable);
							if (amountToTransfer > 0) {
								boolean success = manager.getExecutionBuffer().tryWrite(srcInventoryPos, srcSlot,
										srcSide, tgtInventoryPos, tgtSlot, tgtSide, srcStack, amountToTransfer);
								if (success) {
									// Copy FIRST before shrinking to preserve type and components completely [3]
									if (tgtStack.isEmpty()) {
										ItemStack newTgt = srcStack.copy();
										newTgt.setCount(amountToTransfer);
										tgtInv.slots().put(tgtSlot, new ThreadSafeInventorySnapshot.SlotSnapshot(newTgt,
												tgtEntry.slotLimit(), mainTgtSlot));
									} else {
										tgtStack.grow(amountToTransfer);
									}
									srcStack.shrink(amountToTransfer);
									srcRemaining -= amountToTransfer;
								}
							}
						}
					}
				}
			}
		}
	}
}