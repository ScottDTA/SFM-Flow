package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.helper.FlowLayoutHelper;
import dta.sfmflow.flowcomponents.GroupComponent;
import dta.sfmflow.networking.packets.serverbound.MoveComponentGroupPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Directory selector modal popup allowing users to migrate elements between canvas levels.
 */
@OnlyIn(Dist.CLIENT)
public class MoveGroupModalPopup extends AbstractModalPopup {
	private final AbstractFlowComponent targetComponent;
	private final DirectoryScrollListWidget directoryList;

	public record DirectoryChoice(UUID id, String path) {}

	public MoveGroupModalPopup(ManagerScreen parentScreen, FlowWidgetContainer targetContainer) {
		super(parentScreen, 180, 150, Component.literal("Move Group"));
		this.targetComponent = targetContainer.getComponent();

		this.directoryList = new DirectoryScrollListWidget(getX() + 15, getY() + 20, 150, 100);
		this.children.add(this.directoryList);
	}

	private void saveAndClose(@javax.annotation.Nullable UUID targetGroupId) {
		BlockPos pos = parentScreen.getMenu().getManagerBlockEntity().getBlockPos();
		
		// Send migration packet over the network
		PacketDistributor.sendToServer(new MoveComponentGroupPacket(pos, targetComponent.getId(), targetGroupId));
		
		// Instantly clear viewed sub-canvas if we migrated the currently viewed folder
		if (targetComponent.getId().equals(parentScreen.getCurrentGroupId())) {
			parentScreen.setCurrentGroupId(targetGroupId);
		}
		
		parentScreen.refreshWidgetLayout();
		close();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;

		if (button == 0 && mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14) {
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			close();
			return true;
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		guiGraphics.drawCenteredString(parentScreen.getFont(), "SELECT DIRECTORY", getX() + width / 2, getY() + 6, 0xFFD4AF37);

		for (var child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				if (widget.visible) {
					widget.active = this.active;
					widget.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}
		}

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;
		boolean btnHovered = mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, btnHovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Cancel", btnX + 40, btnY + 3, 0xFFFFFFFF);
	}

	@OnlyIn(Dist.CLIENT)
	private class DirectoryScrollListWidget extends AbstractFlowWidget {
		private float scrollY = 0.0F;
		private final List<DirectoryChoice> choices = new ArrayList<>();

		public DirectoryScrollListWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Component.literal("Directory List"));
			buildChoices();
		}

		private void buildChoices() {
			choices.clear();
			var blockEntity = parentScreen.getMenu().getManagerBlockEntity();
			
			choices.add(new DirectoryChoice(null, "Root"));

			List<UUID> groups = new ArrayList<>();
			for (var comp : blockEntity.getFlowComponents().values()) {
				if (comp instanceof GroupComponent) {
					groups.add(comp.getId());
				}
			}

			// Compile path strings, omitting the moving group and its nested children
			for (UUID groupId : groups) {
				if (isChildOf(groupId, targetComponent.getId())) {
					continue;
				}
				choices.add(new DirectoryChoice(groupId, getGroupPathString(groupId)));
			}
		}

		private boolean isChildOf(UUID childId, UUID parentId) {
			if (childId.equals(parentId)) return true;
			UUID current = childId;
			var blockEntity = parentScreen.getMenu().getManagerBlockEntity();
			while (current != null) {
				var comp = blockEntity.getFlowComponents().get(current);
				if (comp != null) {
					current = comp.getParentGroupId();
					if (parentId.equals(current)) {
						return true;
					}
				} else {
					break;
				}
			}
			return false;
		}

		private String getGroupPathString(UUID groupId) {
			List<String> names = new ArrayList<>();
			UUID current = groupId;
			var blockEntity = parentScreen.getMenu().getManagerBlockEntity();
			while (current != null) {
				var comp = blockEntity.getFlowComponents().get(current);
				if (comp != null) {
					names.add(0, comp.getName().getString());
					current = comp.getParentGroupId();
				} else {
					break;
				}
			}
			return "Root > " + String.join(" > ", names);
		}

		@Override
		protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF111111);
			guiGraphics.renderOutline(getX(), getY(), width, height, 0xFF434343);

			guiGraphics.enableScissor(getX(), getY() + 1, getX() + width, getY() + height - 1);

			int startY = getY() + 4 - (int) scrollY;
			for (int i = 0; i < choices.size(); i++) {
				DirectoryChoice choice = choices.get(i);
				int itemY = startY + i * 12;

				boolean isCurrentLevel = Objects.equals(targetComponent.getParentGroupId(), choice.id());
				boolean hovered = mouseX >= getX() && mouseX < getX() + width && mouseY >= itemY && mouseY < itemY + 11;

				int textColor = isCurrentLevel ? 0xFF555555 : (hovered ? 0xFFFFFFFF : 0xFF8B8B8B);
				guiGraphics.drawString(parentScreen.getFont(), choice.path(), getX() + 4, itemY, textColor, false);
			}

			guiGraphics.disableScissor();

			int maxScroll = Math.max(0, choices.size() * 12 - (height - 8));
			if (maxScroll > 0) {
				int sbX = getX() + width - 4;
				guiGraphics.fill(sbX, getY() + 2, sbX + 2, getY() + height - 2, 0x40000000);

				int thumbHeight = (int) (((double) height / (choices.size() * 12)) * height);
				thumbHeight = Math.max(8, Math.min(height, thumbHeight));
				int thumbY = getY() + 2 + (int) ((scrollY / maxScroll) * (height - 4 - thumbHeight));

				guiGraphics.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, 0xFF8B8B8B);
			}

			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				if (FlowLayoutHelper.isWidgetActiveAndOnTop(this, parentScreen)) {
					int row = (int) ((mouseY - getY() + scrollY - 4) / 12);
					if (row >= 0 && row < choices.size()) {
						guiGraphics.renderTooltip(parentScreen.getFont(), Component.literal(choices.get(row).path()), mouseX, mouseY);
					}
				}
			}
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!this.visible || !this.active) {
				return false;
			}

			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				int startY = getY() + 4 - (int) scrollY;

				for (int i = 0; i < choices.size(); i++) {
					int itemY = startY + i * 12;
					if (mouseY >= itemY && mouseY < itemY + 11) {
						DirectoryChoice choice = choices.get(i);
						
						// Block migrating a node onto its own current folder level
						if (Objects.equals(targetComponent.getParentGroupId(), choice.id())) {
							return false;
						}

						Minecraft.getInstance().getSoundManager().play(
								SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						saveAndClose(choice.id());
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && mouseX >= getX() && mouseX < getX() + width
					&& mouseY >= getY() && mouseY < getY() + height) {
				int maxScroll = Math.max(0, choices.size() * 12 - (height - 8));
				if (maxScroll > 0) {
					this.scrollY = Mth.clamp(this.scrollY - (float) scrollY * 6.0F, 0.0F, (float) maxScroll);
					return true;
				}
			}
			return false;
		}
	}
}