package dta.sfmflow.block.entity;

import dta.sfmflow.block.FluidEjectorValveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Backing BlockEntity for the Fluid Ejector Valve block [3].
 * Employs a non-buffering, instantaneous capability handler to place fluid blocks [3].
 */
public class FluidEjectorValveBlockEntity extends BlockEntity {

	public FluidEjectorValveBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_EJECTOR_VALVE_BE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluidEjectorValveBlockEntity be) {
		// No-op: Ejection is handled instantaneously during task execution [3]
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
	 * Instantaneous, non-buffering fluid placement handler [3].
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
			return FluidStack.EMPTY; // No fluid is ever held [3]
		}

		@Override
		public int getTankCapacity(int tank) {
			return 1000; // Accepts exactly 1 bucket at a time [3]
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
			return !stack.isEmpty() && stack.getFluid().defaultFluidState().createLegacyBlock() != null;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty() || resource.getAmount() < 1000) {
				return 0; // Requires at least 1 full bucket to place a source block [3]
			}
			BlockPos mouthPos = pos.relative(facing);
			BlockState mouthState = level.getBlockState(mouthPos);
			net.minecraft.world.level.material.FluidState fluidState = level.getFluidState(mouthPos);

			// Prevent placing fluid if target location is already a fluid source [3]
			if (!fluidState.isSource() && (mouthState.isAir() || mouthState.canBeReplaced(resource.getFluid()))) {
				BlockState fluidBlockState = resource.getFluid().defaultFluidState().createLegacyBlock();

				if (fluidBlockState != null && !fluidBlockState.isAir()) {
					if (action.execute()) {
						level.setBlock(mouthPos, fluidBlockState, 3);
					}
					return 1000; // Consumed exactly 1 bucket [3]
				}
			}
			return 0;
		}

		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			return FluidStack.EMPTY; // Cannot be drained [3]
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			return FluidStack.EMPTY; // Cannot be drained [3]
		}
	}
}