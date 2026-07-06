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
 * Standard capability interactor bridging vanilla cauldrons to fluid transfer
 * networks. Upgraded to safely support multi-level transfers and round up
 * full-bucket capacity conversions.
 */
public class CauldronFluidHandler implements IFluidHandler {
	private final Level level;
	private final BlockPos pos;

	// Constant: 1 level = 1/3 of a bucket (333 mB)
	private static final int MB_PER_LEVEL = 333;

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
				// FIX 1: If the cauldron is maximum full (Level 3), report exactly 1000mB
				// to avoid losing a microscopic 1mB fraction over network filters.
				int amount = (currentLevel == content.maxLevel) ? 1000 : (currentLevel * MB_PER_LEVEL);
				return new FluidStack(content.fluid, amount);
			}
		}
		return FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return 1000; // Cauldron holds exactly 1 bucket (1000 mB)
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
		if (resource.isEmpty())
			return 0;
		BlockState state = level.getBlockState(pos);

		// CASE A: Interacting with an entirely empty baseline cauldron
		if (state.is(Blocks.CAULDRON)) {
			CauldronFluidContent targetContent = CauldronFluidContent.getForFluid(resource.getFluid());
			if (targetContent != null && targetContent.levelProperty != null) {
				// FIX 2: Dynamic levels math calculation. If resource contains 1000mB,
				// we calculate levelsToFill = Math.min(1000 / 333, 3) -> 3 full levels.
				int levelsToFill = Math.min(resource.getAmount() / MB_PER_LEVEL, targetContent.maxLevel);
				if (levelsToFill > 0) {
					int filledAmount = (levelsToFill == targetContent.maxLevel) ? resource.getAmount()
							: (levelsToFill * MB_PER_LEVEL);
					if (action.execute()) {
						BlockState filledState = targetContent.block.defaultBlockState()
								.setValue(targetContent.levelProperty, levelsToFill);
						level.setBlock(pos, filledState, Block.UPDATE_ALL);
					}
					return filledAmount;
				}
			}
			return 0;
		}

		// CASE B: Incrementing an existing matching fluid cauldron type
		CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());
		if (content != null && content.fluid == resource.getFluid() && content.levelProperty != null) {
			int currentLevel = content.currentLevel(state);
			int availableLevels = content.maxLevel - currentLevel;

			if (availableLevels > 0) {
				int requestedLevels = resource.getAmount() / MB_PER_LEVEL;
				int levelsToFill = Math.min(requestedLevels, availableLevels);

				if (levelsToFill > 0) {
					int targetLevel = currentLevel + levelsToFill;
					// If we fill it to max, consume all available fluid space perfectly
					int filledAmount = (targetLevel == content.maxLevel) ? (1000 - (currentLevel * MB_PER_LEVEL))
							: (levelsToFill * MB_PER_LEVEL);

					if (action.execute()) {
						level.setBlock(pos, state.setValue(content.levelProperty, targetLevel), Block.UPDATE_ALL);
					}
					return filledAmount;
				}
			}
		}

		return 0;
	}

	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty())
			return FluidStack.EMPTY;
		BlockState state = level.getBlockState(pos);
		CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());

		if (content != null && content.fluid == resource.getFluid()) {
			return drain(resource.getAmount(), action);
		}
		return FluidStack.EMPTY;
	}

	@Override
	public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain <= 0)
			return FluidStack.EMPTY;
		BlockState state = level.getBlockState(pos);
		CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());

		if (content != null && content.levelProperty != null) {
			int currentLevel = content.currentLevel(state);
			if (currentLevel > 0) {
				// FIX 3: Dynamic drainage calculation supporting multi-level extractions.
				int requestedLevels = maxDrain / MB_PER_LEVEL;
				int levelsToDrain = Math.min(requestedLevels, currentLevel);

				if (levelsToDrain > 0) {
					// Round up remaining fraction if we drain it completely from a full state
					int drainedAmount = (levelsToDrain == currentLevel && currentLevel == content.maxLevel) ? 1000
							: (levelsToDrain * MB_PER_LEVEL);
					FluidStack drained = new FluidStack(content.fluid, drainedAmount);

					if (action.execute()) {
						int nextLevel = currentLevel - levelsToDrain;
						if (nextLevel == 0) {
							level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
						} else {
							level.setBlock(pos, state.setValue(content.levelProperty, nextLevel), Block.UPDATE_ALL);
						}
					}
					return drained;
				}
			}
		}
		return FluidStack.EMPTY;
	}
}
