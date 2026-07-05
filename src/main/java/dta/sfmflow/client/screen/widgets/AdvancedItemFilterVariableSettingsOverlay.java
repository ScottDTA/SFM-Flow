package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.DataComponentOverlayRegistry;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.helper.FlowLayoutHelper;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.util.Color;
import dta.sfmflow.util.MenuSlotRepositioner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings overlay enabling visual configuration of a single-slot item variable
 * [3].
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedItemFilterVariableSettingsOverlay extends NodeSettingsOverlay {
	private static final ResourceLocation FILTER_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/filter_slot.png");

	private final AdvancedItemFilterVariableComponent component;
	private final Button toggleQtyBtn;
	private final EditBox qtyEdit;
	private final Checkbox useModIdCheckbox;
	private final Checkbox useTagCheckbox;
	private final Checkbox useComponentFilterCheckbox;

	public AdvancedItemFilterVariableSettingsOverlay(ManagerScreen parentScreen,
			AdvancedItemFilterVariableComponent component) {
		super(parentScreen, component);
		this.component = component;
		this.width = 240;
		this.height = 195;
		this.setX((parentScreen.width - this.width) / 2);
		this.setY(parentScreen.getTopPos() + (256 - this.height) / 2);

		parentScreen.getMenu().setActiveComponent(component);
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId()));

		repositionGhostSlot();

		this.qtyEdit = new EditBox(parentScreen.getFont(), getX() + 90, getY() + 56, 120, 18,
				Component.literal("Quantity"));
		this.qtyEdit.setValue(String.valueOf(component.getQuantity()));
		this.qtyEdit.setEditable(component.isUseQuantity());
		this.qtyEdit.setFilter(text -> text.matches("\\d*"));
		this.qtyEdit.setResponder(text -> {
			try {
				int val = Integer.parseInt(text);
				if (val > 0) {
					component.setQuantity(val);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}
			} catch (NumberFormatException ignored) {
			}
		});

		this.toggleQtyBtn = Button
				.builder(Component.literal("Qty: " + (component.isUseQuantity() ? "ON" : "OFF")), btn -> {
					component.setUseQuantity(!component.isUseQuantity());
					btn.setMessage(Component.literal("Qty: " + (component.isUseQuantity() ? "ON" : "OFF")));
					qtyEdit.setEditable(component.isUseQuantity());
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}).pos(getX() + 90, getY() + 34).size(120, 18).build();

		this.useModIdCheckbox = Checkbox.builder(Component.empty(), parentScreen.getFont())
				.pos(getX() + 15, getY() + 80).selected(component.isUseModId()).onValueChange((checkbox, selected) -> {
					component.setUseModId(selected);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}).build();

		this.useTagCheckbox = Checkbox.builder(Component.empty(), parentScreen.getFont()).pos(getX() + 15, getY() + 96)
				.selected(component.isUseTag()).onValueChange((checkbox, selected) -> {
					component.setUseTag(selected);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}).build();

		this.useComponentFilterCheckbox = Checkbox.builder(Component.empty(), parentScreen.getFont())
				.pos(getX() + 125, getY() + 96).selected(component.isUseComponentFilter())
				.onValueChange((checkbox, selected) -> {
					component.setUseComponentFilter(selected);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}).build();

		this.children.add(new ApiWidgetAdapter<>(this.toggleQtyBtn));
		this.children.add(new ApiWidgetAdapter<>(this.qtyEdit));
		this.children.add(new ApiWidgetAdapter<>(this.useModIdCheckbox));
		this.children.add(new ApiWidgetAdapter<>(this.useTagCheckbox));
		this.children.add(new ApiWidgetAdapter<>(this.useComponentFilterCheckbox));

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 36, getY() + 86, 180, 10,
				Component.literal("Use ModID (match all items belonging to same Mod namespace)"), 0.75F, false,
				() -> 0xFF404040));
		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 36, getY() + 102, 80, 10,
				Component.literal("Use Tag"), 0.75F, false, () -> 0xFF404040));
		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 146, getY() + 102, 80, 10,
				Component.literal("Use Comps"), 0.75F, false, () -> 0xFF404040));

		this.children.add(new ColorPanelWidget(getX() + 31, getY() + 56));

		this.children.add(new TagScrollListWidget(getX() + 15, getY() + 114, 100, 50));
		this.children.add(new ComponentScrollListWidget(getX() + 125, getY() + 114, 100, 50));
	}

	private void repositionGhostSlot() {
		int slotX = getX() + 30;
		int slotY = getY() + 34;
		Slot slot = parentScreen.getMenu().slots.get(36);
		MenuSlotRepositioner.setSlotPosition(slot, slotX - parentScreen.getLeftPos(), slotY - parentScreen.getTopPos());
	}

	private void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		repositionGhostSlot();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		repositionGhostSlot();
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderComponent(guiGraphics, mouseX, mouseY, partialTick);

		int slotX = getX() + 30;
		int slotY = getY() + 34;

		boolean hasItem = !component.getFilterStack().isEmpty();
		int vOffset = hasItem ? 18 : 0;

		guiGraphics.blit(FILTER_SLOT_TEXTURE, slotX, slotY, 0, vOffset, 18, 18, 18, 36);

		if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
			guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF8B8B8B);
		}

		if (hasItem) {
			guiGraphics.renderItem(component.getFilterStack(), slotX + 1, slotY + 1);
			guiGraphics.renderItemDecorations(parentScreen.getFont(), component.getFilterStack(), slotX + 1, slotY + 1);
		}
	}

	@Override
	public void closeAndSave() {
		parentScreen.getMenu().setActiveComponent(null);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
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
			if (this.visible && this.active && mouseX >= getX() && mouseX < getX() + 16 && mouseY >= getY()
					&& mouseY < getY() + 16) {
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

	@OnlyIn(Dist.CLIENT)
	private class TagScrollListWidget extends AbstractFlowWidget {
		private float scrollY = 0.0F;

		public TagScrollListWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Component.literal("Tag List"));
		}

		private List<String> getTags() {
			ItemStack stack = component.getFilterStack();
			List<String> tags = new ArrayList<>();
			if (!stack.isEmpty()) {
				stack.getTags().forEach(tagKey -> tags.add(tagKey.location().toString()));
			}
			return tags;
		}

		@Override
		protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			if (!component.isUseTag()) {
				return;
			}

			guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF111111);
			guiGraphics.renderOutline(getX(), getY(), width, height, 0xFF434343);

			List<String> tags = getTags();
			if (tags.isEmpty()) {
				guiGraphics.drawString(parentScreen.getFont(), "No tags", getX() + 4, getY() + 4, 0xFF8B8B8B, false);
				return;
			}

			guiGraphics.enableScissor(getX(), getY() + 1, getX() + width, getY() + height - 1);

			int startY = getY() + 4 - (int) scrollY;
			for (int i = 0; i < tags.size(); i++) {
				String tag = tags.get(i);
				int itemY = startY + i * 12;

				boolean isSelected = component.getSelectedTag().equals(tag);
				boolean hovered = mouseX >= getX() && mouseX < getX() + width && mouseY >= itemY && mouseY < itemY + 11;

				int textColor = isSelected ? 0xFFD4AF37 : (hovered ? 0xFFFFFFFF : 0xFF8B8B8B);
				guiGraphics.drawString(parentScreen.getFont(), tag, getX() + 4, itemY, textColor, false);
			}

			guiGraphics.disableScissor();

			int maxScroll = Math.max(0, tags.size() * 12 - (height - 8));
			if (maxScroll > 0) {
				int sbX = getX() + width - 4;
				guiGraphics.fill(sbX, getY() + 2, sbX + 2, getY() + height - 2, 0x40000000);

				int thumbHeight = (int) (((double) height / (tags.size() * 12)) * height);
				thumbHeight = Math.max(8, Math.min(height, thumbHeight));
				int thumbY = getY() + 2 + (int) ((scrollY / maxScroll) * (height - 4 - thumbHeight));

				guiGraphics.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, 0xFF8B8B8B);
			}

			// Render hover tooltip of the full tag name string (Fixed: Gated behind on-top
			// check) [3]
			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				if (FlowLayoutHelper.isWidgetActiveAndOnTop(this, parentScreen)) {
					int row = (int) ((mouseY - getY() + scrollY - 4) / 12);
					if (row >= 0 && row < tags.size()) {
						guiGraphics.renderTooltip(parentScreen.getFont(), Component.literal(tags.get(row)), mouseX,
								mouseY);
					}
				}
			}
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!this.visible || !this.active || !component.isUseTag()) {
				return false;
			}

			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				List<String> tags = getTags();
				int startY = getY() + 4 - (int) scrollY;

				for (int i = 0; i < tags.size(); i++) {
					int itemY = startY + i * 12;
					if (mouseY >= itemY && mouseY < itemY + 11) {
						component.setSelectedTag(tags.get(i));
						parentScreen.getMenu().getManagerBlockEntity().setChanged();
						sendSettingsUpdate();
						Minecraft.getInstance().getSoundManager()
								.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && component.isUseTag() && mouseX >= getX() && mouseX < getX() + width
					&& mouseY >= getY() && mouseY < getY() + height) {
				List<String> tags = getTags();
				int maxScroll = Math.max(0, tags.size() * 12 - (height - 8));
				if (maxScroll > 0) {
					this.scrollY = Mth.clamp(this.scrollY - (float) scrollY * 6.0F, 0.0F, (float) maxScroll);
					return true;
				}
			}
			return false;
		}
	}

	@OnlyIn(Dist.CLIENT)
	private class ComponentScrollListWidget extends AbstractFlowWidget {
		private float scrollY = 0.0F;

		public ComponentScrollListWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Component.literal("Component List"));
		}

		private List<DataComponentType<?>> getComponents() {
			ItemStack stack = component.getFilterStack();
			List<DataComponentType<?>> types = new ArrayList<>();
			if (!stack.isEmpty()) {
				for (var entry : stack.getComponents()) {
					types.add(entry.type());
				}
			}
			return types;
		}

		@Override
		protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			if (!component.isUseComponentFilter()) {
				return;
			}

			guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF111111);
			guiGraphics.renderOutline(getX(), getY(), width, height, 0xFF434343);

			List<DataComponentType<?>> types = getComponents();
			if (types.isEmpty()) {
				guiGraphics.drawString(parentScreen.getFont(), "No comps", getX() + 4, getY() + 4, 0xFF8B8B8B, false);
				return;
			}

			guiGraphics.enableScissor(getX(), getY() + 1, getX() + width, getY() + height - 1);

			int startY = getY() + 4 - (int) scrollY;
			for (int i = 0; i < types.size(); i++) {
				DataComponentType<?> type = types.get(i);
				ResourceLocation loc = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
				if (loc == null)
					continue;

				int itemY = startY + i * 12;

				boolean isEnabled = component.getEnabledComponentTypes().contains(loc.toString());
				boolean hoveredBox = mouseX >= getX() + 4 && mouseX < getX() + 12 && mouseY >= itemY
						&& mouseY < itemY + 8;
				boolean hoveredText = mouseX >= getX() + 14 && mouseX < getX() + width && mouseY >= itemY
						&& mouseY < itemY + 11;

				int checkboxBorder = hoveredBox ? 0xFFD4AF37 : 0xFF8B8B8B;
				guiGraphics.fill(getX() + 4, itemY + 2, getX() + 10, itemY + 8, isEnabled ? 0xFF39FF14 : 0xFF222222);
				guiGraphics.renderOutline(getX() + 4, itemY + 2, 6, 6, checkboxBorder);

				int textColor = hoveredText ? 0xFFFFFFFF : 0xFF8B8B8B;
				guiGraphics.drawString(parentScreen.getFont(), loc.getPath(), getX() + 14, itemY, textColor, false);
			}

			guiGraphics.disableScissor();

			int maxScroll = Math.max(0, types.size() * 12 - (height - 8));
			if (maxScroll > 0) {
				int sbX = getX() + width - 4;
				guiGraphics.fill(sbX, getY() + 2, sbX + 2, getY() + height - 2, 0x40000000);

				int thumbHeight = (int) (((double) height / (types.size() * 12)) * height);
				thumbHeight = Math.max(8, Math.min(height, thumbHeight));
				int thumbY = getY() + 2 + (int) ((scrollY / maxScroll) * (height - 4 - thumbHeight));

				guiGraphics.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, 0xFF8B8B8B);
			}

			// Render hover tooltip of the full component registry name string (Fixed: Gated
			// behind on-top check) [3]
			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				if (FlowLayoutHelper.isWidgetActiveAndOnTop(this, parentScreen)) {
					int row = (int) ((mouseY - getY() + scrollY - 4) / 12);
					if (row >= 0 && row < types.size()) {
						ResourceLocation loc = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(types.get(row));
						if (loc != null) {
							guiGraphics.renderTooltip(parentScreen.getFont(), Component.literal(loc.toString()), mouseX,
									mouseY);
						}
					}
				}
			}
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!this.visible || !this.active || !component.isUseComponentFilter()) {
				return false;
			}

			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				List<DataComponentType<?>> types = getComponents();
				int startY = getY() + 4 - (int) scrollY;

				for (int i = 0; i < types.size(); i++) {
					DataComponentType<?> type = types.get(i);
					ResourceLocation loc = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
					if (loc == null)
						continue;

					int itemY = startY + i * 12;

					if (mouseX >= getX() + 4 && mouseX < getX() + 12 && mouseY >= itemY && mouseY < itemY + 8) {
						String typeStr = loc.toString();
						if (component.getEnabledComponentTypes().contains(typeStr)) {
							component.getEnabledComponentTypes().remove(typeStr);
						} else {
							component.getEnabledComponentTypes().add(typeStr);
						}
						parentScreen.getMenu().getManagerBlockEntity().setChanged();
						sendSettingsUpdate();
						Minecraft.getInstance().getSoundManager()
								.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						return true;
					}

					if (mouseX >= getX() + 14 && mouseX < getX() + width && mouseY >= itemY && mouseY < itemY + 11) {
						Minecraft.getInstance().getSoundManager()
								.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

						AbstractModalPopup popup = DataComponentOverlayRegistry.createOverlay(type, parentScreen,
								component.getFilterStack());
						if (popup == null) {
							popup = new GenericDataComponentModal(parentScreen, type, component.getFilterStack());
						}
						parentScreen.setActiveModalPopup(popup);
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && component.isUseComponentFilter() && mouseX >= getX()
					&& mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				List<DataComponentType<?>> types = getComponents();
				int maxScroll = Math.max(0, types.size() * 12 - (height - 8));
				if (maxScroll > 0) {
					this.scrollY = Mth.clamp(this.scrollY - (float) scrollY * 6.0F, 0.0F, (float) maxScroll);
					return true;
				}
			}
			return false;
		}
	}
}