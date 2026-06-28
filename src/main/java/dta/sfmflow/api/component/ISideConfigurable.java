package dta.sfmflow.api.component;

import net.minecraft.core.Direction;

/**
 * Common public API interface representing any component that supports
 * side-specific configuration on a selected block [3].
 */
public interface ISideConfigurable {
	/**
	 * Checks if a specific face direction is actively configured [3].
	 *
	 * @param dir the direction face query [3]
	 * @return true if the side is active, false otherwise [3]
	 */
	boolean isSideActive(Direction dir);

	/**
	 * Toggles the configuration state for a specific face direction [3].
	 *
	 * @param dir the direction face target [3]
	 */
	void toggleSide(Direction dir);
}