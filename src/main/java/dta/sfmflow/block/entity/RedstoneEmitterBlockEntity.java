package dta.sfmflow.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Backing BlockEntity for the Redstone Network Emitter block [3]. Manages
 * discrete multi-sided analog outputs safely decoupled from visual BlockStates
 * [3].
 */
public class RedstoneEmitterBlockEntity extends BlockEntity {
	private final int[] powerLevels = new int[6];

	/**
	 * Instantiates a new RedstoneEmitterBlockEntity [3].
	 *
	 * @param pos   the block coordinates [3]
	 * @param state the current block state [3]
	 */
	public RedstoneEmitterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REDSTONE_EMITTER_BE.get(), pos, state);
	}

	/**
	 * Retrieves the stored analog power level for a specific direction [3].
	 *
	 * @param dir block face to query [3]
	 * @return analog power level from 0 to 15 [3]
	 */
	public int getPowerForSide(Direction dir) {
		return this.powerLevels[dir.ordinal()];
	}

	/**
	 * Configures the analog power output for a specific block face, stamping
	 * changes [3].
	 *
	 * @param dir   block face [3]
	 * @param level analog power level between 0 and 15 [3]
	 */
	public void setPowerForSide(Direction dir, int level) {
		int clampedPower = Math.max(0, Math.min(15, level));
		if (this.powerLevels[dir.ordinal()] != clampedPower) {
			this.powerLevels[dir.ordinal()] = clampedPower;
			this.setChanged();
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putIntArray("SidePowerLevels", this.powerLevels);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("SidePowerLevels")) {
			int[] loaded = tag.getIntArray("SidePowerLevels");
			System.arraycopy(loaded, 0, this.powerLevels, 0, Math.min(6, loaded.length));
		}
	}
}