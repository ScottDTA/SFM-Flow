package dta.sfmflow.client.screen.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.systems.RenderSystem;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.client.NineSliceUtil;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.serverbound.CreateNodePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A sliding hover submenu displaying creator nodes mapped to a hovered sidebar
 * category. Features 9-slice background scaling, centered and dynamically
 * scaled FlowWidgetText headers, OpenGL scissor masking, customizable
 * width/column structures, and button hover states with custom padding.
 */
@OnlyIn(Dist.CLIENT)
public class CategoryHoverSubmenu extends AbstractFlowWidget {

	private final NodeCategory category;
	private final ManagerScreen parentScreen;
	private final List<FlowComponentType> matchedTypes = new ArrayList<>();
	private double scrollOffset = 0;
	
	private final int rows;
	private final int numColumns;
	private final int gridWidth;
	private final int gridHeight;
	private final int scrollbarWidth;
	private final int startXOffset;

	private final int cellSize = 14;
	private final int cellPadding = 4;
	private final int cellStride = cellSize + cellPadding;

	/**
	 * Submenu title label, powered by FlowWidgetText for scaled ellipsis support.
	 */
	private final FlowWidgetText titleWidget;

	public CategoryHoverSubmenu(NodeCategory category, int x, int y, ManagerScreen parentScreen) {
		super(x, y, 68, 24, Component.literal(category.name()));
		this.category = category;
		this.parentScreen = parentScreen;

		// Filter registry for enabled nodes matching the target category
		for (FlowComponentType type : FlowComponentType.REGISTRY) {
			INodeClientProperties props = FlowClientRegistry.getProperties(type);
			if (props != null && props.getCategory() == category && props.isEnabled().get()) {
				this.matchedTypes.add(type);
			}
		}

		// Dynamically calculate grid columns and scroll boundaries with padding
		this.numColumns = Math.max(1, Math.min(4, matchedTypes.size()));
		this.gridWidth = numColumns * cellSize + (numColumns - 1) * cellPadding;
		this.scrollbarWidth = (matchedTypes.size() > 16) ? 8 : 0;

		// Scale width dynamically (min 48px to prevent extreme title squeezing)
		this.width = Math.max(48, 6 + gridWidth + scrollbarWidth + 6);

		// Center grid columns horizontally if they don't occupy full 4-column space
		int availableGridSpace = this.width - 12 - scrollbarWidth;
		this.startXOffset = 6 + (availableGridSpace - gridWidth) / 2;

		this.rows = (matchedTypes.size() + numColumns - 1) / numColumns;
		this.gridHeight = Math.min(50, rows * cellStride - cellPadding);
		this.height = 6 + 12 + gridHeight + 6;

		// Ensure layout coordinates clamp perfectly inside vertical viewport boundaries to prevent cutoff
		int maxY = parentScreen.height - this.height - 4;
		this.setY(Math.max(4, Math.min(y, maxY)));

		// Set up dynamically scaled FlowWidgetText for the category name header
		Component titleText = Component
				.translatable("gui.sfmflow.menu." + category.name().toLowerCase(Locale.ROOT));
		int titleWidth = parentScreen.getFont().width(titleText);
		float titleScale = 0.8F;

		int availableTitleWidth = this.width - 8;
		int titleX = this.getX() + 4;
		int titleY = this.getY() + 4;

		if (titleWidth * titleScale > availableTitleWidth) {
			titleScale = (float) availableTitleWidth / titleWidth;
			titleScale = Math.max(0.4F, titleScale); // Clamp to a minimum of 40% scale
		}

		this.titleWidget = new FlowWidgetText(parentScreen.getFont(), titleX, titleY, availableTitleWidth, 10,
				titleText, titleScale, true
		);
	}

	public NodeCategory getCategory() {
		return category;
	}

	public boolean isHoveredOrFocused(double mouseX, double mouseY) {
		return mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.visible && isHoveredOrFocused(mouseX, mouseY) && maxScroll() > 0) {
			this.scrollOffset = Mth.clamp(this.scrollOffset - scrollY * 6, 0, maxScroll());
			return true;
		}
		return false;
	}

	private int maxScroll() {
		return Math.max(0, rows * cellStride - cellPadding - 50);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.visible && isHoveredOrFocused(mouseX, mouseY)) {
			int gridX = getX() + startXOffset;
			int gridY = getY() + 18;

			if (mouseX >= gridX && mouseX < gridX + gridWidth && mouseY >= gridY && mouseY < gridY + this.gridHeight) {
				int col = -1;
				int row = -1;

				// Evaluate columns cleanly
				for (int c = 0; c < numColumns; c++) {
					int cellX = gridX + c * cellStride;
					if (mouseX >= cellX && mouseX < cellX + cellSize) {
						col = c;
						break;
					}
				}

				// Evaluate rows cleanly
				for (int r = 0; r < rows; r++) {
					int cellY = gridY + r * cellStride - (int) scrollOffset;
					if (mouseY >= cellY && mouseY < cellY + cellSize) {
						row = r;
						break;
					}
				}

				if (col != -1 && row != -1) {
					int index = row * numColumns + col;
					if (index >= 0 && index < matchedTypes.size()) {
						FlowComponentType clickedType = matchedTypes.get(index);
						ResourceLocation typeLoc = FlowComponentType.REGISTRY.getKey(clickedType);
						if (typeLoc != null) {
							PacketDistributor.sendToServer(new CreateNodePacket(
									parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), typeLoc, parentScreen.getCurrentGroupId()));
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		render9SliceBackground(guiGraphics);

		this.titleWidget.render(guiGraphics, mouseX, mouseY, partialTick);

		int gridX = getX() + startXOffset;
		int gridY = getY() + 18;

		// Expanded horizontal scissor limits to fully support centered layout columns
		guiGraphics.enableScissor(getX() + 6, gridY, getX() + width - 6, gridY + this.gridHeight);

		for (int i = 0; i < matchedTypes.size(); i++) {
			int col = i % numColumns;
			int row = i / numColumns;

			int itemX = gridX + col * cellStride;
			int itemY = gridY + row * cellStride - (int) scrollOffset;

			FlowComponentType type = matchedTypes.get(i);
			INodeClientProperties props = FlowClientRegistry.getProperties(type);
			if (props != null) {
				int vOffset = 0;
				if (mouseX >= itemX && mouseX < itemX + cellSize && mouseY >= itemY && mouseY < itemY + cellSize) {
					vOffset = cellSize;
				}
				guiGraphics.blit(props.getIconTexture(), itemX, itemY, 0, vOffset, cellSize, cellSize, cellSize, cellSize * 2);
			}
		}

		guiGraphics.disableScissor();

		// Scrollbar rendering
		if (maxScroll() > 0) {
			int scrollbarX = getX() + width - 6 - 2;
			int scrollbarY = getY() + 18;
			int scrollbarHeight = this.gridHeight;

			guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x40000000);

			int thumbHeight = (int) (((double) this.gridHeight / (rows * cellStride - cellPadding)) * this.gridHeight);
			thumbHeight = Math.max(10, Math.min(this.gridHeight, thumbHeight));
			int thumbY = scrollbarY + (int) ((scrollOffset / maxScroll()) * (this.gridHeight - thumbHeight));

			guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFF8B8B8B);
		}

		// Grid hovering cell tooltip rendering pass
		if (mouseX >= gridX && mouseX < gridX + gridWidth && mouseY >= gridY && mouseY < gridY + this.gridHeight) {
			int col = -1;
			int row = -1;

			for (int c = 0; c < numColumns; c++) {
				int cellX = gridX + c * cellStride;
				if (mouseX >= cellX && mouseX < cellX + cellSize) {
					col = c;
					break;
				}
			}

			for (int r = 0; r < rows; r++) {
				int cellY = gridY + r * cellStride - (int) scrollOffset;
				if (mouseY >= cellY && mouseY < cellY + cellSize) {
					row = r;
					break;
				}
			}

			if (col != -1 && row != -1) {
				int index = row * numColumns + col;
				if (index >= 0 && index < matchedTypes.size()) {
					FlowComponentType hoveredType = matchedTypes.get(index);
					INodeClientProperties props = FlowClientRegistry.getProperties(hoveredType);
					if (props != null) {
						guiGraphics.renderTooltip(parentScreen.getFont(), props.getDisplayName(), mouseX, mouseY);
					}
				}
			}
		}
	}

	private void render9SliceBackground(GuiGraphics guiGraphics) {
		NineSliceUtil.drawDefault(guiGraphics, getX(), getY(), width, height);
	}
}