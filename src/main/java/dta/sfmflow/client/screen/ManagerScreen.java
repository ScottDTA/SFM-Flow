package dta.sfmflow.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.widgets.*;
import dta.sfmflow.client.screen.helper.WorkspaceValidator;
import dta.sfmflow.util.MenuSlotRepositioner;
import dta.sfmflow.client.render.VectorWireRenderer;
import dta.sfmflow.client.screen.helper.FlowLayoutHelper;
import dta.sfmflow.client.screen.helper.GuiScaleManager;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Main visual workspace representing ManagerBlock configurations [3].
 * Incorporates a sliding drawer panel to display dynamic filter cards and
 * warning counters [3].
 */
@OnlyIn(Dist.CLIENT)
public class ManagerScreen extends AbstractContainerScreen<ManagerMenu> {

	private static final ResourceLocation GUI_BG1 = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/background1.png");
	private static final ResourceLocation GUI_BG2 = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/background2.png");

	private static final ResourceLocation PLAYER_INV_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/player_inventory.png");

	private final List<FlowWidgetContainer> componentsToRemove = new ArrayList<>();
	private CategoryHoverSubmenu activeSubmenu = null;
	private AbstractModalPopup activeModalPopup = null;
	private FlowWidgetContainer lastClickedContainer = null;
	private NodeSettingsOverlay activeSettingsOverlay = null;
	private DropdownMenuWidget openedDropdown = null;
	private final ManagerMouseHandler mouseHandler;
	private final int originalGuiScale;
	private final Minecraft mc;

	public ManagerScreen(ManagerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 512;
		this.imageHeight = 256; // Centered on the 256px canvas [3]
		this.mc = Minecraft.getInstance();
		this.originalGuiScale = this.mc.options.guiScale().get();
		this.mouseHandler = new ManagerMouseHandler(this);
	}

	@Override
	protected void init() {
		super.init();

		boolean resized = GuiScaleManager.applyOverrides(this.mc, this.width, this.height, this.originalGuiScale, 512,
				352);
		if (resized) {
			return;
		}

		int x = (width - imageWidth) / 2;
		// Centers the canvas while guaranteeing at least an 8px margin above the player
		// inventory [3]
		int y = Math.min((height - imageHeight) / 2, height - imageHeight - 90 - 8);
		this.leftPos = x;
		this.topPos = y;

		int categoryYOffset = 4;
		for (NodeCategory category : NodeCategory.values()) {
			this.addRenderableWidget(new CategoryButton(category, x + 4, y + categoryYOffset, this));
			categoryYOffset += 16;
		}

		buildComponents(x, y);

		int textureX = (this.width - 176) / 2;
		int textureY = this.height - 90;

		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				int slotIndex = r * 9 + c;
				if (slotIndex < this.menu.slots.size()) {
					Slot slot = this.menu.slots.get(slotIndex);
					MenuSlotRepositioner.setSlotPosition(slot, 8 + c * 18 + textureX - this.leftPos,
							8 + r * 18 + textureY - this.topPos);
				}
			}
		}

		for (int c = 0; c < 9; c++) {
			int slotIndex = 27 + c;
			if (slotIndex < this.menu.slots.size()) {
				Slot slot = this.menu.slots.get(slotIndex);
				MenuSlotRepositioner.setSlotPosition(slot, 8 + c * 18 + textureX - this.leftPos,
						66 + textureY - this.topPos);
			}
		}

		// Add sliding variable drawer widget aligned with the player inventory [3]
		this.addRenderableWidget(new VariableDrawerWidget(this, x + 344, this.height - 86, 75, 82));

		if (this.activeModalPopup != null) {
			int pWidth = this.activeModalPopup.getWidth();
			int pHeight = this.activeModalPopup.getHeight();
			int targetX = (this.width - pWidth) / 2;
			int targetY = (this.height - pHeight) / 2;
			if (this.activeModalPopup.getX() != targetX) {
				this.activeModalPopup.setX(targetX);
			}
			if (this.activeModalPopup.getY() != targetY) {
				this.activeModalPopup.setY(targetY);
			}
		}

		// Centers the active settings overlay only if the screen coordinates have changed [3]
		if (this.activeSettingsOverlay != null) {
			int pWidth = this.activeSettingsOverlay.getWidth();
			int pHeight = this.activeSettingsOverlay.getHeight();
			int targetX = (this.width - pWidth) / 2;
			int targetY = this.getOverlayTargetY(pHeight); // Purely dynamic calculations [3]
			
			if (this.activeSettingsOverlay.getX() != targetX) {
				this.activeSettingsOverlay.setX(targetX);
			}
			if (this.activeSettingsOverlay.getY() != targetY) {
				this.activeSettingsOverlay.setY(targetY);
			}
		}
	}

	@Override
	public void removed() {
		super.removed();
		this.mc.tell(() -> {
			if (this.mc.options.guiScale().get() != this.originalGuiScale) {
				this.mc.options.guiScale().set(this.originalGuiScale);
				this.mc.resizeDisplay();
			}
		});
	}

	private void buildComponents(int x, int y) {
		List<FlowWidgetContainer> componentContainers = new ArrayList<>();
		for (AbstractFlowComponent component : this.getMenu().getManagerBlockEntity().getFlowComponents().values()) {
			FlowWidgetContainer componetContainer = new FlowWidgetContainer(this, component, x, y);
			componentContainers.add(componetContainer);
		}
		componentContainers.sort(Comparator.comparing(FlowWidgetContainer::getZ));
		for (int i = 0; i < componentContainers.size(); i++) {
			this.addRenderableWidget(componentContainers.get(i));
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, GUI_BG1);
		int x = this.leftPos;
		int y = this.topPos;

		guiGraphics.blit(GUI_BG1, x, y, 0, 0, 256, 256);
		RenderSystem.setShaderTexture(0, GUI_BG2);
		guiGraphics.blit(GUI_BG2, x + 256, y, 0, 0, 256, 256);

		int textureX = (this.width - 176) / 2;
		int textureY = this.height - 90;
		guiGraphics.blit(PLAYER_INV_TX, textureX, textureY, 0, 0, 176, 90, 176, 90);

		VectorWireRenderer.renderWires(guiGraphics, this, mouseX, mouseY, partialTick);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		if (!componentsToRemove.isEmpty()) {
			this.renderables.removeAll(componentsToRemove);
			this.children().removeAll(componentsToRemove);
			this.componentsToRemove.clear();
		}

		this.mouseHandler.updateTopHoveredElement(mouseX, mouseY);
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

		int x = this.leftPos;
		int y = this.topPos;

		// Canvas boundary dimming: Drawn BEFORE super.render to keep slots on top [3]
		if (this.activeSettingsOverlay != null && this.activeSettingsOverlay.visible) {
			guiGraphics.fill(x, y, x + 512, y + 256, 0xD0000000);
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);

		// Render Left Panel variable entries aligned with player inventory [3]
		guiGraphics.enableScissor(x + 4, this.height - 90, x + 166, this.height);
		var groupVars = getMenu().getManagerBlockEntity().getGroupVariables();
		for (int i = 0; i < groupVars.size(); i++) {
			var varItem = groupVars.get(i);
			int entryX = x + 4;
			int entryY = (this.height - 90) + 4 + i * 16;
			boolean hovered = mouseX >= entryX && mouseX < entryX + 162 && mouseY >= entryY && mouseY < entryY + 14;

			guiGraphics.fill(entryX, entryY, entryX + 162, entryY + 14, hovered ? 0xFF555555 : 0xFF222222);
			guiGraphics.renderOutline(entryX, entryY, 162, 14, 0xFFD4AF37);
			guiGraphics.drawString(font, varItem.name(), entryX + 4, entryY + 3, 0xFFFFFFFF, false);
		}
		guiGraphics.disableScissor();

		if (this.mouseHandler.isDraggingVariable()) {
			int drawX = mouseX - 40;
			int drawY = mouseY - 7;
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, 700.0F);
			guiGraphics.fill(drawX, drawY, drawX + 80, drawY + 14, 0xAA222222);
			guiGraphics.renderOutline(drawX, drawY, 80, 14, 0xAAD4AF37);
			guiGraphics.drawString(font, this.mouseHandler.getDraggedVariableName(), drawX + 4, drawY + 3, 0xAAFFFFFF,
					false);
			guiGraphics.pose().popPose();
		}

		CategoryButton hoveredCategoryButton = null;
		for (Renderable renderable : this.renderables) {
			if (renderable instanceof CategoryButton catBtn) {
				if (catBtn.isMouseOver(mouseX, mouseY)) {
					hoveredCategoryButton = catBtn;
					break;
				}
			}
		}

		if (hoveredCategoryButton != null) {
			if (this.activeSubmenu == null || this.activeSubmenu.getCategory() != hoveredCategoryButton.getCategory()) {
				this.activeSubmenu = new CategoryHoverSubmenu(hoveredCategoryButton.getCategory(),
						hoveredCategoryButton.getX() + 7, hoveredCategoryButton.getY(), this);
			}
		} else if (this.activeSubmenu != null && !this.activeSubmenu.isHoveredOrFocused(mouseX, mouseY)) {
			this.activeSubmenu = null;
		}

		int maxZ = 0;
		for (Renderable r : this.renderables) {
			if (r instanceof FlowWidgetContainer other) {
				int otherZ = other.getComponent().getZ();
				if (otherZ > maxZ) {
					maxZ = otherZ;
				}
			}
		}
		float baseZ = (maxZ * 2.0F) + 5.0F;

		RenderSystem.clear(256, Minecraft.ON_OSX);

		if (this.activeSubmenu != null) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 300.0F);
			this.activeSubmenu.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.openedDropdown != null) {
			RenderSystem.clear(256, Minecraft.ON_OSX);
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 400.0F);
			this.openedDropdown.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.activeSettingsOverlay != null && this.activeSettingsOverlay.visible) {
			RenderSystem.clear(256, Minecraft.ON_OSX);
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 500.0F);
			this.activeSettingsOverlay.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.activeModalPopup != null && this.activeModalPopup.visible) {
			RenderSystem.clear(256, Minecraft.ON_OSX);
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 800.0F);
			this.activeModalPopup.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		boolean hasActiveOverlay = (this.activeSettingsOverlay != null && this.activeSettingsOverlay.visible)
				|| (this.activeModalPopup != null && this.activeModalPopup.visible) || (this.openedDropdown != null);

		if (hasActiveOverlay) {
			ItemStack carried = this.menu.getCarried();
			if (!carried.isEmpty()) {
				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 1000.0F);
				guiGraphics.renderItem(carried, mouseX - 8, mouseY - 8);
				guiGraphics.renderItemDecorations(this.font, carried, mouseX - 8, mouseY - 8);
				guiGraphics.pose().popPose();
			}
		}

		// Fix: Push the container's standard slot tooltips onto a high Z-layer to
		// prevent overlay overlap [3]
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 1000.0F);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		guiGraphics.pose().popPose();

		for (Renderable renderable : this.renderables) {
			resetFlagsRecursive(renderable);
		}
		if (this.mouseHandler.getTopHoveredElement() instanceof AbstractFlowWidget widget) {
			widget.setShowCustomTooltip(true);
			widget.setIsHovered(true);
		}
	}

	public FlowWidgetContainer getContainerOfWidget(GuiEventListener element) {
		for (Renderable r : this.renderables) {
			if (r instanceof FlowWidgetContainer container) {
				if (container.children().contains(element)) {
					return container;
				}
			}
		}
		return null;
	}

	public FlowWidgetBase getBaseOfWidget(GuiEventListener element) {
		for (Renderable r : this.renderables) {
			if (r instanceof FlowWidgetContainer container) {
				for (GuiEventListener child : container.children()) {
					if (child instanceof FlowWidgetBase base) {
						if (FlowLayoutHelper.isAncestorOf(base, element)) {
							return base;
						}
					}
				}
			}
		}
		return null;
	}

	private void resetFlagsRecursive(Renderable renderable) {
		if (renderable instanceof AbstractFlowWidget widget) {
			widget.setShowCustomTooltip(false);
			widget.setIsHovered(false);
			for (GuiEventListener child : widget.children()) {
				if (child instanceof Renderable childRenderable) {
					resetFlagsRecursive(childRenderable);
				}
			}
		} else if (renderable instanceof FlowWidgetContainer container) {
			for (GuiEventListener child : container.children()) {
				if (child instanceof Renderable childRenderable) {
					resetFlagsRecursive(childRenderable);
				}
			}
		}
	}

	public void refreshWidgetLayout() {
		this.clearWidgets();
		this.init();
	}

	public void removeComponent(FlowWidgetContainer container) {
		this.componentsToRemove.add(container);
	}

	public AbstractModalPopup getActiveModalPopup() {
		return this.activeModalPopup;
	}

	public void setActiveModalPopup(AbstractModalPopup popup) {
		this.activeModalPopup = popup;
		if (popup != null) {
			this.setFocused(popup);
		} else {
			this.setFocused(null);
		}
	}

	public NodeSettingsOverlay getActiveSettingsOverlay() {
		return this.activeSettingsOverlay;
	}

	public void setActiveSettingsOverlay(NodeSettingsOverlay overlay) {
		this.activeSettingsOverlay = overlay;
		if (overlay != null) {
			this.setFocused(overlay);
		} else {
			this.setFocused(null);
		}
	}

	public DropdownMenuWidget getOpenedDropdown() {
		return this.openedDropdown;
	}

	public void setOpenedDropdown(DropdownMenuWidget dropdown) {
		this.openedDropdown = dropdown;
	}

	public void closeContextMenu() {
		this.openedDropdown = null;
	}

	public CategoryHoverSubmenu getActiveSubmenu() {
		return this.activeSubmenu;
	}

	public void setActiveSubmenu(CategoryHoverSubmenu submenu) {
		this.activeSubmenu = submenu;
	}

	public List<Renderable> getRenderables() {
		return this.renderables;
	}

	public FlowWidgetContainer getLastClickedContainer() {
		return this.lastClickedContainer;
	}

	public void setLastClickedContainer(FlowWidgetContainer container) {
		this.lastClickedContainer = container;
	}

	public void setRefreshCooldown(int ticks) {
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean handled = this.mouseHandler.mouseClicked(mouseX, mouseY, button);
		return handled || super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public List<? extends GuiEventListener> children() {
		if (this.activeSettingsOverlay != null && this.activeSettingsOverlay.visible) {
			return List.of(this.activeSettingsOverlay);
		}
		return super.children();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			if (this.activeModalPopup != null) {
				this.activeModalPopup.close();
				return true;
			}
			if (this.activeSettingsOverlay != null) {
				this.activeSettingsOverlay.closeAndSave();
				return true;
			}
			if (this.openedDropdown != null) {
				this.openedDropdown = null;
				return true;
			}
		}

		if (this.activeModalPopup != null) {
			this.activeModalPopup.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}

		if (this.activeSettingsOverlay != null) {
			this.activeSettingsOverlay.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}

		if (this.openedDropdown != null) {
			if (this.openedDropdown.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		}

		// Prevent keypresses (like "E") from closing the screen when an edit box is
		// focused [3]
		if (this.getFocused() != null) {
			if (this.getFocused().keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
			if (isEditingText(this.getFocused())) {
				if (keyCode == 256) { // Escape key should still close [3]
					return super.keyPressed(keyCode, scanCode, modifiers);
				}
				return true;
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private boolean isEditingText(GuiEventListener listener) {
		if (listener instanceof ApiWidgetAdapter<?> adapter) {
			return adapter.getVanillaWidget() instanceof EditBox;
		}
		return listener instanceof EditBox;
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (this.activeModalPopup != null) {
			this.activeModalPopup.charTyped(codePoint, modifiers);
			return true;
		}

		if (this.activeSettingsOverlay != null) {
			this.activeSettingsOverlay.charTyped(codePoint, modifiers);
			return true;
		}

		if (this.openedDropdown != null) {
			if (this.openedDropdown.charTyped(codePoint, modifiers)) {
				return true;
			}
		}
		return super.charTyped(codePoint, modifiers);
	}

	public int getImageWidth() {
		return this.imageWidth;
	}

	public int getImageHeight() {
		return this.imageHeight;
	}

	public int getLeftPos() {
		return this.leftPos;
	}

	public int getTopPos() {
		return this.topPos;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		boolean handled = this.mouseHandler.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		return handled || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		boolean handled = this.mouseHandler.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		return handled || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = this.mouseHandler.mouseReleased(mouseX, mouseY, button);

		if (this.getFocused() != null) {
			this.getFocused().mouseReleased(mouseX, mouseY, button);
		}
		return handled || super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		int commandWidth = this.font.width(Component.translatable("gui.sfmflow.commands", getMenu().getCommandCount()));
		guiGraphics.drawString(this.font, Component.translatable("gui.sfmflow.commands", getMenu().getCommandCount()),
				4, 244, 4210752, false);

		int errorCount = 0;
		int warningCount = 0;
		for (var comp : getMenu().getManagerBlockEntity().getFlowComponents().values()) {
			if (WorkspaceValidator.hasUnboundInventoryError(this, comp)) {
				errorCount++;
			}
			if (WorkspaceValidator.hasEmptyFilterVariableWarning(this, comp)) {
				warningCount++;
			}
		}

		int currentOffset = 4 + commandWidth + 12;

		if (errorCount > 0) {
			int errorWidth = this.font.width(Component.translatable("gui.sfmflow.errors", errorCount));
			guiGraphics.drawString(this.font, Component.translatable("gui.sfmflow.errors", errorCount), currentOffset,
					244, 0xFFD00000, false);
			currentOffset += errorWidth + 12;
		}

		if (warningCount > 0) {
			guiGraphics.drawString(this.font, Component.translatable("gui.sfmflow.warnings", warningCount),
					currentOffset, 244, 0xFFFED83D, false); // Yellow warning color 0xFED83D [3]
		}
	}

	public Font getFont() {
		return this.font;
	}

	public boolean isElementHoverable(GuiEventListener element) {
		return this.mouseHandler.isElementHoverable(element);
	}

	public ManagerMouseHandler getMouseHandler() {
		return this.mouseHandler;
	}
	
	/**
	 * Computes the correct, safe centered Y coordinate for any overlay panel based on its height [3].
	 * Centers the panel vertically relative to the active canvas and enforces safety boundaries [3].
	 *
	 * @param pHeight the physical height of the overlay panel [3]
	 * @return the safe centered Y coordinate [3]
	 */
	public int getOverlayTargetY(int pHeight) {
		int targetY = this.topPos + (256 - pHeight) / 2;
		return Math.max(25, targetY); // Prevent clipping under standard screen top title bounds [3]
	}
	
}