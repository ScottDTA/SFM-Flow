package dta.sfmflow.api.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;

/**
 * Public API container representing fluids flowing along a specific connection wire,
 * preserving source block coordinate metadata for downstream evaluation [3].
 */
public final class FlowFluidBuffer {
	/**
	 * Holds the original source block metrics of an extracted fluid stack [3].
	 */
	public record BufferedFluid(BlockPos srcPos, int srcSlot, Direction srcSide, FluidStack stack) {
		public BufferedFluid {
			stack = stack.copy();
		}
	}

	private final List<BufferedFluid> fluids = new ArrayList<>();

	public List<BufferedFluid> getFluids() {
		return this.fluids;
	}

	/**
	 * Appends an extracted fluid stack to the buffer, merging matching types [3].
	 */
	public void add(BlockPos srcPos, int srcSlot, Direction srcSide, FluidStack stack) {
		if (stack.isEmpty()) return;
		for (BufferedFluid existing : fluids) {
			if (existing.srcPos().equals(srcPos) 
					&& existing.srcSlot() == srcSlot 
					&& existing.srcSide() == srcSide 
					&& FluidStack.isSameFluid(existing.stack(), stack)) {
				existing.stack().grow(stack.getAmount());
				return;
			}
		}
		fluids.add(new BufferedFluid(srcPos, srcSlot, srcSide, stack));
	}

	public boolean isEmpty() {
		return fluids.isEmpty() || fluids.stream().allMatch(fluid -> fluid.stack().isEmpty());
	}
}