package dta.sfmflow.block.entity;

import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.api.component.IFlowchartVariable;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Public API client-server visual capability slot that prevents item placements
 * and extractions. Handles dynamic capability targets using IGhostSlotAware.
 */
public class FilterGhostSlot extends Slot {
	private final ManagerMenu menu;
	private final int filterIndex;

	public FilterGhostSlot(ManagerMenu menu, int filterIndex, int x, int y) {
		super(new SimpleContainer(12), filterIndex, x, y); // Pass filterIndex cleanly
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
			// Symmetrical Guard: Block setting inventory list variable cards into filter ghost slots
			if (isInventoryListCard(stack, menu.getManagerBlockEntity())) {
				return;
			}
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

	/**
	 * Safe helper to identify if a given ItemStack represents an inventory list variable card.
	 */
	public static boolean isInventoryListCard(ItemStack stack, @Nullable ManagerBlockEntity manager) {
		if (stack.isEmpty() || !stack.is(ModItems.VARIABLE_CARD.get())) {
			return false;
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTag();
			if (tag.contains("VariableType")) {
				String type = tag.getString("VariableType");
				if ("sfmflow:item_inventories_list_variable".equals(type)
						|| "sfmflow:fluid_inventories_list_variable".equals(type)
						|| "sfmflow:energy_inventories_list_variable".equals(type)) {
					return true;
				}
			}
			// Symmetrical backward-compatibility fallback
			if (tag.contains("entries")) {
				return true;
			}

			// Canvas mapping verification fallback
			if (manager != null && tag.contains("VariableId")) {
				UUID varId = tag.getUUID("VariableId");
				AbstractFlowComponent comp = manager.getFlowComponents().get(varId);
				if (comp instanceof IFlowchartVariable flowchartVar) {
					return flowchartVar.isInventoryList();
				}
			}
		}
		return false;
	}
}