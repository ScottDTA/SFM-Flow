package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.security.ManagerAccessLevel;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.serverbound.SaveManagerAccessPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ManagerAccessSettingsModal extends AbstractModalPopup {
	private final ManagerScreen screen;
	private final ManagerAccessLevel originalLevel;
	private ManagerAccessLevel selectedLevel;
	private final boolean isOwner;
	private CycleButton<ManagerAccessLevel> levelButton;

	public ManagerAccessSettingsModal(ManagerScreen parentScreen) {
		super(parentScreen, 150, 100, Component.literal("Access Settings"));
		this.screen = parentScreen;
		var manager = parentScreen.getMenu().getManagerBlockEntity();
		this.originalLevel = manager.getAccessLevel();
		this.selectedLevel = this.originalLevel;
		this.isOwner = Minecraft.getInstance().player.getUUID().equals(manager.getOwnerUUID());

		this.levelButton = CycleButton.<ManagerAccessLevel>builder(val -> Component.literal(val.name()))
				.withValues(ManagerAccessLevel.values())
				.withInitialValue(selectedLevel)
				.displayOnlyValue()
				.create(getX() + 15, getY() + 32, 120, 18, Component.literal("Access Level"), (btn, value) -> {
					this.selectedLevel = value;
				});

		// Symmetrically bind the active state to the adapter to prevent rendering-loop overwrites
		ApiWidgetAdapter<CycleButton<ManagerAccessLevel>> levelButtonAdapter = new ApiWidgetAdapter<>(this.levelButton);
		levelButtonAdapter.active = isOwner;

		this.children.add(levelButtonAdapter);

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 15, getY() + 18, 120, 10,
				Component.literal("Owner: " + (manager.getOwnerName().isEmpty() ? "None" : manager.getOwnerName())).withStyle(ChatFormatting.GRAY), 0.75F, false, () -> 0xFF404040));
	}


	private void saveAndClose() {
		if (isOwner && selectedLevel != originalLevel) {
			PacketDistributor.sendToServer(new SaveManagerAccessPacket(screen.getMenu().getManagerBlockEntity().getBlockPos(), selectedLevel.ordinal()));
			screen.getMenu().getManagerBlockEntity().setAccessLevel(selectedLevel);
		}
		close();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 257 || keyCode == 335) { // Enter or Keypad Enter
			saveAndClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;

		if (button == 0 && mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14) {
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			saveAndClose();
			return true;
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		guiGraphics.drawCenteredString(parentScreen.getFont(), "ACCESS SETTINGS", getX() + width / 2, getY() + 6, 0xFFD4AF37);

		for (var child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				if (widget.visible) {
					// Blend the active states to preserve the child's disabled configuration
					widget.active = this.active && widget.active;
					widget.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}
		}

		if (!isOwner) {
			String warningText = "Owner modification only";
			int textW = parentScreen.getFont().width(warningText);
			guiGraphics.drawString(parentScreen.getFont(), warningText, getX() + (width - textW) / 2, getY() + 56, 0xFFB02E26, false);
		}

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;
		boolean btnHovered = mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, btnHovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 40, btnY + 3, 0xFFFFFFFF);
	}
}