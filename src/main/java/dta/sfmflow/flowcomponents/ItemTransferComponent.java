package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IFilterable;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.execution.FlowItemBuffer;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified logic component handling both item inputs (extractions) and item
 * outputs (depositions) [3]. Supports NBT serialization for per-side enabled
 * slot bitmasks, variable bindings, and custom item quantity limits [3].
 */
public class ItemTransferComponent extends AbstractFlowComponent
		implements IFilterable, IInventoryTarget, ISideConfigurable {
	private final boolean isInput;
	private int inventoryId = -1;
	private boolean useAll = true;
	private int targetSlot = -1;

	private int activeSidesMask = 63;

	private final List<Long> enabledSlotsMasks = new ArrayList<>(List.of(-1L, -1L, -1L, -1L, -1L, -1L));

	private UUID boundGroupVariableId = null;
	private UUID boundFilterVariableId = null;

	private boolean whitelist = true;
	private final List<ItemStack> filterItems = new ArrayList<>();
	private final List<Integer> filterLimits = new ArrayList<>();

	public static final MapCodec<ItemTransferComponent> INPUT_CODEC = makeCodec(true);
	public static final MapCodec<ItemTransferComponent> OUTPUT_CODEC = makeCodec(false);

	private static MapCodec<ItemTransferComponent> makeCodec(boolean isInput) {
		return RecordCodecBuilder.mapCodec(instance -> instance
				.group(BaseProperties.CODEC.fieldOf("base").forGetter(ItemTransferComponent::getBaseProperties),
						Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(ItemTransferComponent::getInventoryId),
						Codec.BOOL.optionalFieldOf("useAll", true).forGetter(ItemTransferComponent::isUseAll),
						Codec.INT.optionalFieldOf("targetSlot", -1).forGetter(ItemTransferComponent::getTargetSlot),
						Codec.INT.optionalFieldOf("activeSidesMask", 63)
								.forGetter(ItemTransferComponent::getActiveSidesMask),
						Codec.LONG.listOf().optionalFieldOf("enabledSlotsMasks", List.of(-1L, -1L, -1L, -1L, -1L, -1L))
								.forGetter(ItemTransferComponent::getEnabledSlotsMasks),
						UUIDUtil.CODEC.optionalFieldOf("boundGroupVariableId")
								.forGetter(comp -> Optional.ofNullable(comp.getBoundGroupVariableId())),
						UUIDUtil.CODEC.optionalFieldOf("boundFilterVariableId")
								.forGetter(comp -> Optional.ofNullable(comp.getBoundFilterVariableId())),
						Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(ItemTransferComponent::isWhitelist),
						ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("filterItems", List.of())
								.forGetter(ItemTransferComponent::getFilterItems),
						Codec.INT.listOf()
								.optionalFieldOf("filterLimits",
										List.of(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1))
								.forGetter(ItemTransferComponent::getFilterLimits))
				.apply(instance, (baseProps, invId, useAllVal, slot, sidesMask, masksList, groupVar, filterVar,
						whitelistVal, filtersList, limitsList) -> {
					ItemTransferComponent comp = new ItemTransferComponent(baseProps.id(), isInput);
					comp.setBaseProperties(baseProps);
					comp.inventoryId = invId;
					comp.useAll = useAllVal;
					comp.targetSlot = slot;
					comp.activeSidesMask = sidesMask;
					comp.enabledSlotsMasks.clear();
					comp.enabledSlotsMasks.addAll(masksList);
					comp.boundGroupVariableId = groupVar.orElse(null);
					comp.boundFilterVariableId = filterVar.orElse(null);
					comp.whitelist = whitelistVal;
					comp.filterItems.clear();
					comp.filterItems.addAll(filtersList);
					while (comp.filterItems.size() < 12) {
						comp.filterItems.add(ItemStack.EMPTY);
					}
					comp.filterLimits.clear();
					comp.filterLimits.addAll(limitsList);
					while (comp.filterLimits.size() < 12) {
						comp.filterLimits.add(-1);
					}
					return comp;
				}));
	}

	public ItemTransferComponent(UUID uuid, boolean isInput) {
		super(uuid);
		this.isInput = isInput;
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 1;
		for (int i = 0; i < 12; i++) {
			this.filterItems.add(ItemStack.EMPTY);
			this.filterLimits.add(-1);
		}
	}

	@Override
	public FlowComponentType getType() {
		// Change from FlowComponentType.ITEM_INPUT to VanillaSFMFlowPlugin.ITEM_INPUT
		// [3]
		return isInput ? VanillaSFMFlowPlugin.ITEM_INPUT.get() : VanillaSFMFlowPlugin.ITEM_OUTPUT.get();
	}

	public List<Long> getEnabledSlotsMasks() {
		return this.enabledSlotsMasks;
	}

	public long getEnabledSlotsMask(Direction dir) {
		if (dir == null)
			return -1L;
		int idx = dir.ordinal();
		if (idx >= 0 && idx < enabledSlotsMasks.size()) {
			return enabledSlotsMasks.get(idx);
		}
		return -1L;
	}

	public void setEnabledSlotsMask(Direction dir, long mask) {
		if (dir == null)
			return;
		int idx = dir.ordinal();
		while (enabledSlotsMasks.size() <= idx) {
			enabledSlotsMasks.add(-1L);
		}
		enabledSlotsMasks.set(idx, mask);
	}

	public boolean isSlotEnabled(Direction dir, int slot) {
		if (dir == null)
			return true;
		if (slot < 0 || slot >= 64)
			return true;
		long mask = getEnabledSlotsMask(dir);
		return (mask & (1L << slot)) != 0;
	}

	public void toggleSlot(Direction dir, int slot) {
		if (dir == null)
			return;
		if (slot < 0 || slot >= 64)
			return;
		long mask = getEnabledSlotsMask(dir);
		mask ^= (1L << slot);
		setEnabledSlotsMask(dir, mask);
	}

	public boolean isInput() {
		return isInput;
	}

	public int getInventoryId() {
		return inventoryId;
	}

	public void setInventoryId(int inventoryId) {
		this.inventoryId = inventoryId;
	}

	public boolean isUseAll() {
		return useAll;
	}

	public void setUseAll(boolean useAll) {
		this.useAll = useAll;
	}

	public int getTargetSlot() {
		return targetSlot;
	}

	public void setTargetSlot(int targetSlot) {
		this.targetSlot = targetSlot;
	}

	public boolean isSideActive(Direction dir) {
		return (activeSidesMask & (1 << dir.ordinal())) != 0;
	}

	public void toggleSide(Direction dir) {
		activeSidesMask ^= (1 << dir.ordinal());
	}

	public void setSideActive(Direction dir, boolean active) {
		if (active) {
			activeSidesMask |= (1 << dir.ordinal());
		} else {
			activeSidesMask &= ~(1 << dir.ordinal());
		}
	}

	public int getActiveSidesMask() {
		return activeSidesMask;
	}

	public void setActiveSidesMask(int mask) {
		this.activeSidesMask = mask;
	}

	public @Nullable UUID getBoundGroupVariableId() {
		return boundGroupVariableId;
	}

	public void setBoundGroupVariableId(@Nullable UUID id) {
		this.boundGroupVariableId = id;
	}

	public @Nullable UUID getBoundFilterVariableId() {
		return boundFilterVariableId;
	}

	public void setBoundFilterVariableId(@Nullable UUID id) {
		this.boundFilterVariableId = id;
	}

	public boolean isWhitelist() {
		return whitelist;
	}

	public void setWhitelist(boolean whitelist) {
		this.whitelist = whitelist;
	}

	public List<ItemStack> getFilterItems() {
		return filterItems;
	}

	@Override
	public List<Integer> getFilterLimits() {
		return filterLimits;
	}

	public int getFilterLimit(ItemStack stack) {
		for (int i = 0; i < filterItems.size(); i++) {
			ItemStack filter = filterItems.get(i);
			if (filter != null && !filter.isEmpty() && ItemStack.isSameItemSameComponents(stack, filter)) {
				return i < filterLimits.size() ? filterLimits.get(i) : -1;
			}
		}
		return -1;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		if (this.isInput()) {
			/* STREAMING_CHUNK:Extruding items virtually into output wire */
			FlowItemBuffer myOutputBuffer = new FlowItemBuffer();

			// 1. Carry over any incoming items passed from upstream nodes first [3]
			FlowItemBuffer myInputBuffer = context.getComponentBuffer(this.getId());
			if (!myInputBuffer.isEmpty()) {
				for (FlowItemBuffer.BufferedItem item : myInputBuffer.getItems()) {
					myOutputBuffer.add(item.srcPos(), item.srcSlot(), item.srcSide(), item.stack().copy());
				}
			}

			// 2. Extract our own configured inventory items into the combined buffer [3]
			extractItemsIntoBuffer(context, myOutputBuffer);

			// 3. Always propagate control flow downstream, even if the buffer is empty [3]
			for (FlowComponentConnections conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(this.getId())) {
					UUID targetId = conn.getTargetComponentId();

					if (!myOutputBuffer.isEmpty()) {
						FlowItemBuffer targetInputBuffer = context.getComponentBuffer(targetId);
						for (FlowItemBuffer.BufferedItem item : myOutputBuffer.getItems()) {
							targetInputBuffer.add(item.srcPos(), item.srcSlot(), item.srcSide(), item.stack().copy());
						}
					}
					// Always enqueue the next node to keep the flowchart executing [3]
					context.enqueue(targetId);
				}
			}
		} else {
			/* STREAMING_CHUNK:Siphoning items virtually from input wire */
			FlowItemBuffer myInputBuffer = context.getComponentBuffer(this.getId());
			FlowItemBuffer myOutputBuffer = new FlowItemBuffer();

			// 1. Only attempt depositions if we actually have incoming items to handle [3]
			if (!myInputBuffer.isEmpty()) {
				depositItemsFromBuffer(context, myInputBuffer, myOutputBuffer);
			}

			// 2. Always propagate any remaining leftovers downstream [3]
			for (FlowComponentConnections conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(this.getId())) {
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

	private void extractItemsIntoBuffer(FlowchartPlanningContext context, FlowItemBuffer buffer) {
		var inventories = context.getConnectedInventories();
		BlockPos srcInventoryPos = null;

		for (var block : inventories) {
			if (block.getId() == this.getInventoryId() && !block.isSleeping()) {
				srcInventoryPos = block.getBlockPos();
				break;
			}
		}

		if (srcInventoryPos == null) {
			return;
		}

		java.util.List<Direction> activeSrcSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (this.isSideActive(dir)) {
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

				if (this.getTargetSlot() != -1 && this.getTargetSlot() != srcSlot) {
					continue;
				}

				int mainSrcSlot = srcEntry.mainSlotIndex();
				if (!this.isSlotEnabled(srcSide, mainSrcSlot)) {
					continue;
				}

				if (!matchesFilter(this, srcStack)) {
					continue;
				}

				int totalInSource = 0;
				for (var entry : srcInv.slots().values()) {
					if (ItemStack.isSameItemSameComponents(srcStack, entry.stack())) {
						totalInSource += entry.stack().getCount();
					}
				}

				int srcLimit = this.getFilterLimit(srcStack);
				int srcRemaining = srcStack.getCount();

				if (this.isWhitelist()) {
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
					ItemStack extracted = srcStack.copy();
					extracted.setCount(srcRemaining);
					buffer.add(srcInventoryPos, srcSlot, srcSide, extracted);

					srcStack.shrink(srcRemaining);
					grabbedCounts.put(srcStack.getItem(),
							grabbedCounts.getOrDefault(srcStack.getItem(), 0) + srcRemaining);
				}
			}
		}
	}

	private void depositItemsFromBuffer(FlowchartPlanningContext context, FlowItemBuffer inputBuffer,
			FlowItemBuffer outputBuffer) {
		var inventories = context.getConnectedInventories();
		BlockPos tgtInventoryPos = null;

		for (var block : inventories) {
			if (block.getId() == this.getInventoryId() && !block.isSleeping()) {
				tgtInventoryPos = block.getBlockPos();
				break;
			}
		}

		if (tgtInventoryPos == null) {
			for (FlowItemBuffer.BufferedItem item : inputBuffer.getItems()) {
				outputBuffer.add(item.srcPos(), item.srcSlot(), item.srcSide(), item.stack().copy());
			}
			return;
		}

		List<Direction> activeTgtSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (this.isSideActive(dir)) {
				activeTgtSides.add(dir);
			}
		}
		if (activeTgtSides.isEmpty()) {
			activeTgtSides.add(null);
		}

		for (FlowItemBuffer.BufferedItem incomingItem : inputBuffer.getItems()) {
			ItemStack incoming = incomingItem.stack();
			if (incoming.isEmpty())
				continue;

			if (!matchesFilter(this, incoming)) {
				outputBuffer.add(incomingItem.srcPos(), incomingItem.srcSlot(), incomingItem.srcSide(),
						incoming.copy());
				continue;
			}

			int remainingToDeposit = incoming.getCount();

			for (Direction tgtSide : activeTgtSides) {
				if (remainingToDeposit <= 0)
					break;

				var tgtInv = context.getSnapshot().getInventory(tgtInventoryPos, tgtSide);
				if (tgtInv == null)
					continue;

				List<Integer> sortedTgtSlots = new ArrayList<>(tgtInv.slots().keySet());
				Collections.sort(sortedTgtSlots);

				for (int tgtSlot : sortedTgtSlots) {
					if (remainingToDeposit <= 0)
						break;

					ThreadSafeInventorySnapshot.SlotSnapshot tgtEntry = tgtInv.slots().get(tgtSlot);
					ItemStack tgtStack = tgtEntry.stack();

					if (this.getTargetSlot() != -1 && this.getTargetSlot() != tgtSlot) {
						continue;
					}

					int mainTgtSlot = tgtEntry.mainSlotIndex();
					if (!this.isSlotEnabled(tgtSide, mainTgtSlot)) {
						continue;
					}

					int totalInTarget = 0;
					for (var entry : tgtInv.slots().values()) {
						if (ItemStack.isSameItemSameComponents(incoming, entry.stack())) {
							totalInTarget += entry.stack().getCount();
						}
					}

					int tgtLimit = this.getFilterLimit(incoming);
					int maxToDeposit = incoming.getMaxStackSize();

					if (this.isWhitelist()) {
						if (tgtLimit > 0) {
							maxToDeposit = Math.max(0, tgtLimit - totalInTarget);
							if (maxToDeposit <= 0) {
								continue;
							}
						}
					} else {
						if (tgtLimit > 0) {
							maxToDeposit = Math.max(0, remainingToDeposit - tgtLimit);
							if (maxToDeposit <= 0) {
								continue;
							}
						}
					}

					boolean canInsert = false;
					int maxInsertable = 0;
					int actualLimit = Math.min(tgtEntry.slotLimit(), maxToDeposit);

					if (tgtStack.isEmpty()) {
						canInsert = true;
						maxInsertable = actualLimit;
					} else if (ItemStack.isSameItemSameComponents(incoming, tgtStack)) {
						int remainingSpace = actualLimit - tgtStack.getCount();
						if (remainingSpace > 0) {
							canInsert = true;
							maxInsertable = remainingSpace;
						}
					}

					if (canInsert) {
						int amountToTransfer = Math.min(remainingToDeposit, maxInsertable);
						if (amountToTransfer > 0) {
							boolean success = context.tryWriteTask(incomingItem.srcPos(), incomingItem.srcSlot(),
									incomingItem.srcSide(), tgtInventoryPos, tgtSlot, tgtSide, incoming,
									amountToTransfer);
							if (success) {
								if (tgtStack.isEmpty()) {
									ItemStack newTgt = incoming.copy();
									newTgt.setCount(amountToTransfer);
									tgtInv.slots().put(tgtSlot, new ThreadSafeInventorySnapshot.SlotSnapshot(newTgt,
											tgtEntry.slotLimit(), mainTgtSlot));
								} else {
									tgtStack.grow(amountToTransfer);
								}
								incoming.shrink(amountToTransfer);
								remainingToDeposit -= amountToTransfer;
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

	private boolean matchesFilter(ItemTransferComponent component, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		boolean found = false;
		int limit = -1;
		for (int i = 0; i < component.getFilterItems().size(); i++) {
			ItemStack filter = component.getFilterItems().get(i);
			if (filter != null && !filter.isEmpty() && ItemStack.isSameItemSameComponents(stack, filter)) {
				found = true;
				limit = i < component.getFilterLimits().size() ? component.getFilterLimits().get(i) : -1;
				break;
			}
		}

		if (component.isWhitelist()) {
			return found;
		} else {
			return !found || limit > 0;
		}
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		var codec = isInput ? ItemTransferComponent.INPUT_CODEC : ItemTransferComponent.OUTPUT_CODEC;
		codec.codec().parse(NbtOps.INSTANCE, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse item transfer component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.useAll = decoded.isUseAll();
					this.targetSlot = decoded.getTargetSlot();
					this.activeSidesMask = decoded.getActiveSidesMask();
					this.enabledSlotsMasks.clear();
					this.enabledSlotsMasks.addAll(decoded.getEnabledSlotsMasks());
					this.boundGroupVariableId = decoded.getBoundGroupVariableId();
					this.boundFilterVariableId = decoded.getBoundFilterVariableId();
					this.whitelist = decoded.isWhitelist();
					this.filterItems.clear();
					this.filterItems.addAll(decoded.getFilterItems());
					while (this.filterItems.size() < 12) {
						this.filterItems.add(ItemStack.EMPTY);
					}
					this.filterLimits.clear();
					this.filterLimits.addAll(decoded.getFilterLimits());
					while (this.filterLimits.size() < 12) {
						this.filterLimits.add(-1);
					}
				});
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable(isInput ? "gui.sfmflow.item_input" : "gui.sfmflow.item_output");
	}
}
