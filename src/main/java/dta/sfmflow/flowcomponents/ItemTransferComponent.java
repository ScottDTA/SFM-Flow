package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IFilterable;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Unified logic component handling both item inputs (extractions) and item
 * outputs (depositions) [3]. Upgraded to serialize optional group, filter
 * variables, and a bitmask representing active directions [3]. Additionally
 * supports saving per-side enabled slot bitmasks [3].
 */
public class ItemTransferComponent extends AbstractFlowComponent
		implements IFilterable, IInventoryTarget, ISideConfigurable {
	private final boolean isInput;
	private int inventoryId = -1;
	private boolean useAll = true;
	private int targetSlot = -1;

	// Bitmask representing active directions (all 6 active by default: 111111
	// binary = 63) [3]
	private int activeSidesMask = 63;

	// Slot bitmasks tracking enabled states per face direction (Default: -1L
	// meaning all slots enabled) [3]
	private final List<Long> enabledSlotsMasks = new java.util.ArrayList<>(
			java.util.List.of(-1L, -1L, -1L, -1L, -1L, -1L));

	private UUID boundGroupVariableId = null;
	private UUID boundFilterVariableId = null;

	// Symmetrical Filter Variables [3]
	private boolean whitelist = true;
	private final List<ItemStack> filterItems = new java.util.ArrayList<>();

	public static final MapCodec<ItemTransferComponent> INPUT_CODEC = makeCodec(true);
	public static final MapCodec<ItemTransferComponent> OUTPUT_CODEC = makeCodec(false);

	private static MapCodec<ItemTransferComponent> makeCodec(boolean isInput) {
		return RecordCodecBuilder
				.mapCodec(instance -> instance
						.group(BaseProperties.CODEC.fieldOf("base").forGetter(ItemTransferComponent::getBaseProperties),
								Codec.INT
										.optionalFieldOf("inventoryId", -1)
										.forGetter(ItemTransferComponent::getInventoryId),
								Codec.BOOL.optionalFieldOf("useAll", true).forGetter(ItemTransferComponent::isUseAll),
								Codec.INT.optionalFieldOf("targetSlot", -1)
										.forGetter(ItemTransferComponent::getTargetSlot),
								Codec.INT.optionalFieldOf("activeSidesMask", 63)
										.forGetter(ItemTransferComponent::getActiveSidesMask),
								Codec.LONG.listOf()
										.optionalFieldOf("enabledSlotsMasks",
												java.util.List.of(-1L, -1L, -1L, -1L, -1L, -1L))
										.forGetter(ItemTransferComponent::getEnabledSlotsMasks),
								UUIDUtil.CODEC.optionalFieldOf("boundGroupVariableId")
										.forGetter(comp -> Optional.ofNullable(comp.getBoundGroupVariableId())),
								UUIDUtil.CODEC
										.optionalFieldOf("boundFilterVariableId")
										.forGetter(comp -> Optional.ofNullable(comp.getBoundFilterVariableId())),
								Codec.BOOL.optionalFieldOf("whitelist", true)
										.forGetter(ItemTransferComponent::isWhitelist),
								ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("filterItems", List.of())
										.forGetter(ItemTransferComponent::getFilterItems))
						.apply(instance, (baseProps, invId, useAllVal, slot, sidesMask, masksList, groupVar, filterVar,
								whitelistVal, filtersList) -> {
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
		}
	}

	@Override
	public FlowComponentType getType() {
		return isInput ? FlowComponentType.ITEM_INPUT.get() : FlowComponentType.ITEM_OUTPUT.get();
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
	public void loadData(net.minecraft.nbt.CompoundTag compoundTag) {
		var codec = isInput ? ItemTransferComponent.INPUT_CODEC : ItemTransferComponent.OUTPUT_CODEC;
		codec.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag).resultOrPartial(
				err -> dta.sfmflow.SFMFlow.LOGGER.error("Failed to parse item transfer component data: {}", err))
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
				});
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable(isInput ? "gui.sfmflow.item_input" : "gui.sfmflow.item_output");
	}
	
	@Override
	public void plan(dta.sfmflow.api.execution.FlowchartPlanningContext context) {
		if (this.isInput()) {
			for (dta.sfmflow.flowcomponents.FlowComponentConnections conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(this.getId())) {
					AbstractFlowComponent targetComponent = context.getComponents().get(conn.getTargetComponentId());
					if (targetComponent instanceof ItemTransferComponent targetOutput && !targetOutput.isInput()) {
						planItemTransfer(context, this, targetOutput);
						context.enqueue(targetOutput.getId());
					}
				}
			}
		}
	}

	private void planItemTransfer(dta.sfmflow.api.execution.FlowchartPlanningContext context, ItemTransferComponent source, ItemTransferComponent target) {
		var inventories = context.getConnectedInventories();
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

		java.util.List<Direction> activeSrcSides = new java.util.ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (source.isSideActive(dir)) {
				activeSrcSides.add(dir);
			}
		}
		if (activeSrcSides.isEmpty()) {
			activeSrcSides.add(null);
		}

		java.util.List<Direction> activeTgtSides = new java.util.ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (target.isSideActive(dir)) {
				activeTgtSides.add(dir);
			}
		}
		if (activeTgtSides.isEmpty()) {
			activeTgtSides.add(null);
		}

		for (Direction srcSide : activeSrcSides) {
			var srcInv = context.getSnapshot().getInventory(srcInventoryPos, srcSide);
			if (srcInv == null)
				continue;

			for (Direction tgtSide : activeTgtSides) {
				var tgtInv = context.getSnapshot().getInventory(tgtInventoryPos, tgtSide);
				if (tgtInv == null)
					continue;

				java.util.List<Integer> sortedSrcSlots = new java.util.ArrayList<>(srcInv.slots().keySet());
				java.util.Collections.sort(sortedSrcSlots);

				java.util.List<Integer> sortedTgtSlots = new java.util.ArrayList<>(tgtInv.slots().keySet());
				java.util.Collections.sort(sortedTgtSlots);

				for (int srcSlot : sortedSrcSlots) {
					dta.sfmflow.api.execution.ThreadSafeInventorySnapshot.SlotSnapshot srcEntry = srcInv.slots().get(srcSlot);
					ItemStack srcStack = srcEntry.stack();
					if (srcStack.isEmpty()) {
						continue;
					}

					if (source.getTargetSlot() != -1 && source.getTargetSlot() != srcSlot) {
						continue;
					}

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

						dta.sfmflow.api.execution.ThreadSafeInventorySnapshot.SlotSnapshot tgtEntry = tgtInv.slots().get(tgtSlot);
						ItemStack tgtStack = tgtEntry.stack();

						if (target.getTargetSlot() != -1 && target.getTargetSlot() != tgtSlot) {
							continue;
						}

						int mainTgtSlot = tgtEntry.mainSlotIndex();
						if (!target.isSlotEnabled(tgtSide, mainTgtSlot)) {
							continue;
						}

						if (!matchesFilter(target, srcStack)) {
							continue;
						}

						boolean canInsert = false;
						int maxInsertable = 0;

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
								boolean success = context.tryWriteTask(srcInventoryPos, srcSlot,
										srcSide, tgtInventoryPos, tgtSlot, tgtSide, srcStack, amountToTransfer);
								if (success) {
									if (tgtStack.isEmpty()) {
										ItemStack newTgt = srcStack.copy();
										newTgt.setCount(amountToTransfer);
										tgtInv.slots().put(tgtSlot, new dta.sfmflow.api.execution.ThreadSafeInventorySnapshot.SlotSnapshot(newTgt,
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
	
}