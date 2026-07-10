package dta.sfmflow.block;

import com.mojang.serialization.MapCodec;
import dta.sfmflow.block.entity.RedstoneReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Analog input monitor block that detects adjacent signal changes on all six
 * faces [3]. Non-directional block designed to drive event triggers matching
 * specific face configurations [3].
 */
public class RedstoneReceiverBlock extends BaseEntityBlock {
	public static final MapCodec<RedstoneReceiverBlock> CODEC = simpleCodec(RedstoneReceiverBlock::new);

	public RedstoneReceiverBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		// Cleanly notify nearby network controllers upon placement [3]
		CableBlock.markNearbyNetworksDirty(level, pos);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			super.onRemove(state, level, pos, newState, isMoving);
			// Cleanly notify nearby network controllers upon removal [3]
			CableBlock.markNearbyNetworksDirty(level, pos);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			BlockPos neighborPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (!level.isClientSide()) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof RedstoneReceiverBlockEntity receiver) {
				receiver.checkPower(level, pos, state);
			}
		}
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RedstoneReceiverBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
}