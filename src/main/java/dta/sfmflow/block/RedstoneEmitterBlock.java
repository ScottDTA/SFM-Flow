package dta.sfmflow.block;

import com.mojang.serialization.MapCodec;
import dta.sfmflow.block.entity.RedstoneEmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Analog redstone output device supporting independent multi-sided signal
 * control [3].
 */
public class RedstoneEmitterBlock extends BaseEntityBlock {
	public static final MapCodec<RedstoneEmitterBlock> CODEC = simpleCodec(RedstoneEmitterBlock::new);

	public RedstoneEmitterBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.NORTH, false)
				.setValue(BlockStateProperties.SOUTH, false).setValue(BlockStateProperties.EAST, false)
				.setValue(BlockStateProperties.WEST, false).setValue(BlockStateProperties.UP, false)
				.setValue(BlockStateProperties.DOWN, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
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

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.NORTH, BlockStateProperties.SOUTH, BlockStateProperties.EAST,
				BlockStateProperties.WEST, BlockStateProperties.UP, BlockStateProperties.DOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState();
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
		Direction outputDirection = side.getOpposite();

		if (!state.getValue(getDirectionProperty(outputDirection))) {
			return 0;
		}

		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof RedstoneEmitterBlockEntity emitter) {
			return emitter.getPowerForSide(outputDirection);
		}

		return 15;
	}

	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
		return getSignal(state, level, pos, side);
	}

	public static BooleanProperty getDirectionProperty(Direction direction) {
		return switch (direction) {
		case UP -> BlockStateProperties.UP;
		case DOWN -> BlockStateProperties.DOWN;
		case NORTH -> BlockStateProperties.NORTH;
		case SOUTH -> BlockStateProperties.SOUTH;
		case EAST -> BlockStateProperties.EAST;
		case WEST -> BlockStateProperties.WEST;
		};
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RedstoneEmitterBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
}