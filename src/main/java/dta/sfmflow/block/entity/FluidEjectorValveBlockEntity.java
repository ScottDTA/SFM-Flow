package dta.sfmflow.block.entity;

import dta.sfmflow.block.FluidEjectorValveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Backing BlockEntity for the Fluid Ejector Valve block. Employs a
 * non-buffering, instantaneous capability handler to place fluid blocks.
 */
public class FluidEjectorValveBlockEntity extends BlockEntity {

	public FluidEjectorValveBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_EJECTOR_VALVE_BE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluidEjectorValveBlockEntity be) {
		// No-op: Ejection is handled instantaneously during task execution
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
	}

	public IFluidHandler getFluidHandler(Direction side) {
		Direction facing = this.getBlockState().getValue(FluidEjectorValveBlock.FACING);
		return new EjectorFluidHandler(this.level, this.worldPosition, facing);
	}

	/**
	 * Instantaneous, non-buffering fluid placement handler.
	 */
	private static class EjectorFluidHandler implements IFluidHandler {
		private final Level level;
		private final BlockPos pos;
		private final Direction facing;

		public EjectorFluidHandler(Level level, BlockPos pos, Direction facing) {
			this.level = level;
			this.pos = pos;
			this.facing = facing;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public @NotNull FluidStack getFluidInTank(int tank) {
			return FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return 1000;
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
			return !stack.isEmpty() && stack.getFluid().defaultFluidState().createLegacyBlock() != null;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty() || resource.getAmount() < 1000) {
				return 0;
			}
			BlockPos mouthPos = pos.relative(facing);
			BlockState mouthState = level.getBlockState(mouthPos);
			FluidState fluidState = level.getFluidState(mouthPos);

			// Prevent placing fluid if target location is already a fluid source
			if (!fluidState.isSource() && (mouthState.isAir() || mouthState.canBeReplaced(resource.getFluid()))) {
				BlockState fluidBlockState = resource.getFluid().defaultFluidState().createLegacyBlock();

				if (fluidBlockState != null && !fluidBlockState.isAir()) {
					if (action.execute()) {
						level.setBlock(mouthPos, fluidBlockState, 3);
					}
					return 1000;
				}
			}
			return 0;
		}

		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			return FluidStack.EMPTY;
		}
	}
}