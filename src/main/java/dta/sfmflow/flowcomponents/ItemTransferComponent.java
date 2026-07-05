package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IFilterable;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.component.ISlotConfigurable;
import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.api.execution.FlowItemBuffer;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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
		implements IFilterable, IInventoryTarget, ISideConfigurable, IGhostSlotAware, ISlotConfigurable {
	private final boolean isInput;
	private int inventoryId = -1;
	private boolean useAll = true;
	private int targetSlot = -1;

	private int activeSidesMask = 0;

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
						Codec.INT.optionalFieldOf("activeSidesMask", 0)
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

	/**
	 * Retrieves the maximum count limit assigned to the specified item stack [3].
	 * Uses side-safe item equality checks to bypass dynamic component mismatches
	 * [3].
	 *
	 * @param stack the item stack to check [3]
	 * @return the stack limit, or -1 if unlimited [3]
	 */
	public int getFilterLimit(ItemStack stack) {
		for (int i = 0; i < filterItems.size(); i++) {
			ItemStack filter = filterItems.get(i);
			if (filter != null && !filter.isEmpty() && ItemStack.isSameItem(stack, filter)) {
				return i < filterLimits.size() ? filterLimits.get(i) : -1;
			}
		}
		return -1;
	}

	@Override
	public ItemStack getGhostStack(int index) {
		if (index >= 0 && index < this.filterItems.size()) {
			return this.filterItems.get(index);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setGhostStack(int index, ItemStack stack) {
		if (index >= 0 && index < this.filterItems.size()) {
			this.filterItems.set(index, stack == null ? ItemStack.EMPTY : stack);
		}
	}

	@Override
	public int getGhostSlotCount() {
		return 12; // Item transfer component has exactly 12 ghost slots
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		// Direct NBT saving fallback as an exploit and desync shield [3]
		compoundTag.putInt("inventoryId", this.inventoryId);
		compoundTag.putBoolean("useAll", this.useAll);
		compoundTag.putInt("targetSlot", this.targetSlot);
		compoundTag.putInt("activeSidesMask", this.activeSidesMask);

		ListTag enabledMasksList = new ListTag();
		for (long mask : this.enabledSlotsMasks) {
			enabledMasksList.add(LongTag.valueOf(mask));
		}
		compoundTag.put("enabledSlotsMasks", enabledMasksList);

		if (this.boundGroupVariableId != null) {
			compoundTag.putUUID("boundGroupVariableId", this.boundGroupVariableId);
		}
		if (this.boundFilterVariableId != null) {
			compoundTag.putUUID("boundFilterVariableId", this.boundFilterVariableId);
		}
		compoundTag.putBoolean("whitelist", this.whitelist);

		var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		ListTag itemsList = new ListTag();
		for (ItemStack stack : this.filterItems) {
			if (stack != null && !stack.isEmpty()) {
				itemsList.add(stack.save(registries));
			} else {
				itemsList.add(new CompoundTag());
			}
		}
		compoundTag.put("filterItems", itemsList);

		ListTag limitsList = new ListTag();
		for (int limit : this.filterLimits) {
			limitsList.add(IntTag.valueOf(limit));
		}
		compoundTag.put("filterLimits", limitsList);

		return compoundTag;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		if (this.isInput()) {
			/* STREAMING_CHUNK:Extruding items virtually into output wire */
			FlowItemBuffer myOutputBuffer = new FlowItemBuffer();

			// Carry over any incoming items passed from upstream input nodes
			FlowItemBuffer myInputBuffer = context.getComponentBuffer(this.getId());
			if (!myInputBuffer.isEmpty()) {
				for (FlowItemBuffer.BufferedItem item : myInputBuffer.getItems()) {
					myOutputBuffer.add(item.srcPos(), item.srcSlot(), item.srcSide(), item.stack().copy());
				}
			}

			// Extract our own configured inventory items into the combined buffer
			extractItemsIntoBuffer(context, myOutputBuffer);

			// Copy extracted buffer contents onto connected target input buffers
			// sequentially
			if (!myOutputBuffer.isEmpty()) {
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
		} else {
			/* STREAMING_CHUNK:Siphoning items virtually from input wire */
			FlowItemBuffer myInputBuffer = context.getComponentBuffer(this.getId());

			if (!myInputBuffer.isEmpty()) {
				FlowItemBuffer myOutputBuffer = new FlowItemBuffer();
				depositItemsFromBuffer(context, myInputBuffer, myOutputBuffer);

				// Propagate remaining un-deposited leftovers downstream along the connection
				// lines
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
	}

	private void updateSnapshotCopies(FlowchartPlanningContext context, BlockPos pos, int slot,
			ItemStack updatedStack) {
		for (Direction dir : Direction.values()) {
			var inv = context.getSnapshot().getInventory(pos, dir);
			if (inv != null) {
				var slotSnap = inv.slots().get(slot);
				if (slotSnap != null) {
					inv.slots().put(slot, new ThreadSafeInventorySnapshot.SlotSnapshot(updatedStack.copy(),
							slotSnap.slotLimit(), slotSnap.mainSlotIndex()));
				}
			}
		}
		var nullInv = context.getSnapshot().getInventory(pos, null);
		if (nullInv != null) {
			var slotSnap = nullInv.slots().get(slot);
			if (slotSnap != null) {
				nullInv.slots().put(slot, new ThreadSafeInventorySnapshot.SlotSnapshot(updatedStack.copy(),
						slotSnap.slotLimit(), slotSnap.mainSlotIndex()));
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

		List<Direction> activeSrcSides = new ArrayList<>();
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

				// Fixed: matchesFilter updated to use context [3]
				if (!matchesFilter(context, this, srcStack)) {
					continue;
				}

				int totalInSource = 0;
				for (var entry : srcInv.slots().values()) {
					// Symmetrical Fix: Changed from isSameItemSameComponents to isSameItem to
					// prevent component count desyncs [3]
					if (ItemStack.isSameItem(srcStack, entry.stack())) {
						totalInSource += entry.stack().getCount();
					}
				}

				// Fixed: getFilterLimit updated to use context [3]
				int srcLimit = this.getFilterLimit(context, this, srcStack);
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
					Item srcItem = srcStack.getItem();
					ItemStack extracted = srcStack.copy();
					extracted.setCount(srcRemaining);

					if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
						SFMFlow.LOGGER.info("[SFM-Flow] Extracting Slot {}: Side={}, Count={}, AlreadyGrabbed={}",
								srcSlot, srcSide, srcRemaining, grabbedCounts.getOrDefault(srcItem, 0));
					}

					buffer.add(srcInventoryPos, srcSlot, srcSide, extracted);

					srcStack.shrink(srcRemaining);
					updateSnapshotCopies(context, srcInventoryPos, srcSlot, srcStack);
					grabbedCounts.put(srcItem, grabbedCounts.getOrDefault(srcItem, 0) + srcRemaining);
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

		Map<Item, Integer> alreadyHeldBack = new HashMap<>();

		for (FlowItemBuffer.BufferedItem incomingItem : inputBuffer.getItems()) {
			ItemStack incoming = incomingItem.stack();
			if (incoming.isEmpty())
				continue;

			// Fixed: matchesFilter updated to use context [3]
			if (!matchesFilter(context, this, incoming)) {
				outputBuffer.add(incomingItem.srcPos(), incomingItem.srcSlot(), incomingItem.srcSide(),
						incoming.copy());
				continue;
			}

			int remainingToDeposit = incoming.getCount();
			int tgtLimit = this.getFilterLimit(context, this, incoming);

			// Calculate the max allowable deposit amount once for this entire incoming
			// stack [3]
			int allowedDeposit = remainingToDeposit;
			if (!this.isWhitelist() && tgtLimit > 0) {
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
						// Symmetrical Fix: use isSameItem [3]
						if (ItemStack.isSameItem(incoming, entry.stack())) {
							totalInTarget += entry.stack().getCount();
						}
					}

					int maxToDeposit = incoming.getMaxStackSize();

					if (this.isWhitelist()) {
						if (tgtLimit > 0) {
							maxToDeposit = Math.max(0, tgtLimit - totalInTarget);
							if (maxToDeposit <= 0) {
								continue;
							}
						}
					} else {
						// Symmetrically use the pre-calculated allowedDeposit count for Blacklist [3]
						maxToDeposit = allowedDeposit;
						if (maxToDeposit <= 0) {
							continue;
						}
					}

					boolean canInsert = false;
					int maxInsertable = 0;
					int actualLimit = Math.min(tgtEntry.slotLimit(), maxToDeposit);

					if (tgtStack.isEmpty()) {
						canInsert = true;
						maxInsertable = actualLimit;
					} else if (ItemStack.isSameItem(incoming, tgtStack)) { // Symmetrical Fix: use isSameItem [3]
						int remainingSpace = actualLimit - tgtStack.getCount();
						if (remainingSpace > 0) {
							canInsert = true;
							maxInsertable = remainingSpace;
						}
					}

					if (canInsert) {
						int amountToTransfer = Math.min(remainingToDeposit, maxInsertable);
						if (amountToTransfer > 0) {
							if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
								SFMFlow.LOGGER.info(
										"[SFM-Flow] Depositing Slot {}: Side={}, Amount={}, RemainingToDeposit={}",
										tgtSlot, tgtSide, amountToTransfer, remainingToDeposit);
							}

							boolean success = context.tryWriteTask(incomingItem.srcPos(), incomingItem.srcSlot(),
									incomingItem.srcSide(), tgtInventoryPos, tgtSlot, tgtSide, incoming,
									amountToTransfer);
							if (success) {
								if (tgtStack.isEmpty()) {
									ItemStack newTgt = incoming.copy();
									newTgt.setCount(amountToTransfer);
									tgtInv.slots().put(tgtSlot, new ThreadSafeInventorySnapshot.SlotSnapshot(newTgt,
											tgtEntry.slotLimit(), mainTgtSlot));
									updateSnapshotCopies(context, tgtInventoryPos, tgtSlot, newTgt);
								} else {
									tgtStack.grow(amountToTransfer);
									updateSnapshotCopies(context, tgtInventoryPos, tgtSlot, tgtStack);
								}
								incoming.shrink(amountToTransfer);
								remainingToDeposit -= amountToTransfer;
								if (!this.isWhitelist()) {
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

	private boolean matchesFilter(FlowchartPlanningContext context, ItemTransferComponent component, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		ItemStack filterItem = null;
		int limit = -1;
		boolean found = false;

		// 1. Resolve bound variable components placed directly on the flowchart canvas
		if (component.getBoundFilterVariableId() != null) {
			AbstractFlowComponent boundComp = context.getComponents().get(component.getBoundFilterVariableId());
			if (boundComp instanceof AdvancedItemFilterVariableComponent varComp) {
				// Symmetrical check utilizing our robust dynamic namespaces/tag matcher [3]
				if (AdvancedItemFilterVariableComponent.matchesVariableFilter(varComp, stack)) {
					found = true;
					limit = varComp.isUseQuantity() ? varComp.getQuantity() : -1;
				}
			}
		} else {
			// 2. Scan each of the 12 ghost slots, checking for virtual variable cards
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
							// Symmetrical check utilizing our robust dynamic namespaces/tag matcher [3]
							if (AdvancedItemFilterVariableComponent.matchesVariableFilter(advancedVar, stack)) {
								found = true;
								limit = advancedVar.isUseQuantity() ? advancedVar.getQuantity() : -1;
								break;
							}
						} else {
							if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
								SFMFlow.LOGGER.warn(
										"[SFM-Flow] matchesFilter: Variable component NOT found in context for ID: {}",
										varId);
							}
						}
					}
				} else if (ItemStack.isSameItem(stack, filter)) {
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

	private int getFilterLimit(FlowchartPlanningContext context, ItemTransferComponent component, ItemStack stack) {
		if (component.getBoundFilterVariableId() != null) {
			AbstractFlowComponent boundComp = context.getComponents().get(component.getBoundFilterVariableId());
			if (boundComp instanceof AdvancedItemFilterVariableComponent varComp) {
				// Symmetrical check utilizing our robust dynamic namespaces/tag matcher [3]
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
						// Symmetrical check utilizing our robust dynamic namespaces/tag matcher [3]
						if (AdvancedItemFilterVariableComponent.matchesVariableFilter(advancedVar, stack)) {
							int resolvedLimit = advancedVar.isUseQuantity() ? advancedVar.getQuantity() : -1;
							if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
								SFMFlow.LOGGER.info(
										"[SFM-Flow] getFilterLimit Resolved Variable Card: ID={}, Item={}, UseQty={}, Limit={}",
										varId, stack.getItem().toString(), advancedVar.isUseQuantity(), resolvedLimit);
							}
							return resolvedLimit;
						}
					} else {
						if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
							SFMFlow.LOGGER.warn(
									"[SFM-Flow] getFilterLimit: Variable component NOT found in context for ID: {}",
									varId);
						}
					}
				}
			} else if (ItemStack.isSameItem(stack, filter)) {
				return component.getFilterLimit(stack);
			}
		}

		return -1;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		// Secure setup: obtain the static composite HolderLookup.Provider cleanly [3]
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		var codec = isInput ? ItemTransferComponent.INPUT_CODEC : ItemTransferComponent.OUTPUT_CODEC;
		codec.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse item transfer component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.useAll = decoded.isUseAll();
					this.targetSlot = decoded.getTargetSlot();
					this.activeSidesMask = decoded.activeSidesMask;
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

		// Direct NBT loading fallback as an exploit and desync shield [3]
		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("useAll")) {
			this.useAll = compoundTag.getBoolean("useAll");
		}
		if (compoundTag.contains("targetSlot")) {
			this.targetSlot = compoundTag.getInt("targetSlot");
		}
		if (compoundTag.contains("activeSidesMask")) {
			this.activeSidesMask = compoundTag.getInt("activeSidesMask");
		}
		if (compoundTag.contains("enabledSlotsMasks")) {
			ListTag list = compoundTag.getList("enabledSlotsMasks", Tag.TAG_LONG);
			this.enabledSlotsMasks.clear();
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof NumericTag numericTag) {
					this.enabledSlotsMasks.add(numericTag.getAsLong());
				} else {
					this.enabledSlotsMasks.add(-1L);
				}
			}
		}
		if (compoundTag.contains("boundGroupVariableId")) {
			this.boundGroupVariableId = compoundTag.getUUID("boundGroupVariableId");
		} else {
			this.boundGroupVariableId = null;
		}
		if (compoundTag.contains("boundFilterVariableId")) {
			this.boundFilterVariableId = compoundTag.getUUID("boundFilterVariableId");
		} else {
			this.boundFilterVariableId = null;
		}
		if (compoundTag.contains("whitelist")) {
			this.whitelist = compoundTag.getBoolean("whitelist");
		}
		if (compoundTag.contains("filterItems")) {
			ListTag list = compoundTag.getList("filterItems", Tag.TAG_COMPOUND);
			this.filterItems.clear();
			for (int i = 0; i < list.size(); i++) {
				CompoundTag itemTag = list.getCompound(i);
				if (itemTag.isEmpty() || !itemTag.contains("id")) {
					this.filterItems.add(ItemStack.EMPTY);
				} else {
					this.filterItems.add(ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY));
				}
			}
			while (this.filterItems.size() < 12) {
				this.filterItems.add(ItemStack.EMPTY);
			}
		}
		if (compoundTag.contains("filterLimits")) {
			ListTag list = compoundTag.getList("filterLimits", Tag.TAG_INT);
			this.filterLimits.clear();
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof NumericTag numericTag) {
					this.filterLimits.add(numericTag.getAsInt());
				} else {
					this.filterLimits.add(-1);
				}
			}
			while (this.filterLimits.size() < 12) {
				this.filterLimits.add(-1);
			}
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable(isInput ? "gui.sfmflow.item_input" : "gui.sfmflow.item_output");
	}
}