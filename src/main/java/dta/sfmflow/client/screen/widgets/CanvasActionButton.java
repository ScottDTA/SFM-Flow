package dta.sfmflow.client.screen.widgets;

import java.util.UUID;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.serverbound.CanvasActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A sidebar visual menu button for executing canvas-level tasks such as copying
 * and deleting nodes [3].
 */
@OnlyIn(Dist.CLIENT)
public class CanvasActionButton extends AbstractFlowWidget {
	private final ResourceLocation buttonImage;
	private final CanvasAction action;
	private final ManagerScreen parentScreen;

	public CanvasActionButton(CanvasAction action, ManagerScreen parentScreen, int x, int y) {
		super(x, y, 14, 14,
				Component.translatable("gui.sfmflow.menu." + action.name().toLowerCase(java.util.Locale.ROOT)));
		this.action = action;
		this.parentScreen = parentScreen;
		this.buttonImage = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
				"textures/gui/menu_buttons/" + action.name().toLowerCase(java.util.Locale.ROOT) + "_button.png");
		this.setTooltip(Tooltip.create(this.getMessage()));
	}

	public CanvasAction getAction() {
		return action;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		int vOffset = 0;
		if (this.visible && this.active && actuallyHovered(mouseX, mouseY)) {
			vOffset = 14;
		}
		guiGraphics.blit(buttonImage, getX(), getY(), 0, vOffset, 14, 14, 14, 28);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY)) {
			return false;
		}
		if (button == 0) {
			playDownSound(Minecraft.getInstance().getSoundManager());
			executeAction();
			return true;
		}
		return false;
	}

	private void executeAction() {
		// Fire dynamic canvas action package targeting general block configurations [3]
		PacketDistributor.sendToServer(new CanvasActionPacket(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), UUID.randomUUID(), action));
	}

	@Override
	public void playDownSound(SoundManager handler) {
		handler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}
}