package dta.sfmflow.kernel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A mutable, pre-allocated task frame designed to avoid heap allocations and GC
 * spikes [3]. Employs a volatile state barrier to prevent thread-safety read
 * issues [3].
 */
public final class ExecutionTask {
	private BlockPos sourcePos = BlockPos.ZERO;
	private int sourceSlot = -1;
	private BlockPos targetPos = BlockPos.ZERO;
	private int targetSlot = -1;
	private ItemStack item = ItemStack.EMPTY;
	private int count = 0;
	private volatile boolean isUsed = false; // Strict cross-thread memory barrier [3]

	/**
	 * Populates task parameters and publishes the frame cleanly to thread memory
	 * limits [3].
	 */
	public void set(BlockPos src, int srcSlot, BlockPos dest, int destSlot, ItemStack stack, int amount) {
		this.sourcePos = src;
		this.sourceSlot = srcSlot;
		this.targetPos = dest;
		this.targetSlot = destSlot;
		this.item = stack;
		this.count = amount;
		this.isUsed = true; // Volatile publish barrier
	}

	/**
	 * Resets parameters, recycling the node container safely for subsequent thread
	 * writes [3].
	 */
	public void reset() {
		this.sourcePos = BlockPos.ZERO;
		this.sourceSlot = -1;
		this.targetPos = BlockPos.ZERO;
		this.targetSlot = -1;
		this.item = ItemStack.EMPTY;
		this.count = 0;
		this.isUsed = false; // Recycles the node safely
	}

	public BlockPos getSourcePos() {
		return sourcePos;
	}

	public int getSourceSlot() {
		return sourceSlot;
	}

	public BlockPos getTargetPos() {
		return targetPos;
	}

	public int getTargetSlot() {
		return targetSlot;
	}

	public ItemStack getItem() {
		return item;
	}

	public int getCount() {
		return count;
	}

	public boolean isUsed() {
		return isUsed;
	}
}