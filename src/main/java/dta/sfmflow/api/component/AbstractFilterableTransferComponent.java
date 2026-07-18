package dta.sfmflow.api.component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Intermediate base class consolidating Whitelist filters, 12-slot ghost grids, and slot bitmasks for transfers.
 */
public abstract class AbstractFilterableTransferComponent extends AbstractTransferComponent
		implements IFilterable, IGhostSlotAware, ISlotConfigurable {

	protected boolean useAll = true;
	protected int targetSlot = -1;
	protected final List<Long> enabledSlotsMasks = new ArrayList<>(List.of(-1L, -1L, -1L, -1L, -1L, -1L));
	protected UUID boundGroupVariableId = null;
	protected UUID boundFilterVariableId = null;
	protected boolean whitelist = true;
	protected final List<ItemStack> filterItems = new ArrayList<>();
	protected final List<Integer> filterLimits = new ArrayList<>();

	protected AbstractFilterableTransferComponent(UUID uuid, boolean isInput) {
		super(uuid, isInput);
		for (int i = 0; i < 12; i++) {
			this.filterItems.add(ItemStack.EMPTY);
			this.filterLimits.add(-1);
		}
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

	public List<Long> getEnabledSlotsMasks() {
		return this.enabledSlotsMasks;
	}

	public long getEnabledSlotsMask(Direction dir) {
		if (dir == null) return -1L;
		int idx = dir.ordinal();
		if (idx >= 0 && idx < enabledSlotsMasks.size()) {
			return enabledSlotsMasks.get(idx);
		}
		return -1L;
	}

	public void setEnabledSlotsMask(Direction dir, long mask) {
		if (dir == null) return;
		int idx = dir.ordinal();
		while (enabledSlotsMasks.size() <= idx) {
			enabledSlotsMasks.add(-1L);
		}
		enabledSlotsMasks.set(idx, mask);
	}

	@Override
	public boolean isSlotEnabled(Direction dir, int slot) {
		if (dir == null) return true;
		if (slot < 0 || slot >= 64) return true;
		long mask = getEnabledSlotsMask(dir);
		return (mask & (1L << slot)) != 0;
	}

	@Override
	public void toggleSlot(Direction dir, int slot) {
		if (dir == null) return;
		if (slot < 0 || slot >= 64) return;
		long mask = getEnabledSlotsMask(dir);
		mask ^= (1L << slot);
		setEnabledSlotsMask(dir, mask);
	}

	public UUID getBoundGroupVariableId() {
		return boundGroupVariableId;
	}

	public void setBoundGroupVariableId(UUID id) {
		this.boundGroupVariableId = id;
	}

	public UUID getBoundFilterVariableId() {
		return boundFilterVariableId;
	}

	public void setBoundFilterVariableId(UUID id) {
		this.boundFilterVariableId = id;
	}

	@Override
	public boolean isWhitelist() {
		return whitelist;
	}

	@Override
	public void setWhitelist(boolean whitelist) {
		this.whitelist = whitelist;
	}

	@Override
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
		compoundTag.putBoolean("useAll", this.useAll);
		compoundTag.putInt("targetSlot", this.targetSlot);

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
	public void loadData(CompoundTag compoundTag) {
		super.loadData(compoundTag);
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

		if (compoundTag.contains("useAll")) {
			this.useAll = compoundTag.getBoolean("useAll");
		}
		if (compoundTag.contains("targetSlot")) {
			this.targetSlot = compoundTag.getInt("targetSlot");
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
}