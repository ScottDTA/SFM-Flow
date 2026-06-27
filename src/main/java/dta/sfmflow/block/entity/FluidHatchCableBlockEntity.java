package dta.sfmflow.block.entity;

import dta.sfmflow.block.FluidHatchCableBlock;
import dta.sfmflow.block.HatchMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Backing BlockEntity for the fluid source vacuum/ejector Hatch Cable block [3].
 * Utilizes equivalent volume structures to represent 8000mB capacities safely [3].
 */
public class FluidHatchCableBlockEntity extends BlockEntity {

    private final FluidTank fluidTank = new FluidTank(8000);

    /**
     * Instantiates a new FluidHatchCableBlockEntity [3].
     *
     * @param pos the block coordinates [3]
     * @param state the current block state [3]
     */
    public FluidHatchCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_HATCH_CABLE_BE.get(), pos, state);
    }

    /**
     * Ticks the fluid hatch block, executing distributed sweeps over tick intervals [3].
     *
     * @param level level instance [3]
     * @param pos block coordinates [3]
     * @param state block state properties [3]
     * @param be fluid block entity [3]
     */
    public static void tick(Level level, BlockPos pos, BlockState state, FluidHatchCableBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }

        int tickOffset = Math.abs(pos.hashCode()) % 10;
        if ((level.getGameTime() + tickOffset) % 10 != 0) {
            return;
        }

        HatchMode mode = state.getValue(FluidHatchCableBlock.HATCH_MODE);
        if (mode == HatchMode.VACUUM) {
            be.performVacuumIngestion(level, pos, state);
        } else if (mode == HatchMode.EJECT) {
            be.performEjection(level, pos, state);
        }
    }

    /**
     * Ingests adjacent fluid sources, placing air blocks to clear the target location [3].
     *
     * @param level level context [3]
     * @param pos block position [3]
     * @param state current block state [3]
     */
    public void performVacuumIngestion(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FluidHatchCableBlock.FACING);
        BlockPos mouthPos = pos.relative(facing);
        FluidState fluidState = level.getFluidState(mouthPos);

        if (fluidState.isSource()) {
            Fluid fluid = fluidState.getType();
            FluidStack sample = new FluidStack(fluid, 1000);
            int accepted = this.fluidTank.fill(sample, IFluidHandler.FluidAction.SIMULATE);
            
            if (accepted == 1000) {
                this.fluidTank.fill(sample, IFluidHandler.FluidAction.EXECUTE);
                level.setBlock(mouthPos, Blocks.AIR.defaultBlockState(), 3);
                this.setChanged();
            }
        }
    }

    /**
     * Ejects fluids from internal buffers to create legacy blocks at target coordinates [3].
     *
     * @param level level context [3]
     * @param pos block position [3]
     * @param state current block state [3]
     */
    public void performEjection(Level level, BlockPos pos, BlockState state) {
        FluidStack stored = this.fluidTank.getFluid();
        if (stored.getAmount() >= 1000) {
            Direction facing = state.getValue(FluidHatchCableBlock.FACING);
            BlockPos mouthPos = pos.relative(facing);
            BlockState mouthState = level.getBlockState(mouthPos);

            if (mouthState.isAir() || mouthState.canBeReplaced(stored.getFluid())) {
                BlockState fluidBlockState = stored.getFluid().defaultFluidState().createLegacyBlock();
                
                if (!fluidBlockState.isAir()) {
                    level.setBlock(mouthPos, fluidBlockState, 3);
                    this.fluidTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    this.setChanged();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.fluidTank.writeToNBT(registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.fluidTank.readFromNBT(registries, tag);
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return this.fluidTank;
    }
}