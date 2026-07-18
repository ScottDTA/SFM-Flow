package dta.sfmflow.api.component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Intermediate base class consolidating Whitelist filters, 12-slot ghost grids, and slot bitmasks.
 */
public abstract class AbstractFilterableConditionalComponent extends AbstractConditionalComponent
		implements IFilterable, IGhostSlotAware, ISlotConfigurable {

	protected final List<Long> enabledSlotsMasks = new ArrayList<>(List.of(-1L, -1L, -1L, -1L, -1L, -1L));
	protected UUID boundGroupVariableId = null;
	protected UUID boundFilterVariableId = null;
	protected boolean whitelist = true;
	protected final List<ItemStack> filterItems = new ArrayList<>();
	protected final List<Integer> filterLimits = new ArrayList<>();

	protected AbstractFilterableConditionalComponent(UUID uuid) {
		super(uuid);
		for (int i = 0; i < 12; i++) {
			this.filterItems.add(ItemStack.EMPTY);
			this.filterLimits.add(-1);
		}
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
}