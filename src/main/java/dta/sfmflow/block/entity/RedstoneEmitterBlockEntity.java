package dta.sfmflow.block.entity;

import dta.sfmflow.block.RedstoneEmitterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Backing BlockEntity for the Redstone Network Emitter block.
 */
public class RedstoneEmitterBlockEntity extends BlockEntity {
	private final int[] powerLevels = new int[6];
	private final boolean[] pulsedFaces = new boolean[6];

	public RedstoneEmitterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REDSTONE_EMITTER_BE.get(), pos, state);
	}

	public int getPowerForSide(Direction dir) {
		return this.powerLevels[dir.ordinal()];
	}

	public void setPowerForSide(Direction dir, int level) {
		int clampedPower = Math.max(0, Math.min(15, level));
		if (this.powerLevels[dir.ordinal()] != clampedPower) {
			this.powerLevels[dir.ordinal()] = clampedPower;
			this.setChanged();

			if (this.level != null && !this.level.isClientSide()) {
				BlockState state = this.getBlockState();
				BooleanProperty prop = RedstoneEmitterBlock.getDirectionProperty(dir);
				boolean shouldPower = clampedPower > 0;
				if (state.getValue(prop) != shouldPower) {
					this.level.setBlock(this.worldPosition, state.setValue(prop, shouldPower), 3);
				} else {
					this.level.updateNeighborsAt(this.worldPosition, state.getBlock());
				}
			}
		}
	}

	public void setPulsed(Direction dir, boolean pulsed) {
		this.pulsedFaces[dir.ordinal()] = pulsed;
	}

	public void clearPulses() {
		for (int i = 0; i < 6; i++) {
			if (pulsedFaces[i]) {
				setPowerForSide(Direction.values()[i], 0);
				pulsedFaces[i] = false;
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putIntArray("SidePowerLevels", this.powerLevels);

		byte[] pulsedBytes = new byte[6];
		for (int i = 0; i < 6; i++) {
			pulsedBytes[i] = (byte) (pulsedFaces[i] ? 1 : 0);
		}
		tag.putByteArray("PulsedFaces", pulsedBytes);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("SidePowerLevels")) {
			int[] loaded = tag.getIntArray("SidePowerLevels");
			System.arraycopy(loaded, 0, this.powerLevels, 0, Math.min(6, loaded.length));
		}
		if (tag.contains("PulsedFaces")) {
			byte[] loaded = tag.getByteArray("PulsedFaces");
			for (int i = 0; i < Math.min(6, loaded.length); i++) {
				this.pulsedFaces[i] = loaded[i] != 0;
			}
		}
	}
}