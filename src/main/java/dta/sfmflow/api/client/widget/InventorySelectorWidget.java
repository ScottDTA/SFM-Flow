package dta.sfmflow.api.client.widget;

import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Reusable side-scrolling UI selector list allowing users to find, search, and
 * select target blocks matching a specific capability registry key [3].
 */
@OnlyIn(Dist.CLIENT)
public class InventorySelectorWidget extends AbstractFlowWidget {
	private final IInventoryTarget model;
	private final ResourceLocation capabilityType;
	private final ManagerScreen parentScreen;
	private final EditBox searchEdit;
	private final Consumer<ConnectionBlock> onSelected;

	private float scrollX = 0.0F;

	public InventorySelectorWidget(int x, int y, IInventoryTarget model, ResourceLocation capabilityType,
			ManagerScreen parentScreen, Consumer<ConnectionBlock> onSelected) {
		super(x, y, 260, 52, Component.literal("Inventory Selector"));
		this.model = model;
		this.capabilityType = capabilityType;
		this.parentScreen = parentScreen;
		this.onSelected = onSelected;

		// Initialize local search box [3]
		this.searchEdit = new EditBox(parentScreen.getFont(), getX(), getY() + 12, 260, 14,
				Component.literal("Search"));
		this.searchEdit.setHint(Component.literal("Search inventories..."));
		this.searchEdit.setCanLoseFocus(true);
		this.children.add(new ApiWidgetAdapter<>(this.searchEdit));
	}

	private List<ConnectionBlock> getFilteredInventories(Level level) {
		List<ConnectionBlock> list = parentScreen.getMenu().getManagerBlockEntity().getInventories();
		String query = searchEdit.getValue().toLowerCase(Locale.ROOT);

		List<ConnectionBlock> filtered = new ArrayList<>();
		for (ConnectionBlock inv : list) {
			// Pre-filter by our target capability type ResourceLocation first [3]
			if (inv.getTypes().contains(capabilityType)) {
				String name = inv.getDisplayName(level).getString().toLowerCase(Locale.ROOT);
				if (query.isEmpty() || name.contains(query)) {
					filtered.add(inv);
				}
			}
		}
		return filtered;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		// Delegate clicks to the search box first and establish the focus delegation
		// chain [3]
		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused(child); // Fix: Set focused child on click so key events propagate [3]
				return true;
			}
		}

		// Check selection click on container block icons [3]
		int listX = getX();
		int listY = getY() + 30;

		if (button == 0 && mouseX >= listX && mouseX < listX + 260 && mouseY >= listY && mouseY < listY + 18) {
			Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
			List<ConnectionBlock> filtered = getFilteredInventories(level);

			for (int i = 0; i < filtered.size(); i++) {
				int cardX = listX + i * 20 - (int) scrollX;
				if (mouseX >= cardX && mouseX < cardX + 18) {
					ConnectionBlock clickedBlock = filtered.get(i);
					model.setInventoryId(clickedBlock.getId());
					this.onSelected.accept(clickedBlock);
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int listX = getX();
		int listY = getY() + 30;

		if (mouseX >= listX && mouseX < listX + 260 && mouseY >= listY && mouseY < listY + 18) {
			Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
			int maxScrollX = Math.max(0, getFilteredInventories(level).size() * 20 - 260);
			if (maxScrollX > 0) {
				this.scrollX = Mth.clamp(this.scrollX - (float) scrollY * 10.0F, 0.0F, (float) maxScrollX);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Render section header [3]
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Search Inventories:"), getX(), getY(),
				0xFF404040, false);

		// Render the search box child [3]
		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}

		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		List<ConnectionBlock> filtered = getFilteredInventories(level);

		int listX = getX();
		int listY = getY() + 30;

		// Apply hardware scissors mask around selection row [3]
		guiGraphics.enableScissor(listX, listY, listX + 260, listY + 18);

		for (int i = 0; i < filtered.size(); i++) {
			ConnectionBlock inv = filtered.get(i);
			int cardX = listX + i * 20 - (int) scrollX;

			boolean isSelected = model.getInventoryId() == inv.getId();
			boolean hovered = mouseX >= cardX && mouseX < cardX + 18 && mouseY >= listY && mouseY < listY + 18;

			int border = isSelected ? 0xFF39FF14 : (hovered ? 0xFF8B8B8B : 0xFF434343);
			guiGraphics.renderOutline(cardX, listY, 18, 18, border);

			BlockState state = level.getBlockState(inv.getBlockPos());
			ItemStack blockStack = new ItemStack(state.getBlock().asItem());
			if (!blockStack.isEmpty()) {
				guiGraphics.renderItem(blockStack, cardX + 1, listY + 1);
			}
		}

		// Flush deferred item renders inside the scissor mask bounds [3]
		guiGraphics.flush();

		guiGraphics.disableScissor();

		// Render horizontal scrollbar if elements exceed viewport boundaries [3]
		int maxScrollX = Math.max(0, filtered.size() * 20 - 260);
		if (maxScrollX > 0) {
			int scrollbarY = listY + 20;
			guiGraphics.fill(listX, scrollbarY, listX + 260, scrollbarY + 2, 0x40000000);

			int thumbWidth = (int) ((260.0F / (filtered.size() * 20.0F)) * 260.0F);
			thumbWidth = Math.max(15, Math.min(260, thumbWidth));
			int thumbX = listX + (int) ((scrollX / (float) maxScrollX) * (260 - thumbWidth));

			guiGraphics.fill(thumbX, scrollbarY, thumbX + thumbWidth, scrollbarY + 2, 0xFF8B8B8B);
		}

		// Tooltip rendering pass for block icons [3]
		if (mouseX >= listX && mouseX < listX + 260 && mouseY >= listY && mouseY < listY + 18) {
			for (int i = 0; i < filtered.size(); i++) {
				ConnectionBlock inv = filtered.get(i);
				int cardX = listX + i * 20 - (int) scrollX;
				if (mouseX >= cardX && mouseX < cardX + 18) {
					guiGraphics.renderTooltip(parentScreen.getFont(), inv.getDisplayName(level), mouseX, mouseY);
				}
			}
		}
	}

	@Override
	public void setX(int x) {
		int dif = this.getX() - x; // Fix inversion: absolute child tracking translation [3]
		super.setX(x);
		updateChildrenXPositions(dif);
	}

	@Override
	public void setY(int y) {
		int dif = this.getY() - y; // Fix inversion: absolute child tracking translation [3]
		super.setY(y);
		updateChildrenYPositions(dif);
	}
}