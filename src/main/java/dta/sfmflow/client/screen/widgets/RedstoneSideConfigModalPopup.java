package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.IRedstoneSidedConfigurable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.RedstoneTriggerComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
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
 * Consolidated, polymorphic side-specific config popup for sided redstone thresholds [3].
 */
@OnlyIn(Dist.CLIENT)
public class RedstoneSideConfigModalPopup extends AbstractModalPopup {
	private final IRedstoneSidedConfigurable component;
	private final Direction side;
	private final Runnable onChanged;

	private final CycleButton<RedstoneTriggerComponent.Operator> operatorButton;
	private final ThresholdSlider thresholdSlider;

	public RedstoneSideConfigModalPopup(ManagerScreen parentScreen, IRedstoneSidedConfigurable component, Direction side, BlockPos pos, Runnable onChanged) {
		super(parentScreen, 140, 100, Component.literal("Sided Redstone"));
		this.component = component;
		this.side = side;
		this.onChanged = onChanged;

		this.operatorButton = CycleButton.<RedstoneTriggerComponent.Operator>builder(op -> Component.literal(op.getSymbol()))
				.withValues(RedstoneTriggerComponent.Operator.values())
				.withInitialValue(component.getOperator(side))
				.displayOnlyValue()
				.create(getX() + 15, getY() + 20, 110, 18, Component.literal("Operator"), (btn, value) -> {
					component.setOperator(side, value);
					this.onChanged.run();
				});

		this.thresholdSlider = new ThresholdSlider(getX() + 15, getY() + 44, 110, 18, component, side, onChanged);

		this.children.add(new ApiWidgetAdapter<>(this.operatorButton));
		this.children.add(new ApiWidgetAdapter<>(this.thresholdSlider));
	}

	private void saveAndClose() {
		CompoundTag nbt = new CompoundTag();
		((dta.sfmflow.api.component.AbstractFlowComponent) component).saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), 
				((dta.sfmflow.api.component.AbstractFlowComponent) component).getId(), nbt));
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

		String title = side != null ? side.name() + " LIMIT" : "LIMIT";
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
		private final IRedstoneSidedConfigurable component;
		private final Direction side;
		private final Runnable onChanged;

		public ThresholdSlider(int x, int y, int width, int height, IRedstoneSidedConfigurable component, Direction side, Runnable onChanged) {
			super(x, y, width, height, Component.empty(), (double) component.getThreshold(side) / 15.0);
			this.component = component;
			this.side = side;
			this.onChanged = onChanged;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = (int) Math.round(this.value * 15.0);
			setMessage(Component.literal("Threshold: " + val));
		}

		@Override
		protected void applyValue() {
			int val = (int) Math.round(this.value * 15.0);
			this.component.setThreshold(side, val);
			this.onChanged.run();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int val = this.component.getThreshold(side);
				int newVal = Mth.clamp(val + (scrollY > 0 ? 1 : -1), 0, 15);
				if (newVal != val) {
					this.component.setThreshold(side, newVal);
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