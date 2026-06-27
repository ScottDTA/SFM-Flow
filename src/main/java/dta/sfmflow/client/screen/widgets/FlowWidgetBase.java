package dta.sfmflow.client.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.GradientBlitUtil;
import dta.sfmflow.util.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Basic panel base class rendering compact visual cards [3].
 */
@OnlyIn(Dist.CLIENT)
public class FlowWidgetBase extends AbstractFlowWidget {
	private static final ResourceLocation COMPONENT_MIN_BG = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/component_min_bg.png");

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

		// Purged: No FlowWidgetOpenCloseButton added here, keeping visual footprint
		// locked [3]
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		AbstractFlowComponent comp = container.getComponent();

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		org.joml.Matrix4f matrix = guiGraphics.pose().last().pose();

		// Symmetrical visual bounds: Render min background only [3]
		GradientBlitUtil.blitWithGradient(matrix, COMPONENT_MIN_BG, getX(), getY(), 64, 20, 0.0F, 0.0F, 64, 20, 64, 20,
				comp.getColorMask());

		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
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