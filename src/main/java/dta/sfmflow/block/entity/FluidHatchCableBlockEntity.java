package dta.sfmflow.block.entity;

import dta.sfmflow.block.FluidHatchCableBlock;
import dta.sfmflow.block.HatchMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Backing BlockEntity for the fluid source vacuum/ejector Hatch Cable block [3].
 * Utilizes equivalents and delegates operation logic to HatchBehaviorHelper [3].
 */
public class FluidHatchCableBlockEntity extends BlockEntity {

	private final FluidTank fluidTank = new FluidTank(8000);

	public FluidHatchCableBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_HATCH_CABLE_BE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluidHatchCableBlockEntity be) {
		if (level.isClientSide()) {
			return;
		}

		int tickOffset = Math.abs(pos.hashCode()) % 10;
		if ((level.getGameTime() + tickOffset) % 10 != 0) {
			return;
		}

		Direction facing = state.getValue(FluidHatchCableBlock.FACING);
		HatchMode mode = state.getValue(FluidHatchCableBlock.HATCH_MODE);

		if (mode == HatchMode.VACUUM) {
			HatchBehaviorHelper.performFluidVacuum(level, pos, facing, be.fluidTank, be::setChanged);
		} else if (mode == HatchMode.EJECT) {
			HatchBehaviorHelper.performFluidEjection(level, pos, facing, be.fluidTank, be::setChanged);
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