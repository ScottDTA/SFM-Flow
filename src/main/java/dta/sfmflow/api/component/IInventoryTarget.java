package dta.sfmflow.api.component;

/**
 * Common public API interface representing any component that target-binds
 * to a specific scanned inventory or block on the physical network [3].
 */
public interface IInventoryTarget {
	/**
	 * Retrieves the targeted inventory identifier [3].
	 *
	 * @return the unique integer ID of the targeted inventory, or -1 if unselected [3]
	 */
	int getInventoryId();

	/**
	 * Sets the targeted inventory identifier [3].
	 *
	 * @param id the unique integer ID of the targeted inventory [3]
	 */
	void setInventoryId(int id);
}