package dta.sfmflow.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Directional network monitor extending vanilla ObserverBlock to reuse
 * positioning and tick logic [3]. Overrides getSignal to remain electrically
 * silent to vanilla redstone, functioning purely as a network target [3].
 */
public class ObserverCableBlock extends ObserverBlock {
	public static final MapCodec<ObserverCableBlock> CODEC = simpleCodec(ObserverCableBlock::new);

	/**
	 * Initializes a new ObserverCableBlock [3].
	 *
	 * @param properties block behavior properties [3]
	 */
	public ObserverCableBlock(Properties properties) {
		super(properties);
	}

	/**
	 * Overrides the parent codec lookup to match vanilla's invariance requirements
	 * [3]. Uses double-casting to safely bypass Java's generic invariance compiler
	 * checks [3].
	 *
	 * @return the mapped ObserverBlock codec [3]
	 */
	@Override
	public MapCodec<ObserverBlock> codec() {
		return (MapCodec<ObserverBlock>) (MapCodec<?>) CODEC;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 0; // Remains electrically inert to external vanilla systems [3]
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 0; // Remains electrically inert to external vanilla systems [3]
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
}