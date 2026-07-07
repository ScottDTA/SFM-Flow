package dta.sfmflow.api.component;

import dta.sfmflow.util.Color;
import net.minecraft.world.item.ItemStack;

/**
 * Public API interface representing a flowchart node that acts as a draggable,
 * slot-bindable variable card [3].
 */
public interface IFlowchartVariable {
	/**
	 * Resolves the underlying variable as a serialized ItemStack representation
	 * (VariableCardItem) to be picked up by the cursor [3].
	 */
	ItemStack toItemStack();

	/**
	 * Gets the custom label tint color of the variable card [3].
	 */
	Color getFilterColor();

	/**
	 * Checks if the variable's filter is empty or unconfigured [3].
	 */
	boolean isFilterEmpty();

	/**
	 * Safely retrieves a localized search-matching string representing the filtered content
	 * (e.g., item hover name or fluid hover name) [3].
	 */
	String getFilteredContentName();
}