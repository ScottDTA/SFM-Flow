package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.RedstoneTriggerComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Side-specific config popup allowing users to set customized comparison thresholds per block face [3].
 */
@OnlyIn(Dist.CLIENT)
public class RedstoneSideConfigModalPopup extends AbstractModalPopup {
	private final RedstoneTriggerComponent component;
	private final Direction side;
	private final Runnable onChanged;

	private final CycleButton<RedstoneTriggerComponent.Operator> operatorButton;
	private final EditBox thresholdEdit;

	public RedstoneSideConfigModalPopup(ManagerScreen parentScreen, RedstoneTriggerComponent component, Direction side, BlockPos pos, Runnable onChanged) {
		super(parentScreen, 140, 100, Component.literal("Sided Redstone"));
		this.component = component;
		this.side = side;
		this.onChanged = onChanged;

		// 1. Comparison Operator Cycle Button [3]
		this.operatorButton = CycleButton.<RedstoneTriggerComponent.Operator>builder(op -> Component.literal(op.getSymbol()))
				.withValues(RedstoneTriggerComponent.Operator.values())
				.withInitialValue(component.getOperator(side))
				.displayOnlyValue()
				.create(getX() + 15, getY() + 20, 110, 18, Component.literal("Operator"), (btn, value) -> {
					component.setOperator(side, value);
					this.onChanged.run();
				});

		// 2. Threshold Input Box [3]
		this.thresholdEdit = new EditBox(parentScreen.getFont(), getX() + 15, getY() + 44, 110, 16, Component.literal("Threshold"));
		this.thresholdEdit.setValue(String.valueOf(component.getThreshold(side)));
		this.thresholdEdit.setFilter(text -> text.matches("\\d*"));
		this.thresholdEdit.setResponder(text -> {
			try {
				int val = Integer.parseInt(text);
				int clamped = Math.max(0, Math.min(15, val));
				component.setThreshold(side, clamped);
				this.onChanged.run();
			} catch (NumberFormatException ignored) {}
		});

		this.children.add(new ApiWidgetAdapter<>(this.operatorButton));
		this.children.add(new ApiWidgetAdapter<>(this.thresholdEdit));
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
}