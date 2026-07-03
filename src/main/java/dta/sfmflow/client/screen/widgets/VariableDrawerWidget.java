package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.networking.packets.serverbound.SyncCarriedItemPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Side-sliding drawer widget displaying a searchable, vertically scrollable 3x3
 * grid of active variables [3].
 */
@OnlyIn(Dist.CLIENT)
public class VariableDrawerWidget extends AbstractFlowWidget {
	private static final ResourceLocation DRAWER_TX = ResourceLocation.fromNamespaceAndPath(dta.sfmflow.SFMFlow.MODID,
			"textures/gui/flowcomponents/generic_slot.png");

	private final ManagerScreen parentScreen;
	private final EditBox searchEdit;
	private int scrollY = 0;

	public VariableDrawerWidget(ManagerScreen parentScreen, int x, int y, int width, int height) {
		super(x, y, width, height, Component.literal("Variables Drawer"));
		this.parentScreen = parentScreen;

		this.searchEdit = new EditBox(parentScreen.getFont(), getX() + 4, getY() + 6, width - 8, 12,
				Component.literal("Search"));
		this.searchEdit.setHint(Component.literal("Search..."));
		this.searchEdit.setCanLoseFocus(true);
		this.children.add(new ApiWidgetAdapter<>(this.searchEdit));
	}

	private List<AdvancedItemFilterVariableComponent> getFilteredVariables() {
		List<AdvancedItemFilterVariableComponent> filtered = new ArrayList<>();
		String query = searchEdit.getValue().toLowerCase(Locale.ROOT);

		var components = parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().values();
		for (var comp : components) {
			if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
				// Symmetrical Check: Skip empty variables in the drawer selection [3]
				if (advancedVar.getFilterStack().isEmpty()) {
					continue;
				}
				String name = advancedVar.getName().getString().toLowerCase(Locale.ROOT);
				if (query.isEmpty() || name.contains(query)) {
					filtered.add(advancedVar);
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

		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}

		// Grid bounds check
		int gridX = getX() + 47;
		int gridY = getY() + 24;

		if (button == 0 && mouseX >= gridX && mouseX < gridX + 58 && mouseY >= gridY && mouseY < gridY + 60) {
			List<AdvancedItemFilterVariableComponent> vars = getFilteredVariables();

			for (int i = 0; i < vars.size(); i++) {
				int col = i % 3;
				int row = i / 3;

				// Local variables renamed to prevent JVM compilation shadow conflicts [3]
				int cellX = getCellX(col);
				int cellY = getCellY(row);

				if (mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18) {
					AdvancedItemFilterVariableComponent clickedVar = vars.get(i);
					ItemStack stack = clickedVar.toItemStack();

					// Activate cursor tracking by setting menu's carried stack natively [3]
					parentScreen.getMenu().setCarried(stack);

					// Dispatch synchronization packet immediately to the server [3]
					PacketDistributor.sendToServer(new SyncCarriedItemPacket(stack));
					return true;
				}
			}
		}

		return false;
	}

	// Methods renamed from gridX/gridY to getCellX/getCellY to prevent local
	// variable shadowing [3]
	private int getCellX(int col) {
		return getX() + 47 + col * 20;
	}

	private int getCellY(int row) {
		return getY() + 24 + row * 20 - scrollY;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int gridX = getX() + 47;
		int gridY = getY() + 24;

		if (mouseX >= gridX && mouseX < gridX + 58 && mouseY >= gridY && mouseY < gridY + 60) {
			List<AdvancedItemFilterVariableComponent> vars = getFilteredVariables();
			int rows = (vars.size() + 2) / 3;
			int maxScrollY = Math.max(0, rows * 20 - 60);

			if (maxScrollY > 0) {
				this.scrollY = Mth.clamp(this.scrollY - (int) scrollY * 10, 0, maxScrollY);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFA151515);
		guiGraphics.renderOutline(getX(), getY(), width, height, 0xFF434343);

		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}

		List<AdvancedItemFilterVariableComponent> vars = getFilteredVariables();

		int gridX = getX() + 47;
		int gridY = getY() + 24;

		// Limit rendering with scissor mask to standard grid box boundaries [3]
		guiGraphics.enableScissor(gridX, gridY, gridX + 58, gridY + 60);

		for (int i = 0; i < matchedVariableSize(vars); i++) {
			AdvancedItemFilterVariableComponent variable = vars.get(i);
			int col = i % 3;
			int row = i / 3;

			// Method names updated to getCellX/getCellY [3]
			int cellX = getCellX(col);
			int cellY = getCellY(row);

			boolean hovered = mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18;

			guiGraphics.blit(DRAWER_TX, cellX, cellY, 0, 0, 18, 18, 18, 18);

			if (hovered) {
				guiGraphics.renderOutline(cellX, cellY, 18, 18, 0xFF8B8B8B);
			}

			ItemStack stack = variable.getFilterStack();
			if (!stack.isEmpty()) {
				guiGraphics.renderItem(stack, cellX + 1, cellY + 1);
				guiGraphics.renderItemDecorations(parentScreen.getFont(), stack, cellX + 1, cellY + 1);
			}
		}

		guiGraphics.flush();
		guiGraphics.disableScissor();

		// Scrollbar
		int rows = (vars.size() + 2) / 3;
		int maxScrollY = Math.max(0, rows * 20 - 60);
		if (maxScrollY > 0) {
			int scrollbarX = getX() + width - 6;
			int scrollbarY = gridY;
			guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 2, scrollbarY + 60, 0x40000000);

			int thumbHeight = (int) ((60.0F / (rows * 20.0F)) * 60.0F);
			thumbHeight = Math.max(10, Math.min(60, thumbHeight));
			int thumbY = scrollbarY + (int) (((float) scrollY / maxScrollY) * (60 - thumbHeight));

			guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 2, thumbY + thumbHeight, 0xFF8B8B8B);
		}

		// Tooltip rendering loop
		if (mouseX >= gridX && mouseX < gridX + 58 && mouseY >= gridY && mouseY < gridY + 60) {
			for (int i = 0; i < vars.size(); i++) {
				int col = i % 3;
				int row = i / 3;

				int cellX = getCellX(col);
				int cellY = getCellY(row);

				if (mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18) {
					guiGraphics.renderTooltip(parentScreen.getFont(), vars.get(i).getName(), mouseX, mouseY);
					break;
				}
			}
		}
	}

	private int matchedVariableSize(List<AdvancedItemFilterVariableComponent> vars) {
		return vars != null ? vars.size() : 0;
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