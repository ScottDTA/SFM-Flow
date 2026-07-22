package dta.sfmflow.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignUpdaterCableBlockEntity extends BlockEntity {
	public SignUpdaterCableBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SIGN_UPDATER_CABLE_BE.get(), pos, state);
	}
}