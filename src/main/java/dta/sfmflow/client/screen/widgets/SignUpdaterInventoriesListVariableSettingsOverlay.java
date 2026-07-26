package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractTargetSettingsOverlay;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.SignUpdaterInventoriesListVariableComponent;
import dta.sfmflow.util.Color;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
public class SignUpdaterInventoriesListVariableSettingsOverlay extends AbstractTargetSettingsOverlay {
	private final SignUpdaterInventoriesListVariableComponent component;
	private final Checkbox addToListCheckbox;

	public SignUpdaterInventoriesListVariableSettingsOverlay(ManagerScreen parentScreen, SignUpdaterInventoriesListVariableComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "sign_updater"), 360);
		this.component = component;

		this.previewWidget.setHeight(210);

		this.addToListCheckbox = Checkbox.builder(Component.literal("Add to List"), parentScreen.getFont())
				.pos(previewWidget.getX() + 4, previewWidget.getY() + previewWidget.getHeight() - 20)
				.selected(component.isSelectedInList())
				.onValueChange((checkbox, selected) -> {
					component.setSelectedInList(selected);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}).build();

		this.children.add(new ApiWidgetAdapter<>(this.addToListCheckbox));
		this.children.add(new ColorPanelWidget(getX() + 260, getY() + 6));
	}

	@Override
	protected void onInventorySelected(ConnectionBlock newInv) {
		super.onInventorySelected(newInv);
		if (this.addToListCheckbox != null) {
			setCheckboxSelected(this.addToListCheckbox, component.isSelectedInList());
		}
	}

	private void setCheckboxSelected(Checkbox checkbox, boolean selected) {
		try {
			Field field = Checkbox.class.getDeclaredField("selected");
			field.setAccessible(true);
			field.setBoolean(checkbox, selected);
		} catch (Exception e) {
			try {
				for (Field field : Checkbox.class.getDeclaredFields()) {
					if (field.getType() == boolean.class) {
						field.setAccessible(true);
						field.setBoolean(checkbox, selected);
						break;
					}
				}
			} catch (Exception ex) {
				SFMFlow.LOGGER.error("Failed to set checkbox state", ex);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private class ColorPanelWidget extends AbstractFlowWidget {
		public ColorPanelWidget(int x, int y) {
			super(x, y, 16, 16, Component.literal("Color Panel"));
		}

		@Override
		protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			Color current = component.getFilterColor();
			boolean hovered = mouseX >= getX() && mouseX < getX() + 16 && mouseY >= getY() && mouseY < getY() + 16;
			int border = hovered ? 0xFFD4AF37 : 0xFF8B8B8B;

			guiGraphics.fill(getX(), getY(), getX() + 16, getY() + 16, current.getHexColor() | 0xFF000000);
			guiGraphics.renderOutline(getX(), getY(), 16, 16, border);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (this.visible && this.active && mouseX >= getX() && mouseX < getX() + 16 && mouseY >= getY() && mouseY < getY() + 16) {
				if (button == 0) {
					Color[] values = Color.values();
					int nextIdx = (component.getFilterColor().ordinal() + 1) % values.length;
					component.setFilterColor(values[nextIdx]);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
					Minecraft.getInstance().getSoundManager()
							.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return true;
				}
			}
			return false;
		}
	}
}