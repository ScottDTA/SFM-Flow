package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.client.event.RegisterDropdownLinksEvent;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.NodeSettingsOverlay;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.GroupInputComponent;
import dta.sfmflow.flowcomponents.GroupOutputComponent;
import dta.sfmflow.networking.packets.serverbound.CanvasActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact right-click visual dropdown overlay replacing old ContextMenuOverlay.
 * Houses scrolling logic, viewport scissor bounds, and launches settings
 * modals. Scaled down to 66% with standard grey styling.
 */
@OnlyIn(Dist.CLIENT)
public class DropdownMenuWidget extends AbstractFlowWidget {
	private final List<FlowTextLink> links = new ArrayList<>();
	private final FlowWidgetContainer targetContainer;
	private final ManagerScreen parentScreen;
	private int scrollOffset = 0;
	private final int totalContentHeight;
	private final boolean needsScrolling;

	public DropdownMenuWidget(FlowWidgetContainer targetContainer, int mouseX, int mouseY, ManagerScreen parentScreen) {
		super(mouseX, mouseY, 84, 56, Component.literal("Dropdown Menu"));
		this.targetContainer = targetContainer;
		this.parentScreen = parentScreen;

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
		event.addLink(Component.literal("Copy Node"), () -> {
			PacketDistributor.sendToServer(new CanvasActionPacket(
					parentScreen.getMenu().getManagerBlockEntity().getBlockPos(),
					targetContainer.getComponent().getId(),
					CanvasAction.COPY));
			parentScreen.setOpenedDropdown(null);
		});
		// Add "Move Group" option to standard components [3]
				if (!(targetContainer.getComponent() instanceof GroupInputComponent) 
						&& !(targetContainer.getComponent() instanceof GroupOutputComponent)) {
					event.addLink(Component.literal("Move Group"), () -> {
						MoveGroupModalPopup movePopup = new MoveGroupModalPopup(parentScreen, targetContainer);
						parentScreen.setActiveModalPopup(movePopup);
						parentScreen.setOpenedDropdown(null);
					});
				}
		event.addLink(Component.literal("Delete Node"), () -> {
			PacketDistributor.sendToServer(new CanvasActionPacket(
					parentScreen.getMenu().getManagerBlockEntity().getBlockPos(),
					targetContainer.getComponent().getId(),
					CanvasAction.DELETE));
			parentScreen.setOpenedDropdown(null);
		});

		NeoForge.EVENT_BUS.post(event);

		List<RegisterDropdownLinksEvent.LinkEntry> compiled = event.getLinks();
		
		// Dynamically compute exact height boundary based on 6px top/bottom padding
		int rawHeight = (compiled.size() * 14) + 12;
		this.totalContentHeight = compiled.size() * 14;
		this.needsScrolling = rawHeight > 112;
		this.height = Math.min(112, rawHeight);

		int clampedX = mouseX;
		int clampedY = mouseY;
		int scaledW = (int) (this.width * 0.66F);
		int scaledH = (int) (this.height * 0.66F);

		if (clampedX + scaledW > parentScreen.width) {
			clampedX = parentScreen.width - scaledW;
		}
		if (clampedY + scaledH > parentScreen.height) {
			clampedY = parentScreen.height - scaledH;
		}

		this.setX(clampedX);
		this.setY(clampedY);

		int index = 0;
		for (RegisterDropdownLinksEvent.LinkEntry entry : compiled) {
			FlowTextLink link = new FlowTextLink(clampedX, clampedY + 6 + (index * 14), entry.label(), entry.action());
			this.links.add(link);
			this.children.add(link);
			index++;
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.visible && mouseX >= getX() && mouseX < getX() + (this.width * 0.66F)
				&& mouseY >= getY() && mouseY < getY() + (this.height * 0.66F);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.visible && this.active && isMouseOver(mouseX, mouseY) && needsScrolling) {
			int maxScroll = totalContentHeight - (this.height - 12);
			this.scrollOffset = Mth.clamp(this.scrollOffset - (int) (scrollY * 14), 0, maxScroll);

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
			double unscaledMouseX = getX() + (mouseX - getX()) / 0.66F;
			double unscaledMouseY = getY() + (mouseY - getY()) / 0.66F;

			for (FlowTextLink link : links) {
				if (unscaledMouseY >= getY() + 6 && unscaledMouseY < getY() + height - 6) {
					if (link.mouseClicked(unscaledMouseX, unscaledMouseY, button)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(getX(), getY(), 0);
		guiGraphics.pose().scale(0.66F, 0.66F, 1.0F);
		guiGraphics.pose().translate(-getX(), -getY(), 0);

		guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFA111111);
		guiGraphics.renderOutline(getX(), getY(), width, height, 0xFF8B8B8B);

		int scissorMinX = getX() + (int) Math.round(1 * 0.66);
		int scissorMinY = getY() + (int) Math.round(6 * 0.66);
		int scissorMaxX = getX() + (int) Math.round((width - 1) * 0.66);
		int scissorMaxY = getY() + (int) Math.round((height - 6) * 0.66);

		guiGraphics.enableScissor(scissorMinX, scissorMinY, scissorMaxX, scissorMaxY);

		int unscaledMouseX = getX() + (int) Math.round((mouseX - getX()) / 0.66F);
		int unscaledMouseY = getY() + (int) Math.round((mouseY - getY()) / 0.66F);

		for (FlowTextLink link : links) {
			link.visible = this.visible;
			link.active = this.active;
			link.render(guiGraphics, unscaledMouseX, unscaledMouseY, partialTick);
		}
		guiGraphics.disableScissor();

		guiGraphics.pose().popPose();
	}
}