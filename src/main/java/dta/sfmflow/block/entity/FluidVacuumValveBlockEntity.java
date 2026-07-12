package dta.sfmflow.block.entity;

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
 * Backing BlockEntity for the Fluid Vacuum Valve block [3].
 */
public class FluidVacuumValveBlockEntity extends BlockEntity {

	private final FluidTank fluidTank = new FluidTank(8000);

	public FluidVacuumValveBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_VACUUM_VALVE_BE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluidVacuumValveBlockEntity be) {
		// Standalone Fluid Vacuum Valve remains completely passive; fluid blocks are scanned as snapshots [3]
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