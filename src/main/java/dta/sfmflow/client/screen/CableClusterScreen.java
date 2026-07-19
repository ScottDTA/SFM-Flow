package dta.sfmflow.client.screen;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.networking.packets.serverbound.SyncClusterSlotDirectionPacket;
import dta.sfmflow.screen.CableClusterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen interface mapping slots to directional faces via instant feedback
 * widgets. Redesigned with dynamic panel metrics and contextual button visibilities.
 */
@OnlyIn(Dist.CLIENT)
public class CableClusterScreen extends AbstractContainerScreen<CableClusterMenu> {
	private static final Direction[] DIRECTIONS_WITH_NONE = { null, Direction.UP, Direction.DOWN, Direction.NORTH,
			Direction.SOUTH, Direction.EAST, Direction.WEST };

	private static final ResourceLocation CABLE_CLUSTER_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/cable_cluster_menu.png");
	private static final ResourceLocation ADV_CABLE_CLUSTER_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/adv_cable_cluster_menu.png");

	private Button[] directionButtons;

	/**
	 * Initializes the screen and calculates safe background layout heights.
	 */
	public CableClusterScreen(CableClusterMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = menu.getBlockEntity().getNumSlots() == 18 ? 214 : 160;

		int numSlots = menu.getBlockEntity().getNumSlots();
		this.titleLabelX = 8;
		this.titleLabelY = 4;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = (numSlots == 18) ? 122 : 68;
	}

	@Override
	protected void init() {
		super.init();
		int numSlots = this.menu.getBlockEntity().getNumSlots();
		int left = this.leftPos;
		int top = this.topPos;

		this.directionButtons = new Button[numSlots]; // Initialize the tracker array

		for (int i = 0; i < numSlots; i++) {
			final int slotIdx = i;
			int col = i % 3;
			int row = i / 3;

			int slotX = 8 + col * 54;
			int slotY = 16 + row * 18;

			int btnX = left + slotX + 18;
			int btnY = top + slotY - 1;

			Direction dir = this.menu.getBlockEntity().getSlotDirection(slotIdx);
			String name = (dir == null) ? "NONE" : dir.name();

			Button btn = Button.builder(Component.literal(name), b -> {
				Direction current = this.menu.getBlockEntity().getSlotDirection(slotIdx);
				int idx = 0;
				for (int k = 0; k < DIRECTIONS_WITH_NONE.length; k++) {
					if (DIRECTIONS_WITH_NONE[k] == current) {
						idx = k;
						break;
					}
				}

				int nextIdx = idx;
				Direction nextDir = current;
				for (int step = 1; step <= DIRECTIONS_WITH_NONE.length; step++) {
					int testIdx = (idx + step) % DIRECTIONS_WITH_NONE.length;
					Direction testDir = DIRECTIONS_WITH_NONE[testIdx];
					if (this.menu.getBlockEntity().isDirectionValid(slotIdx, testDir)) {
						nextIdx = testIdx;
						nextDir = testDir;
						break;
					}
				}

				if (nextDir != current) {
					int ord = (nextDir == null) ? -1 : nextDir.ordinal();
					this.menu.getBlockEntity().setSlotDirection(slotIdx, ord);
					b.setMessage(Component.literal((nextDir == null) ? "NONE" : nextDir.name()));

					PacketDistributor.sendToServer(
							new SyncClusterSlotDirectionPacket(this.menu.getBlockEntity().getBlockPos(), slotIdx, ord));
				}
			}).pos(btnX, btnY).size(32, 14).build();

			this.directionButtons[i] = btn; // Store reference
			this.addRenderableWidget(btn);
		}
	}

	private boolean isSlotDirectional(int slot) {
		return this.menu.getBlockEntity().isSlotDirectional(slot);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Update button visibilities and messages dynamically based on slot contents
		int numSlots = this.menu.getBlockEntity().getNumSlots();
		for (int i = 0; i < numSlots; i++) {
			if (this.directionButtons != null && this.directionButtons[i] != null) {
				boolean show = isSlotDirectional(i);
				this.directionButtons[i].visible = show;
				this.directionButtons[i].active = show;
				if (show) {
					Direction dir = this.menu.getBlockEntity().getSlotDirection(i);
					this.directionButtons[i].setMessage(Component.literal((dir == null) ? "NONE" : dir.name()));
				}
			}
		}

		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF8B8B8B, false);
		guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
				0xFF8B8B8B, false);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;

		ResourceLocation texture = this.menu.getBlockEntity().getNumSlots() == 18 ? ADV_CABLE_CLUSTER_TX
				: CABLE_CLUSTER_TX;
		guiGraphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
	}
}