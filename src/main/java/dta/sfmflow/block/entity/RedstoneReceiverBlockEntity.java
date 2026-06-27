package dta.sfmflow.block.entity;

import dta.sfmflow.block.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Backing BlockEntity for the Redstone Network Receiver block [3].
 * Caches incoming signals for each of the six faces independently to serve future redstone triggers [3].
 */
public class RedstoneReceiverBlockEntity extends BlockEntity {
    private final int[] powerLevels = new int[6];

    /**
     * Instantiates a new RedstoneReceiverBlockEntity [3].
     *
     * @param pos the block coordinates [3]
     * @param state the current block state [3]
     */
    public RedstoneReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_RECEIVER_BE.get(), pos, state);
    }

    /**
     * Queries incoming neighbor signal levels independently for each of the six directions,
     * flagging physical network dirtiness if changes are registered [3].
     *
     * @param level level context [3]
     * @param pos coordinate position [3]
     * @param state current block state [3]
     */
    public void checkPower(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            int currentPower = level.getSignal(pos, dir);
            int idx = dir.ordinal();
            if (this.powerLevels[idx] != currentPower) {
                this.powerLevels[idx] = currentPower;
                changed = true;
            }
        }
        if (changed) {
            this.setChanged();
            CableBlock.markNearbyNetworksDirty(level, pos);
        }
    }

    /**
     * Retrieves the stored analog power level for a specific block face [3].
     *
     * @param dir block face direction to query [3]
     * @return analog power level from 0 to 15 [3]
     */
    public int getPowerForSide(Direction dir) {
        return this.powerLevels[dir.ordinal()];
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