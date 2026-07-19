package dta.sfmflow.block;

import com.mojang.serialization.MapCodec;
import dta.sfmflow.block.entity.ItemRelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Specialty network block that proxies entity inventories on configured sides.
 */
public class ItemRelayBlock extends BaseEntityBlock {
	public static final MapCodec<ItemRelayBlock> CODEC = simpleCodec(ItemRelayBlock::new);

	public ItemRelayBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ItemRelayBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		CableBlock.markNearbyNetworksDirty(level, pos);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			super.onRemove(state, level, pos, newState, isMoving);
			CableBlock.markNearbyNetworksDirty(level, pos);
		}
	}
}