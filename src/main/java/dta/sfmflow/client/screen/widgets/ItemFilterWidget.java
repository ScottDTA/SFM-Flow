package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.IFilterable;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Reusable UI widget managing Whitelist/Blacklist filtering and a 1x12 ghost slot item grid [3].
 * Upgraded to render dynamic empty/filled slot states using custom 18x36 textures [3].
 */
@OnlyIn(Dist.CLIENT)
public class ItemFilterWidget extends AbstractFlowWidget {
	private static final ResourceLocation FILTER_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			dta.sfmflow.SFMFlow.MODID, "textures/gui/flowcomponents/filter_slot.png");

	private final IFilterable model;
	private final ManagerScreen parentScreen;
	private final Button toggleWhitelistBtn;
	private final Runnable onChanged;

	public ItemFilterWidget(int x, int y, IFilterable model, ManagerScreen parentScreen, Runnable onChanged) {
		super(x, y, 260, 40, Component.literal("Item Filter"));
		this.model = model;
		this.parentScreen = parentScreen;
		this.onChanged = onChanged;

		// Button positioned at the top right of our local bounds [3]
		this.toggleWhitelistBtn = Button
				.builder(Component.literal(model.isWhitelist() ? "Whitelist" : "Blacklist"), btn -> {
					model.setWhitelist(!model.isWhitelist());
					btn.setMessage(Component.literal(model.isWhitelist() ? "Whitelist" : "Blacklist"));
					this.onChanged.run();
				}).pos(getX() + 130, getY()).size(120, 14).build();
		this.children.add(new ApiWidgetAdapter<>(this.toggleWhitelistBtn));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		// Check standard click on the child toggle button first [3]
		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}

		// 1x12 Ghost Slot Grid Clicks (X: getX(), Y: getY() + 20) [3]
		int gridStartX = getX();
		int gridStartY = getY() + 20;

		if (mouseX >= gridStartX && mouseX < gridStartX + 12 * 20 && mouseY >= gridStartY && mouseY < gridStartY + 20) {
			int col = (int) ((mouseX - gridStartX) / 20);
			if (col >= 0 && col < 12) {
				ItemStack carried = parentScreen.getMenu().getCarried();
				if (carried != null && !carried.isEmpty()) {
					ItemStack copy = carried.copy();
					copy.setCount(1);
					model.getFilterItems().set(col, copy);
				} else {
					model.getFilterItems().set(col, ItemStack.EMPTY);
				}
				this.onChanged.run();
				return true;
			}
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Label rendered on the left of our button [3]
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Item Filter:"), getX(),
				getY() + 3, 0xFF404040, false);

		// Render children (the toggle button) [3]
		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}

		// Centered 1x12 Ghost Slot Grid Rendering [3]
		int gridStartX = getX();
		int gridStartY = getY() + 20;

		for (int c = 0; c < 12; c++) {
			int slotX = gridStartX + c * 20;
			int slotY = gridStartY;
			boolean hovered = mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18;

			ItemStack stack = model.getFilterItems().get(c);
			boolean hasItem = stack != null && !stack.isEmpty();
			int vOffset = hasItem ? 18 : 0;

			// Blit the custom slot background texture (V=0 for empty/question mark, V=18 for filled)
			guiGraphics.blit(FILTER_SLOT_TEXTURE, slotX, slotY, 0, vOffset, 18, 18, 18, 36);

			// Draw a gold highlight border if hovered [3]
			if (hovered) {
				guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFFD4AF37);
			}

			if (hasItem) {
				// Symmetrical +1px padding shifts the item stack inside standard 16x16 bounds
				guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
			}
		}

		// Tooltip rendering pass for 1x12 ghost slot items [3]
		if (mouseX >= gridStartX && mouseX < gridStartX + 12 * 20 && mouseY >= gridStartY && mouseY < gridStartY + 20) {
			int col = (int) ((mouseX - gridStartX) / 20);
			if (col >= 0 && col < 12) {
				ItemStack stack = model.getFilterItems().get(col);
				if (stack != null && !stack.isEmpty()) {
					guiGraphics.renderTooltip(parentScreen.getFont(), stack, mouseX, mouseY);
				}
			}
		}
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
}