package dta.sfmflow.block;

import com.mojang.serialization.MapCodec;
import dta.sfmflow.block.entity.RedstoneEmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
 * Analog redstone output device supporting independent multi-sided signal control [3].
 * Decouples visual boolean states from logical 0-15 analog power to prevent Combinatorial BlockState Explosion [3].
 * State modifications are strictly controlled programmatically via flowchart commands [3].
 */
public class RedstoneEmitterBlock extends BaseEntityBlock {
    public static final MapCodec<RedstoneEmitterBlock> CODEC = simpleCodec(RedstoneEmitterBlock::new);

    /**
     * Initializes a new RedstoneEmitterBlock instance [3].
     *
     * @param properties block behavior properties [3]
     */
    public RedstoneEmitterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(BlockStateProperties.NORTH, false)
            .setValue(BlockStateProperties.SOUTH, false)
            .setValue(BlockStateProperties.EAST, false)
            .setValue(BlockStateProperties.WEST, false)
            .setValue(BlockStateProperties.UP, false)
            .setValue(BlockStateProperties.DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
            BlockStateProperties.NORTH,
            BlockStateProperties.SOUTH,
            BlockStateProperties.EAST,
            BlockStateProperties.WEST,
            BlockStateProperties.UP,
            BlockStateProperties.DOWN
        );
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
        // The side parameter represents the face of the adjacent neighbor being powered.
        // To query our block's corresponding emitting face, we check the opposite direction.
        Direction outputDirection = side.getOpposite();
        
        // 1. Verify if the visual state for this direction is set to true
        if (!state.getValue(getDirectionProperty(outputDirection))) {
            return 0;
        }

        // 2. Fetch the analog power value from the Block Entity side-car
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneEmitterBlockEntity emitter) {
            return emitter.getPowerForSide(outputDirection);
        }
        
        return 15; // Fallback to maximum signal if Block Entity is loading
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }

    /**
     * Resolves directional enums to their respective standard BlockState booleans [3].
     *
     * @param direction block direction face [3]
     * @return the associated BooleanProperty [3]
     */
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