package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.network.ClientInventoryCache;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.networking.packets.serverbound.RequestInventorySlotsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

/**
 * Symmetrical slot configuration modal popup that allows players to toggle
 * which slots in an inventory are active for a specific side [3].
 * Rendered at 50% scale to maintain high compact density and supports
 * rendering client-cached items inside each slot [3].
 */
@OnlyIn(Dist.CLIENT)
public class SlotLayoutModalPopup extends AbstractModalPopup {
	private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			dta.sfmflow.SFMFlow.MODID, "textures/gui/flowcomponents/generic_slot.png");

	private final ISideConfigurable sideModel;
	private final Direction side;
	private final BlockPos blockPos;
	private final Runnable onChanged;

	private final int totalSlots;
	private final Set<Integer> accessibleSlots = new HashSet<>();

	private final int unscaledWidth;
	private final int unscaledHeight;

	public SlotLayoutModalPopup(ManagerScreen parentScreen, ISideConfigurable sideModel, Direction side, BlockPos blockPos, Runnable onChanged) {
		super(parentScreen, 110, 100, Component.literal("Slot Layout"));
		this.sideModel = sideModel;
		this.side = side;
		this.blockPos = blockPos;
		this.onChanged = onChanged;

		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		IItemHandler nullHandler = null;
		IItemHandler sideHandler = null;
		net.minecraft.world.level.block.entity.BlockEntity be = null;

		if (level != null && blockPos != null) {
			nullHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos, null);
			sideHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos, side);
			be = level.getBlockEntity(blockPos);
		}

		this.totalSlots = nullHandler != null ? nullHandler.getSlots() : 0;

		// Map accessible slots based on worldly containers or standard sizing
		if (be instanceof net.minecraft.world.WorldlyContainer worldly && side != null) {
			int[] slots = worldly.getSlotsForFace(side);
			if (slots != null) {
				for (int s : slots) {
					accessibleSlots.add(s);
				}
			}
		} else if (sideHandler != null) {
			int sideCount = sideHandler.getSlots();
			if (sideCount == this.totalSlots) {
				for (int i = 0; i < this.totalSlots; i++) {
					accessibleSlots.add(i);
				}
			} else {
				for (int i = 0; i < sideCount; i++) {
					accessibleSlots.add(i);
				}
			}
		}

		// Calculate 2x layout size
		int cols = 9;
		int rows = (this.totalSlots + 8) / 9;
		if (rows <= 0) rows = 1;

		int gridW = cols * 20 - 2;
		int gridH = rows * 20 - 2;

		this.unscaledWidth = 10 + gridW + 10;
		this.unscaledHeight = 20 + gridH + 30;

		// Apply 50% scale properties to base dimensions
		this.width = this.unscaledWidth / 2;
		this.height = this.unscaledHeight / 2;

		// Center the popup symmetrically inside parent screen
		this.setX((parentScreen.width - this.width) / 2);
		this.setY((parentScreen.height - this.height) / 2);

		// Dispatch C2S request to synchronize target inventory slot item stacks cleanly over the network [3]
		PacketDistributor.sendToServer(new RequestInventorySlotsPacket(this.blockPos));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		// Map screen space inputs into unscaled 2x coordinates relative to top-left
		double localX = (mouseX - getX()) * 2.0;
		double localY = (mouseY - getY()) * 2.0;

		// Save/Close button bounds check
		int btnX = (this.unscaledWidth - 80) / 2;
		int btnY = this.unscaledHeight - 22;

		if (button == 0 && localX >= btnX && localX < btnX + 80 && localY >= btnY && localY < btnY + 14) {
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			close();
			return true;
		}

		// Dynamic slot toggle bounds check
		int gridStartX = 10;
		int gridStartY = 20;

		if (button == 0 && this.totalSlots > 0) {
			for (int i = 0; i < this.totalSlots; i++) {
				int row = i / 9;
				int col = i % 9;

				int slotX = gridStartX + col * 20;
				int slotY = gridStartY + row * 20;

				if (localX >= slotX && localX < slotX + 18 && localY >= slotY && localY < slotY + 18) {
					if (accessibleSlots.contains(i)) {
						if (sideModel instanceof ItemTransferComponent transfer) {
							transfer.toggleSlot(side, i);
							this.onChanged.run();
							Minecraft.getInstance().getSoundManager().play(
									SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Calculate local mouse coordinates in the 2x scaled coordinates space
		double localMouseX = (mouseX - getX()) * 2.0;
		double localMouseY = (mouseY - getY()) * 2.0;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(getX(), getY(), 0);
		guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);

		// Render the 9-slice background
		int c = 6;
		int m = 10;
		int w = this.unscaledWidth;
		int h = this.unscaledHeight;

		// Corners
		guiGraphics.blit(SUBMENU_BG, 0, 0, 0, 0, c, c, 22, 22);
		guiGraphics.blit(SUBMENU_BG, w - c, 0, 16, 0, c, c, 22, 22);
		guiGraphics.blit(SUBMENU_BG, 0, h - c, 0, 16, c, c, 22, 22);
		guiGraphics.blit(SUBMENU_BG, w - c, h - c, 16, 16, c, c, 22, 22);

		// Borders
		guiGraphics.blit(SUBMENU_BG, c, 0, w - 2 * c, c, (float) c, 0.0F, m, c, 22, 22);
		guiGraphics.blit(SUBMENU_BG, c, h - c, w - 2 * c, c, (float) c, 16.0F, m, c, 22, 22);
		guiGraphics.blit(SUBMENU_BG, 0, c, c, h - 2 * c, 0.0F, (float) c, c, m, 22, 22);
		guiGraphics.blit(SUBMENU_BG, w - c, c, c, h - 2 * c, 16.0F, (float) c, c, m, 22, 22);

		// Central stretch
		guiGraphics.blit(SUBMENU_BG, c, c, w - 2 * c, h - 2 * c, (float) c, (float) c, m, m, 22, 22);

		// Localized face header title
		String sideName = side != null ? side.name() : "GENERAL";
		Component titleComponent = Component.literal(sideName + " SLOTS");
		guiGraphics.drawCenteredString(parentScreen.getFont(), titleComponent, w / 2, 6, 0xFFD4AF37);

		// Slots grid
		int gridStartX = 10;
		int gridStartY = 20;

		ItemStack[] cachedItems = ClientInventoryCache.get(this.blockPos);

		for (int i = 0; i < this.totalSlots; i++) {
			int row = i / 9;
			int col = i % 9;

			int slotX = gridStartX + col * 20;
			int slotY = gridStartY + row * 20;

			// Blit slot asset texture
			guiGraphics.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);

			// Render slot contents if item is synchronized and present in client-side cache [3]
			if (cachedItems != null && i >= 0 && i < cachedItems.length) {
				ItemStack stack = cachedItems[i];
				if (stack != null && !stack.isEmpty()) {
					guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
				}
			}

			boolean isAccessible = accessibleSlots.contains(i);

			if (!isAccessible) {
				// Grayed overlay with Red X inside slot bounds
				guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x80151515);

				// Symmetrical thick red X
				for (int k = 0; k < 12; k++) {
					guiGraphics.fill(slotX + 3 + k, slotY + 3 + k, slotX + 5 + k, slotY + 4 + k, 0xFFFF0000);
					guiGraphics.fill(slotX + 3 + k, slotY + 14 - k, slotX + 5 + k, slotY + 15 - k, 0xFFFF0000);
				}
			} else {
				boolean isEnabled = true;
				if (sideModel instanceof ItemTransferComponent transfer) {
					isEnabled = transfer.isSlotEnabled(side, i);
				}

				if (isEnabled) {
					// Translucent green tint & borders
					guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x5039FF14);
					guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF39FF14);
				} else {
					// Translucent red tint & borders
					guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x50FF0000);
					guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFFFF0000);
				}
			}
		}

		// Render the close button
		int btnX = (w - 80) / 2;
		int btnY = h - 22;
		boolean btnHovered = localMouseX >= btnX && localMouseX < btnX + 80 && localMouseY >= btnY && localMouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, btnHovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 40, btnY + 3, 0xFFFFFFFF);

		guiGraphics.pose().popPose();

		// Symmetrical unscaled overlay rendering pass for slot item tooltips [3]
		if (this.totalSlots > 0 && cachedItems != null) {
			int unscaledStartX = getX() + 5;  // 10 * 0.5 = 5
			int unscaledStartY = getY() + 10; // 20 * 0.5 = 10

			for (int i = 0; i < this.totalSlots; i++) {
				int row = i / 9;
				int col = i % 9;

				int slotX = unscaledStartX + col * 10; // 20 * 0.5 = 10
				int slotY = unscaledStartY + row * 10;

				if (mouseX >= slotX && mouseX < slotX + 9 && mouseY >= slotY && mouseY < slotY + 9) {
					if (i >= 0 && i < cachedItems.length) {
						ItemStack stack = cachedItems[i];
						if (stack != null && !stack.isEmpty()) {
							guiGraphics.renderTooltip(parentScreen.getFont(), stack, mouseX, mouseY);
							break;
						}
					}
				}
			}
		}
	}
}