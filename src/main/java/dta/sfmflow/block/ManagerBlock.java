package dta.sfmflow.block;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import dta.sfmflow.api.security.ManagerAccessLevel;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.block.entity.ModBlockEntities;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.item.ProgramDiskItem;
import dta.sfmflow.networking.packets.clientbound.OpenDiskOverwriteConfirmPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.network.PacketDistributor;

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
		if (!pState.is(pNewState.getBlock())) {
			if (!pLevel.isClientSide()) {
				BlockEntity bEntity = pLevel.getBlockEntity(pPos);
				if (bEntity instanceof ManagerBlockEntity manager) {
					// Clean up the external file before final block removal
					manager.deleteExternalData();
				}
			}
		}
		super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);

		updateInventories(pLevel, pPos);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && placer instanceof Player player) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof ManagerBlockEntity manager) {
				manager.setOwner(player.getUUID(), player.getGameProfile().getName());
			}
		}
	}
	
	@Override
	protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos,
			Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
		if (!pLevel.isClientSide()) {
			BlockEntity bEntity = pLevel.getBlockEntity(pPos);
			if (bEntity instanceof ManagerBlockEntity managerBlockEntity) {
				// Shifting players with admin permissions (level 2+) bypass access restrictions
				boolean isAdminBypass = pPlayer.isSecondaryUseActive() && pPlayer.hasPermissions(2);

				if (!isAdminBypass && !canAccess(pPlayer, managerBlockEntity)) {
					pPlayer.sendSystemMessage(Component.translatable("gui.sfmflow.error.no_access", managerBlockEntity.getOwnerName())
							.withStyle(ChatFormatting.RED));
					return ItemInteractionResult.FAIL;
				}

				// Intercept Program Disk usage
				if (pStack.is(ModItems.PROGRAM_DISK.get())) {
					if (!ProgramDiskItem.isProgrammed(pStack)) {
						// COPY PHASE [3]
						CompoundTag serializedTag = managerBlockEntity.getUpdateTag(pLevel.registryAccess());
						CompoundTag diskData = new CompoundTag();
						if (serializedTag.contains("flowchart")) {
							diskData.put("flowchart", serializedTag.get("flowchart"));
						}
						if (serializedTag.contains("GroupVariables")) {
							diskData.put("GroupVariables", serializedTag.get("GroupVariables"));
						}
						if (serializedTag.contains("FilterVariables")) {
							diskData.put("FilterVariables", serializedTag.get("FilterVariables"));
						}

						pStack.set(DataComponents.CUSTOM_DATA, CustomData.of(diskData));
						pPlayer.sendSystemMessage(Component.translatable("gui.sfmflow.disk.copied").withStyle(ChatFormatting.GREEN));
						pLevel.playSound(null, pPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
						return ItemInteractionResult.SUCCESS;
					} else {
						// PASTE PHASE [3]
						if (managerBlockEntity.getFlowComponents().isEmpty()) {
							// Empty target, paste immediately
							pasteDiskLayout(managerBlockEntity, pStack, pLevel);
							pPlayer.sendSystemMessage(Component.translatable("gui.sfmflow.disk.pasted").withStyle(ChatFormatting.GREEN));
							return ItemInteractionResult.SUCCESS;
						} else {
							// Active target, query confirmation popup
							PacketDistributor.sendToPlayer((ServerPlayer) pPlayer, new OpenDiskOverwriteConfirmPacket(pPos));
							return ItemInteractionResult.SUCCESS;
						}
					}
				}

				((ServerPlayer) pPlayer)
						.openMenu(new SimpleMenuProvider(managerBlockEntity, Component.literal("Manager")), pPos);
				return ItemInteractionResult.SUCCESS;
			} else {
				return ItemInteractionResult.FAIL;
			}
		}

		return ItemInteractionResult.sidedSuccess(pLevel.isClientSide());
	}

	public static void pasteDiskLayout(ManagerBlockEntity manager, ItemStack diskStack, Level level) {
		CustomData customData = diskStack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTag();
			CompoundTag updateTag = new CompoundTag();
			if (tag.contains("flowchart")) {
				updateTag.put("flowchart", tag.get("flowchart"));
			}
			if (tag.contains("GroupVariables")) {
				updateTag.put("GroupVariables", tag.get("GroupVariables"));
			}
			if (tag.contains("FilterVariables")) {
				updateTag.put("FilterVariables", tag.get("FilterVariables"));
			}
			updateTag.putUUID("ManagerId", UUID.randomUUID()); // Assign a fresh layout ID
			
			manager.loadAdditional(updateTag, level.registryAccess());
			manager.setDataDirty(true);
			manager.setChanged();
			manager.rebuildListeners();
			level.playSound(null, manager.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
		}
	}
	
	private boolean canAccess(Player player, ManagerBlockEntity manager) {
		// Symmetrical compatibility fallback: allow access if no owner is assigned
		if (manager.getOwnerUUID() == null) {
			return true;
		}
		if (player.getUUID().equals(manager.getOwnerUUID())) {
			return true;
		}

		ManagerAccessLevel level = manager.getAccessLevel();
		if (level == ManagerAccessLevel.PUBLIC) {
			return true;
		}

		if (level == ManagerAccessLevel.TEAM) {
			// Symmetrical team access verification using Vanilla Scoreboard teams
			Scoreboard scoreboard = player.level().getScoreboard();
			PlayerTeam playerTeam = scoreboard.getPlayersTeam(player.getScoreboardName());
			PlayerTeam ownerTeam = scoreboard.getPlayersTeam(manager.getOwnerName());
			if (playerTeam != null && ownerTeam != null && playerTeam.equals(ownerTeam)) {
				return true;
			}
		}

		return false;
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