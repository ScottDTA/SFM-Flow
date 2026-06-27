package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.event.RegisterDropdownLinksEvent;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.helper.FlowTextLink;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import java.util.ArrayList;
import java.util.List;

/**
 * Compact right-click visual dropdown overlay replacing old ContextMenuOverlay
 * [3]. Houses scrolling logic, viewport scissor bounds, and launches settings
 * modals [3].
 */
@OnlyIn(Dist.CLIENT)
public class DropdownMenuWidget extends AbstractFlowWidget {
	private final FlowWidgetContainer targetContainer;
	private final ManagerScreen parentScreen;
	private final List<FlowTextLink> links = new ArrayList<>();
	private int scrollOffset = 0;
	private final int totalContentHeight;
	private final boolean needsScrolling;

	/**
	 * Initializes a Dropdown Menu with a fixed width of 84px and dynamic scrollable
	 * height [3].
	 *
	 * @param targetContainer the card containing targeted properties [3]
	 * @param mouseX          click placement coordinate [3]
	 * @param mouseY          click placement coordinate [3]
	 * @param parentScreen    parent screen interface [3]
	 */
	public DropdownMenuWidget(FlowWidgetContainer targetContainer, int mouseX, int mouseY, ManagerScreen parentScreen) {
		super(mouseX, mouseY, 84, 56, Component.literal("Dropdown Menu"));
		this.targetContainer = targetContainer;
		this.parentScreen = parentScreen;

		// Compile default options first
		RegisterDropdownLinksEvent event = new RegisterDropdownLinksEvent(targetContainer.getComponent());
		event.addLink(Component.literal("Rename Node"), () -> {
			RenameModalPopup renamePopup = new RenameModalPopup(parentScreen, targetContainer);
			parentScreen.setActiveModalPopup(renamePopup);
			renamePopup.focusTextBox();
			parentScreen.setOpenedDropdown(null);
		});
		event.addLink(Component.literal("Node Color"), () -> {
			ColorModalPopup colorPopup = new ColorModalPopup(parentScreen, targetContainer);
			parentScreen.setActiveModalPopup(colorPopup);
			parentScreen.setOpenedDropdown(null);
		});
		event.addLink(Component.literal("Settings"), () -> {
			NodeSettingsOverlay settingsOverlay = NodeSettingsOverlayFactory.create(parentScreen,
					targetContainer.getComponent());
			parentScreen.setActiveSettingsOverlay(settingsOverlay);
			parentScreen.setOpenedDropdown(null);
		});

		// Fire public NeoForge event to gather registrations from third-party add-ons
		// [3]
		NeoForge.EVENT_BUS.post(event);

		// Dynamically compute size boundaries (Height = 56 + links * 14) [3]
		int rawHeight = 56 + (event.getLinks().size() * 14);
		this.totalContentHeight = event.getLinks().size() * 14;
		this.needsScrolling = rawHeight > 112; // Enable scroll tracking if exceeds viewport limitations [3]
		this.height = Math.min(112, rawHeight);

		// Clamping to keep dropdown context menus safely on the active screen frame [3]
		int clampedX = Math.max(parentScreen.getLeftPos() + 22,
				Math.min(mouseX, parentScreen.getLeftPos() + parentScreen.getImageWidth() - this.width - 4));
		int clampedY = Math.max(parentScreen.getTopPos() + 4,
				Math.min(mouseY, parentScreen.getTopPos() + parentScreen.getImageHeight() - this.height - 4));
		this.setX(clampedX);
		this.setY(clampedY);

		// Set up FlowTextLink sub-elements
		int index = 0;
		for (RegisterDropdownLinksEvent.LinkEntry entry : event.getLinks()) {
			FlowTextLink link = new FlowTextLink(clampedX, clampedY + 6 + (index * 14), entry.label(), entry.action());
			this.links.add(link);
			this.children.add(link);
			index++;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.visible && this.active && isMouseOver(mouseX, mouseY) && needsScrolling) {
			int maxScroll = totalContentHeight - (this.height - 12);
			this.scrollOffset = net.minecraft.util.Mth.clamp(this.scrollOffset - (int) (scrollY * 14), 0, maxScroll);

			// Re-align and shift text links based on scrolling ticks
			int index = 0;
			for (FlowTextLink link : links) {
				link.setY(getY() + 6 + (index * 14) - scrollOffset);
				index++;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.visible && this.active && isMouseOver(mouseX, mouseY)) {
			for (FlowTextLink link : links) {
				// Safeguard clicked checks: Click target only if within the active visible
				// scissor viewport [3]
				if (mouseY >= getY() + 6 && mouseY < getY() + height - 6) {
					if (link.mouseClicked(mouseX, mouseY, button)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Dark translucent window backdrop with golden border frame [3]
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFA111111);
		guiGraphics.renderOutline(getX(), getY(), width, height, 0xFFD4AF37);

		// Viewport Scissor to prevent text elements from drawing out-of-bounds on short
		// devices [3]
		guiGraphics.enableScissor(getX() + 1, getY() + 6, getX() + width - 1, getY() + height - 6);
		for (FlowTextLink link : links) {
			link.visible = this.visible;
			link.active = this.active;
			link.render(guiGraphics, mouseX, mouseY, partialTick);
		}
		guiGraphics.disableScissor();
	}
}