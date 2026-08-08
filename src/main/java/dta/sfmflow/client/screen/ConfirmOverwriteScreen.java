package dta.sfmflow.client.screen;

import dta.sfmflow.networking.packets.serverbound.ConfirmPastePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ConfirmOverwriteScreen extends Screen {
	private final BlockPos pos;

	public ConfirmOverwriteScreen(BlockPos pos) {
		super(Component.literal("Confirm Overwrite"));
		this.pos = pos;
	}

	@Override
	protected void init() {
		super.init();

		int btnW = 80;
		int btnH = 20;
		int startX = (this.width - (btnW * 2 + 10)) / 2;
		int startY = this.height / 2 + 10;

		this.addRenderableWidget(Button.builder(Component.literal("Yes"), btn -> {
			PacketDistributor.sendToServer(new ConfirmPastePacket(pos));
			this.onClose();
		}).pos(startX, startY).size(btnW, btnH).build());

		this.addRenderableWidget(Button.builder(Component.literal("No"), btn -> {
			this.onClose();
		}).pos(startX + btnW + 10, startY).size(btnW, btnH).build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		guiGraphics.drawCenteredString(this.font, "OVERWRITE EXISTING DATA?", this.width / 2, this.height / 2 - 25, 0xFFD4AF37);
		guiGraphics.drawCenteredString(this.font, "Pasting will permanently replace all nodes and variables.", this.width / 2, this.height / 2 - 10, 0xFFAAAAAA);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}