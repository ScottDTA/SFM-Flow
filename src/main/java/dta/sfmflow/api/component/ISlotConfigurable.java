package dta.sfmflow.api.component;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;

/**
 * Common public API interface representing any component that supports
 * per-slot configurations for specific face directions [3].
 * Allows third-party plugin developers to reuse modal slot configuration popups cleanly [3].
 */
public interface ISlotConfigurable {
	/**
	 * Checks if a specific slot index is active/enabled for a face direction [3].
	 *
	 * @param dir  the face direction context [3]
	 * @param slot the slot index to query [3]
	 * @return true if the slot is enabled, false otherwise [3]
	 */
	boolean isSlotEnabled(@Nullable Direction dir, int slot);

	/**
	 * Toggles the active configuration state of a slot for a specific face direction [3].
	 *
	 * @param dir  the face direction context [3]
	 * @param slot the slot index to toggle [3]
	 */
	void toggleSlot(@Nullable Direction dir, int slot);
}