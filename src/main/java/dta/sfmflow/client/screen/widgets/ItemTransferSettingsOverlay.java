package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom settings overlay mapped specifically to configure Item Input/Output
 * cards [3]. Houses horizontal side-scrolling block-model lists, text searches,
 * and slot-range limits [3]. Upgraded with 260px centered list scrollers and
 * 3-row, 4-column ghost grids [3].
 */
@OnlyIn(Dist.CLIENT)
public class ItemTransferSettingsOverlay extends NodeSettingsOverlay {
	private final EditBox searchEdit;
	private final EditBox slotEdit;
	private final Button toggleWhitelistBtn;

	private float scrollX = 0.0F;

	/**
	 * Instantiates the overlay panels and aligns settings layout [3].
	 *
	 * @param parentScreen active manager screen panel [3]
	 * @param component    logical transfer component data model [3]
	 */
	public ItemTransferSettingsOverlay(ManagerScreen parentScreen, ItemTransferComponent component) {
		super(parentScreen, component);

		this.width = 300;
		this.height = 210;
		this.setX((parentScreen.width - 300) / 2);
		this.setY((256 - 210) / 2);

		component.setUseAll(false);

		// Centered Search Box setup [3]
		this.searchEdit = new EditBox(parentScreen.getFont(), getX() + 20, getY() + 40, 260, 14,
				Component.literal("Search"));
		this.searchEdit.setHint(Component.literal("Search inventories..."));
		this.searchEdit.setCanLoseFocus(true);
		this.children.add(new ApiWidgetAdapter<>(this.searchEdit));

		// Repositioned Slot Edit Box [3]
		this.slotEdit = new EditBox(parentScreen.getFont(), getX() + 20, getY() + 126, 110, 16,
				Component.literal("Slot"));
		this.slotEdit.setValue(String.valueOf(component.getTargetSlot()));
		this.children.add(new ApiWidgetAdapter<>(this.slotEdit));

		// Right Column Whitelist/Blacklist toggle button - aligned centrally inside
		// right pane [3]
		this.toggleWhitelistBtn = Button
				.builder(Component.literal(component.isWhitelist() ? "Mode: Whitelist" : "Mode: Blacklist"), btn -> {
					component.setWhitelist(!component.isWhitelist());
					btn.setMessage(Component.literal(component.isWhitelist() ? "Mode: Whitelist" : "Mode: Blacklist"));
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
				}).pos(getX() + 155, getY() + 84).size(120, 16).build();
		this.children.add(new ApiWidgetAdapter<>(this.toggleWhitelistBtn));
	}

	private List<ConnectionBlock> getFilteredInventories(Level level) {
		List<ConnectionBlock> list = parentScreen.getMenu().getManagerBlockEntity().getInventories();
		String query = searchEdit.getValue().toLowerCase(java.util.Locale.ROOT);
		if (query.isEmpty()) {
			return list;
		}
		List<ConnectionBlock> filtered = new ArrayList<>();
		for (ConnectionBlock inv : list) {
			String name = inv.getDisplayName(level).getString().toLowerCase(java.util.Locale.ROOT);
			if (name.contains(query)) {
				filtered.add(inv);
			}
		}
		return filtered;
	}

	@Override
	public void saveAndClose() {
		try {
			int slot = Integer.parseInt(this.slotEdit.getValue());
			((ItemTransferComponent) component).setTargetSlot(slot);
		} catch (NumberFormatException e) {
			((ItemTransferComponent) component).setTargetSlot(-1);
		}

		super.saveAndClose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			int listMinX = getX() + 20;
			int listMaxX = getX() + 280;
			int listMinY = getY() + 58;
			int listMaxY = getY() + 76;

			// 1. Intercept clicks on scrolling inventory icons (centered 260px width) [3]
			if (mouseX >= listMinX && mouseX < listMaxX && mouseY >= listMinY && mouseY < listMaxY) {
				var level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
				List<ConnectionBlock> filtered = getFilteredInventories(level);
				for (int i = 0; i < filtered.size(); i++) {
					int cardX = getX() + 20 + i * 20 - (int) scrollX;
					int cardY = getY() + 58;
					if (mouseX >= cardX && mouseX < cardX + 18 && mouseY >= cardY && mouseY < cardY + 18) {
						ItemTransferComponent transfer = (ItemTransferComponent) component;
						transfer.setInventoryId(filtered.get(i).getId());
						parentScreen.getMenu().getManagerBlockEntity().setChanged();
						return true;
					}
				}
			}

			// 2. Intercept clicks on 4-wide, 3-high Ghost Slot grid [3]
			int gridMinX = getX() + 175;
			int gridMaxX = getX() + 175 + 4 * 20;
			int gridMinY = getY() + 112;
			int gridMaxY = getY() + 112 + 3 * 20;

			if (mouseX >= gridMinX && mouseX < gridMaxX && mouseY >= gridMinY && mouseY < gridMaxY) {
				int col = (int) ((mouseX - gridMinX) / 20);
				int row = (int) ((mouseY - gridMinY) / 20);
				if (col >= 0 && col < 4 && row >= 0 && row < 3) {
					int slotIdx = row * 4 + col;
					ItemStack carried = parentScreen.getMenu().getCarried();
					if (carried != null && !carried.isEmpty()) {
						ItemStack copy = carried.copy();
						copy.setCount(1);
						((ItemTransferComponent) component).getFilterItems().set(slotIdx, copy);
					} else {
						((ItemTransferComponent) component).getFilterItems().set(slotIdx, ItemStack.EMPTY);
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int listMinX = getX() + 20;
		int listMaxX = getX() + 280;
		int listMinY = getY() + 58;
		int listMaxY = getY() + 76;

		if (mouseX >= listMinX && mouseX < listMaxX && mouseY >= listMinY && mouseY < listMaxY) {
			var level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
			int maxScrollX = Math.max(0, getFilteredInventories(level).size() * 20 - 260);
			if (maxScrollX > 0) {
				this.scrollX = net.minecraft.util.Mth.clamp(this.scrollX - (float) scrollY * 10.0F, 0.0F,
						(float) maxScrollX);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderComponent(guiGraphics, mouseX, mouseY, partialTick);

		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Search Inventories:"), getX() + 20,
				getY() + 28, 0xFF404040, false);

		var level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		List<ConnectionBlock> filtered = getFilteredInventories(level);

		int listX = getX() + 20;
		int listY = getY() + 58;

		// Expanded scissoring clipping area (260px wide) [3]
		guiGraphics.enableScissor(listX, listY, listX + 260, listY + 18);

		for (int i = 0; i < filtered.size(); i++) {
			var inv = filtered.get(i);
			int cardX = listX + i * 20 - (int) scrollX;
			int cardY = listY;

			boolean isSelected = ((ItemTransferComponent) component).getInventoryId() == inv.getId()
					&& !((ItemTransferComponent) component).isUseAll();
			boolean hovered = mouseX >= cardX && mouseX < cardX + 18 && mouseY >= cardY && mouseY < cardY + 18;

			int border = isSelected ? 0xFF39FF14 : (hovered ? 0xFF8B8B8B : 0xFF434343);
			guiGraphics.renderOutline(cardX, cardY, 18, 18, border);

			var state = level.getBlockState(inv.getBlockPos());
			ItemStack blockStack = new ItemStack(state.getBlock().asItem());
			if (!blockStack.isEmpty()) {
				guiGraphics.renderItem(blockStack, cardX + 1, cardY + 1);
			}
		}

		guiGraphics.disableScissor();

		// Render Expanded Horizontal Scrollbar (260px width) [3]
		int maxScrollX = Math.max(0, filtered.size() * 20 - 260);
		if (maxScrollX > 0) {
			int scrollbarX = listX;
			int scrollbarY = listY + 20;

			guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 260, scrollbarY + 2, 0x40000000);

			int thumbWidth = (int) ((260.0F / (filtered.size() * 20.0F)) * 260.0F);
			thumbWidth = Math.max(15, Math.min(260, thumbWidth));
			int thumbX = scrollbarX + (int) ((scrollX / (float) maxScrollX) * (260 - thumbWidth));

			guiGraphics.fill(thumbX, scrollbarY, thumbX + thumbWidth, scrollbarY + 2, 0xFF8B8B8B);
		}

		// Draw vertical columns dividing line
		guiGraphics.fill(getX() + 145, getY() + 86, getX() + 146, getY() + 184, 0xFF434343);

		// Left Column Configurations (Repositioned down) [3]
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Target Slot (-1 for Any):"), getX() + 20,
				getY() + 114, 0xFF404040, false);

		// Right Column 4x3 Ghost Slot Grid Rendering [3]
		ItemTransferComponent transfer = (ItemTransferComponent) component;
		int gridMinX = getX() + 175;
		int gridMaxX = getX() + 175 + 4 * 20;
		int gridMinY = getY() + 112;
		int gridMaxY = getY() + 112 + 3 * 20;

		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 4; c++) {
				int slotX = getX() + 175 + c * 20;
				int slotY = getY() + 112 + r * 20;
				boolean hovered = mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18;

				guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, hovered ? 0xFF353535 : 0xFF151515);
				guiGraphics.renderOutline(slotX, slotY, 18, 18, hovered ? 0xFF8B8B8B : 0xFF434343);

				int slotIdx = r * 4 + c;
				ItemStack stack = transfer.getFilterItems().get(slotIdx);
				if (stack != null && !stack.isEmpty()) {
					guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
				}
			}
		}

		// Tooltip rendering pass for scrolling inventories list
		if (mouseX >= listX && mouseX < listX + 260 && mouseY >= listY && mouseY < listY + 18) {
			for (int i = 0; i < filtered.size(); i++) {
				var inv = filtered.get(i);
				int cardX = listX + i * 20 - (int) scrollX;
				if (mouseX >= cardX && mouseX < cardX + 18) {
					guiGraphics.renderTooltip(parentScreen.getFont(), inv.getDisplayName(level), mouseX, mouseY);
				}
			}
		}

		// Tooltip rendering pass for 4x3 ghost slot grid items [3]
		if (mouseX >= gridMinX && mouseX < gridMaxX && mouseY >= gridMinY && mouseY < gridMaxY) {
			int col = (int) ((mouseX - gridMinX) / 20);
			int row = (int) ((mouseY - gridMinY) / 20);
			if (col >= 0 && col < 4 && row >= 0 && row < 3) {
				int slotIdx = row * 4 + col;
				ItemStack stack = transfer.getFilterItems().get(slotIdx);
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