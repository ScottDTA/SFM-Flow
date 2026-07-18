package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.CollectorComponent;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CollectorSettingsOverlay extends NodeSettingsOverlay {
	private final InputsSlider inputsSlider;

	public CollectorSettingsOverlay(ManagerScreen parentScreen, CollectorComponent component) {
		super(parentScreen, component);
		this.width = 240;
		this.height = 110;
		this.setX((parentScreen.width - 240) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		this.inputsSlider = new InputsSlider(getX() + 20, getY() + 45, 200, 18, component, this);
		this.children.add(new ApiWidgetAdapter<>(this.inputsSlider));

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 20, getY() + 32, 200, 10,
				Component.literal("Collector Inputs"), 0.75F, false, () -> 0xFF404040));
	}

	@OnlyIn(Dist.CLIENT)
	private static class InputsSlider extends AbstractSliderButton {
		private final CollectorComponent component;
		private final CollectorSettingsOverlay overlay;

		public InputsSlider(int x, int y, int width, int height, CollectorComponent component, CollectorSettingsOverlay overlay) {
			super(x, y, width, height, Component.empty(), (double) (component.getNumInputs() - 2) / 3.0);
			this.component = component;
			this.overlay = overlay;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = 2 + (int) Math.round(this.value * 3.0);
			setMessage(Component.literal("Input Pins: " + val));
		}

		@Override
		protected void applyValue() {
			int val = 2 + (int) Math.round(this.value * 3.0);
			this.component.setNumInputs(val);
			this.overlay.sendSettingsUpdate();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int val = this.component.getNumInputs();
				int newVal = Mth.clamp(val + (scrollY > 0 ? 1 : -1), 2, 5);
				if (newVal != val) {
					this.component.setNumInputs(newVal);
					this.value = (double) (newVal - 2) / 3.0;
					this.updateMessage();
					this.overlay.sendSettingsUpdate();
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