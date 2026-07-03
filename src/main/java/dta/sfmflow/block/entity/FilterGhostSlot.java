package dta.sfmflow.block.entity;

import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Public API client-server visual capability slot that prevents item placements
 * and extractions [3]. Handles dynamic capability targets using IGhostSlotAware
 * [3].
 */
public class FilterGhostSlot extends Slot {
	private final ManagerMenu menu;
	private final int filterIndex;

	public FilterGhostSlot(ManagerMenu menu, int filterIndex, int x, int y) {
		super(new SimpleContainer(12), filterIndex, x, y); // Pass filterIndex cleanly [3]
		this.menu = menu;
		this.filterIndex = filterIndex;
	}

	@Override
	public ItemStack getItem() {
		AbstractFlowComponent comp = menu.getActiveComponent();
		if (comp instanceof IGhostSlotAware aware && filterIndex < aware.getGhostSlotCount()) {
			return aware.getGhostStack(filterIndex);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void set(ItemStack stack) {
		AbstractFlowComponent comp = menu.getActiveComponent();
		if (comp instanceof IGhostSlotAware aware && filterIndex < aware.getGhostSlotCount()) {
			aware.setGhostStack(filterIndex, stack);
			menu.getManagerBlockEntity().setChanged();
		}
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public boolean mayPickup(Player playerIn) {
		return false;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}
}
