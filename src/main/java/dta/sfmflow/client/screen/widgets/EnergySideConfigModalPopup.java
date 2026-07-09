package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.network.ClientSidePropertyCache;
import dta.sfmflow.networking.packets.serverbound.RequestSideConfigPropertiesPacket;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Custom popup modal allowing users to configure energy transfer limits
 * dynamically bounded by the adjacent block's server-side verified capacity [3].
 */
@OnlyIn(Dist.CLIENT)
public class EnergySideConfigModalPopup extends AbstractModalPopup {
	private final EnergyTransferComponent component;
	private final Direction side;
	private final BlockPos pos;
	private final Runnable onChanged;
	private int maxLimit;
	private final EnergySlider slider;

	public EnergySideConfigModalPopup(ManagerScreen parentScreen, EnergyTransferComponent component, Direction side, BlockPos pos, Runnable onChanged) {
		super(parentScreen, 112, 60, Component.literal("Energy Limit"));
		this.component = component;
		this.side = side;
		this.pos = pos;
		this.onChanged = onChanged;

		// 1. Resolve starting bounds dynamically based on component node context [3]
		int resolvedMax = 10000; // Fallback [3]
		CompoundTag props = ClientSidePropertyCache.get(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"));
		if (component.isInput()) {
			if (props.contains("MaxExtract")) {
				resolvedMax = props.getInt("MaxExtract");
			} else if (props.contains("MaxEnergy")) {
				resolvedMax = props.getInt("MaxEnergy");
			}
		} else {
			if (props.contains("MaxReceive")) {
				resolvedMax = props.getInt("MaxReceive");
			} else if (props.contains("MaxEnergy")) {
				resolvedMax = props.getInt("MaxEnergy");
			}
		}
		this.maxLimit = resolvedMax;

		// 2. Dispatch dynamic query packet [3]
		PacketDistributor.sendToServer(new RequestSideConfigPropertiesPacket(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy")));

		// Clamp current limit inside starting bounds [3]
		if (component.getMaxTransferAmount() > maxLimit) {
			component.setMaxTransferAmount(maxLimit);
		}

		// 3. Initialize the slider
		this.slider = new EnergySlider(getX() + 10, getY() + 16, 92, 16, component, maxLimit, onChanged);
		this.children.add(new ApiWidgetAdapter<>(this.slider));
	}

	/**
	 * Callback triggered when server property configurations are returned [3].
	 * Dynamically re-adjusts slider values and bounds [3].
	 */
	public void refreshProperties() {
		if (pos != null && side != null) {
			CompoundTag props = ClientSidePropertyCache.get(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"));
			int resolvedMax = 10000;
			if (component.isInput()) {
				if (props.contains("MaxExtract")) {
					resolvedMax = props.getInt("MaxExtract");
				} else if (props.contains("MaxEnergy")) {
					resolvedMax = props.getInt("MaxEnergy");
				}
			} else {
				if (props.contains("MaxReceive")) {
					resolvedMax = props.getInt("MaxReceive");
				} else if (props.contains("MaxEnergy")) {
					resolvedMax = props.getInt("MaxEnergy");
				}
			}
			this.maxLimit = resolvedMax;
			this.slider.updateMaxLimit(resolvedMax);
		}
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
		private int maxLimit;
		private final Runnable onChanged;

		public EnergySlider(int x, int y, int width, int height, EnergyTransferComponent component, int maxLimit, Runnable onChanged) {
			super(x, y, width, height, Component.empty(), maxLimit > 0 ? (double) component.getMaxTransferAmount() / maxLimit : 0.0);
			this.component = component;
			this.maxLimit = maxLimit;
			this.onChanged = onChanged;
			this.updateMessage();
		}

		public void updateMaxLimit(int newMax) {
			this.maxLimit = newMax;
			if (component.getMaxTransferAmount() > maxLimit) {
				component.setMaxTransferAmount(maxLimit);
			}
			this.value = maxLimit > 0 ? (double) component.getMaxTransferAmount() / maxLimit : 0.0;
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
				int step = Math.max(1, maxLimit / 100); // 1% steps
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
		public void playDownSound(SoundManager soundManager) {
			// Silent scrolling
		}
	}
}