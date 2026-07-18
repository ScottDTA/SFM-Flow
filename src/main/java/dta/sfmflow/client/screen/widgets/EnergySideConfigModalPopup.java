package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.network.ClientSidePropertyCache;
import dta.sfmflow.networking.packets.serverbound.RequestSideConfigPropertiesPacket;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Custom popup modal allowing users to toggle unlimited energy transfer or
 * input a precise numeric value [3].
 */
@OnlyIn(Dist.CLIENT)
public class EnergySideConfigModalPopup extends AbstractModalPopup {
	private final EnergyTransferComponent component;
	private final Direction side;
	private final BlockPos pos;
	private final Runnable onChanged;

	private final Button modeButton;
	private final EditBox limitEdit;
	private boolean isUnlimited;
	private int lastCustomValue = 10000;

	public EnergySideConfigModalPopup(ManagerScreen parentScreen, EnergyTransferComponent component, Direction side, BlockPos pos, Runnable onChanged) {
		super(parentScreen, 140, 100, Component.literal("Energy Limit"));
		this.component = component;
		this.side = side;
		this.pos = pos;
		this.onChanged = onChanged;

		// 1. Resolve initial unlimited state and custom values [3]
		this.isUnlimited = (component.getMaxTransferAmount() == Integer.MAX_VALUE);
		if (!isUnlimited) {
			this.lastCustomValue = component.getMaxTransferAmount();
		} else {
			// Resolve fallback custom default from server-verified properties cache if available [3]
			int resolvedMax = 10000;
			CompoundTag props = ClientSidePropertyCache.get(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"));
			if (props.contains("MaxExtract") && component.isInput()) {
				resolvedMax = props.getInt("MaxExtract");
			} else if (props.contains("MaxReceive") && !component.isInput()) {
				resolvedMax = props.getInt("MaxReceive");
			} else if (props.contains("MaxEnergy")) {
				resolvedMax = props.getInt("MaxEnergy");
			}
			this.lastCustomValue = resolvedMax;
		}

		// 2. Instantiate the edit box first to satisfy Java's blank final assignment rules [3]
		this.limitEdit = new EditBox(parentScreen.getFont(), getX() + 15, getY() + 44, 110, 16, Component.literal("Limit Amount"));
		this.limitEdit.setFilter(text -> text.matches("\\d*") || (isUnlimited && "Unlimited".equals(text)));
		
		if (isUnlimited) {
			this.limitEdit.setValue("Unlimited");
			this.limitEdit.setEditable(false);
		} else {
			this.limitEdit.setValue(String.valueOf(lastCustomValue));
			this.limitEdit.setEditable(true);
		}

		this.limitEdit.setResponder(text -> {
			if (isUnlimited) return;
			try {
				long val = Long.parseLong(text);
				int clampedVal = (int) Math.min(Integer.MAX_VALUE - 1, Math.max(1, val));
				this.lastCustomValue = clampedVal;
				this.component.setMaxTransferAmount(clampedVal);
				this.onChanged.run();
			} catch (NumberFormatException ignored) {}
		});

		// 3. Create the Toggle button second, referencing the fully instantiated limitEdit [3]
		this.modeButton = Button.builder(
			Component.literal(isUnlimited ? "Mode: UNLIMITED" : "Mode: SET AMOUNT"),
			btn -> {
				this.isUnlimited = !this.isUnlimited;
				btn.setMessage(Component.literal(this.isUnlimited ? "Mode: UNLIMITED" : "Mode: SET AMOUNT"));
				
				if (this.isUnlimited) {
					this.component.setMaxTransferAmount(Integer.MAX_VALUE);
					this.limitEdit.setValue("Unlimited");
					this.limitEdit.setEditable(false);
				} else {
					this.component.setMaxTransferAmount(this.lastCustomValue);
					this.limitEdit.setValue(String.valueOf(this.lastCustomValue));
					this.limitEdit.setEditable(true);
				}
				this.onChanged.run();
			}
		).pos(getX() + 15, getY() + 20).size(110, 18).build();

		this.children.add(new ApiWidgetAdapter<>(this.modeButton));
		this.children.add(new ApiWidgetAdapter<>(this.limitEdit));

		// Dispatch background query to ensure server capability caches are primed [3]
		PacketDistributor.sendToServer(new RequestSideConfigPropertiesPacket(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy")));
	}

	public void refreshProperties() {
		// Only update custom cache baseline if we are currently unlimited [3]
		if (pos != null && side != null && isUnlimited) {
			CompoundTag props = ClientSidePropertyCache.get(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"));
			int resolvedMax = 10000;
			if (props.contains("MaxExtract") && component.isInput()) {
				resolvedMax = props.getInt("MaxExtract");
			} else if (props.contains("MaxReceive") && !component.isInput()) {
				resolvedMax = props.getInt("MaxReceive");
			} else if (props.contains("MaxEnergy")) {
				resolvedMax = props.getInt("MaxEnergy");
			}
			this.lastCustomValue = resolvedMax;
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

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;

		if (button == 0 && mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14) {
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