package dta.sfmflow.util;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;

/**
 * Declares scannable target inventory indices mapped to physical coordinates
 * [3]. Upgraded to track sleeping states to insulate off-thread execution runs
 * from unloaded chunk crashes [3].
 */
public class ConnectionBlock implements IContainerSelection {
	private BlockPos blockPos;
	private int cableDistance;
	private EnumSet<ConnectionBlockType> types;
	private int id;
	private boolean sleeping = false; // Loaded-chunk safety filter state [3]

	public ConnectionBlock(BlockPos blockPos, int cableDistance) {
		this.blockPos = blockPos;
		this.cableDistance = cableDistance;
		types = EnumSet.noneOf(ConnectionBlockType.class);

	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public void setTypes(EnumSet<ConnectionBlockType> caps) {
		types = caps;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Checks if this scanned coordinate is currently sleeping due to unloaded chunk
	 * boundaries [3].
	 *
	 * @return true if sleeping and capability lookups must be bypassed [3]
	 */
	public boolean isSleeping() {
		return this.sleeping;
	}

	/**
	 * Flags this target node's active sleeping status [3].
	 *
	 * @param sleeping target state [3]
	 */
	public void setSleeping(boolean sleeping) {
		this.sleeping = sleeping;
	}

	@Override
	public int getId() {
		return 0;
	}

	@Override
	public boolean isVariable() {
		return false;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}
}