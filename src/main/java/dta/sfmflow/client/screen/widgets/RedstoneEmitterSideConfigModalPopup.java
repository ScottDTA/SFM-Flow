package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.RedstoneEmitterComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Custom side config popup configuring math modifications, rollover toggles, and pulse states per face [3].
 */
@OnlyIn(Dist.CLIENT)
public class RedstoneEmitterSideConfigModalPopup extends AbstractModalPopup {
	private final RedstoneEmitterComponent component;
	private final Direction side;
	private final Runnable onChanged;

	private final CycleButton<RedstoneEmitterComponent.RedstoneOp> operatorButton;
	private final ThresholdSlider thresholdSlider;
	private final Checkbox pulseCheckbox;
	private final Checkbox rolloverCheckbox;

	public RedstoneEmitterSideConfigModalPopup(ManagerScreen parentScreen, RedstoneEmitterComponent component, Direction side, BlockPos pos, Runnable onChanged) {
		super(parentScreen, 140, 140, Component.literal("Sided Emitter"));
		this.component = component;
		this.side = side;
		this.onChanged = onChanged;

		// 1. Operator cycle [3]
		this.operatorButton = CycleButton.<RedstoneEmitterComponent.RedstoneOp>builder(op -> Component.literal(op.name()))
				.withValues(RedstoneEmitterComponent.RedstoneOp.values())
				.withInitialValue(component.getOperator(side))
				.displayOnlyValue()
				.create(getX() + 15, getY() + 20, 110, 18, Component.literal("Operator"), (btn, value) -> {
					component.setOperator(side, value);
					this.onChanged.run();
				});

		// 2. Modifier threshold slider (0-15) [3]
		this.thresholdSlider = new ThresholdSlider(getX() + 15, getY() + 44, 110, 18, component, side, onChanged);

		// 3. Pulse Checkbox [3]
		this.pulseCheckbox = Checkbox.builder(Component.empty(), parentScreen.getFont())
				.pos(getX() + 15, getY() + 68).selected(component.isPulse(side)).onValueChange((checkbox, selected) -> {
					component.setPulse(side, selected);
					this.onChanged.run();
				}).build();

		// 4. Rollover Checkbox [3]
		this.rolloverCheckbox = Checkbox.builder(Component.empty(), parentScreen.getFont())
				.pos(getX() + 15, getY() + 86).selected(component.isRollover(side)).onValueChange((checkbox, selected) -> {
					component.setRollover(side, selected);
					this.onChanged.run();
				}).build();

		this.children.add(new ApiWidgetAdapter<>(this.operatorButton));
		this.children.add(new ApiWidgetAdapter<>(this.thresholdSlider));
		this.children.add(new ApiWidgetAdapter<>(this.pulseCheckbox));
		this.children.add(new ApiWidgetAdapter<>(this.rolloverCheckbox));

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 36, getY() + 73, 90, 10,
				Component.literal("Pulse Mode"), 0.75F, false, () -> 0xFF404040));
		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 36, getY() + 91, 90, 10,
				Component.literal("Rollover (Wrap)"), 0.75F, false, () -> 0xFF404040));
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

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;

		if (button == 0 && mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14) {
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			saveAndClose();
			return true;
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		String title = side != null ? side.name() + " STATE" : "STATE";
		guiGraphics.drawCenteredString(parentScreen.getFont(), title, getX() + width / 2, getY() + 6, 0xFFD4AF37);

		for (var child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				if (widget.visible) {
					widget.active = this.active;
					widget.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}
		}

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;
		boolean btnHovered = mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, btnHovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 40, btnY + 3, 0xFFFFFFFF);
	}

	@OnlyIn(Dist.CLIENT)
	private static class ThresholdSlider extends AbstractSliderButton {
		private final RedstoneEmitterComponent component;
		private final Direction side;
		private final Runnable onChanged;

		public ThresholdSlider(int x, int y, int width, int height, RedstoneEmitterComponent component, Direction side, Runnable onChanged) {
			super(x, y, width, height, Component.empty(), (double) component.getValue(side) / 15.0);
			this.component = component;
			this.side = side;
			this.onChanged = onChanged;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = (int) Math.round(this.value * 15.0);
			setMessage(Component.literal("Value: " + val));
		}

		@Override
		protected void applyValue() {
			int val = (int) Math.round(this.value * 15.0);
			this.component.setValue(side, val);
			this.onChanged.run();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int val = this.component.getValue(side);
				int newVal = Mth.clamp(val + (scrollY > 0 ? 1 : -1), 0, 15);
				if (newVal != val) {
					this.component.setValue(side, newVal);
					this.value = (double) newVal / 15.0;
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