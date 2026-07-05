package dta.sfmflow.api.capability;

import dta.sfmflow.block.entity.CableClusterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Public API callback executing custom ticking behavior for a card installed in a Cable Cluster [3].
 */
@FunctionalInterface
public interface ClusterCardBehavior {
	/**
	 * Executes the card behavior on server tick [3].
	 *
	 * @param level       the level context [3]
	 * @param pos         the position of the Cable Cluster [3]
	 * @param dir         the configured direction of the slot [3]
	 * @param slotIndex   the index of the slot the card is installed in [3]
	 * @param card        the card item stack itself [3]
	 * @param blockEntity the Cable Cluster block entity [3]
	 */
	void tick(Level level, BlockPos pos, Direction dir, int slotIndex, ItemStack card, CableClusterBlockEntity blockEntity);
}