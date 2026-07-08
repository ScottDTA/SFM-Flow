package dta.sfmflow.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

/**
 * Client-only 3D rendering assistant isolating mathematical calculations,
 * rotation projections, and translucent scene layers from visual interface
 * components [3].
 */
@OnlyIn(Dist.CLIENT)
public final class Preview3DRenderer {

	// Custom RenderType ignoring both depth writes and depth testing [3]
	private static final RenderType MARKER_RENDER_TYPE = RenderType.create("sfm_marker",
			DefaultVertexFormat.POSITION_COLOR, Mode.QUADS, 256, false, true,
			RenderType.CompositeState.builder()
					.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(new RenderStateShard.CullStateShard(false))
					.setWriteMaskState(RenderStateShard.COLOR_WRITE) // Disables depth writes [3]
					.setDepthTestState(RenderStateShard.NO_DEPTH_TEST) // Disables depth testing [3]
					.createCompositeState(false));

	// Custom RenderType for ghost blocks that disables depth writing and binds
	// standard LIGHTMAP values [3]
	private static final RenderType GHOST_BLOCK_RENDER_TYPE = RenderType.create("sfm_ghost_block",
			DefaultVertexFormat.BLOCK, Mode.QUADS, 2097152, true, true,
			RenderType.CompositeState.builder()
					.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTranslucentShader))
					.setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(new RenderStateShard.CullStateShard(false))
					.setLightmapState(RenderStateShard.LIGHTMAP) // Restores environmental lighting [3]
					.setWriteMaskState(RenderStateShard.COLOR_WRITE) // Disables depth writes [3]
					.createCompositeState(false));

	private Preview3DRenderer() {
	}

	/**
	 * Represents a 3D projected coordinate mapping vector [3].
	 */
	public record ProjectedVec(float x, float y, float z) {
	}

	/**
	 * Pairs a direction face with its corresponding 3D projected vector [3].
	 */
	public record ProjectedFace(Direction face, ProjectedVec proj) {
	}

	/**
	 * Projects 3D face coordinates onto 2D screen spaces based on yaw and pitch
	 * angles [3].
	 *
	 * @param face    the block face to evaluate [3]
	 * @param yaw     the camera horizontal orbit angle [3]
	 * @param pitch   the camera vertical orbit angle [3]
	 * @param scale   the camera zoom scalar factor [3]
	 * @param centerX horizontal screen origin coordinate [3]
	 * @param centerY vertical screen origin coordinate [3]
	 * @return projected 3D coordinates vector [3]
	 */
	public static ProjectedVec getFaceScreenCoords(Direction face, float yaw, float pitch, float scale, int centerX,
			int centerY) {
		float x = face.getStepX() * 0.5F;
		float y = face.getStepY() * 0.5F;
		float z = face.getStepZ() * 0.5F;

		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);

		double x1 = x * Math.cos(yawRad) + z * Math.sin(yawRad);
		double y1 = y;
		double z1 = -x * Math.sin(yawRad) + z * Math.cos(yawRad);

		double x2 = x1;
		double y2 = y1 * Math.cos(pitchRad) - z1 * Math.sin(pitchRad);
		double z2 = y1 * Math.sin(pitchRad) + z1 * Math.cos(pitchRad);

		float screenX = centerX + (float) (x2 * scale);
		float screenY = centerY - (float) (y2 * scale);

		return new ProjectedVec(screenX, screenY, (float) z2);
	}

	/**
	 * Evaluates visible faces from front to back using Z-depth camera sorting [3].
	 *
	 * @param yaw     horizontal camera orbit [3]
	 * @param pitch   vertical camera orbit [3]
	 * @param scale   camera zoom scale factor [3]
	 * @param centerX horizontal viewport offset [3]
	 * @param centerY vertical viewport offset [3]
	 * @return sorted list of 3 closest faces [3]
	 */
	public static List<Direction> getVisibleFaces(float yaw, float pitch, float scale, int centerX, int centerY) {
		List<ProjectedFace> faceList = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			ProjectedVec proj = getFaceScreenCoords(dir, yaw, pitch, scale, centerX, centerY);
			faceList.add(new ProjectedFace(dir, proj));
		}

		faceList.sort((f1, f2) -> Float.compare(f2.proj().z(), f1.proj().z()));

		List<Direction> visibleFaces = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			visibleFaces.add(faceList.get(i).face());
		}
		return visibleFaces;
	}

	/**
	 * Renders a full 3D block preview block and its translucent neighbors with face
	 * markers [3].
	 */
	public static void render3DScene(GuiGraphics guiGraphics, Level level, BlockPos centerPos, float yaw, float pitch,
			int centerX, int centerY, ISideConfigurable sideModel, Predicate<Direction> sideSupportChecker) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();

		// Reduced Z-translation to 80.0F to prevent depth conflicts with overlays [3]
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

		// Build a lightweight, compile-safe proxy to force maximum light levels [3]
		BlockAndTintGetter brightLevel = new BlockAndTintGetter() {
			@Override
			public float getShade(Direction direction, boolean shade) {
				return 1.0F; // Return 1.0F to completely disable face shadow shading [3]
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

			@Override
			public int getBrightness(LightLayer lightLayer, BlockPos pos) {
				return 15; // Force maximum brightness [3]
			}

			@Override
			public int getRawBrightness(BlockPos pos, int amount) {
				return 15; // Force maximum brightness [3]
			}
		};

		BlockState centerState = level.getBlockState(centerPos);
		if (!centerState.isAir()) {
			poseStack.pushPose();
			if (centerState.is(ModTags.SPECIAL_3D_RENDERS)) {
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
					poseStack.scale(2.0F, 2.0F, 2.0F);
					Minecraft.getInstance().getItemRenderer().renderStatic(itemStack, ItemDisplayContext.FIXED,
							15728880, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0);
				}
			} else {
				// Use brightLevel proxy and invoke tesselateWithoutAO to completely bypass neighboring Ambient Occlusion pollution [3]
				BakedModel model = blockRenderer.getBlockModel(centerState);
				RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(centerState);
				VertexConsumer consumer = bufferSource.getBuffer(renderType);
				blockRenderer.getModelRenderer().tesselateWithoutAO(
						brightLevel,
						model,
						centerState,
						centerPos,
						poseStack,
						consumer,
						false,
						RandomSource.create(),
						centerState.getSeed(centerPos),
						15728880,						
						ModelData.EMPTY,
						renderType
				);
			}
			poseStack.popPose();

			// Force-flush the central block immediately so it is drawn to the frame/depth
			// buffers before the markers [3]
			bufferSource.endBatch();

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

		RenderSystem.depthMask(false);

		// =========================================================================
		// PRISTINE OPENGL & SHADER STATE RESTORATION MATRIX [3]
		// Fully shields standard ghost blocks from external mod render state leaks.
		// =========================================================================
		Lighting.setupFor3DItems();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.depthFunc(515); // Reset depth function to GL_LEQUAL
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disablePolygonOffset();
		RenderSystem.disableColorLogicOp();
		// =========================================================================

		GhostBufferSource ghostSource = new GhostBufferSource(bufferSource, 0.3F);
		GhostEntityBufferSource ghostEntitySource = new GhostEntityBufferSource(bufferSource, 0.3F);

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

							ResourceLocation texture = getChestTexture(state);
							RenderType actualType = RenderType.entityTranslucentCull(texture);

							Minecraft.getInstance().getItemRenderer().renderStatic(itemStack, ItemDisplayContext.FIXED,
									15728880, OverlayTexture.NO_OVERLAY, poseStack, ghostEntitySource, level, 0);

							bufferSource.endBatch(actualType);

							RenderSystem.depthMask(false);
							// Restore standard shader color after drawing each special block [3]
							RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
						}
					} else {
						// Use brightLevel proxy and invoke tesselateWithoutAO to completely bypass neighboring Ambient Occlusion pollution [3]
						BakedModel model = blockRenderer.getBlockModel(state);
						RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
						VertexConsumer consumer = ghostSource.getBuffer(renderType);
						blockRenderer.getModelRenderer().tesselateWithoutAO(
								brightLevel,
								model,
								state,
								currentPos,
								poseStack,
								consumer,
								false,
								RandomSource.create(),
								state.getSeed(currentPos),
								15728880,
								ModelData.EMPTY,
								renderType
						);
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

	private static void draw3DQuad(VertexConsumer builder, Matrix4f matrix, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g,
			int b) {
		// Set to 128 (50% alpha) to increase text and orientation visibility underneath
		// [3]
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 128);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 128);
		builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, 128);
		builder.addVertex(matrix, x4, y4, z4).setColor(r, g, b, 128);
	}

	private static void draw3DLine(VertexConsumer builder, Matrix4f matrix, Direction face, float x1, float y1,
			float z1, float x2, float y2, float z2, int r, int g, int b) {
		float nx = face.getStepX();
		float ny = face.getStepY();
		float nz = face.getStepZ();
		// Set to 128 (50% alpha) to increase text and orientation visibility underneath
		// [3]
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 128).setNormal(nx, ny, nz);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 128).setNormal(nx, ny, nz);
	}

	private static void draw3DMarker(PoseStack poseStack, MultiBufferSource bufferSource, Direction face,
			boolean active, boolean supported) {
		org.joml.Matrix4f matrix = poseStack.last().pose();
		int r, g, b;
		if (supported) {
			if (active) {
				r = 57;
				g = 255;
				b = 20;
			} else {
				r = 255;
				g = 0;
				b = 0;
			}
		} else {
			r = 255;
			g = 0;
			b = 0;
		}

		if (supported) {
			// Use our custom MARKER_RENDER_TYPE to properly support alpha transparency
			// blending [3]
			VertexConsumer builder = bufferSource.getBuffer(MARKER_RENDER_TYPE);
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
			VertexConsumer builder = bufferSource.getBuffer(RenderType.lines());
			switch (face) {
			case UP -> {
				draw3DLine(builder, matrix, face, -0.25F, 0.505F, -0.25F, 0.25F, 0.505F, 0.25F, r, g, b);
				draw3DLine(builder, matrix, face, -0.25F, 0.505F, 0.25F, 0.25F, 0.505F, -0.25F, r, g, b);
			}
			case DOWN -> {
				draw3DLine(builder, matrix, face, -0.25F, -0.505F, -0.25F, 0.25F, -0.505F, 0.25F, r, g, b);
				draw3DLine(builder, matrix, face, -0.25F, -0.505F, 0.25F, 0.25F, -0.505F, -0.25F, r, g, b);
			}
			case NORTH -> {
				draw3DLine(builder, matrix, face, -0.25F, -0.25F, -0.505F, 0.25F, 0.25F, -0.505F, r, g, b);
				draw3DLine(builder, matrix, face, -0.25F, 0.25F, -0.505F, 0.25F, -0.25F, -0.505F, r, g, b);
			}
			case SOUTH -> {
				draw3DLine(builder, matrix, face, -0.25F, -0.25F, 0.505F, 0.25F, 0.25F, 0.505F, r, g, b);
				draw3DLine(builder, matrix, face, -0.25F, 0.25F, 0.505F, 0.25F, -0.25F, -0.505F, r, g, b);
			}
			case WEST -> {
				draw3DLine(builder, matrix, face, -0.505F, -0.25F, -0.25F, -0.505F, 0.25F, 0.25F, r, g, b);
				draw3DLine(builder, matrix, face, -0.505F, -0.25F, 0.25F, -0.505F, 0.25F, -0.25F, r, g, b);
			}
			case EAST -> {
				draw3DLine(builder, matrix, face, 0.505F, -0.25F, -0.25F, 0.505F, 0.25F, 0.25F, r, g, b);
				draw3DLine(builder, matrix, face, 0.505F, -0.25F, 0.25F, 0.505F, 0.25F, -0.25F, r, g, b);
			}
			}
		}
	}

	private static ResourceLocation getChestTexture(BlockState state) {
		if (state.is(Blocks.TRAPPED_CHEST)) {
			return ResourceLocation.withDefaultNamespace("textures/entity/chest/trapped.png");
		}
		if (state.is(Blocks.ENDER_CHEST)) {
			return ResourceLocation.withDefaultNamespace("textures/entity/chest/ender.png");
		}
		return ResourceLocation.withDefaultNamespace("textures/entity/chest/normal.png");
	}

	@OnlyIn(Dist.CLIENT)
	private static class GhostEntityBufferSource implements MultiBufferSource {
		private final MultiBufferSource delegate;
		private final float alpha;

		public GhostEntityBufferSource(MultiBufferSource delegate, float alpha) {
			this.delegate = delegate;
			this.alpha = alpha;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			RenderType actualType = renderType;
			String str = renderType.toString();
			ResourceLocation texture = null;
			int startIdx = str.indexOf("texture[");
			if (startIdx != -1) {
				int endIdx = str.indexOf("]", startIdx);
				if (endIdx != -1) {
					String sub = str.substring(startIdx + 8, endIdx);
					if (sub.startsWith("Optional[")) {
						sub = sub.substring(9, sub.length() - 1);
					}
					if (!sub.startsWith("Optional.empty")) {
						texture = ResourceLocation.tryParse(sub);
					}
				}
			}

			if (texture != null) {
				String name = renderType.toString().toLowerCase(Locale.ROOT);
				if (name.contains("entity_solid") || name.contains("entity_cutout")
						|| name.contains("entity_cutout_no_mips")) {
					actualType = RenderType.entityTranslucentCull(Sheets.CHEST_SHEET);
				}
			}
			return new GhostVertexConsumer(delegate.getBuffer(actualType), alpha);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class GhostBufferSource implements MultiBufferSource {
		private final MultiBufferSource delegate;
		private final float alpha;

		public GhostBufferSource(MultiBufferSource delegate, float alpha) {
			this.delegate = delegate;
			this.alpha = alpha;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			return new GhostVertexConsumer(delegate.getBuffer(GHOST_BLOCK_RENDER_TYPE), alpha);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class GhostVertexConsumer implements VertexConsumer {
		private final VertexConsumer delegate;
		private final float ghostAlpha;

		public GhostVertexConsumer(VertexConsumer delegate, float ghostAlpha) {
			this.delegate = delegate;
			this.ghostAlpha = ghostAlpha;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			delegate.setColor(r, g, b, (int) (a * ghostAlpha));
			return this;
		}

		@Override
		public VertexConsumer setColor(int color) {
			int a = (color >> 24) & 0xFF;
			int r = (color >> 16) & 0xFF;
			int g = (color >> 8) & 0xFF;
			int b = color & 0xFF;
			int newA = (int) (a * ghostAlpha);
			int newColor = (newA << 24) | (r << 16) | (g << 8) | b;
			delegate.setColor(newColor);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(float r, float g, float b, float a) {
			delegate.setColor(r, g, b, a * ghostAlpha);
			return this;
		}

		@Override
		public VertexConsumer setLight(int light) {
			delegate.setLight(light);
			return this;
		}

		@Override
		public VertexConsumer setOverlay(int overlay) {
			delegate.setOverlay(overlay);
			return this;
		}
	}
}