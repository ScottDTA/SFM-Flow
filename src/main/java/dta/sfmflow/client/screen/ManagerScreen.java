package dta.sfmflow.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.widgets.*;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Main visual workspace representing ManagerBlock configurations [3]. Delegates
 * mouse interactions directly to a clean helper handler class [3]. Upgraded to
 * draw wires directly inside renderBg under the Painter's Algorithm [3].
 */
@OnlyIn(Dist.CLIENT)
public class ManagerScreen extends AbstractContainerScreen<ManagerMenu> {

	private static final ResourceLocation GUI_BG1 = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/background1.png");
	private static final ResourceLocation GUI_BG2 = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/background2.png");

	private final List<FlowWidgetContainer> componentsToRemove = new ArrayList<>();
	private CategoryHoverSubmenu activeSubmenu = null;
	private AbstractModalPopup activeModalPopup = null;
	private FlowWidgetContainer lastClickedContainer = null;
	private NodeSettingsOverlay activeSettingsOverlay = null;
	private DropdownMenuWidget openedDropdown = null;
	private final ManagerMouseHandler mouseHandler;
	private int refreshCooldown = 0;
	private final int originalGuiScale;
	private final Minecraft mc;

	public ManagerScreen(ManagerMenu menu, net.minecraft.world.entity.player.Inventory playerInventory,
			Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 512;
		this.imageHeight = 352;
		this.mc = Minecraft.getInstance();
		this.originalGuiScale = this.mc.options.guiScale().get();
		this.mouseHandler = new ManagerMouseHandler(this);
	}

	/**
	 * Evaluates if a given component has an unbound connected inventory error [3].
	 * Upgraded to verify that the selected inventory ID actually exists on the
	 * network [3].
	 *
	 * @param screen    active screen manager [3]
	 * @param component flow component query [3]
	 * @return true if the component has an unbound inventory error [3]
	 */
	public static boolean hasUnboundInventoryError(ManagerScreen screen, AbstractFlowComponent component) {
		if (component instanceof dta.sfmflow.flowcomponents.ItemTransferComponent transfer) {
			// 1. Check if bound inventory is actively connected to the network [3]
			boolean foundBoundInventory = false;
			if (transfer.getInventoryId() != -1) {
				for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
					if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
						foundBoundInventory = true;
						break;
					}
				}
			}

			// Flag error if unassigned or if the bound chest is disconnected [3]
			if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
				var connections = screen.getMenu().getManagerBlockEntity().getFlowConnections();
				for (var conn : connections) {
					if (conn.getSourceComponentId().equals(transfer.getId())
							|| conn.getTargetComponentId().equals(transfer.getId())) {
						return true;
					}
				}
			}

			// 2. Empty Whitelist validation check [3]
			if (transfer.isWhitelist()) {
				boolean empty = true;
				for (ItemStack stack : transfer.getFilterItems()) {
					if (stack != null && !stack.isEmpty()) {
						empty = false;
						break;
					}
				}
				if (empty) {
					var connections = screen.getMenu().getManagerBlockEntity().getFlowConnections();
					for (var conn : connections) {
						if (conn.getSourceComponentId().equals(transfer.getId())
								|| conn.getTargetComponentId().equals(transfer.getId())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void init() {
		super.init();

		int requiredWidth = 512;
		int requiredHeight = 352;
		int rawWidth = this.mc.getWindow().getWidth();
		int rawHeight = this.mc.getWindow().getHeight();
		int currentScale = this.mc.options.guiScale().get();

		boolean scaleApplied = false;

		int forcedScale = dta.sfmflow.ClientConfig.FORCE_GUI_SCALE.get();
		if (forcedScale > 0) {
			int testWidth = rawWidth / forcedScale;
			int testHeight = rawHeight / forcedScale;

			if (testWidth >= requiredWidth && testHeight >= requiredHeight) {
				if (currentScale != forcedScale) {
					this.mc.options.guiScale().set(forcedScale);
					this.mc.resizeDisplay();
					return;
				}
				scaleApplied = true;
			} else {
				if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
					//SFMFlow.LOGGER.warn(
					//		"[SFM-Flow] Window size is too small for configured FORCE_GUI_SCALE ({}). Falling back to adaptive scaling.",
					//		forcedScale);
				}
			}
		}

		if (!scaleApplied) {
			int actualScale = (int) this.mc.getWindow().getGuiScale();

			if ((this.width < requiredWidth || this.height < requiredHeight) && actualScale > 1) {
				int targetScale = (currentScale == 0) ? (actualScale - 1) : (currentScale - 1);
				if (currentScale != targetScale) {
					this.mc.options.guiScale().set(targetScale);
					this.mc.resizeDisplay();
					return;
				}
			}

			int maxPossibleScale = Math.max(1, Math.min(rawWidth / 320, rawHeight / 240));
			int maxScaleLimit = (this.originalGuiScale == 0) ? maxPossibleScale : this.originalGuiScale;

			if (currentScale > 0 && currentScale < maxScaleLimit) {
				int nextScale = currentScale + 1;
				int testWidth = rawWidth / nextScale;
				int testHeight = rawHeight / nextScale;

				if (testWidth >= requiredWidth && testHeight >= requiredHeight) {
					int targetScale = (nextScale == maxScaleLimit && this.originalGuiScale == 0) ? 0 : nextScale;
					if (currentScale != targetScale) {
						this.mc.options.guiScale().set(targetScale);
						this.mc.resizeDisplay();
						return;
					}
				}
			}
		}

		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		this.leftPos = x;
		this.topPos = y;

		this.addRenderableWidget(new CanvasActionButton(CanvasAction.COPY, this, x + 38, y + 4));
		this.addRenderableWidget(new CanvasActionButton(CanvasAction.DELETE, this, x + 22, y + 4));

		int categoryYOffset = 4;
		for (NodeCategory category : NodeCategory.values()) {
			this.addRenderableWidget(new CategoryButton(category, x + 4, y + categoryYOffset, this));
			categoryYOffset += 16;
		}

		buildComponents(x, y);

		if (this.activeModalPopup != null) {
			int pWidth = this.activeModalPopup.getWidth();
			int pHeight = this.activeModalPopup.getHeight();
			this.activeModalPopup.setX((this.width - pWidth) / 2);
			this.activeModalPopup.setY((this.height - pHeight) / 2);
		}

		if (this.activeSettingsOverlay != null) {
			int pWidth = this.activeSettingsOverlay.getWidth();
			int pHeight = this.activeSettingsOverlay.getHeight();
			this.activeSettingsOverlay.setX((this.width - pWidth) / 2);
			this.activeSettingsOverlay.setY((256 - pHeight) / 2);
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
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		guiGraphics.blit(GUI_BG1, x, y, 0, 0, 256, 256);
		RenderSystem.setShaderTexture(0, GUI_BG2); // Standard unit 0 mapping [3]
		guiGraphics.blit(GUI_BG2, x + 256, y, 0, 0, 256, 256);

		guiGraphics.fill(x, y + 256, x + 512, y + 352, 0xFF2B2B2B);
		guiGraphics.renderOutline(x, y + 256, 512, 96, 0xFFD4AF37);

		guiGraphics.fill(x + 170, y + 256, x + 172, y + 352, 0xFF151515);
		guiGraphics.fill(x + 342, y + 256, x + 344, y + 352, 0xFF151515);

		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				int slotX = x + 174 + c * 18;
				int slotY = y + 265 + r * 18;
				guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF151515);
				guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF434343);
			}
		}
		for (int c = 0; c < 9; c++) {
			int slotX = x + 174 + c * 18;
			int slotY = y + 323;
			guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF151515);
			guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF434343);
		}

		// 🔥 PAINTER'S ALGORITHM: Draw connection wires directly inside renderBg [3]
		// Drawn on top of standard background panels, but beneath card widgets and
		// overlays [3]
		dta.sfmflow.client.render.VectorWireRenderer.renderWires(guiGraphics, this, mouseX, mouseY, partialTick);
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

		// Call standard render directly. Depth tests disabled to prevent projection
		// clipping [3]
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		int x = this.leftPos;
		int y = this.topPos;

		// Canvas boundary dimming: Symmetrically dims only the top canvas [3]
		if (this.activeSettingsOverlay != null && this.activeSettingsOverlay.visible) {
			guiGraphics.fill(0, 0, this.width, y + 256, 0xD0000000);
		}

		guiGraphics.enableScissor(x + 4, y + 256, x + 166, y + 352);
		var groupVars = getMenu().getManagerBlockEntity().getGroupVariables();
		for (int i = 0; i < groupVars.size(); i++) {
			var varItem = groupVars.get(i);
			int entryX = x + 4;
			int entryY = y + 260 + i * 16;
			boolean hovered = mouseX >= entryX && mouseX < entryX + 162 && mouseY >= entryY && mouseY < entryY + 14;

			guiGraphics.fill(entryX, entryY, entryX + 162, entryY + 14, hovered ? 0xFF555555 : 0xFF222222);
			guiGraphics.renderOutline(entryX, entryY, 162, 14, 0xFFD4AF37);
			guiGraphics.drawString(font, varItem.name(), entryX + 4, entryY + 3, 0xFFFFFFFF, false);
		}
		guiGraphics.disableScissor();

		guiGraphics.enableScissor(x + 346, y + 256, x + 508, y + 352);
		var filterVars = getMenu().getManagerBlockEntity().getFilterVariables();
		for (int i = 0; i < filterVars.size(); i++) {
			var varItem = filterVars.get(i);
			int entryX = x + 346;
			int entryY = y + 260 + i * 16;
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

		if (this.activeSubmenu != null) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ);
			this.activeSubmenu.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.openedDropdown != null) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 2.0F);
			this.openedDropdown.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.activeSettingsOverlay != null && this.activeSettingsOverlay.visible) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 10.0F);
			this.activeSettingsOverlay.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.activeModalPopup != null && this.activeModalPopup.visible) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 20.0F);
			this.activeModalPopup.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		this.renderTooltip(guiGraphics, mouseX, mouseY);

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
					if (child instanceof FlowWidgetBase base && base.children().contains(element)) {
						return base;
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
		this.refreshCooldown = ticks;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean handled = this.mouseHandler.mouseClicked(mouseX, mouseY, button);
		return handled || super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			if (this.activeSettingsOverlay != null) {
				this.activeSettingsOverlay.closeAndSave();
				return true;
			}
			if (this.activeModalPopup != null) {
				this.activeModalPopup.close();
				return true;
			}
			if (this.openedDropdown != null) {
				this.openedDropdown = null;
				return true;
			}
		}

		if (this.activeSettingsOverlay != null) {
			this.activeSettingsOverlay.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}

		if (this.activeModalPopup != null) {
			this.activeModalPopup.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}

		if (this.openedDropdown != null) {
			if (this.openedDropdown.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (this.activeSettingsOverlay != null) {
			this.activeSettingsOverlay.charTyped(codePoint, modifiers);
			return true;
		}

		if (this.activeModalPopup != null) {
			this.activeModalPopup.charTyped(codePoint, modifiers);
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
		for (var comp : getMenu().getManagerBlockEntity().getFlowComponents().values()) {
			if (hasUnboundInventoryError(this, comp)) {
				errorCount++;
			}
		}

		if (errorCount > 0) {
			guiGraphics.drawString(this.font, Component.translatable("gui.sfmflow.errors", errorCount),
					4 + commandWidth + 12, 244, 0xFFD00000, false);
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

	@Override
	protected void containerTick() {
		super.containerTick();
		if (this.refreshCooldown > 0) {
			this.refreshCooldown--;
		}
		if (!this.mouseHandler.isDragging() && this.refreshCooldown == 0
				&& this.getMenu().getManagerBlockEntity().pollNeedsRefresh()) {
			refreshWidgetLayout();
		}
	}

	public void handleDeltaUpdate(SyncComponentDeltaPacket packet) {
		AbstractFlowComponent localComponent = this.getMenu().getManagerBlockEntity().getFlowComponents()
				.get(packet.componentId());

		switch (packet.deltaType()) {
		case MOVE -> {
			if (localComponent != null) {
				localComponent.setX(packet.data().getInt("x"));
				localComponent.setY(packet.data().getInt("y"));
				localComponent.setZ(packet.data().getInt("z"));

				for (Renderable r : this.renderables) {
					if (r instanceof FlowWidgetContainer container
							&& container.getComponent().getId().equals(packet.componentId())) {
						int screenX = (width - imageWidth) / 2 + localComponent.getX();
						int screenY = (height - imageHeight) / 2 + localComponent.getY();
						container.setX(screenX);
						container.setY(screenY);
						break;
					}
				}
			}
		}
		case SETTINGS -> {
			if (localComponent != null) {
				localComponent.loadData(packet.data());
				refreshWidgetLayout();
			}
		}
		case ADD -> {
			AbstractFlowComponent.CODEC.parse(NbtOps.INSTANCE, packet.data())
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse added delta component: {}", err))
					.ifPresent(decoded -> {
						this.getMenu().getManagerBlockEntity().getFlowComponents().put(packet.componentId(), decoded);
						refreshWidgetLayout();
					});
		}
		case REMOVE -> {
			this.getMenu().getManagerBlockEntity().getFlowComponents().remove(packet.componentId());
			this.getMenu().getManagerBlockEntity().getFlowConnections()
					.removeIf(wire -> wire.getSourceComponentId().equals(packet.componentId())
							|| wire.getTargetComponentId().equals(packet.componentId()));
			refreshWidgetLayout();
		}
		}
	}
}