package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.api.client.widget.AbstractTargetSettingsOverlay;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.SculkTriggerComponent;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.Locale;

/**
 * Settings overlay enabling visual configuration of Sculk Triggers.
 */
@OnlyIn(Dist.CLIENT)
public class SculkTriggerSettingsOverlay extends AbstractTargetSettingsOverlay {
	private final RadiusSlider radiusSlider;
	private final CooldownSlider cooldownSlider;

	public SculkTriggerSettingsOverlay(ManagerScreen parentScreen, SculkTriggerComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "sculk"), 360);

		this.radiusSlider = new RadiusSlider(getX() + 20, getY() + 242, 260, 18, component, this);
		this.cooldownSlider = new CooldownSlider(getX() + 20, getY() + 286, 260, 18, component, this);

		this.children.add(new ApiWidgetAdapter<>(this.radiusSlider));
		this.children.add(new ApiWidgetAdapter<>(this.cooldownSlider));

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 20, getY() + 230, 260, 10,
				Component.literal("Acoustic Listening Range"), 0.75F, false, () -> 0xFF404040));
		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 20, getY() + 274, 260, 10,
				Component.literal("Trigger Cooldown Configuration"), 0.75F, false, () -> 0xFF404040));
	}

	@Override
	protected boolean onInventoryFilter(ConnectionBlock block) {
		// Sided Filter: Only show Sculk Trigger Cables in our selection list
		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		if (level != null) {
			return level.getBlockState(block.getBlockPos()).is(ModBlocks.SCULK_TRIGGER_CABLE_BLOCK.get());
		}
		return true;
	}

	@Override
	protected void onInventorySelected(ConnectionBlock newInv) {
		((SculkTriggerComponent) component).setActiveSidesMask(63); // Default to all faces active on select
	}

	@OnlyIn(Dist.CLIENT)
	private static class RadiusSlider extends AbstractSliderButton {
		private final SculkTriggerComponent component;
		private final SculkTriggerSettingsOverlay overlay;

		public RadiusSlider(int x, int y, int width, int height, SculkTriggerComponent component,
				SculkTriggerSettingsOverlay overlay) {
			super(x, y, width, height, Component.empty(), (double) (component.getRadius() - 1) / 15.0);
			this.component = component;
			this.overlay = overlay;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = 1 + (int) Math.round(this.value * 15.0);
			setMessage(Component.literal("Radius: " + val + " blocks"));
		}

		@Override
		protected void applyValue() {
			int val = 1 + (int) Math.round(this.value * 15.0);
			this.component.setRadius(val);
			this.overlay.sendSettingsUpdate();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int val = this.component.getRadius();
				int newVal = Mth.clamp(val + (scrollY > 0 ? 1 : -1), 1, 16);
				if (newVal != val) {
					this.component.setRadius(newVal);
					this.value = (double) (newVal - 1) / 15.0;
					this.updateMessage();
					this.overlay.sendSettingsUpdate();
				}
				return true;
			}
			return false;
		}

		@Override
		public void playDownSound(SoundManager soundManager) {
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class CooldownSlider extends AbstractSliderButton {
		private final SculkTriggerComponent component;
		private final SculkTriggerSettingsOverlay overlay;

		public CooldownSlider(int x, int y, int width, int height, SculkTriggerComponent component,
				SculkTriggerSettingsOverlay overlay) {
			super(x, y, width, height, Component.empty(), (double) component.getCooldownTicks() / 200.0);
			this.component = component;
			this.overlay = overlay;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = (int) Math.round(this.value * 200.0);
			double sec = val / 20.0;
			setMessage(Component
					.literal("Cooldown: " + val + " ticks (" + String.format(Locale.ROOT, "%.1f", sec) + "s)"));
		}

		@Override
		protected void applyValue() {
			int val = (int) Math.round(this.value * 200.0);
			this.component.setCooldownTicks(val);
			this.overlay.sendSettingsUpdate();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int val = this.component.getCooldownTicks();
				int newVal = Mth.clamp(val + (scrollY > 0 ? 5 : -5), 0, 200);
				if (newVal != val) {
					this.component.setCooldownTicks(newVal);
					this.value = (double) newVal / 200.0;
					this.updateMessage();
					this.overlay.sendSettingsUpdate();
				}
				return true;
			}
			return false;
		}

		@Override
		public void playDownSound(SoundManager soundManager) {
		}
	}
}