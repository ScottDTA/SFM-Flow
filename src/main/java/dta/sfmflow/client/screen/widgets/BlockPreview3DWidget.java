package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.client.render.HighlightManager;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Reusable 3D block preview widget rendering a central targeted block and its
 * adjacent neighbors [3]. Supports right-click mouse dragging to orbit,
 * left-click face toggles, shift-left-click slot configuration, and integrated
 * highlighting [3].
 */
@OnlyIn(Dist.CLIENT)
public class BlockPreview3DWidget extends AbstractFlowWidget {
	private final Supplier<BlockPos> posSupplier;
	private final ISideConfigurable sideModel;
	private final Predicate<Direction> sideSupportChecker;
	private final ManagerScreen parentScreen;
	private final Checkbox highlightCheckbox;
	private final Runnable onChanged;

	private float yawRotation = -45.0F;
	private float pitchRotation = 30.0F;

	public BlockPreview3DWidget(int x, int y, int width, int height, Supplier<BlockPos> posSupplier,
			ISideConfigurable sideModel, Predicate<Direction> sideSupportChecker, ManagerScreen parentScreen,
			Runnable onChanged) {
		super(x, y, width, height, Component.literal("3D Preview"));
		this.posSupplier = posSupplier;
		this.sideModel = sideModel;
		this.sideSupportChecker = sideSupportChecker;
		this.parentScreen = parentScreen;
		this.onChanged = onChanged;

		// Initialize integrated highlights checkbox at local (4, 4) offsets [3]
		this.highlightCheckbox = Checkbox.builder(Component.empty(), parentScreen.getFont()).pos(getX() + 4, getY() + 4)
				.selected(false).onValueChange((checkbox, selected) -> {
					BlockPos currentPos = posSupplier.get();
					if (currentPos != null) {
						if (selected) {
							HighlightManager.addHighlight(currentPos);
						} else {
							HighlightManager.removeHighlight(currentPos);
						}
						this.onChanged.run();
					}
				}).build();
		this.children.add(new ApiWidgetAdapter<>(this.highlightCheckbox));

		updateHighlightState();
	}

	/**
	 * Reactive helper that synchronizes the checkbox selections state to the
	 * HighlightManager [3].
	 */
	public void updateHighlightState() {
		BlockPos currentPos = posSupplier.get();
		boolean highlighted = currentPos != null && HighlightManager.isHighlighted(currentPos);
		setCheckboxSelected(this.highlightCheckbox, highlighted);
	}

	private void setCheckboxSelected(Checkbox checkbox, boolean selected) {
		try {
			java.lang.reflect.Field field = Checkbox.class.getDeclaredField("selected");
			field.setAccessible(true);
			field.setBoolean(checkbox, selected);
		} catch (Exception e) {
			try {
				for (java.lang.reflect.Field field : Checkbox.class.getDeclaredFields()) {
					if (field.getType() == boolean.class) {
						field.setAccessible(true);
						field.setBoolean(checkbox, selected);
						break;
					}
				}
			} catch (Exception ex) {
				dta.sfmflow.SFMFlow.LOGGER.error("Failed to set checkbox state", ex);
			}
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.visible && mouseX >= getX() && mouseX < getX() + width && mouseY >= getY()
				&& mouseY < getY() + height;
	}

	private Vec2 getFaceScreenCoords(Direction face, float yaw, float pitch, float scale, int centerX, int centerY) {
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

		return new Vec2(screenX, screenY, (float) z2);
	}

	private List<Direction> getVisibleFaces() {
		int centerX = getX() + width / 2;
		int centerY = getY() + height / 2;

		List<FaceProj> faceList = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			Vec2 proj = getFaceScreenCoords(dir, yawRotation, pitchRotation, 40.0F, centerX, centerY);
			faceList.add(new FaceProj(dir, proj));
		}

		faceList.sort((f1, f2) -> Float.compare(f2.proj.z, f1.proj.z));

		List<Direction> visibleFaces = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			visibleFaces.add(faceList.get(i).face);
		}
		return visibleFaces;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active || !isMouseOver(mouseX, mouseY)) {
			return false;
		}

		// Check the checkbox click first [3]
		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}

		// Consume right-click to capture focusedChild status for dragging orbits [3]
		if (button == 1) {
			this.setDragging(true); // Enable local widget drag tracking [3]
			return true;
		}

		BlockPos centerPos = posSupplier.get();
		if (button == 0 && centerPos != null) {
			int centerX = getX() + width / 2;
			int centerY = getY() + height / 2;
			List<Direction> visibleFaces = getVisibleFaces();

			for (Direction face : visibleFaces) {
				Vec2 proj = getFaceScreenCoords(face, yawRotation, pitchRotation, 40.0F, centerX, centerY);
				double dx = mouseX - proj.x;
				double dy = mouseY - proj.y;
				if (dx * dx + dy * dy <= 36.0) { // 6px hit detection radius [3]
					if (sideSupportChecker.test(face)) {
						if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
							openSlotLayoutGui(face);
						} else {
							sideModel.toggleSide(face);
							this.onChanged.run();

							Minecraft.getInstance().getSoundManager()
									.play(net.minecraft.client.resources.sounds.SimpleSoundInstance
											.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
						}
						return true;
					}
				}
			}
		}

		return false;
	}

	private void openSlotLayoutGui(Direction face) {
		SlotLayoutModalPopup popup = new SlotLayoutModalPopup(this.parentScreen, this.sideModel, face,
				this.posSupplier.get(), this.onChanged);
		this.parentScreen.setActiveModalPopup(popup);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 1) {
			this.setDragging(false); // Reset local widget drag tracking [3]
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!this.visible || !this.active) {
			return false;
		}

		// Dragging right click orbits the camera [3]
		if (button == 1) {
			this.yawRotation += (float) dragX * 0.8F;
			this.pitchRotation = net.minecraft.util.Mth.clamp(this.pitchRotation + (float) dragY * 0.8F, -90.0F, 90.0F);
			return true;
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Sunken backdrop fill [3]
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF151515);

		// RECESS BEVEL: Top and Left are #555555, Bottom and Right are FFFFFF [3]
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF555555);
		guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, 0xFF555555);
		guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFFFFFFFF);
		guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0xFFFFFFFF);

		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		BlockPos centerPos = posSupplier.get();

		if (centerPos != null && level != null && level.hasChunkAt(centerPos)) {
			render3DScene(guiGraphics, level, centerPos);
		} else {
			guiGraphics.drawCenteredString(parentScreen.getFont(), "NO", getX() + width / 2, getY() + height / 2 - 10,
					0xFF8B8B8B);
			guiGraphics.drawCenteredString(parentScreen.getFont(), "PREVIEW", getX() + width / 2,
					getY() + height / 2 + 2, 0xFF8B8B8B);
		}

		// Draw checkbox text label at 75% scale to fit the box corner nicely [3]
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(getX() + 22, getY() + 9, 0);
		guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("In-World Highlight"), 0, 0, 0xFFAAAAAA,
				false);
		guiGraphics.pose().popPose();

		// Render the checkbox child widget [3]
		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}
	}

	private void draw3DQuad(VertexConsumer builder, org.joml.Matrix4f matrix, float x1, float y1, float z1, float x2,
			float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int b) {
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 255);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 255);
		builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, 255);
		builder.addVertex(matrix, x4, y4, z4).setColor(r, g, b, 255);
	}

	private void draw3DLine(VertexConsumer builder, org.joml.Matrix4f matrix, Direction face, float x1, float y1,
			float z1, float x2, float y2, float z2, int r, int g, int b) {
		float nx = face.getStepX();
		float ny = face.getStepY();
		float nz = face.getStepZ();
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 255).setNormal(nx, ny, nz);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 255).setNormal(nx, ny, nz);
	}

	private void draw3DMarker(PoseStack poseStack, MultiBufferSource bufferSource, Direction face, boolean active,
			boolean supported) {
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
			// Using debugQuads() so our 4-vertex shapes render as solid squares [3]
			VertexConsumer builder = bufferSource.getBuffer(RenderType.debugQuads());
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
				draw3DLine(builder, matrix, face, -0.25F, 0.25F, 0.505F, 0.25F, -0.25F, 0.505F, r, g, b);
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

	private void render3DScene(GuiGraphics guiGraphics, Level level, BlockPos centerPos) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();

		int centerX = getX() + width / 2;
		int centerY = getY() + height / 2;
		poseStack.translate(centerX, centerY, 150.0F);

		float scale = 40.0F;
		poseStack.scale(scale, -scale, scale);

		poseStack.mulPose(Axis.XP.rotationDegrees(pitchRotation));
		poseStack.mulPose(Axis.YP.rotationDegrees(yawRotation));

		poseStack.translate(-0.5F, -0.5F, -0.5F);

		BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

		com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
		com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

		BlockState centerState = level.getBlockState(centerPos);
		if (!centerState.isAir()) {
			poseStack.pushPose();
			blockRenderer.renderSingleBlock(centerState, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();

			poseStack.pushPose();
			poseStack.translate(0.5F, 0.5F, 0.5F);
			List<Direction> visibleFaces = getVisibleFaces();
			for (Direction face : visibleFaces) {
				boolean active = sideModel.isSideActive(face);
				boolean supported = sideSupportChecker.test(face);
				draw3DMarker(poseStack, bufferSource, face, active, supported);
			}
			poseStack.popPose();
		}
		bufferSource.endBatch();

		com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
		GhostBufferSource ghostSource = new GhostBufferSource(bufferSource, 0.3F);

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
					blockRenderer.renderSingleBlock(state, poseStack, ghostSource, 15728880, OverlayTexture.NO_OVERLAY);
					poseStack.popPose();
				}
			}
		}

		bufferSource.endBatch();

		com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
		com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
		com.mojang.blaze3d.platform.Lighting.setupForFlatItems();

		poseStack.popPose();
	}

	@Override
	public void setX(int x) {
		int dif = this.getX() - x;
		super.setX(x);
		updateChildrenXPositions(dif);
	}

	@Override
	public void setY(int y) {
		int dif = this.getY() - y;
		super.setY(y);
		updateChildrenYPositions(dif);
	}

	private static class Vec2 {
		public final float x;
		public final float y;
		public final float z;

		public Vec2(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static class FaceProj {
		public final Direction face;
		public final Vec2 proj;

		public FaceProj(Direction face, Vec2 proj) {
			this.face = face;
			this.proj = proj;
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class GhostBufferSource implements net.minecraft.client.renderer.MultiBufferSource {
		private final net.minecraft.client.renderer.MultiBufferSource delegate;
		private final float alpha;

		public GhostBufferSource(net.minecraft.client.renderer.MultiBufferSource delegate, float alpha) {
			this.delegate = delegate;
			this.alpha = alpha;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(net.minecraft.client.renderer.RenderType renderType) {
			return new GhostVertexConsumer(delegate.getBuffer(net.minecraft.client.renderer.RenderType.translucent()),
					alpha);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class GhostVertexConsumer implements com.mojang.blaze3d.vertex.VertexConsumer {
		private final com.mojang.blaze3d.vertex.VertexConsumer delegate;
		private final float ghostAlpha;

		public GhostVertexConsumer(com.mojang.blaze3d.vertex.VertexConsumer delegate, float ghostAlpha) {
			this.delegate = delegate;
			this.ghostAlpha = ghostAlpha;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) {
			delegate.setColor(r, g, b, (int) (a * ghostAlpha));
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(int color) {
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
		public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(float r, float g, float b, float a) {
			delegate.setColor(r, g, b, a * ghostAlpha);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setLight(int light) {
			delegate.setLight(light);
			return this;
		}
	}
}