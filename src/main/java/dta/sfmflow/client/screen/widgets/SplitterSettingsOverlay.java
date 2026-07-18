package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.client.widget.NodeSettingsOverlay;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.SplitterComponent;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SplitterSettingsOverlay extends NodeSettingsOverlay {
	private final CycleButton<SplitterComponent.SplitterMode> modeBtn;
	private final OutputsSlider outputsSlider;

	public SplitterSettingsOverlay(ManagerScreen parentScreen, SplitterComponent component) {
		super(parentScreen, component);
		this.width = 240;
		this.height = 140;
		this.setX((parentScreen.width - 240) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		this.modeBtn = CycleButton.<SplitterComponent.SplitterMode>builder(val -> {
					return Component.literal(val.name().replace("_", " "));
				})
				.withValues(SplitterComponent.SplitterMode.values())
				.withInitialValue(component.getSplitterMode())
				.displayOnlyValue()
				.create(getX() + 20, getY() + 45, 200, 18, Component.literal("Splitter Mode"), (btn, value) -> {
					component.setSplitterMode(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.outputsSlider = new OutputsSlider(getX() + 20, getY() + 75, 200, 18, component, this);

		this.children.add(new ApiWidgetAdapter<>(this.modeBtn));
		this.children.add(new ApiWidgetAdapter<>(this.outputsSlider));

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 20, getY() + 32, 200, 10,
				Component.literal("Execution Splitter Mode"), 0.75F, false, () -> 0xFF404040));
	}

	@OnlyIn(Dist.CLIENT)
	private static class OutputsSlider extends AbstractSliderButton {
		private final SplitterComponent component;
		private final SplitterSettingsOverlay overlay;

		public OutputsSlider(int x, int y, int width, int height, SplitterComponent component, SplitterSettingsOverlay overlay) {
			super(x, y, width, height, Component.empty(), (double) (component.getNumOutputs() - 2) / 3.0);
			this.component = component;
			this.overlay = overlay;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = 2 + (int) Math.round(this.value * 3.0);
			setMessage(Component.literal("Output Pins: " + val));
		}

		@Override
		protected void applyValue() {
			int val = 2 + (int) Math.round(this.value * 3.0);
			this.component.setNumOutputs(val);
			this.overlay.sendSettingsUpdate();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int val = this.component.getNumOutputs();
				int newVal = Mth.clamp(val + (scrollY > 0 ? 1 : -1), 2, 5);
				if (newVal != val) {
					this.component.setNumOutputs(newVal);
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