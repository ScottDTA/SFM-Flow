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
import dta.sfmflow.api.component.ISlotConfigurable;
import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
		return 12;
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
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
			ItemTransferPlanner.planInput(context, this);
		} else {
			ItemTransferPlanner.planOutput(context, this);
		}
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
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