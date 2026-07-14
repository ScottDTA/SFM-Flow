package dta.sfmflow.api.component;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * Common public API interface representing any component that supports
 * Whitelist/Blacklist item filtering.
 */
public interface IFilterable {
	/**
	 * Checks if the filter is operating in whitelist mode.
	 *
	 * @return true if whitelist, false if blacklist
	 */
	boolean isWhitelist();

	/**
	 * Sets the filter operating mode.
	 *
	 * @param whitelist true for whitelist, false for blacklist
	 */
	void setWhitelist(boolean whitelist);

	/**
	 * Retrieves the list of filtered item stacks.
	 *
	 * @return the list of ItemStack instances
	 */
	List<ItemStack> getFilterItems();

	/**
	 * Retrieves the mutable list of quantity limits corresponding to each filter item slot.
	 * -1 represents unlimited, and positive integers represent specific limits.
	 *
	 * @return list of integer limits 
	 */
	List<Integer> getFilterLimits();
	
	/**
	 * Dictates whether the filter ghost slots should render their contents as fluid textures.
	 *
	 * @return true if the slots represent fluids, false otherwise
	 */
	default boolean renderAsFluid() {
		return false;
	}
}