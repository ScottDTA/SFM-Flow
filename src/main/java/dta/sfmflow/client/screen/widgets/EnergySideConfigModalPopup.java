package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Custom popup modal allowing users to configure energy transfer limits
 * statically scaled to 75% (112x60) for a compact look [3].
 */
@OnlyIn(Dist.CLIENT)
public class EnergySideConfigModalPopup extends AbstractModalPopup {
	private final EnergyTransferComponent component;
	private final Direction side;
	private final BlockPos pos;
	private final Runnable onChanged;
	private final int maxLimit;
	private final EnergySlider slider;

	public EnergySideConfigModalPopup(ManagerScreen parentScreen, EnergyTransferComponent component, Direction side, BlockPos pos, Runnable onChanged) {
		super(parentScreen, 112, 60, Component.literal("Energy Limit")); // Statically scaled to 75% (112x60) [3]
		this.component = component;
		this.side = side;
		this.pos = pos;
		this.onChanged = onChanged;

		// 1. Query adjacent block capability on the client thread to fetch capacity bounds [3]
		int resolvedMax = 10000; // default fallback [3]
		var level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		if (level != null && pos != null && side != null) {
			var adjPos = pos.relative(side);
			var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, adjPos, side.getOpposite());
			if (energy != null) {
				resolvedMax = energy.getEnergyStored(); // Standard limit is 0 to max storage capacity on the side [3]
			}
		}
		this.maxLimit = resolvedMax;

		// Clamp current limit inside the resolved bounds [3]
		if (component.getMaxTransferAmount() > maxLimit) {
			component.setMaxTransferAmount(maxLimit);
		}

		// 2. Initialize the custom slider aligned inside the 112px layout
		this.slider = new EnergySlider(getX() + 10, getY() + 16, 92, 16, component, maxLimit, onChanged);
		this.children.add(new ApiWidgetAdapter<>(this.slider));
	}

	private void saveAndClose() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId(), nbt));
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

		int btnX = getX() + (width - 60) / 2;
		int btnY = getY() + height - 18;

		if (button == 0 && mouseX >= btnX && mouseX < btnX + 60 && mouseY >= btnY && mouseY < btnY + 14) {
			saveAndClose();
			return true;
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		String title = side != null ? side.name() + " LIMIT" : "LIMIT";
		guiGraphics.drawCenteredString(parentScreen.getFont(), title, getX() + width / 2, getY() + 4, 0xFFD4AF37);

		for (var child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				if (widget.visible) {
					widget.active = this.active;
					widget.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}
		}

		int btnX = getX() + (width - 60) / 2;
		int btnY = getY() + height - 18;
		boolean btnHovered = mouseX >= btnX && mouseX < btnX + 60 && mouseY >= btnY && mouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 60, btnY + 14, btnHovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 60, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 30, btnY + 3, 0xFFFFFFFF);
	}

	@OnlyIn(Dist.CLIENT)
	private static class EnergySlider extends AbstractSliderButton {
		private final EnergyTransferComponent component;
		private final int maxLimit;
		private final Runnable onChanged;

		public EnergySlider(int x, int y, int width, int height, EnergyTransferComponent component, int maxLimit, Runnable onChanged) {
			super(x, y, width, height, Component.empty(), maxLimit > 0 ? (double) component.getMaxTransferAmount() / maxLimit : 0.0);
			this.component = component;
			this.maxLimit = maxLimit;
			this.onChanged = onChanged;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = (int) Math.round(this.value * maxLimit);
			setMessage(Component.literal(val + " FE"));
		}

		@Override
		protected void applyValue() {
			int val = (int) Math.round(this.value * maxLimit);
			this.component.setMaxTransferAmount(val);
			this.onChanged.run();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY) && maxLimit > 0) {
				int val = this.component.getMaxTransferAmount();
				int step = Math.max(1, maxLimit / 100); // 1% steps [3]
				int newVal = Mth.clamp(val + (scrollY > 0 ? step : -step), 0, maxLimit);

				if (newVal != val) {
					this.component.setMaxTransferAmount(newVal);
					this.value = (double) newVal / maxLimit;
					this.updateMessage();
					this.onChanged.run();
				}
				return true;
			}
			return false;
		}

		@Override
		public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
			// Silent scrolling
		}
	}
}