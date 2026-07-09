package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.NineSliceUtil;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Base modal window popup that centers itself dynamically, intercepts all
 * workspace inputs, and delegates drag inputs cleanly to children [3].
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractModalPopup extends AbstractFlowWidget {
	protected final ManagerScreen parentScreen;

	private GuiEventListener focusedChild = null;

	public AbstractModalPopup(ManagerScreen parentScreen, int width, int height, Component title) {
		super((parentScreen.width - width) / 2, (parentScreen.height - height) / 2, width, height, title);
		this.parentScreen = parentScreen;
	}

	/**
	 * Safely dismisses the modal and returns active focus to the main canvas [3].
	 */
	public void close() {
		this.parentScreen.setActiveModalPopup(null);
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

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				this.focusedChild = child;
				this.setFocused(child); // Align container-focus mapping cleanly [3]
				this.setDragging(true); // Flag active drag state to process sliders [3]
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = false;
		if (this.focusedChild != null) {
			handled = this.focusedChild.mouseReleased(mouseX, mouseY, button);
			this.focusedChild = null; // Clear focused widget reference [3]
		}
		this.setDragging(false); // Reset drag state [3]
		return handled || super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.focusedChild != null) {
			// Direct vector coordinates bypass to active slider elements [3]
			return this.focusedChild.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) // GLFW_KEY_ESCAPE
		{
			close(); // Cancel and dismiss modal cleanly on ESC [3]
			return true;
		}
		for (GuiEventListener child : children) {
			if (child.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		for (GuiEventListener child : children) {
			if (child.charTyped(codePoint, modifiers)) {
				return true;
			}
		}
		return super.charTyped(codePoint, modifiers);
	}

	/**
	 * Performs the 9-slice stretching calculations on the submenu background textures [3].
	 */
	protected void render9SliceBackground(GuiGraphics guiGraphics) {
		NineSliceUtil.drawDefault(guiGraphics, getX(), getY(), width, height);
	}
}