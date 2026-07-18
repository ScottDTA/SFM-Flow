package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Custom settings modal dedicated to configuring min/max limits for DataComponents.DAMAGE [3].
 */
@OnlyIn(Dist.CLIENT)
public class DamageComponentSettingsModal extends AbstractModalPopup {
	private final AdvancedItemFilterVariableComponent component;
	private final int maxItemDamage;
	private final CycleButton<Boolean> rangeTypeBtn;
	private final EditBox minEdit;
	private final EditBox maxEdit;

	public DamageComponentSettingsModal(ManagerScreen parentScreen, ItemStack filterStack) {
		super(parentScreen, 170, 110, Component.literal("Damage settings"));
		
		AbstractFlowComponent comp = parentScreen.getMenu().getActiveComponent();
		if (comp instanceof AdvancedItemFilterVariableComponent varComp) {
			this.component = varComp;
		} else {
			throw new IllegalStateException("Active component is not an Advanced Filter Card!");
		}

		this.maxItemDamage = filterStack.getMaxDamage();

		CompoundTag dmgTag = component.getCustomComponentSettings().getCompound("minecraft:damage");
		if (dmgTag.isEmpty()) {
			dmgTag.putBoolean("usePercentage", false);
			dmgTag.putInt("minDmg", 0);
			dmgTag.putInt("maxDmg", maxItemDamage);
			dmgTag.putInt("minPct", 0);
			dmgTag.putInt("maxPct", 100);
			component.getCustomComponentSettings().put("minecraft:damage", dmgTag);
		}

		boolean isPercentage = dmgTag.getBoolean("usePercentage");

		this.rangeTypeBtn = CycleButton.<Boolean>builder(val -> val ? Component.literal("PERCENTAGE") : Component.literal("RAW DAMAGE"))
				.withValues(true, false)
				.withInitialValue(isPercentage)
				.displayOnlyValue()
				.create(getX() + 15, getY() + 16, 140, 18, Component.literal("Range Type"), (btn, val) -> {
					CompoundTag tag = component.getCustomComponentSettings().getCompound("minecraft:damage");
					tag.putBoolean("usePercentage", val);
					updateTextBoxValues(tag);
					sendSettingsUpdate();
				});

		this.minEdit = new EditBox(parentScreen.getFont(), getX() + 15, getY() + 54, 65, 18, Component.literal("Min"));
		this.minEdit.setFilter(text -> text.matches("\\d*"));
		this.minEdit.setResponder(text -> {
			try {
				int val = Integer.parseInt(text);
				CompoundTag tag = component.getCustomComponentSettings().getCompound("minecraft:damage");
				boolean pct = tag.getBoolean("usePercentage");
				if (pct) {
					tag.putInt("minPct", Mth.clamp(val, 0, 100));
				} else {
					tag.putInt("minDmg", Mth.clamp(val, 0, maxItemDamage));
				}
				sendSettingsUpdate();
			} catch (NumberFormatException ignored) {
			}
		});

		this.maxEdit = new EditBox(parentScreen.getFont(), getX() + 90, getY() + 54, 65, 18, Component.literal("Max"));
		this.maxEdit.setFilter(text -> text.matches("\\d*"));
		this.maxEdit.setResponder(text -> {
			try {
				int val = Integer.parseInt(text);
				CompoundTag tag = component.getCustomComponentSettings().getCompound("minecraft:damage");
				boolean pct = tag.getBoolean("usePercentage");
				if (pct) {
					tag.putInt("maxPct", Mth.clamp(val, 0, 100));
				} else {
					tag.putInt("maxDmg", Mth.clamp(val, 0, maxItemDamage));
				}
				sendSettingsUpdate();
			} catch (NumberFormatException ignored) {
			}
		});

		updateTextBoxValues(dmgTag);

		this.children.add(new ApiWidgetAdapter<>(this.rangeTypeBtn));
		this.children.add(new ApiWidgetAdapter<>(this.minEdit));
		this.children.add(new ApiWidgetAdapter<>(this.maxEdit));
	}

	private void updateTextBoxValues(CompoundTag tag) {
		boolean pct = tag.getBoolean("usePercentage");
		if (pct) {
			this.minEdit.setValue(String.valueOf(tag.contains("minPct") ? tag.getInt("minPct") : 0));
			this.maxEdit.setValue(String.valueOf(tag.contains("maxPct") ? tag.getInt("maxPct") : 100));
		} else {
			this.minEdit.setValue(String.valueOf(tag.contains("minDmg") ? tag.getInt("minDmg") : 0));
			this.maxEdit.setValue(String.valueOf(tag.contains("maxDmg") ? tag.getInt("maxDmg") : maxItemDamage));
		}
	}

	private void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		// Delegate focus setting to base class [3]
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;

		if (button == 0 && mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14) {
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			close();
			return true;
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "DAMAGE RANGE", getX() + width / 2, getY() + 6, 0xFFD4AF37);
		drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "Min Bounds", getX() + 45, getY() + 42, 0xFF404040);
		drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "Max Bounds", getX() + 120, getY() + 42, 0xFF404040);

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
		boolean hovered = mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, hovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 40, btnY + 3, 0xFFFFFFFF);
	}

	private void drawStringWithoutShadow(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
		int w = font.width(text);
		guiGraphics.drawString(font, text, x - w / 2, y, color, false);
	}
}