package dta.sfmflow.block;

import com.mojang.serialization.MapCodec;
import dta.sfmflow.block.entity.CableClusterBlockEntity;
import dta.sfmflow.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Consolidated network sub-chassis block holding multiple hardware execution
 * cards [3]. Standard (9-slot) and Advanced (18-slot) blocks share this
 * implementation [3].
 */
public class CableClusterBlock extends BaseEntityBlock {
	public static final MapCodec<CableClusterBlock> CODEC = simpleCodec(CableClusterBlock::new);

	/**
	 * Initializes a CableClusterBlock [3].
	 *
	 * @param properties block behavior properties [3]
	 */
	public CableClusterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableClusterBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos,
			Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
		if (!pLevel.isClientSide()) {
			BlockEntity bEntity = pLevel.getBlockEntity(pPos);
			if (bEntity instanceof CableClusterBlockEntity cluster) {
				pPlayer.openMenu(cluster, pPos);
				return ItemInteractionResult.SUCCESS;
			}
		}
		return ItemInteractionResult.sidedSuccess(pLevel.isClientSide());
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return createTickerHelper(type, ModBlockEntities.CABLE_CLUSTER_BE.get(), CableClusterBlockEntity::tick);
	}
}