package dta.sfmflow.client.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.GradientBlitUtil;
import dta.sfmflow.util.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Handle button used to drag and move component containers across the canvas
 * workspace [3].
 */
@OnlyIn(Dist.CLIENT)
public class FlowWidgetMoveButton extends AbstractFlowWidget {
	private static final ResourceLocation MOVE_BUTTON = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/move_button.png");
	private final FlowWidgetBase parent;

	public FlowWidgetMoveButton(FlowWidgetBase parent, int x, int y) {
		super(x, y, 6, 6, Component.literal("FlowComponentMoveButton"));
		this.parent = parent;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		AbstractFlowComponent comp = parent.getContainer().getComponent();
		Color mask = comp.getColorMask();

		float[] colors = GradientBlitUtil.getBottomColorComponents(mask);
		RenderSystem.setShaderColor(colors[0], colors[1], colors[2], 1.0F);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, MOVE_BUTTON);
		guiGraphics.blit(MOVE_BUTTON, getX(), getY(), 0, 0, 6, 6, 6, 6);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset shader color
	}

	public FlowWidgetBase getParent() {
		return parent;
	}
}