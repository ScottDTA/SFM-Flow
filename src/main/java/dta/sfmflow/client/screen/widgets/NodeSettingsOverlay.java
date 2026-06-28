package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Base abstract modal popup container centering symmetrically within the top canvas viewport [3].
 * Intercepts inputs, aligns container focus maps, and bubbles events directly to children [3].
 */
@OnlyIn(Dist.CLIENT)
public abstract class NodeSettingsOverlay extends AbstractFlowWidget {
    protected final ManagerScreen parentScreen;
    protected final AbstractFlowComponent component;
    protected static final ResourceLocation SUBMENU_BG = ResourceLocation.fromNamespaceAndPath(dta.sfmflow.SFMFlow.MODID, "textures/gui/submenu_bg.png");

    private GuiEventListener focusedChild = null;

    /**
     * Symmetrically instantiates and positions the NodeSettingsOverlay modal in the top viewport [3].
     *
     * @param parentScreen the screen displaying the overlay [3]
     * @param component the flow component being configured [3]
     */
    public NodeSettingsOverlay(ManagerScreen parentScreen, AbstractFlowComponent component) {
        super((parentScreen.width - 240) / 2, (256 - 180) / 2, 240, 180, component.getName());
        this.parentScreen = parentScreen;
        this.component = component;
    }

    /**
     * Safely dismisses the modal configuration screen and returns focus to the clean canvas [3].
     */
    public void closeAndSave() {
        this.parentScreen.setActiveSettingsOverlay(null);
    }

    /**
     * Encodes component modifications onto NBT, sends a save packet to the server, and closes [3].
     */
    public void saveAndClose() {
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        component.saveData(nbt);
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dta.sfmflow.networking.packets.serverbound.SaveComponentSettings(
            parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), 
            component.getId(), 
            nbt
        ));
        closeAndSave();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            closeAndSave(); // Esc key guarantees clean escape and bypasses local focused locks [3]
            return true;
        }
        for (GuiEventListener child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Overridden to bubble character typing events to active, focused sub-children [3].
     *
     * @param codePoint the unicode character code [3]
     * @param modifiers typing keyboard modifiers [3]
     * @return true if the character is consumed [3]
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (GuiEventListener child : children) {
            if (child.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int btnX = getX() + (width - 80) / 2;
            int btnY = getY() + height - 22;
            if (mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14) {
                saveAndClose();
                return true;
            }
        }
        for (GuiEventListener child : children) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                this.focusedChild = child;
                this.setFocused(child); // Align container-focus mapping cleanly [3]
                this.setDragging(true);  // Force active dragging state [3]
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (this.focusedChild != null) {
            handled = this.focusedChild.mouseReleased(mouseX, mouseY, button);
            this.focusedChild = null; // Clear focused widget candidate [3]
        }
        this.setDragging(false); // Reset dragging state [3]
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.focusedChild != null) {
            return this.focusedChild.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (GuiEventListener child : children) {
            if (child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderOverlayBackground(guiGraphics);

        int titleWidth = parentScreen.getFont().width(component.getName());
        int titleX = getX() + (this.width - titleWidth) / 2;
        guiGraphics.drawString(parentScreen.getFont(), component.getName(), titleX, getY() + 8, 0xFF404040, false);

        for (GuiEventListener child : children) {
            if (child instanceof AbstractFlowWidget widget) {
                widget.visible = this.visible;
                widget.active = this.active;
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        int btnX = getX() + (width - 80) / 2;
        int btnY = getY() + height - 22;
        boolean hovered = mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14;
        guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, hovered ? 0xFF555555 : 0xFF222222);
        guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
        guiGraphics.drawCenteredString(parentScreen.getFont(), "Save & Close", btnX + 40, btnY + 3, 0xFFFFFFFF);
    }

    protected void renderOverlayBackground(GuiGraphics guiGraphics) {
        int c = 6;  
        int m = 10; 
        int x = getX();
        int y = getY();

        guiGraphics.blit(SUBMENU_BG, x, y, 0, 0, c, c, 22, 22);
        guiGraphics.blit(SUBMENU_BG, x + width - c, y, 16, 0, c, c, 22, 22);
        guiGraphics.blit(SUBMENU_BG, x, y + height - c, 0, 16, c, c, 22, 22);
        guiGraphics.blit(SUBMENU_BG, x + width - c, y + height - c, 16, 16, c, c, 22, 22);

        guiGraphics.blit(SUBMENU_BG, x + c, y, width - 2 * c, c, (float) c, 0.0F, m, c, 22, 22);
        guiGraphics.blit(SUBMENU_BG, x + c, y + height - c, width - 2 * c, c, (float) c, 16.0F, m, c, 22, 22);
        guiGraphics.blit(SUBMENU_BG, x, y + c, c, height - 2 * c, 0.0F, (float) c, c, m, 22, 22);
        guiGraphics.blit(SUBMENU_BG, x + width - c, y + c, c, height - 2 * c, 16.0F, (float) c, c, m, 22, 22);

        guiGraphics.blit(SUBMENU_BG, x + c, y + c, width - 2 * c, height - 2 * c, (float) c, (float) c, m, m, 22, 22);
    }
}