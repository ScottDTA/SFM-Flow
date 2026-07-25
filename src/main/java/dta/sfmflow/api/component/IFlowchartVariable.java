package dta.sfmflow.api.component;

import dta.sfmflow.util.Color;
import net.minecraft.world.item.ItemStack;

/**
 * Public API interface representing a flowchart node that acts as a draggable,
 * slot-bindable variable card.
 */
public interface IFlowchartVariable {
	/**
	 * Resolves the underlying variable as a serialized ItemStack representation
	 * (VariableCardItem) to be picked up by the cursor.
	 */
	ItemStack toItemStack();

	/**
	 * Gets the custom label tint color of the variable card.
	 */
	Color getFilterColor();

	/**
	 * Checks if the variable's filter is empty or unconfigured.
	 */
	boolean isFilterEmpty();

	/**
	 * Safely retrieves a localized search-matching string representing the filtered content
	 * (e.g., item hover name or fluid hover name).
	 */
	String getFilteredContentName();

	/**
	 * Dictates whether this variable represents a list of target inventories/blocks
	 * and should be displayed in the Inventories Drawer instead of the generic variables drawer.
	 */
	default boolean isInventoryList() {
		return false;
	}

	/**
	 * Dictates whether the variable item stack should display the enchantment glint effect.
	 */
	default boolean hasGlint() {
		return false;
	}
}