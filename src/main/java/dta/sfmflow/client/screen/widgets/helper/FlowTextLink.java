package dta.sfmflow.client.screen.widgets.helper;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Compact side-safe clickable text widget for lists and dropdown overlays [3].
 * Inverts styling on cursor hovering to create responsive highlighted options
 * [3].
 */
@OnlyIn(Dist.CLIENT)
public class FlowTextLink extends AbstractFlowWidget {
	private final Runnable action;

	/**
	 * Instantiates the FlowTextLink with a fixed dimension of 84x14px [3].
	 *
	 * @param x      coordinate placement [3]
	 * @param y      coordinate placement [3]
	 * @param label  localized display text [3]
	 * @param action execution block triggered on click [3]
	 */
	public FlowTextLink(int x, int y, Component label, Runnable action) {
		super(x, y, 84, 14, label);
		this.action = action;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		boolean hovered = actuallyHovered(mouseX, mouseY);
		Font font = Minecraft.getInstance().font;

		if (hovered) {
			// Hovered state: Solid gold background, inverted charcoal black text with
			// shadow disabled [3]
			guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFFD4AF37);
			guiGraphics.drawString(font, getMessage(), getX() + 4, getY() + 3, 0xFF111111, false);
		} else {
			// Default state: Transparent background, golden text [3]
			guiGraphics.drawString(font, getMessage(), getX() + 4, getY() + 3, 0xFFD4AF37, false);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.visible && this.active && actuallyHovered((int) mouseX, (int) mouseY)) {
			if (button == 0) {
				this.action.run();
				return true;
			}
		}
		return false;
	}
}