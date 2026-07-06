package dta.sfmflow.plugin.vanilla;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Standard capability interactor bridging vanilla cauldrons to fluid transfer networks [3].
 */
public class CauldronFluidHandler implements IFluidHandler {
	private final Level level;
	private final BlockPos pos;

	public CauldronFluidHandler(Level level, BlockPos pos) {
		this.level = level;
		this.pos = pos;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public @NotNull FluidStack getFluidInTank(int tank) {
		BlockState state = level.getBlockState(pos);
		CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());
		if (content != null) {
			int currentLevel = content.currentLevel(state);
			if (currentLevel > 0) {
				// 1 level in cauldron = 333 mB (since 1 full bucket = 1000 mB) [3]
				return new FluidStack(content.fluid, currentLevel * 333);
			}
		}
		return FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return 1000; // Cauldron holds exactly 1 bucket (1000 mB) [3]
	}

	@Override
	public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
		BlockState state = level.getBlockState(pos);
		if (state.is(Blocks.CAULDRON)) {
			return CauldronFluidContent.getForFluid(stack.getFluid()) != null;
		}
		CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());
		return content != null && content.fluid == stack.getFluid();
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) return 0;
		BlockState state = level.getBlockState(pos);

		if (state.is(Blocks.CAULDRON)) {
			CauldronFluidContent targetContent = CauldronFluidContent.getForFluid(resource.getFluid());
			if (targetContent != null && targetContent.levelProperty != null) {
				// A cauldron level needs 333 mB of fluid. To insert 1 level: [3]
				if (resource.getAmount() >= 333) {
					int filledAmount = 333;
					if (action.execute()) {
						BlockState filledState = targetContent.block.defaultBlockState()
								.setValue(targetContent.levelProperty, 1);
						level.setBlock(pos, filledState, Block.UPDATE_ALL);
					}
					return filledAmount;
				}
			}
			return 0;
		}

		CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());
		if (content != null && content.fluid == resource.getFluid() && content.levelProperty != null) {
			int currentLevel = content.currentLevel(state);
			if (currentLevel < content.maxLevel) {
				if (resource.getAmount() >= 333) {
					int filledAmount = 333;
					if (action.execute()) {
						level.setBlock(pos, state.setValue(content.levelProperty, currentLevel + 1), Block.UPDATE_ALL);
					}
					return filledAmount;
				}
			}
		}

		return 0;
	}

	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) return FluidStack.EMPTY;
		BlockState state = level.getBlockState(pos);
		CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());

		if (content != null && content.fluid == resource.getFluid()) {
			int currentLevel = content.currentLevel(state);
			if (currentLevel > 0 && resource.getAmount() >= 333) {
				FluidStack drained = new FluidStack(content.fluid, 333);
				if (action.execute()) {
					int nextLevel = currentLevel - 1;
					if (nextLevel == 0) {
						level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
					} else if (content.levelProperty != null) {
						level.setBlock(pos, state.setValue(content.levelProperty, nextLevel), Block.UPDATE_ALL);
					}
				}
				return drained;
			}
		}
		return FluidStack.EMPTY;
	}

	@Override
	public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain <= 0) return FluidStack.EMPTY;
		BlockState state = level.getBlockState(pos);
		CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());

		if (content != null) {
			int currentLevel = content.currentLevel(state);
			if (currentLevel > 0 && maxDrain >= 333) {
				FluidStack drained = new FluidStack(content.fluid, 333);
				if (action.execute()) {
					int nextLevel = currentLevel - 1;
					if (nextLevel == 0) {
						level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
					} else if (content.levelProperty != null) {
						level.setBlock(pos, state.setValue(content.levelProperty, nextLevel), Block.UPDATE_ALL);
					}
				}
				return drained;
			}
		}
		return FluidStack.EMPTY;
	}
}