package dta.sfmflow.flowcomponents;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.execution.FlowItemBuffer;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Common, stateless helper consolidating item transfer simulation, extraction,
 * and deposition planning routines.
 */
public final class ItemTransferPlanner {

	private ItemTransferPlanner() {
	}

	public static void planInput(FlowchartPlanningContext context, ItemTransferComponent component) {
		FlowItemBuffer myOutputBuffer = new FlowItemBuffer();

		// Carry over any incoming items passed from upstream input nodes
		FlowItemBuffer myInputBuffer = context.getComponentBuffer(component.getId());
		if (!myInputBuffer.isEmpty()) {
			for (FlowItemBuffer.BufferedItem item : myInputBuffer.getItems()) {
				myOutputBuffer.add(item.srcPos(), item.srcSlot(), item.srcSide(), item.stack().copy());
			}
		}

		// Extract our own configured inventory items into the combined buffer 
		extractItemsIntoBuffer(context, component, myOutputBuffer);

		// Copy extracted buffer contents onto connected target input buffers
		// sequentially
		if (!myOutputBuffer.isEmpty()) {
			for (var conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(component.getId())) {
					UUID targetId = conn.getTargetComponentId();
					FlowItemBuffer targetInputBuffer = context.getComponentBuffer(targetId);

					for (FlowItemBuffer.BufferedItem item : myOutputBuffer.getItems()) {
						targetInputBuffer.add(item.srcPos(), item.srcSlot(), item.srcSide(), item.stack().copy());
					}
					context.enqueue(targetId);
				}
			}
		}
	}

	public static void planOutput(FlowchartPlanningContext context, ItemTransferComponent component) {
		FlowItemBuffer myInputBuffer = context.getComponentBuffer(component.getId());

		if (!myInputBuffer.isEmpty()) {
			FlowItemBuffer myOutputBuffer = new FlowItemBuffer();
			depositItemsFromBuffer(context, component, myInputBuffer, myOutputBuffer);

			// Propagate remaining un-deposited leftovers downstream along the connection lines
			for (var conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(component.getId())) {
					UUID targetId = conn.getTargetComponentId();
					FlowItemBuffer targetInputBuffer = context.getComponentBuffer(targetId);

					for (FlowItemBuffer.BufferedItem item : myOutputBuffer.getItems()) {
						targetInputBuffer.add(item.srcPos(), item.srcSlot(), item.srcSide(), item.stack().copy());
					}
					context.enqueue(targetId);
				}
			}
		}
	}

	/**
	 * Updates simulation copies dynamically by mapping to the verified main slot index.
	 */
	private static void updateSnapshotCopies(FlowchartPlanningContext context, BlockPos pos, int mainSlot,
			ItemStack updatedStack) {
		for (Direction dir : Direction.values()) {
			var inv = context.getSnapshot().getInventory(pos, dir);
			if (inv != null) {
				for (var entry : inv.slots().entrySet()) {
					if (entry.getValue().mainSlotIndex() == mainSlot) {
						inv.slots().put(entry.getKey(), new ThreadSafeInventorySnapshot.SlotSnapshot(
								updatedStack.copy(),
								entry.getValue().slotLimit(),
								mainSlot
						));
					}
				}
			}
		}
		var nullInv = context.getSnapshot().getInventory(pos, null);
		if (nullInv != null) {
			for (var entry : nullInv.slots().entrySet()) {
				if (entry.getValue().mainSlotIndex() == mainSlot) {
					nullInv.slots().put(entry.getKey(), new ThreadSafeInventorySnapshot.SlotSnapshot(
							updatedStack.copy(),
							entry.getValue().slotLimit(),
							mainSlot
					));
				}
			}
		}
	}

	private static void extractItemsIntoBuffer(FlowchartPlanningContext context, ItemTransferComponent component,
			FlowItemBuffer buffer) {
		var inventories = context.getConnectedInventories();
		ConnectionBlock srcInventory = null;

		for (var block : inventories) {
			if (block.getId() == component.getInventoryId() && !block.isSleeping()) {
				srcInventory = block;
				break;
			}
		}

		if (srcInventory == null) {
			return;
		}

		BlockPos srcInventoryPos = srcInventory.getBlockPos();
		List<Direction> activeSrcSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (component.isSideActive(dir)) {
				activeSrcSides.add(dir);
			}
		}
		if (activeSrcSides.isEmpty()) {
			activeSrcSides.add(null);
		}

		Map<Item, Integer> grabbedCounts = new HashMap<>();

		for (Direction srcSide : activeSrcSides) {
			var srcInv = context.getSnapshot().getInventory(srcInventoryPos, srcSide);
			if (srcInv == null)
				continue;

			List<Integer> sortedSrcSlots = new ArrayList<>(srcInv.slots().keySet());
			Collections.sort(sortedSrcSlots);

			for (int srcSlot : sortedSrcSlots) {
				ThreadSafeInventorySnapshot.SlotSnapshot srcEntry = srcInv.slots().get(srcSlot);
				ItemStack srcStack = srcEntry.stack();
				if (srcStack.isEmpty()) {
					continue;
				}

				// Restrict extraction strictly to the card's slot inside a cluster
				int allowedSlot = srcInventory.getSlotIndex();
				if (allowedSlot != -1) {
					if (srcSlot != allowedSlot) {
						continue;
					}
				} else if (component.getTargetSlot() != -1 && component.getTargetSlot() != srcSlot) {
					continue;
				}

				int mainSrcSlot = srcEntry.mainSlotIndex();
				if (!component.isSlotEnabled(srcSide, mainSrcSlot)) {
					continue;
				}

				if (!matchesFilter(context, component, srcStack)) {
					continue;
				}

				int totalInSource = 0;
				for (var entry : srcInv.slots().values()) {
					if (ItemStack.isSameItemSameComponents(srcStack, entry.stack())) {
						totalInSource += entry.stack().getCount();
					}
				}

				int srcLimit = getFilterLimit(context, component, srcStack);
				int srcRemaining = srcStack.getCount();

				if (component.isWhitelist()) {
					if (srcLimit > 0) {
						int alreadyGrabbed = grabbedCounts.getOrDefault(srcStack.getItem(), 0);
						int remainingGrabLimit = srcLimit - alreadyGrabbed;
						if (remainingGrabLimit <= 0) {
							continue;
						}
						srcRemaining = Math.min(srcRemaining, remainingGrabLimit);
					}
				} else {
					if (srcLimit > 0) {
						int availableOverLimit = totalInSource - srcLimit;
						if (availableOverLimit <= 0) {
							continue;
						}
						srcRemaining = Math.min(srcRemaining, availableOverLimit);
					}
				}

				if (srcRemaining > 0) {
					Item srcItem = srcStack.getItem();
					ItemStack extracted = srcStack.copy();
					extracted.setCount(srcRemaining);

					FlowLogger.execution("Extracting Slot %d: Side=%s, Count=%d, AlreadyGrabbed=%d", srcSlot, srcSide,
							srcRemaining, grabbedCounts.getOrDefault(srcItem, 0));

					buffer.add(srcInventoryPos, srcSlot, srcSide, extracted);

					srcStack.shrink(srcRemaining);
					updateSnapshotCopies(context, srcInventoryPos, mainSrcSlot, srcStack); // PASS mainSrcSlot here
					grabbedCounts.put(srcItem, grabbedCounts.getOrDefault(srcItem, 0) + srcRemaining);
				}
			}
		}
	}

	private static void depositItemsFromBuffer(FlowchartPlanningContext context, ItemTransferComponent component,
			FlowItemBuffer inputBuffer, FlowItemBuffer outputBuffer) {
		var inventories = context.getConnectedInventories();
		ConnectionBlock tgtInventory = null;

		for (var block : inventories) {
			if (block.getId() == component.getInventoryId() && !block.isSleeping()) {
				tgtInventory = block;
				break;
			}
		}

		if (tgtInventory == null) {
			for (FlowItemBuffer.BufferedItem item : inputBuffer.getItems()) {
				outputBuffer.add(item.srcPos(), item.srcSlot(), item.srcSide(), item.stack().copy());
			}
			return;
		}

		BlockPos tgtInventoryPos = tgtInventory.getBlockPos();
		List<Direction> activeTgtSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (component.isSideActive(dir)) {
				activeTgtSides.add(dir);
			}
		}
		if (activeTgtSides.isEmpty()) {
			activeTgtSides.add(null);
		}

		Map<Item, Integer> alreadyHeldBack = new HashMap<>();

		for (FlowItemBuffer.BufferedItem incomingItem : inputBuffer.getItems()) {
			ItemStack incoming = incomingItem.stack();
			if (incoming.isEmpty())
				continue;

			if (!matchesFilter(context, component, incoming)) {
				outputBuffer.add(incomingItem.srcPos(), incomingItem.srcSlot(), incomingItem.srcSide(),
						incoming.copy());
				continue;
			}

			int remainingToDeposit = incoming.getCount();
			int tgtLimit = getFilterLimit(context, component, incoming);

			int allowedDeposit = remainingToDeposit;
			if (!component.isWhitelist() && tgtLimit > 0) {
				int held = alreadyHeldBack.getOrDefault(incoming.getItem(), 0);
				int remainingToHoldBack = Math.max(0, tgtLimit - held);
				int amountToHoldBack = Math.min(remainingToDeposit, remainingToHoldBack);
				allowedDeposit = remainingToDeposit - amountToHoldBack;

				alreadyHeldBack.put(incoming.getItem(), held + amountToHoldBack);
			}

			for (Direction tgtSide : activeTgtSides) {
				if (remainingToDeposit <= 0)
					break;

				var tgtInv = context.getSnapshot().getInventory(tgtInventoryPos, tgtSide);
				if (tgtInv == null)
					continue;

				List<Integer> sortedTgtSlots = new ArrayList<>(tgtInv.slots().keySet());
				Collections.sort(sortedTgtSlots);

				// PASS 1: Fill up existing matching stacks strictly first to preserve inventory space 
				for (int tgtSlot : sortedTgtSlots) {
					if (remainingToDeposit <= 0)
						break;

					ThreadSafeInventorySnapshot.SlotSnapshot tgtEntry = tgtInv.slots().get(tgtSlot);
					ItemStack tgtStack = tgtEntry.stack();

					// Restrict deposition strictly to the card's slot inside a cluster 
					int allowedSlot = tgtInventory.getSlotIndex();
					if (allowedSlot != -1) {
						if (tgtSlot != allowedSlot) {
							continue;
						}
					} else if (component.getTargetSlot() != -1 && component.getTargetSlot() != tgtSlot) {
						continue;
					}

					int mainTgtSlot = tgtEntry.mainSlotIndex();
					if (!component.isSlotEnabled(tgtSide, mainTgtSlot)) {
						continue;
					}

					if (!tgtStack.isEmpty() && ItemStack.isSameItemSameComponents(incoming, tgtStack)) {
						int totalInTarget = 0;
						for (var entry : tgtInv.slots().values()) {
							if (ItemStack.isSameItemSameComponents(incoming, entry.stack())) {
								totalInTarget += entry.stack().getCount();
							}
						}

						int maxToDeposit = incoming.getMaxStackSize();
						if (component.isWhitelist()) {
							if (tgtLimit > 0) {
								maxToDeposit = Math.max(0, tgtLimit - totalInTarget);
								if (maxToDeposit <= 0) {
									continue;
								}
							}
						} else {
							maxToDeposit = allowedDeposit;
							if (maxToDeposit <= 0) {
								continue;
							}
						}

						int remainingSpace = Math.min(tgtEntry.slotLimit(), maxToDeposit) - tgtStack.getCount();
						if (remainingSpace > 0) {
							int amountToTransfer = Math.min(remainingToDeposit, remainingSpace);
							if (amountToTransfer > 0) {
								int taskDestSlot = (component.getTargetSlot() == -1 && allowedSlot == -1) ? -1 : tgtSlot;
								boolean success = context.tryWriteTask(incomingItem.srcPos(), incomingItem.srcSlot(),
										incomingItem.srcSide(), tgtInventoryPos, taskDestSlot, tgtSide, incoming,
										amountToTransfer);
								if (success) {
									tgtStack.grow(amountToTransfer);
									updateSnapshotCopies(context, tgtInventoryPos, mainTgtSlot, tgtStack); // PASS mainTgtSlot here
									incoming.shrink(amountToTransfer);
									remainingToDeposit -= amountToTransfer;
									if (!component.isWhitelist()) {
										allowedDeposit -= amountToTransfer;
									}
								}
							}
						}
					}
				}

				// PASS 2: Place remaining items into empty slots 
				for (int tgtSlot : sortedTgtSlots) {
					if (remainingToDeposit <= 0)
						break;

					ThreadSafeInventorySnapshot.SlotSnapshot tgtEntry = tgtInv.slots().get(tgtSlot);
					ItemStack tgtStack = tgtEntry.stack();

					// Restrict deposition strictly to the card's slot inside a cluster 
					int allowedSlot = tgtInventory.getSlotIndex();
					if (allowedSlot != -1) {
						if (tgtSlot != allowedSlot) {
							continue;
						}
					} else if (component.getTargetSlot() != -1 && component.getTargetSlot() != tgtSlot) {
						continue;
					}

					int mainTgtSlot = tgtEntry.mainSlotIndex();
					if (!component.isSlotEnabled(tgtSide, mainTgtSlot)) {
						continue;
					}

					if (tgtStack.isEmpty()) {
						int totalInTarget = 0;
						for (var entry : tgtInv.slots().values()) {
							if (ItemStack.isSameItemSameComponents(incoming, entry.stack())) {
								totalInTarget += entry.stack().getCount();
							}
						}

						int maxToDeposit = incoming.getMaxStackSize();
						if (component.isWhitelist()) {
							if (tgtLimit > 0) {
								maxToDeposit = Math.max(0, tgtLimit - totalInTarget);
								if (maxToDeposit <= 0) {
									continue;
								}
							}
						} else {
							maxToDeposit = allowedDeposit;
							if (maxToDeposit <= 0) {
								continue;
							}
						}

						int amountToTransfer = Math.min(remainingToDeposit,
								Math.min(tgtEntry.slotLimit(), maxToDeposit));
						if (amountToTransfer > 0) {
							int taskDestSlot = (component.getTargetSlot() == -1 && allowedSlot == -1) ? -1 : tgtSlot;
							boolean success = context.tryWriteTask(incomingItem.srcPos(), incomingItem.srcSlot(),
									incomingItem.srcSide(), tgtInventoryPos, taskDestSlot, tgtSide, incoming,
									amountToTransfer);
							if (success) {
								ItemStack newTgt = incoming.copy();
								newTgt.setCount(amountToTransfer);
								tgtInv.slots().put(tgtSlot, new ThreadSafeInventorySnapshot.SlotSnapshot(newTgt,
										tgtEntry.slotLimit(), mainTgtSlot));
								updateSnapshotCopies(context, tgtInventoryPos, mainTgtSlot, newTgt); // PASS mainTgtSlot here
								incoming.shrink(amountToTransfer);
								remainingToDeposit -= amountToTransfer;
								if (!component.isWhitelist()) {
									allowedDeposit -= amountToTransfer;
								}
							}
						}
					}
				}
			}

			if (remainingToDeposit > 0) {
				ItemStack remainingStack = incoming.copy();
				remainingStack.setCount(remainingToDeposit);
				outputBuffer.add(incomingItem.srcPos(), incomingItem.srcSlot(), incomingItem.srcSide(), remainingStack);
			}
		}
	}

	private static boolean matchesFilter(FlowchartPlanningContext context, ItemTransferComponent component,
			ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		int limit = -1;
		boolean found = false;

		if (component.getBoundFilterVariableId() != null) {
			AbstractFlowComponent boundComp = context.getComponents().get(component.getBoundFilterVariableId());
			if (boundComp instanceof AdvancedItemFilterVariableComponent varComp) {
				if (AdvancedItemFilterVariableComponent.matchesVariableFilter(varComp, stack)) {
					found = true;
					limit = varComp.isUseQuantity() ? varComp.getQuantity() : -1;
				}
			}
		} else {
			for (int i = 0; i < component.getFilterItems().size(); i++) {
				ItemStack filter = component.getFilterItems().get(i);
				if (filter == null || filter.isEmpty()) {
					continue;
				}

				if (filter.getItem() == ModItems.VARIABLE_CARD.get() || filter.is(ModItems.VARIABLE_CARD.get())) {
					CompoundTag tag = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
					if (tag.contains("VariableId")) {
						UUID varId = tag.getUUID("VariableId");
						AbstractFlowComponent varComp = context.getComponents().get(varId);
						if (varComp instanceof AdvancedItemFilterVariableComponent advancedVar) {
							if (AdvancedItemFilterVariableComponent.matchesVariableFilter(advancedVar, stack)) {
								found = true;
								limit = advancedVar.isUseQuantity() ? advancedVar.getQuantity() : -1;
								break;
							}
						}
					}
				} else if (ItemStack.isSameItemSameComponents(stack, filter)) {
					found = true;
					limit = i < component.getFilterLimits().size() ? component.getFilterLimits().get(i) : -1;
					break;
				}
			}
		}

		if (component.isWhitelist()) {
			return found;
		} else {
			return !found || limit > 0;
		}
	}

	private static int getFilterLimit(FlowchartPlanningContext context, ItemTransferComponent component,
			ItemStack stack) {
		if (component.getBoundFilterVariableId() != null) {
			AbstractFlowComponent boundComp = context.getComponents().get(component.getBoundFilterVariableId());
			if (boundComp instanceof AdvancedItemFilterVariableComponent varComp) {
				if (AdvancedItemFilterVariableComponent.matchesVariableFilter(varComp, stack)) {
					return varComp.isUseQuantity() ? varComp.getQuantity() : -1;
				}
			}
			return -1;
		}

		for (ItemStack filter : component.getFilterItems()) {
			if (filter == null || filter.isEmpty())
				continue;

			if (filter.getItem() == ModItems.VARIABLE_CARD.get() || filter.is(ModItems.VARIABLE_CARD.get())) {
				CompoundTag tag = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
				if (tag.contains("VariableId")) {
					UUID varId = tag.getUUID("VariableId");
					AbstractFlowComponent varComp = context.getComponents().get(varId);
					if (varComp instanceof AdvancedItemFilterVariableComponent advancedVar) {
						if (AdvancedItemFilterVariableComponent.matchesVariableFilter(advancedVar, stack)) {
							int resolvedLimit = advancedVar.isUseQuantity() ? advancedVar.getQuantity() : -1;
							FlowLogger.execution(
									"getFilterLimit Resolved Variable Card: ID=%s, Item=%s, UseQty=%b, Limit=%d", varId,
									stack.getItem().toString(), advancedVar.isUseQuantity(), resolvedLimit);
							return resolvedLimit;
						}
					}
				}
			} else if (ItemStack.isSameItemSameComponents(stack, filter)) {
				return component.getFilterLimit(stack);
			}
		}

		return -1;
	}
}