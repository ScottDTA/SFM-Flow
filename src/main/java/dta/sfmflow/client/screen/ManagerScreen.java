package dta.sfmflow.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.SFMFlow;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Main visual workspace representing ManagerBlock configurations [3]. Delegates
 * mouse interactions directly to a clean helper handler class [3]. Features
 * localized scale adjustments, rendering depth protection layers, and side-safe
 * operation [3].
 */
@OnlyIn(Dist.CLIENT)
public class ManagerScreen extends AbstractContainerScreen<ManagerMenu> {

	/**
	 * Resource path locating the left-half background panel asset [3].
	 */
	private static final ResourceLocation GUI_BG1 = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/background1.png");

	/**
	 * Resource path locating the right-half background panel asset [3].
	 */
	private static final ResourceLocation GUI_BG2 = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/background2.png");

	/**
	 * Deferred queue holding components marked for removal to bypass concurrent
	 * modification checks during rendering [3].
	 */
	private final List<FlowWidgetContainer> componentsToRemove = new ArrayList<>();

	/**
	 * Currently visible floating category hover sub-panel, or null [3].
	 */
	private CategoryHoverSubmenu activeSubmenu = null;

	/**
	 * Currently visible blocking overlay modal popup, or null [3].
	 */
	private AbstractModalPopup activeModalPopup = null;

	/**
	 * Reference pointing to the most recently clicked block entity container card
	 * [3].
	 */
	private FlowWidgetContainer lastClickedContainer = null;

	/**
	 * Currently visible component detail customization settings panel, or null [3].
	 */
	private NodeSettingsOverlay activeSettingsOverlay = null;

	/**
	 * Reference pointing to the active context menu dropdown list, or null [3].
	 */
	private DropdownMenuWidget openedDropdown = null;

	/**
	 * Organized Mouse Input Coordinator managing interaction physics [3].
	 */
	private final ManagerMouseHandler mouseHandler;

	/**
	 * Remaining network sync tick-rate throttle ensuring layout changes do not
	 * corrupt active drags [3].
	 */
	private int refreshCooldown = 0;

	/**
	 * Cached system gui scale configuration retrieved on initialization [3].
	 */
	private final int originalGuiScale;

	/**
	 * Localized game client pointer [3].
	 */
	private final Minecraft mc;

	/**
	 * Constructs the main manager screen layout context [3].
	 *
	 * @param menu            the screen's menu container handler [3]
	 * @param playerInventory local player inventory context [3]
	 * @param title           title header text [3]
	 */
	public ManagerScreen(ManagerMenu menu, net.minecraft.world.entity.player.Inventory playerInventory,
			Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 512;
		this.imageHeight = 256;
		this.mc = Minecraft.getInstance();
		this.originalGuiScale = this.mc.options.guiScale().get();
		this.mouseHandler = new ManagerMouseHandler(this);
	}

	@Override
	protected void init() {
		super.init();

		int requiredWidth = 512;
		int requiredHeight = 256;
		int rawWidth = this.mc.getWindow().getWidth();
		int rawHeight = this.mc.getWindow().getHeight();
		int currentScale = this.mc.options.guiScale().get();

		boolean scaleApplied = false;

		// Pass 1: Forced Scale Evaluation [3]
		int forcedScale = dta.sfmflow.ClientConfig.FORCE_GUI_SCALE.get();
		if (forcedScale > 0) {
			int testWidth = rawWidth / forcedScale;
			int testHeight = rawHeight / forcedScale;

			if (testWidth >= requiredWidth && testHeight >= requiredHeight) {
				if (currentScale != forcedScale) {
					this.mc.options.guiScale().set(forcedScale);
					this.mc.resizeDisplay();
					return; // Exit initialization immediately to let Minecraft refresh the screen state
							// [3].
				}
				scaleApplied = true;
			} else {
				SFMFlow.LOGGER.warn(
						"[SFM-Flow] Window size is too small for configured FORCE_GUI_SCALE ({}). Falling back to adaptive scaling.",
						forcedScale);
			}
		}

		// Pass 2: Adaptive Fallback Scaling (Only run if forced scale was not applied)
		// [3]
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

		// Scale Restoration Shield: Ensure scale is reverted cleanly only if it differs
		// from the original cached setting [3].
		this.mc.tell(() -> {
			if (this.mc.options.guiScale().get() != this.originalGuiScale) {
				this.mc.options.guiScale().set(this.originalGuiScale);
				this.mc.resizeDisplay();
			}
		});
	}

	/**
	 * Sorts and populates all visual node card containers retrieved from the
	 * manager entity [3].
	 *
	 * @param x parent boundary offset alignment on X axis [3]
	 * @param y parent boundary offset alignment on Y axis [3]
	 */
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
		RenderSystem.setShaderTexture(1, GUI_BG2);
		guiGraphics.blit(GUI_BG2, x + 256, y, 0, 0, 256, 256);
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

		RenderSystem.enableDepthTest();
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		// 🔥 MILESTONE 1.10: Vector Wire Rendering Pass [3]
		// Drawn on top of standard components, but beneath hover panels and dropdowns
		// [3]
		dta.sfmflow.client.render.VectorWireRenderer.renderWires(guiGraphics, this, mouseX, mouseY, partialTick);

		RenderSystem.disableDepthTest();

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
		// Shift menu overlays up to +150.0F to render beautifully on top of shifted
		// card elements [3]
		float baseZ = (maxZ * 10.0F) + 150.0F;

		if (this.activeSubmenu != null) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ);
			this.activeSubmenu.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.openedDropdown != null) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 10.0F);
			this.openedDropdown.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.activeSettingsOverlay != null && this.activeSettingsOverlay.visible) {
			guiGraphics.fill(0, 0, this.width, this.height, 0xD0000000);

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, 600.0F);
			this.activeSettingsOverlay.render(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.flush();
			guiGraphics.pose().popPose();
		}

		if (this.activeModalPopup != null && this.activeModalPopup.visible) {
			guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, baseZ + 200.0F);
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

	/**
	 * Recursively resets interaction flags (hovered, tooltip visibility) on the
	 * widget tree [3].
	 *
	 * @param renderable the root renderable to reset [3]
	 */
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

	/**
	 * Clears existing visual component widgets and completely rebuilds the layout
	 * [3]. This is triggered during server delta syncs or layout refreshes [3].
	 */
	public void refreshWidgetLayout() {
		this.clearWidgets();
		this.init();
	}

	/**
	 * Schedules a container to be safely removed from the rendering and event
	 * handling pipelines on the next render pass [3].
	 *
	 * @param container the container widget to remove [3]
	 */
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

	/**
	 * Package-private getter allowing the mouse coordinator subclass to
	 * compile-safely access elements [3].
	 *
	 * @return renderables list [3]
	 */
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
		guiGraphics.drawString(this.font, Component.translatable("gui.sfmflow.commands", getMenu().getCommandCount()),
				4, 244, 4210752, false);
	}

	public Font getFont() {
		return this.font;
	}

	public boolean isElementHoverable(GuiEventListener element) {
		return this.mouseHandler.isElementHoverable(element);
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

	/**
	 * Unpacks a received serverbound node layout delta packet, updating client-side
	 * data maps [3].
	 *
	 * @param packet sync component layout descriptor packet [3]
	 */
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

	/**
	 * Retrieves the centralized Mouse Input Coordinator managing interaction
	 * physics [3].
	 *
	 * @return the screen's mouseHandler context [3]
	 */
	public ManagerMouseHandler getMouseHandler() {
		return this.mouseHandler;
	}

}