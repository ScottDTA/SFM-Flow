package dta.sfmflow.client.screen;

import dta.sfmflow.block.entity.CableClusterBlockEntity;
import dta.sfmflow.screen.CableClusterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Screen interface mapping slots to directional faces via instant feedback widgets [3].
 * Redesigned with dynamic panel metrics to eliminate visual overlaps completely [3].
 */
@OnlyIn(Dist.CLIENT)
public class CableClusterScreen extends AbstractContainerScreen<CableClusterMenu> {
    private static final Direction[] DIRECTIONS_WITH_NONE = {
        null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    /**
     * Instantiates the screen and calculates safe background layout heights [3].
     */
    public CableClusterScreen(CableClusterMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        // Dynamically scale panel height: 166 for Standard, 198 for Advanced [3]
        this.imageHeight = menu.getBlockEntity().getNumSlots() == 18 ? 198 : 166;

        int numSlots = menu.getBlockEntity().getNumSlots();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = (numSlots == 18) ? 92 : 60;
    }

    @Override
    protected void init() {
        super.init();
        int numSlots = this.menu.getBlockEntity().getNumSlots();
        int left = this.leftPos;
        int top = this.topPos;

        if (numSlots == 9) {
            for (int i = 0; i < 9; i++) {
                final int slotIdx = i;
                Direction dir = this.menu.getBlockEntity().getSlotDirection(slotIdx);
                String name = (dir == null) ? "NONE" : dir.name().substring(0, 2);

                net.minecraft.client.gui.components.Button btn = net.minecraft.client.gui.components.Button.builder(
                    Component.literal(name),
                    b -> {
                        Direction current = this.menu.getBlockEntity().getSlotDirection(slotIdx);
                        int idx = 0;
                        for (int k = 0; k < DIRECTIONS_WITH_NONE.length; k++) {
                            if (DIRECTIONS_WITH_NONE[k] == current) {
                                idx = k;
                                break;
                            }
                        }
                        int nextIdx = (idx + 1) % DIRECTIONS_WITH_NONE.length;
                        Direction nextDir = DIRECTIONS_WITH_NONE[nextIdx];
                        int ord = (nextDir == null) ? -1 : nextDir.ordinal();

                        this.menu.getBlockEntity().setSlotDirection(slotIdx, ord);
                        b.setMessage(Component.literal((nextDir == null) ? "NONE" : nextDir.name().substring(0, 2)));

                        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new dta.sfmflow.networking.packets.serverbound.SyncClusterSlotDirectionPacket(
                                this.menu.getBlockEntity().getBlockPos(), slotIdx, ord
                            )
                        );
                    }
                ).pos(left + 8 + i * 18 + 1, top + 36).size(16, 12).build();

                this.addRenderableWidget(btn);
            }
        } else {
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 9; c++) {
                    final int slotIdx = r * 9 + c;
                    Direction dir = this.menu.getBlockEntity().getSlotDirection(slotIdx);
                    String name = (dir == null) ? "NONE" : dir.name().substring(0, 2);

                    net.minecraft.client.gui.components.Button btn = net.minecraft.client.gui.components.Button.builder(
                        Component.literal(name),
                        b -> {
                            Direction current = this.menu.getBlockEntity().getSlotDirection(slotIdx);
                            int idx = 0;
                            for (int k = 0; k < DIRECTIONS_WITH_NONE.length; k++) {
                                if (DIRECTIONS_WITH_NONE[k] == current) {
                                    idx = k;
                                    break;
                                }
                            }
                            int nextIdx = (idx + 1) % DIRECTIONS_WITH_NONE.length;
                            Direction nextDir = DIRECTIONS_WITH_NONE[nextIdx];
                            int ord = (nextDir == null) ? -1 : nextDir.ordinal();

                            this.menu.getBlockEntity().setSlotDirection(slotIdx, ord);
                            b.setMessage(Component.literal((nextDir == null) ? "NONE" : nextDir.name().substring(0, 2)));

                            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                new dta.sfmflow.networking.packets.serverbound.SyncClusterSlotDirectionPacket(
                                    this.menu.getBlockEntity().getBlockPos(), slotIdx, ord
                                )
                            );
                        }
                    ).pos(left + 8 + c * 18 + 1, top + 36 + r * 34).size(16, 12).build();

                    this.addRenderableWidget(btn);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFD4AF37, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF8B8B8B, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.fill(x, y, x + 176, y + imageHeight, 0xFF2B2B2B);
        guiGraphics.renderOutline(x, y, 176, imageHeight, 0xFFD4AF37);

        int numSlots = this.menu.getBlockEntity().getNumSlots();
        if (numSlots == 9) {
            for (int i = 0; i < 9; i++) {
                guiGraphics.fill(x + 7 + i * 18, y + 17, x + 7 + i * 18 + 18, y + 17 + 18, 0xFF151515);
                guiGraphics.renderOutline(x + 7 + i * 18, y + 17, 18, 18, 0xFF434343);
            }
        } else {
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 9; c++) {
                    guiGraphics.fill(x + 7 + c * 18, y + 17 + r * 34, x + 7 + c * 18 + 18, y + 17 + r * 34 + 18, 0xFF151515);
                    guiGraphics.renderOutline(x + 7 + c * 18, y + 17 + r * 34, 18, 18, 0xFF434343);
                }
            }
        }

        int startY = (numSlots == 18) ? 103 : 71;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                guiGraphics.fill(x + 7 + c * 18, y + startY + r * 18, x + 7 + c * 18 + 18, y + startY + r * 18 + 18, 0xFF151515);
                guiGraphics.renderOutline(x + 7 + c * 18, y + startY + r * 18, 18, 18, 0xFF434343);
            }
        }

        for (int c = 0; c < 9; c++) {
            guiGraphics.fill(x + 7 + c * 18, y + startY + 57, x + 7 + c * 18 + 18, y + startY + 57 + 18, 0xFF151515);
            guiGraphics.renderOutline(x + 7 + c * 18, y + startY + 57, 18, 18, 0xFF434343);
        }
    }
}