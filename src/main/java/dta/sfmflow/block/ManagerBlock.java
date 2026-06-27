package dta.sfmflow.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

public class ManagerBlock extends BaseEntityBlock {
	public static final MapCodec<ManagerBlock> CODEC = simpleCodec(ManagerBlock::new);

	public ManagerBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);

		updateInventories(level, pos);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			BlockPos neighborPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

		updateInventories(level, pos);
	}

	private void updateInventories(Level level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity != null && blockEntity instanceof ManagerBlockEntity managerBlockEntity) {
			managerBlockEntity.updateInventories();
		}
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ManagerBlockEntity(pos, state);
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.BLOCK;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);

		updateInventories(pLevel, pPos);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos,
			Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
		if (!pLevel.isClientSide()) {
			BlockEntity bEntity = pLevel.getBlockEntity(pPos);
			if (bEntity instanceof ManagerBlockEntity managerBlockEntity) {
				((ServerPlayer) pPlayer)
						.openMenu(new SimpleMenuProvider(managerBlockEntity, Component.literal("Manager")), pPos);
				return ItemInteractionResult.SUCCESS;
			} else {
				return ItemInteractionResult.FAIL;
			}
		}

		return ItemInteractionResult.sidedSuccess(pLevel.isClientSide());
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState,
			BlockEntityType<T> pBlockEntityType) {
		if (pLevel.isClientSide()) {
			return null;
		}
		return createTickerHelper(pBlockEntityType, ModBlockEntities.MANAGER_BE.get(),
				(level, blockPos, blockState, blockEntity) -> blockEntity.tick(level, blockPos, blockState));
	}

}