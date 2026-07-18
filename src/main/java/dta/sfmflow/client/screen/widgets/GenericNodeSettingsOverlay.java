package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.NodeSettingsOverlay;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Standard visual settings fallback popup for components with zero
 * configurations.
 */
@OnlyIn(Dist.CLIENT)
public class GenericNodeSettingsOverlay extends NodeSettingsOverlay {
	public GenericNodeSettingsOverlay(ManagerScreen parentScreen, AbstractFlowComponent component) {
		super(parentScreen, component);
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderComponent(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "No configurations available.", getX() + width / 2,
				getY() + height / 2 - 10, 0xFFAAAAAA);
	}
}