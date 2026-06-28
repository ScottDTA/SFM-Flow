package dta.sfmflow.kernel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A mutable, pre-allocated task frame designed to avoid heap allocations and GC
 * spikes [3]. Upgraded to store deep copies of planned stacks to isolate frames
 * from background mutations [3].
 */
public final class ExecutionTask {
	private BlockPos sourcePos = BlockPos.ZERO;
	private int sourceSlot = -1;
	private BlockPos targetPos = BlockPos.ZERO;
	private int targetSlot = -1;
	private ItemStack item = ItemStack.EMPTY;
	private int count = 0;
	private volatile boolean isUsed = false;

	/**
	 * Populates task parameters and publishes the frame safely [3]. Stores an
	 * isolated copy of the item stack to prevent background thread mutations [3].
	 */
	public void set(BlockPos src, int srcSlot, BlockPos dest, int destSlot, ItemStack stack, int amount) {
		this.sourcePos = src;
		this.sourceSlot = srcSlot;
		this.targetPos = dest;
		this.targetSlot = destSlot;
		this.item = stack.copy(); // Deep copy isolates parameters from background mutations [3]
		this.count = amount;
		this.isUsed = true; // Volatile publish barrier
	}

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