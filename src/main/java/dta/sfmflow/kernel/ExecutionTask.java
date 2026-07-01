package dta.sfmflow.kernel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A mutable, pre-allocated task frame designed to avoid heap allocations and GC
 * spikes [3]. Dynamic task parameter binding supports extensible plugin capability transfers [3].
 */
public final class ExecutionTask {
	private ResourceLocation capabilityId = null;
	private BlockPos sourcePos = BlockPos.ZERO;
	private int sourceSlot = -1;
	private @Nullable Direction sourceSide = null;
	private BlockPos targetPos = BlockPos.ZERO;
	private int targetSlot = -1;
	private @Nullable Direction targetSide = null;
	private Object taskParams = null;
	private volatile boolean isUsed = false;

	/**
	 * Populates task parameters and publishes the frame safely [3].
	 */
	public void set(ResourceLocation capabilityId, BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, Object params) {
		this.capabilityId = capabilityId;
		this.sourcePos = src;
		this.sourceSlot = srcSlot;
		this.sourceSide = srcSide;
		this.targetPos = dest;
		this.targetSlot = destSlot;
		this.targetSide = destSide;
		this.taskParams = params;
		this.isUsed = true; // Volatile publish barrier
	}

	public void reset() {
		this.capabilityId = null;
		this.sourcePos = BlockPos.ZERO;
		this.sourceSlot = -1;
		this.sourceSide = null;
		this.targetPos = BlockPos.ZERO;
		this.targetSlot = -1;
		this.targetSide = null;
		this.taskParams = null;
		this.isUsed = false; // Recycles the node safely
	}

	public ResourceLocation getCapabilityId() {
		return capabilityId;
	}

	public BlockPos getSourcePos() {
		return sourcePos;
	}

	public int getSourceSlot() {
		return sourceSlot;
	}

	public @Nullable Direction getSourceSide() {
		return sourceSide;
	}

	public BlockPos getTargetPos() {
		return targetPos;
	}

	public int getTargetSlot() {
		return targetSlot;
	}

	public @Nullable Direction getTargetSide() {
		return targetSide;
	}

	public Object getTaskParams() {
		return taskParams;
	}

	public boolean isUsed() {
		return isUsed;
	}
}
