package dta.sfmflow.client.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.GradientBlitUtil;
import dta.sfmflow.client.screen.helper.WorkspaceValidator;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.util.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Basic panel base class rendering compact visual cards [3]. Upgraded to
 * dynamically process error and warning indicators and outline highlights [3].
 */
@OnlyIn(Dist.CLIENT)
public class FlowWidgetBase extends AbstractFlowWidget {
	private static final ResourceLocation COMPONENT_MIN_BG = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/component_min_bg.png");
	private static final ResourceLocation ERROR_INDICATOR = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/error_indicator.png");
	private static final ResourceLocation WARNING_INDICATOR = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/warning_indicator.png");

	private final FlowWidgetContainer container;
	private FlowWidgetMoveButton moveButton;
	private FlowWidgetText titleText;

	public FlowWidgetBase(FlowWidgetContainer container, int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
		this.container = container;

		AbstractFlowComponent comp = container.getComponent();

		this.moveButton = new FlowWidgetMoveButton(this, this.getX(), this.getY());
		this.children.add(this.moveButton);

		this.titleText = new FlowWidgetText(container.getParent().getFont(), getX() + 4, getY() + 8, width - 12, 7,
				message, 0.8F, false, () -> {
					Color mask = comp.getColorMask();
					return mask != null ? mask.getHexTextColor() : 4210752;
				});
		this.children.add(this.titleText);
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		AbstractFlowComponent comp = container.getComponent();

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		org.joml.Matrix4f matrix = guiGraphics.pose().last().pose();

		// Check error and warning states dynamically [3]
		boolean hasError = WorkspaceValidator.hasUnboundInventoryError(container.getParent(), comp);
		boolean hasWarning = WorkspaceValidator.hasEmptyFilterVariableWarning(container.getParent(), comp);

		// Adjust title text position depending on error/warning states [3]
		if (hasError || hasWarning) {
			this.titleText.setX(getX() + 10);
		} else {
			this.titleText.setX(getX() + 4);
		}

		GradientBlitUtil.blitWithGradient(matrix, COMPONENT_MIN_BG, getX(), getY(), 64, 20, 0.0F, 0.0F, 64, 20, 64, 20,
				comp.getColorMask());

		// Draw red outline and error indicator if card is in error state [3]
		if (hasError) {
			guiGraphics.renderOutline(getX(), getY(), 64, 20, 0xFFFF0000); // Red card outline [3]

			int vOffset = 0;
			// Check mouse hover over the indicator position [3]
			if (mouseX >= getX() + 4 && mouseX < getX() + 8 && mouseY >= getY() + 3 && mouseY < getY() + 17) {
				vOffset = 14; // Hovered state V offset [3]
			}
			guiGraphics.blit(ERROR_INDICATOR, getX() + 4, getY() + 3, 0, vOffset, 4, 14, 4, 28);
		} else if (hasWarning) {
			// Symmetrically outline the card in bright yellow and blit your warning
			// indicator texture [3]
			guiGraphics.renderOutline(getX(), getY(), 64, 20, 0xFFFED83D); // Yellow warning outline [3]

			int vOffset = 0;
			// Check mouse hover over the indicator position [3]
			if (mouseX >= getX() + 4 && mouseX < getX() + 8 && mouseY >= getY() + 3 && mouseY < getY() + 17) {
				vOffset = 14; // Hovered state V offset [3]
			}
			guiGraphics.blit(WARNING_INDICATOR, getX() + 4, getY() + 3, 0, vOffset, 4, 14, 4, 28);
		}

		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}

		// Render tooltip directly if indicator is hovered [3]
		if (hasError && mouseX >= getX() + 4 && mouseX < getX() + 8 && mouseY >= getY() + 3 && mouseY < getY() + 17) {
			Component errorMsg = Component.translatable("gui.sfmflow.error.unbound_inventory");
			if (comp instanceof ItemTransferComponent transfer) {
				if (transfer.getActiveSidesMask() == 0) {
					errorMsg = Component.translatable("gui.sfmflow.error.no_active_sides");
				} else if (transfer.getInventoryId() != -1 && transfer.isWhitelist()) {
					boolean empty = true;
					for (ItemStack stack : transfer.getFilterItems()) {
						if (stack != null && !stack.isEmpty()) {
							empty = false;
							break;
						}
					}
					if (empty) {
						errorMsg = Component.translatable("gui.sfmflow.error.empty_whitelist");
					}
				}
			}
			guiGraphics.renderTooltip(container.getParent().getFont(), errorMsg, mouseX, mouseY);
		} else if (hasWarning && mouseX >= getX() + 4 && mouseX < getX() + 8 && mouseY >= getY() + 3
				&& mouseY < getY() + 17) {
			// Symmetrically render empty variable warning tooltip [3]
			Component warningMsg = Component.translatable("gui.sfmflow.warning.empty_filter_variable");
			guiGraphics.renderTooltip(container.getParent().getFont(), warningMsg, mouseX, mouseY);
		}
	}

	public FlowWidgetContainer getContainer() {
		return container;
	}

	public FlowWidgetMoveButton getMoveButton() {
		return moveButton;
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