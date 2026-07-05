package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Locale;

/**
 * Standard visual fallback modal used when a data component does not exhibit 
 * a registered custom configuration overlay [3].
 */
@OnlyIn(Dist.CLIENT)
public class GenericDataComponentModal extends AbstractModalPopup {
	private final ResourceLocation componentLoc;

	public GenericDataComponentModal(ManagerScreen parentScreen, DataComponentType<?> type, ItemStack stack) {
		super(parentScreen, 150, 70, Component.literal("Component Settings"));
		this.componentLoc = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;

		if (button == 0 && mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14) {
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			close();
			return true;
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		String path = componentLoc != null ? componentLoc.getPath().toUpperCase(Locale.ROOT) : "UNKNOWN";
		
		// Draw all lines with no shadows to maximize readability [3]
		drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), path, getX() + width / 2, getY() + 6, 0xFFD4AF37);
		drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "Standard value-based", getX() + width / 2, getY() + 22, 0xFF8B8B8B);
		drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "matching is enabled.", getX() + width / 2, getY() + 34, 0xFF8B8B8B);

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;
		boolean hovered = mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, hovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 40, btnY + 3, 0xFFFFFFFF);
	}

	private void drawStringWithoutShadow(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
		int w = font.width(text);
		guiGraphics.drawString(font, text, x - w / 2, y, color, false); // No shadow [3]
	}
}