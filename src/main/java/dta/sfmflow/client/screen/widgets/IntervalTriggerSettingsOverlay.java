package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Concrete settings overlay mapped specifically to configure Interval Trigger
 * execution periods [3]. Configured with standard, uncolored widgets and silent
 * slider actions [3].
 */
@OnlyIn(Dist.CLIENT)
public class IntervalTriggerSettingsOverlay extends NodeSettingsOverlay {
	/**
	 * Instantiates stacked unit cycles and speed control sliders inside the modal
	 * overlay [3].
	 *
	 * @param parentScreen active manager screen panel [3]
	 * @param component    the logical interval trigger component data model [3]
	 */
	public IntervalTriggerSettingsOverlay(ManagerScreen parentScreen, IntervalTriggerComponent component) {
		super(parentScreen, component);

		final IntervalSlider[] sliderHolder = new IntervalSlider[1];

		CycleButton<IntervalTriggerComponent.TimeUnit> cycleButton = CycleButton
				.builder(IntervalTriggerComponent.TimeUnit::getDisplayName)
				.withValues(IntervalTriggerComponent.TimeUnit.values()).withInitialValue(component.getTimeUnit())
				.displayOnlyValue()
				.create(getX() + 20, getY() + 45, 200, 20, Component.literal("TimeUnit"), (btn, value) -> {
					component.setTimeUnit(value);

					int minVal = (value == IntervalTriggerComponent.TimeUnit.TICKS)
							? dta.sfmflow.ServerConfig.MIN_INTERVAL_TICKS.get()
							: 1;
					int maxVal = (value == IntervalTriggerComponent.TimeUnit.TICKS) ? 100 : 60;
					component.setIntervalValue(Mth.clamp(component.getIntervalValue(), minVal, maxVal));

					parentScreen.getMenu().getManagerBlockEntity().setChanged();

					if (sliderHolder[0] != null) {
						sliderHolder[0].refresh();
					}
				});

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 20, getY() + 32, 200, 10,
				Component.translatable("gui.sfmflow.time_unit"), 1.0F, false, () -> 0xFF404040));

		IntervalSlider slider = new IntervalSlider(getX() + 20, getY() + 75, 200, 20, component, this);
		sliderHolder[0] = slider;

		this.children.add(new ApiWidgetAdapter<>(cycleButton));
		this.children.add(new ApiWidgetAdapter<>(slider));
	}

	@Override
	public void setX(int x) {
		int dif = this.getX() - x;
		super.setX(x);
		updateChildrenXPositions(dif);
	}

	@Override
	public void setY(int y) {
		int dif = this.getY() - y;
		super.setY(y);
		updateChildrenYPositions(dif);
	}

	@OnlyIn(Dist.CLIENT)
	private static class IntervalSlider extends AbstractSliderButton {
		private final IntervalTriggerComponent component;
		private final NodeSettingsOverlay overlay;

		public IntervalSlider(int x, int y, int width, int height, IntervalTriggerComponent component,
				NodeSettingsOverlay overlay) {
			super(x, y, width, height, Component.literal("Interval"), getInitialValueProgress(component));
			this.component = component;
			this.overlay = overlay;
			this.updateMessage();
		}

		private static double getInitialValueProgress(IntervalTriggerComponent comp) {
			int val = comp.getIntervalValue();
			int min = getMinLimit(comp);
			int max = getMaxLimit(comp);
			return (double) (val - min) / (double) (max - min);
		}

		private static int getMinLimit(IntervalTriggerComponent comp) {
			return (comp.getTimeUnit() == IntervalTriggerComponent.TimeUnit.TICKS)
					? dta.sfmflow.ServerConfig.MIN_INTERVAL_TICKS.get()
					: 1;
		}

		private static int getMaxLimit(IntervalTriggerComponent comp) {
			return (comp.getTimeUnit() == IntervalTriggerComponent.TimeUnit.TICKS) ? 100 : 60;
		}

		public void refresh() {
			this.value = getInitialValueProgress(this.component);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int min = getMinLimit(this.component);
			int max = getMaxLimit(this.component);
			int val = min + (int) Math.round(this.value * (max - min));
			setMessage(Component.literal(val + " " + this.component.getTimeUnit().getDisplayName().getString()));
		}

		@Override
		protected void applyValue() {
			int min = getMinLimit(this.component);
			int max = getMaxLimit(this.component);
			int val = min + (int) Math.round(this.value * (max - min));
			this.component.setIntervalValue(val);
			this.overlay.parentScreen.getMenu().getManagerBlockEntity().setChanged();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int min = getMinLimit(this.component);
				int max = getMaxLimit(this.component);
				int val = this.component.getIntervalValue();
				int step = 1;
				int newVal = Mth.clamp(val + (scrollY > 0 ? step : -step), min, max);

				if (newVal != val) {
					this.component.setIntervalValue(newVal);
					this.value = (double) (newVal - min) / (double) (max - min);
					this.updateMessage();
					this.overlay.parentScreen.getMenu().getManagerBlockEntity().setChanged();
				}
				return true;
			}
			return false;
		}

		/**
		 * Disables UI click sounds specifically on the slider for a smoother feel [3].
		 *
		 * @param soundManager system sound manager [3]
		 */
		@Override
		public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
			// No-op: Removes default UI clicking noise during drag or click [3]
		}
	}
}