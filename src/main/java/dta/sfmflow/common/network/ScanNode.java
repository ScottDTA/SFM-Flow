package dta.sfmflow.common.network;

import net.minecraft.core.BlockPos;

/**
 * BFS queue tracker node tracking relative traversal depth [3].
 *
 * @param pos   coordinate position [3]
 * @param depth coordinate search depth [3]
 */
public record ScanNode(BlockPos pos, int depth) {
}