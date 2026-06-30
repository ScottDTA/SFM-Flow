package dta.sfmflow.api.component;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * Common public API interface representing any component that supports
 * Whitelist/Blacklist item filtering [3].
 */
public interface IFilterable {
	/**
	 * Checks if the filter is operating in whitelist mode [3].
	 *
	 * @return true if whitelist, false if blacklist [3]
	 */
	boolean isWhitelist();

	/**
	 * Sets the filter operating mode [3].
	 *
	 * @param whitelist true for whitelist, false for blacklist [3]
	 */
	void setWhitelist(boolean whitelist);

	/**
	 * Retrieves the list of filtered item stacks [3].
	 *
	 * @return the list of ItemStack instances [3]
	 */
	List<ItemStack> getFilterItems();

	/**
	 * Retrieves the mutable list of quantity limits corresponding to each filter item slot [3].
	 * -1 represents unlimited, and positive integers represent specific limits [3].
	 *
	 * @return list of integer limits [3]
	 */
	List<Integer> getFilterLimits();
}