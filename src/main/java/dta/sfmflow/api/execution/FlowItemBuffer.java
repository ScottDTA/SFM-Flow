package dta.sfmflow.api.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * Public API container representing items flowing along a specific connection wire,
 * preserving source block coordinate metadata for downstream evaluation [3].
 */
public final class FlowItemBuffer {
	/**
	 * Holds the original source block metrics of an extracted item stack [3].
	 */
	public record BufferedItem(BlockPos srcPos, int srcSlot, Direction srcSide, ItemStack stack) {
		public BufferedItem {
			stack = stack.copy();
		}
	}

	private final List<BufferedItem> items = new ArrayList<>();

	public List<BufferedItem> getItems() {
		return this.items;
	}

	/**
	 * Appends an extracted item stack to the buffer, merging matching types [3].
	 */
	public void add(BlockPos srcPos, int srcSlot, Direction srcSide, ItemStack stack) {
		if (stack.isEmpty()) return;
		for (BufferedItem existing : items) {
			if (existing.srcPos().equals(srcPos) 
					&& existing.srcSlot() == srcSlot 
					&& existing.srcSide() == srcSide 
					&& ItemStack.isSameItemSameComponents(existing.stack(), stack)) {
				existing.stack().grow(stack.getCount());
				return;
			}
		}
		items.add(new BufferedItem(srcPos, srcSlot, srcSide, stack));
	}

	public boolean isEmpty() {
		return items.isEmpty() || items.stream().allMatch(item -> item.stack().isEmpty());
	}
}
