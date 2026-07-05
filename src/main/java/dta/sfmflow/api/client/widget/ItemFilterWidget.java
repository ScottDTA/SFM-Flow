package dta.sfmflow.api.client.widget;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.IFilterable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.util.MenuSlotRepositioner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Reusable UI widget managing Whitelist/Blacklist filtering and a 1x12 ghost
 * slot item grid [3]. Repositions physical menu slots to enable robust vanilla
 * drag-and-drop mechanics [3].
 */
@OnlyIn(Dist.CLIENT)
public class ItemFilterWidget extends AbstractFlowWidget {
	private static final ResourceLocation FILTER_SLOT_TEXTURE = ResourceLocation
			.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/flowcomponents/filter_slot.png");

	private final IFilterable model;
	private final ManagerScreen parentScreen;
	private final Button toggleWhitelistBtn;
	private final Runnable onChanged;

	public ItemFilterWidget(int x, int y, IFilterable model, ManagerScreen parentScreen, Runnable onChanged) {
		super(x, y, 260, 40, Component.literal("Item Filter"));
		this.model = model;
		this.parentScreen = parentScreen;
		this.onChanged = onChanged;

		this.toggleWhitelistBtn = Button
				.builder(Component.literal(model.isWhitelist() ? "Whitelist" : "Blacklist"), btn -> {
					model.setWhitelist(!model.isWhitelist());
					btn.setMessage(Component.literal(model.isWhitelist() ? "Whitelist" : "Blacklist"));
					this.onChanged.run();
				}).pos(getX() + 130, getY()).size(120, 14).build();
		this.children.add(new ApiWidgetAdapter<>(this.toggleWhitelistBtn));

		repositionGhostSlots();
	}

	private void repositionGhostSlots() {
		int gridStartX = getX();
		int gridY = getY() + 20;

		for (int i = 0; i < 12; i++) {
			int slotX = gridStartX + i * 20 + 1;
			int slotY = gridY + 1;

			int slotIndexInMenu = 36 + i;
			if (slotIndexInMenu < parentScreen.getMenu().slots.size()) {
				var slot = parentScreen.getMenu().slots.get(slotIndexInMenu);
				MenuSlotRepositioner.setSlotPosition(slot, slotX - parentScreen.getLeftPos(),
						slotY - parentScreen.getTopPos());
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}

		return false; // Let clicks fall through to the physical slots! [3]
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Item Filter:"), getX(), getY() + 3,
				0xFF404040, false);

		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}

		int gridStartX = getX();
		int gridY = getY() + 20;

		for (int c = 0; c < 12; c++) {
			int slotX = gridStartX + c * 20;
			int slotY = gridY;
			boolean hovered = mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18;

			ItemStack stack = model.getFilterItems().get(c);
			boolean hasItem = stack != null && !stack.isEmpty();
			int vOffset = hasItem ? 18 : 0;

			guiGraphics.blit(FILTER_SLOT_TEXTURE, slotX, slotY, 0, vOffset, 18, 18, 18, 36);

			if (hovered) {
				guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF8B8B8B);
			}

			if (hasItem) {
				guiGraphics.renderFakeItem(stack, slotX + 1, slotY + 1);
				guiGraphics.renderItemDecorations(parentScreen.getFont(), stack, slotX + 1, slotY + 1);

				// Render "MID" overlay on variable cards that have UseModId enabled [3]
				if (stack.is(ModItems.VARIABLE_CARD.get())) {
					CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
					if (customData != null) {
						CompoundTag tag = customData.copyTag();
						if (tag.getBoolean("UseModId")) {
							guiGraphics.pose().pushPose();
							guiGraphics.pose().translate(0, 0, 200.0F);
							String modIdText = "MID";
							int strW = parentScreen.getFont().width(modIdText);
							float textScale = 0.55F;
							guiGraphics.pose().pushPose();
							guiGraphics.pose().translate(slotX + 17 - (strW * textScale), slotY + 12, 0);
							guiGraphics.pose().scale(textScale, textScale, 1.0F);
							guiGraphics.drawString(parentScreen.getFont(), modIdText, 0, 0, 0xFFFFFF00, true);
							guiGraphics.pose().popPose();
							guiGraphics.pose().popPose();
						}
					}
				}

				int limit = model.getFilterLimits().get(c);
				if (limit > 0) {
					guiGraphics.pose().pushPose();
					guiGraphics.pose().translate(0, 0, 200.0F);
					String limitStr = String.valueOf(limit);
					int strW = parentScreen.getFont().width(limitStr);

					float textScale = 0.65F;
					guiGraphics.pose().pushPose();
					guiGraphics.pose().translate(slotX + 17 - (strW * textScale), slotY + 11, 0);
					guiGraphics.pose().scale(textScale, textScale, 1.0F);
					guiGraphics.drawString(parentScreen.getFont(), limitStr, 0, 0, 0xFFFFFFFF, true);
					guiGraphics.pose().popPose();

					guiGraphics.pose().popPose();
				}
			}
		}
	}


	@Override
	public void setX(int x) {
		int dif = this.getX() - x; // Fix inversion: absolute child tracking translation [3]
		super.setX(x);
		updateChildrenXPositions(dif);
		repositionGhostSlots();
	}

	@Override
	public void setY(int y) {
		int dif = this.getY() - y; // Fix inversion: absolute child tracking translation [3]
		super.setY(y);
		updateChildrenYPositions(dif);
		repositionGhostSlots();
	}
}