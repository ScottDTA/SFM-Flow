package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Custom overlay designed to update and rename a component's nickname [3].
 */
@OnlyIn(Dist.CLIENT)
public class RenameModalPopup extends AbstractModalPopup {
	private final AbstractFlowComponent component;
	private final EditBox nameEdit;
	private final String originalName;

	public RenameModalPopup(ManagerScreen parentScreen, FlowWidgetContainer targetContainer) {
		super(parentScreen, 110, 56, Component.literal("Rename Node"));
		this.component = targetContainer.getComponent();

		// Set up standard input box
		this.nameEdit = new EditBox(parentScreen.getFont(), getX() + 5, getY() + 16, 100, 14,
				Component.literal("Rename"));
		this.nameEdit.setMaxLength(20);
		this.nameEdit.setBordered(true);
		this.nameEdit.setCanLoseFocus(false);

		this.originalName = component.getCustomName();
		String initialValue = this.originalName;
		if (initialValue == null || initialValue.isEmpty()) {
			Component defaultName = component.getName();
			if (defaultName.getContents() instanceof TranslatableContents translatable) {
				initialValue = I18n.get(translatable.getKey());
			} else {
				initialValue = defaultName.getString();
			}
		}
		this.nameEdit.setValue(initialValue);

		this.children.add(new ApiWidgetAdapter<>(this.nameEdit));
	}

	public void focusTextBox() {
		this.nameEdit.setFocused(true);
	}

	private void saveAndClose() {
		component.setCustomName(this.nameEdit.getValue());
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
		close();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Intercept Enter/KP_Enter keys programmatically to fire the save sequence [3]
		if (keyCode == 257 || keyCode == 335) {
			saveAndClose();
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

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// 🔥 FIXED: Scan custom button coordinate bounds FIRST before passing to child
		// widgets [3]
		// This stops parent bounds from pre-consuming the click and blocking the
		// Save/Cancel buttons [3]
		int sx = getX() + 5;
		int sy = getY() + 36;
		if (mouseX >= sx && mouseX < sx + 48 && mouseY >= sy && mouseY < sy + 14) {
			saveAndClose();
			return true;
		}

		int cx = getX() + 57;
		int cy = getY() + 36;
		if (mouseX >= cx && mouseX < cx + 48 && mouseY >= cy && mouseY < cy + 14) {
			component.setCustomName(this.originalName); // Revert changes [3]
			close();
			return true;
		}

		// Delegate clicks directly to child widgets to focus the EditBox [3]
		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		// Section header - Updated to dark gray for high readability on concrete gray
		// backdrops [3]
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Rename Node"), getX() + 5, getY() + 4,
				0xFF404040, false);

		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}

		// Render Save & Cancel buttons side-by-side [3]
		int sx = getX() + 5;
		int sy = getY() + 36;
		boolean saveHovered = mouseX >= sx && mouseX < sx + 48 && mouseY >= sy && mouseY < sy + 14;
		guiGraphics.fill(sx, sy, sx + 48, sy + 14, saveHovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(sx, sy, 48, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Save", sx + 24, sy + 3, 0xFFFFFFFF);

		int cx = getX() + 57;
		int cy = getY() + 36;
		boolean cancelHovered = mouseX >= cx && mouseX < cx + 48 && mouseY >= cy && mouseY < cy + 14;
		guiGraphics.fill(cx, cy, cx + 48, cy + 14, cancelHovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(cx, cy, 48, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Cancel", cx + 24, cy + 3, 0xFFFFFFFF);
	}

}