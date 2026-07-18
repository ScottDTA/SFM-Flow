package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.helper.FlowLayoutHelper;
import dta.sfmflow.flowcomponents.SculkTriggerComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom side configuration popup for Sculk Trigger Cables.
 * Displays an interactive scrolling checklist of all registered vibration events.
 */
@OnlyIn(Dist.CLIENT)
public class SculkTriggerSideConfigModalPopup extends AbstractModalPopup {
	private final SculkTriggerComponent component;
	private final Direction side;
	private final Runnable onChanged;
	private final GameEventChecklistWidget checklistWidget;

	public SculkTriggerSideConfigModalPopup(ManagerScreen parentScreen, SculkTriggerComponent component, Direction side, BlockPos pos, Runnable onChanged) {
		super(parentScreen, 160, 160, Component.literal("Sided Sculk"));
		this.component = component;
		this.side = side;
		this.onChanged = onChanged;

		// Interactive scrolling checklist widget
		this.checklistWidget = new GameEventChecklistWidget(getX() + 15, getY() + 22, 130, 110);
		this.children.add(this.checklistWidget);
	}

	private void saveAndClose() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
		close();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 257 || keyCode == 335) { 
			saveAndClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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
			saveAndClose();
			return true;
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		String title = side != null ? side.name() + " FILTER" : "FILTER";
		guiGraphics.drawCenteredString(parentScreen.getFont(), title, getX() + width / 2, getY() + 6, 0xFFD4AF37);

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
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 40, btnY + 3, 0xFFFFFFFF);
	}

	/**
	 * Interactive scrolling checklist displaying all registered vibration GameEvents.
	 */
	@OnlyIn(Dist.CLIENT)
	private class GameEventChecklistWidget extends AbstractFlowWidget {
		private float scrollY = 0.0F;
		private final List<ResourceLocation> events = new ArrayList<>();

		public GameEventChecklistWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Component.literal("Game Events"));
			
			// Retrieve and sort all registered game events alphabetically
			for (var entry : BuiltInRegistries.GAME_EVENT) {
				ResourceLocation loc = BuiltInRegistries.GAME_EVENT.getKey(entry);
				if (loc != null) {
					events.add(loc);
				}
			}
			events.sort((a, b) -> a.getPath().compareToIgnoreCase(b.getPath()));
		}

		private boolean isEventChecked(ResourceLocation loc) {
			return component.hasEventFilter(side, loc);
		}

		private void toggleEvent(ResourceLocation loc) {
			component.toggleEventFilter(side, loc);
			onChanged.run();
		}

		@Override
		protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF111111);
			guiGraphics.renderOutline(getX(), getY(), width, height, 0xFF434343);

			if (events.isEmpty()) {
				guiGraphics.drawString(parentScreen.getFont(), "No Events", getX() + 4, getY() + 4, 0xFF8B8B8B, false);
				return;
			}

			// Clip rendering inside the list boundaries
			guiGraphics.enableScissor(getX(), getY() + 1, getX() + width, getY() + height - 1);

			int startY = getY() + 4 - (int) scrollY;
			for (int i = 0; i < events.size(); i++) {
				ResourceLocation loc = events.get(i);
				int itemY = startY + i * 12;

				boolean checked = isEventChecked(loc);
				boolean hoveredBox = mouseX >= getX() + 4 && mouseX < getX() + 12 && mouseY >= itemY && mouseY < itemY + 8;
				boolean hoveredText = mouseX >= getX() + 14 && mouseX < getX() + width && mouseY >= itemY && mouseY < itemY + 11;

				int checkboxBorder = hoveredBox ? 0xFFD4AF37 : 0xFF8B8B8B;
				guiGraphics.fill(getX() + 4, itemY + 2, getX() + 10, itemY + 8, checked ? 0xFF39FF14 : 0xFF222222);
				guiGraphics.renderOutline(getX() + 4, itemY + 2, 6, 6, checkboxBorder);

				int textColor = hoveredText ? 0xFFFFFFFF : 0xFF8B8B8B;
				guiGraphics.drawString(parentScreen.getFont(), loc.getPath(), getX() + 14, itemY, textColor, false);
			}

			guiGraphics.disableScissor();

			// Draw Scrollbar
			int maxScroll = Math.max(0, events.size() * 12 - (height - 8));
			if (maxScroll > 0) {
				int sbX = getX() + width - 4;
				guiGraphics.fill(sbX, getY() + 2, sbX + 2, getY() + height - 2, 0x40000000);

				int thumbHeight = (int) (((double) height / (events.size() * 12)) * height);
				thumbHeight = Math.max(8, Math.min(height, thumbHeight));
				int thumbY = getY() + 2 + (int) ((scrollY / maxScroll) * (height - 4 - thumbHeight));

				guiGraphics.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, 0xFF8B8B8B);
			}

			// Render hover tooltip with full namespace ID
			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				if (FlowLayoutHelper.isWidgetActiveAndOnTop(this, parentScreen)) {
					int row = (int) ((mouseY - getY() + scrollY - 4) / 12);
					if (row >= 0 && row < events.size()) {
						guiGraphics.renderTooltip(parentScreen.getFont(), Component.literal(events.get(row).toString()), mouseX, mouseY);
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
				for (int i = 0; i < events.size(); i++) {
					int itemY = startY + i * 12;
					if (mouseY >= itemY && mouseY < itemY + 11) {
						toggleEvent(events.get(i));
						Minecraft.getInstance().getSoundManager().play(
								SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
				int maxScroll = Math.max(0, events.size() * 12 - (height - 8));
				if (maxScroll > 0) {
					this.scrollY = Mth.clamp(this.scrollY - (float) scrollY * 6.0F, 0.0F, (float) maxScroll);
					return true;
				}
			}
			return false;
		}
	}
}