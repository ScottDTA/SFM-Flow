package dta.sfmflow.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Orchestrator class managing 3D block preview scene layers [3].
 */
@OnlyIn(Dist.CLIENT)
public final class Preview3DRenderer {

	private static final List<RenderType> STANDARD_LAYERS = List.of(
			RenderType.solid(), 
			RenderType.cutoutMipped(), 
			RenderType.cutout(), 
			RenderType.translucent()
	);

	private Preview3DRenderer() {}

	/**
	 * Symmetrical delegation to projection helpers to preserve backward compatibility with existing widgets [3].
	 */
	public static List<Direction> getVisibleFaces(float yaw, float pitch, float scale, int centerX, int centerY) {
		return SceneProjectionHelper.getVisibleFaces(yaw, pitch, scale, centerX, centerY);
	}

	public static SceneProjectionHelper.ProjectedVec getFaceScreenCoords(Direction face, float yaw, float pitch, float scale, int centerX, int centerY) {
		return SceneProjectionHelper.getFaceScreenCoords(face, yaw, pitch, scale, centerX, centerY);
	}

	/**
	 * Renders a full 3D preview of the targeted block and its translucent neighbors [3].
	 */
	public static void render3DScene(GuiGraphics guiGraphics, Level level, BlockPos centerPos, float yaw, float pitch,
			int centerX, int centerY, ISideConfigurable sideModel, Predicate<Direction> sideSupportChecker) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();

		poseStack.translate(centerX, centerY, 80.0F);

		float scale = 40.0F;
		poseStack.scale(scale, -scale, scale);

		poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
		poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

		poseStack.translate(-0.5F, -0.5F, -0.5F);

		BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

		Lighting.setupFor3DItems();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true); 

		Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();

		// 1. Render Center Block using ItemRenderer to guarantee consistent high-brightness & multipart textures [3]
		BlockState centerState = level.getBlockState(centerPos);
		if (!centerState.isAir()) {
			poseStack.pushPose();
			ItemStack itemStack = new ItemStack(centerState.getBlock().asItem());
			if (!itemStack.isEmpty()) {
				poseStack.translate(0.5F, 0.5F, 0.5F);
				if (centerState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
					float yRot = centerState.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
					poseStack.mulPose(Axis.YP.rotationDegrees(-yRot + 180.0F));
				} else if (centerState.hasProperty(BlockStateProperties.FACING)) {
					float yRot = centerState.getValue(BlockStateProperties.FACING).toYRot();
					poseStack.mulPose(Axis.YP.rotationDegrees(-yRot + 180.0F));
				}
				poseStack.scale(2.0F, 2.0F, 2.0F); // Scale item rendering to match 1x1x1 block size [3]
				Minecraft.getInstance().getItemRenderer().renderStatic(itemStack, ItemDisplayContext.FIXED,
						15728880, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0);
			}
			poseStack.popPose();

			bufferSource.endBatch();

			// 2. Render Markers directly on the Center Block's faces
			poseStack.pushPose();
			poseStack.translate(0.5F, 0.5F, 0.5F);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			List<Direction> visibleFaces = getVisibleFaces(yaw, pitch, 40.0F, centerX, centerY);
			for (Direction face : visibleFaces) {
				boolean active = sideModel.isSideActive(face);
				boolean supported = sideSupportChecker.test(face);
				draw3DMarker(poseStack, bufferSource, face, active, supported);
			}
			poseStack.popPose();
		}
		bufferSource.endBatch();

		// 3. Render Translucent Neighbors
		RenderSystem.depthMask(false);

		Lighting.setupFor3DItems();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.depthFunc(515); 
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disablePolygonOffset();
		RenderSystem.disableColorLogicOp();

		GhostRenderWrapper.GhostBufferSource ghostSource = new GhostRenderWrapper.GhostBufferSource(bufferSource, 0.3F);
		GhostRenderWrapper.GhostEntityBufferSource ghostEntitySource = new GhostRenderWrapper.GhostEntityBufferSource(bufferSource, 0.3F);

		// Build a lightweight, compile-safe proxy to force maximum light levels on neighbors [3]
		BlockAndTintGetter neighborBrightLevel = new BlockAndTintGetter() {
			@Override
			public float getShade(Direction direction, boolean shade) {
				return 1.0F; // Disable ambient shading [3]
			}

			@Override
			public int getBrightness(net.minecraft.world.level.LightLayer type, BlockPos pos) {
				return 15; // Force maximum sky and block light level (15) to guarantee full-bright GUI scenes [3]
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
			public BlockState getBlockState(BlockPos pos) {
				return level.getBlockState(pos);
			}

			@Override
			public FluidState getFluidState(BlockPos pos) {
				return level.getFluidState(pos);
			}

			@Override
			@Nullable
			public BlockEntity getBlockEntity(BlockPos pos) {
				return level.getBlockEntity(pos);
			}

			@Override
			public int getHeight() {
				return level.getHeight();
			}

			@Override
			public int getMinBuildHeight() {
				return level.getMinBuildHeight();
			}
		};

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}

					BlockPos currentPos = centerPos.offset(dx, dy, dz);
					BlockState state = level.getBlockState(currentPos);
					if (state.isAir()) {
						continue;
					}

					poseStack.pushPose();
					poseStack.translate(dx, dy, dz);
					if (state.is(ModTags.SPECIAL_3D_RENDERS)) {
						ItemStack itemStack = new ItemStack(state.getBlock().asItem());
						if (!itemStack.isEmpty()) {
							poseStack.translate(0.5F, 0.5F, 0.5F);
							if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
								float yRot = state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
								poseStack.mulPose(Axis.YP.rotationDegrees(-yRot + 180.0F));
							} else if (state.hasProperty(BlockStateProperties.FACING)) {
								float yRot = state.getValue(BlockStateProperties.FACING).toYRot();
								poseStack.mulPose(Axis.YP.rotationDegrees(-yRot + 180.0F));
							}
							poseStack.scale(2.0F, 2.0F, 2.0F);

							RenderSystem.enableBlend();
							RenderSystem.depthMask(true);
							RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.3F);

							Minecraft.getInstance().getItemRenderer().renderStatic(itemStack, ItemDisplayContext.FIXED,
									15728880, OverlayTexture.NO_OVERLAY, poseStack, ghostEntitySource, level, 0);

							bufferSource.endBatch();

							RenderSystem.depthMask(false);
							RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
						}
					} else {
						BakedModel model = blockRenderer.getBlockModel(state);
						
						net.neoforged.neoforge.client.model.data.ModelData neighborModelData = level.getModelData(currentPos);
						if (neighborModelData == null) {
							neighborModelData = net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
						}

						var renderTypes = model.getRenderTypes(state, RandomSource.create(), neighborModelData);
						if (renderTypes.isEmpty()) {
							RenderType chunkType = ItemBlockRenderTypes.getChunkRenderType(state);
							renderNeighborLayer(level, currentPos, state, model, neighborModelData, chunkType, neighborBrightLevel, poseStack, ghostSource);
						} else {
							for (RenderType chunkType : STANDARD_LAYERS) {
								if (renderTypes.contains(chunkType)) {
									renderNeighborLayer(level, currentPos, state, model, neighborModelData, chunkType, neighborBrightLevel, poseStack, ghostSource);
								}
							}
						}
					}
					poseStack.popPose();
				}
			}
		}

		bufferSource.endBatch();

		RenderSystem.disableDepthTest();
		Lighting.setupForFlatItems();

		poseStack.popPose();
	}

	private static void renderLayer(Level level, BlockPos pos, BlockState state, BakedModel model, 
			net.neoforged.neoforge.client.model.data.ModelData modelData, RenderType chunkType, 
			BlockAndTintGetter centerGetter, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
		
		RenderType guiType = SceneRenderTypes.getGuiRenderType(chunkType);
		VertexConsumer consumer = bufferSource.getBuffer(guiType);
		
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().tesselateWithoutAO(
				centerGetter,
				model,
				state,
				pos,
				poseStack,
				consumer,
				false,
				RandomSource.create(),
				state.getSeed(pos),
				15728880,
				modelData,
				chunkType
		);
	}

	private static void renderNeighborLayer(Level level, BlockPos pos, BlockState state, BakedModel model, 
			net.neoforged.neoforge.client.model.data.ModelData modelData, RenderType chunkType, 
			BlockAndTintGetter brightLevel, PoseStack poseStack, GhostRenderWrapper.GhostBufferSource ghostSource) {
		
		VertexConsumer consumer = ghostSource.getBuffer(chunkType);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().tesselateWithoutAO(
				brightLevel,
				model,
				state,
				pos,
				poseStack,
				consumer,
				false,
				RandomSource.create(),
				state.getSeed(pos),
				15728880,
				modelData,
				chunkType
		);
	}

	private static void draw3DQuad(VertexConsumer builder, org.joml.Matrix4f matrix, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g,
			int b) {
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 128);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 128);
		builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, 128);
		builder.addVertex(matrix, x4, y4, z4).setColor(r, g, b, 128);
	}

	private static void draw3DThickLine(VertexConsumer builder, org.joml.Matrix4f matrix, Direction face, 
			float x1, float y1, float z1, 
			float x2, float y2, float z2, 
			float thickness, int r, int g, int b, int a) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float dz = z2 - z1;
		float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len <= 0) return;
		
		float ox = 0, oy = 0, oz = 0;
		
		if (face.getAxis() == Direction.Axis.Y) {
			ox = -dz / len * thickness;
			oz = dx / len * thickness;
		} else if (face.getAxis() == Direction.Axis.Z) {
			ox = -dy / len * thickness;
			oy = dx / len * thickness;
		} else if (face.getAxis() == Direction.Axis.X) {
			oy = -dz / len * thickness;
			oz = dy / len * thickness;
		}
		
		builder.addVertex(matrix, x1 - ox, y1 - oy, z1 - oz).setColor(r, g, b, a);
		builder.addVertex(matrix, x2 - ox, y2 - oy, z2 - oz).setColor(r, g, b, a);
		builder.addVertex(matrix, x2 + ox, y2 + oy, z2 + oz).setColor(r, g, b, a);
		builder.addVertex(matrix, x1 + ox, y1 + oy, z1 + oz).setColor(r, g, b, a);
	}

	private static void draw3DMarker(PoseStack poseStack, MultiBufferSource bufferSource, Direction face,
			boolean active, boolean supported) {
		org.joml.Matrix4f matrix = poseStack.last().pose();
		int r, g, b;
		if (supported) {
			if (active) {
				r = 57; g = 255; b = 20;
			} else {
				r = 255; g = 0; b = 0;
			}
		} else {
			r = 255; g = 0; b = 0;
		}

		if (supported) {
			VertexConsumer builder = bufferSource.getBuffer(SceneRenderTypes.MARKER_RENDER_TYPE);
			switch (face) {
			case UP -> draw3DQuad(builder, matrix, -0.25F, 0.505F, -0.25F, -0.25F, 0.505F, 0.25F, 0.25F, 0.505F, 0.25F,
					0.25F, 0.505F, -0.25F, r, g, b);
			case DOWN -> draw3DQuad(builder, matrix, -0.25F, -0.505F, -0.25F, 0.25F, -0.505F, -0.25F, 0.25F, -0.505F,
					0.25F, -0.25F, -0.505F, 0.25F, r, g, b);
			case NORTH -> draw3DQuad(builder, matrix, -0.25F, -0.25F, -0.505F, 0.25F, -0.25F, -0.505F, 0.25F, 0.25F,
					-0.505F, -0.25F, 0.25F, -0.505F, r, g, b);
			case SOUTH -> draw3DQuad(builder, matrix, -0.25F, -0.25F, 0.505F, -0.25F, 0.25F, 0.505F, 0.25F, 0.25F,
					0.505F, 0.25F, -0.25F, 0.505F, r, g, b);
			case WEST -> draw3DQuad(builder, matrix, -0.505F, -0.25F, -0.25F, -0.505F, -0.25F, 0.25F, -0.505F, 0.25F,
					0.25F, -0.505F, 0.25F, -0.25F, r, g, b);
			case EAST -> draw3DQuad(builder, matrix, 0.505F, -0.25F, -0.25F, 0.505F, 0.25F, -0.25F, 0.505F, 0.25F,
					0.25F, 0.505F, -0.25F, 0.25F, r, g, b);
			}
		} else {
			VertexConsumer builder = bufferSource.getBuffer(SceneRenderTypes.MARKER_RENDER_TYPE);
			float t = 0.02F;
			switch (face) {
			case UP -> {
				draw3DThickLine(builder, matrix, face, -0.25F, 0.505F, -0.25F, 0.25F, 0.505F, 0.25F, t, r, g, b, 255);
				draw3DThickLine(builder, matrix, face, -0.25F, 0.505F, 0.25F, 0.25F, 0.505F, -0.25F, t, r, g, b, 255);
			}
			case DOWN -> {
				draw3DThickLine(builder, matrix, face, -0.25F, -0.505F, -0.25F, 0.25F, -0.505F, 0.25F, t, r, g, b, 255);
				draw3DThickLine(builder, matrix, face, -0.25F, -0.505F, 0.25F, 0.25F, -0.505F, -0.25F, t, r, g, b, 255);
			}
			case NORTH -> {
				draw3DThickLine(builder, matrix, face, -0.25F, -0.25F, -0.505F, 0.25F, 0.25F, -0.505F, t, r, g, b, 255);
				draw3DThickLine(builder, matrix, face, -0.25F, 0.25F, -0.505F, 0.25F, -0.25F, -0.505F, t, r, g, b, 255);
			}
			case SOUTH -> {
				draw3DThickLine(builder, matrix, face, -0.25F, -0.25F, 0.505F, 0.25F, 0.25F, 0.505F, t, r, g, b, 255);
				draw3DThickLine(builder, matrix, face, -0.25F, 0.25F, 0.505F, 0.25F, -0.25F, 0.505F, t, r, g, b, 255);
			}
			case WEST -> {
				draw3DThickLine(builder, matrix, face, -0.505F, -0.25F, -0.25F, -0.505F, 0.25F, 0.25F, t, r, g, b, 255);
				draw3DThickLine(builder, matrix, face, -0.505F, -0.25F, 0.25F, -0.505F, 0.25F, -0.25F, t, r, g, b, 255);
			}
			case EAST -> {
				draw3DThickLine(builder, matrix, face, 0.505F, -0.25F, -0.25F, 0.505F, 0.25F, 0.25F, t, r, g, b, 255);
				draw3DThickLine(builder, matrix, face, 0.505F, -0.25F, 0.25F, 0.505F, 0.25F, -0.25F, t, r, g, b, 255);
			}
			}
		}
	}
}