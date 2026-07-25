package dta.sfmflow.api.component;

/**
 * Common public API interface representing any component that target-binds
 * to a specific scanned inventory or block on the physical network.
 */
public interface IInventoryTarget {
	/**
	 * Retrieves the targeted inventory identifier.
	 *
	 * @return the unique integer ID of the targeted inventory, or -1 if unselected
	 */
	int getInventoryId();

	/**
	 * Sets the targeted inventory identifier.
	 *
	 * @param id the unique integer ID of the targeted inventory
	 */
	void setInventoryId(int id);

	/**
	 * Dynamically checks if a specific inventory ID is bound or configured within this target.
	 * Defaults to evaluating if the ID matches the primary targeted inventory.
	 */
	default boolean isInventoryBound(int id) {
		return id != -1 && getInventoryId() == id;
	}
}