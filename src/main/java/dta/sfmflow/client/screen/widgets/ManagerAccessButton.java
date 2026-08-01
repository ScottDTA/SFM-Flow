package dta.sfmflow.client.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ManagerAccessButton extends AbstractFlowWidget {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/menu_buttons/access_button.png");
	private final ManagerScreen parentScreen;

	public ManagerAccessButton(int x, int y, ManagerScreen parentScreen) {
		super(x, y, 14, 14, Component.literal("Access Settings"));
		this.parentScreen = parentScreen;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		int vOffset = 0;
		if (this.visible && this.active && actuallyHovered(mouseX, mouseY)) {
			vOffset = 14;
		}
		guiGraphics.blit(TEXTURE, getX(), getY(), 0, vOffset, 14, 14, 14, 28);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.visible && this.active && actuallyHovered((int) mouseX, (int) mouseY)) {
			if (button == 0) {
				Minecraft.getInstance().getSoundManager().play(
						SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				parentScreen.setActiveModalPopup(new ManagerAccessSettingsModal(parentScreen));
				return true;
			}
		}
		return false;
	}
}