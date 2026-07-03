package dta.sfmflow.api.component;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for flowchart components that support configuration via container
 * ghost slots [3].
 */
public interface IGhostSlotAware {
	/**
	 * Gets the item stack at the specified ghost slot index [3].
	 *
	 * @param index the slot index [3]
	 * @return the stack currently stored [3]
	 */
	ItemStack getGhostStack(int index);

	/**
	 * Sets the item stack at the specified ghost slot index [3].
	 *
	 * @param index the slot index [3]
	 * @param stack the stack to set [3]
	 */
	void setGhostStack(int index, ItemStack stack);

	/**
	 * Gets the maximum number of ghost slots supported by this component [3].
	 *
	 * @return the maximum slot capacity [3]
	 */
	int getGhostSlotCount();
}
