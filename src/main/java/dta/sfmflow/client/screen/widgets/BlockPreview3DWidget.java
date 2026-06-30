package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.client.render.HighlightManager;
import dta.sfmflow.client.render.Preview3DRenderer;
import dta.sfmflow.client.screen.ManagerScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Reusable 3D block preview widget rendering a central targeted block and its
 * adjacent neighbors [3]. Delegates mathematical projections and voxel
 * renderings cleanly to Preview3DRenderer [3].
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

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active || !isMouseOver(mouseX, mouseY)) {
			return false;
		}

		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}

		if (button == 1) {
			this.setDragging(true);
			return true;
		}

		BlockPos centerPos = posSupplier.get();
		if (button == 0 && centerPos != null) {
			int centerX = getX() + width / 2;
			int centerY = getY() + height / 2;
			List<Direction> visibleFaces = Preview3DRenderer.getVisibleFaces(yawRotation, pitchRotation, 40.0F, centerX,
					centerY);

			for (Direction face : visibleFaces) {
				Preview3DRenderer.ProjectedVec proj = Preview3DRenderer.getFaceScreenCoords(face, yawRotation,
						pitchRotation, 40.0F, centerX, centerY);
				double dx = mouseX - proj.x();
				double dy = mouseY - proj.y();
				if (dx * dx + dy * dy <= 36.0) {
					if (sideSupportChecker.test(face)) {
						if (Screen.hasShiftDown()) {
							openSlotLayoutGui(face);
						} else {
							sideModel.toggleSide(face);
							this.onChanged.run();

							Minecraft.getInstance().getSoundManager()
									.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
			this.setDragging(false);
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!this.visible || !this.active) {
			return false;
		}

		if (button == 1) {
			this.yawRotation += (float) dragX * 0.8F;
			this.pitchRotation = Mth.clamp(this.pitchRotation + (float) dragY * 0.8F, -90.0F, 90.0F);
			return true;
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF151515);

		guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF555555);
		guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, 0xFF555555);
		guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFFFFFFFF);
		guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0xFFFFFFFF);

		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		BlockPos centerPos = posSupplier.get();

		int centerX = getX() + width / 2;
		int centerY = getY() + height / 2;

		if (centerPos != null && level != null && level.hasChunkAt(centerPos)) {
			Preview3DRenderer.render3DScene(guiGraphics, level, centerPos, yawRotation, pitchRotation, centerX, centerY,
					sideModel, sideSupportChecker);
			// Restore standard depth mask and depth test states to prevent polygon sorting conflicts on subsequently rendered 3D GUI items [3]
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
		} else {
			guiGraphics.drawCenteredString(parentScreen.getFont(), "NO", centerX, centerY - 10, 0xFF8B8B8B);
			guiGraphics.drawCenteredString(parentScreen.getFont(), "PREVIEW", centerX, centerY + 2, 0xFF8B8B8B);
		}

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(getX() + 22, getY() + 9, 0);
		guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("In-World Highlight"), 0, 0, 0xFFAAAAAA,
				false);
		guiGraphics.pose().popPose();

		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}
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
}