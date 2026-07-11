package dta.sfmflow.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * Custom block context proxy that isolates the target coordinate, bypassing
 * face culling checks against neighboring blocks [3].
 */
@OnlyIn(Dist.CLIENT)
public class CullingBypassBlockGetter implements BlockAndTintGetter {
	private final Level level;
	private final BlockPos centerPos;

	public CullingBypassBlockGetter(Level level, BlockPos centerPos) {
		this.level = level;
		this.centerPos = centerPos;
	}

	@Override
	public float getShade(Direction direction, boolean shade) {
		return 1.0F; // Disables ambient shading [3]
	}

	@Override
	public int getBrightness(net.minecraft.world.level.LightLayer type, BlockPos pos) {
		return 15; // Always return maximum light level (15) to guarantee full-bright GUI scenes [3]
	}

	@Override
	public int getRawBrightness(BlockPos pos, int amount) {
		return 15; // Force full brightness [3]
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return level.getLightEngine();
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver resolver) {
		return level.getBlockTint(pos, resolver);
	}

	@Override
	public BlockState getBlockState(BlockPos queryPos) {
		if (queryPos.equals(centerPos)) {
			return level.getBlockState(queryPos);
		}
		return Blocks.AIR.defaultBlockState(); // Bypasses face culling by mimicking empty neighbor space [3]
	}

	@Override
	public FluidState getFluidState(BlockPos queryPos) {
		if (queryPos.equals(centerPos)) {
			return level.getFluidState(queryPos);
		}
		return Fluids.EMPTY.defaultFluidState();
	}

	@Override
	@Nullable
	public BlockEntity getBlockEntity(BlockPos queryPos) {
		if (queryPos.equals(centerPos)) {
			return level.getBlockEntity(queryPos);
		}
		return null;
	}

	@Override
	public int getHeight() {
		return level.getHeight();
	}

	@Override
	public int getMinBuildHeight() {
		return level.getMinBuildHeight();
	}
}