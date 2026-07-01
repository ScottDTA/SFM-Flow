package dta.sfmflow.api.capability;

import net.minecraft.world.item.ItemStack;

/**
 * Standard parameters record used for item transfer task frames [3].
 */
public record ItemTransferParams(int srcSlot, int destSlot, ItemStack item, int count) {
	public ItemTransferParams {
		item = item.copy(); // Guarantee isolated copy
	}
}
