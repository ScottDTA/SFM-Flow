package dta.sfmflow.api.client.widget;

import org.jetbrains.annotations.Nullable;

import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.client.NineSliceUtil;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Base abstract modal popup container centering symmetrically within the top
 * canvas viewport. Intercepts inputs, aligns container focus maps, and
 * bubbles events directly to children.
 */
@OnlyIn(Dist.CLIENT)
public abstract class NodeSettingsOverlay extends AbstractFlowWidget {
	public final ManagerScreen parentScreen;
	protected final AbstractFlowComponent component;

	private GuiEventListener focusedChild = null;

	/**
	 * Symmetrically instantiates and positions the NodeSettingsOverlay modal in the
	 * top viewport.
	 *
	 * @param parentScreen the screen displaying the overlay 
	 * @param component    the flow component being configured 
	 */
	public NodeSettingsOverlay(ManagerScreen parentScreen, AbstractFlowComponent component) {
		super((parentScreen.width - 240) / 2, parentScreen.getOverlayTargetY(180), 240, 180,
				component.getName());
		this.parentScreen = parentScreen;
		this.component = component;
	}

	/**
	 * Safely dismisses the modal configuration screen and returns focus to the
	 * clean canvas.
	 */
	public void closeAndSave() {
		this.parentScreen.setActiveSettingsOverlay(null);
	}

	/**
	 * Encodes component modifications onto NBT, sends a save packet to the server,
	 * and closes.
	 */
	public void saveAndClose() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
		closeAndSave();
	}

	@Override
	public void setX(int x) {
		int dif = this.getX() - x; // Fix inversion: absolute child tracking translation
		super.setX(x);
		updateChildrenXPositions(dif);
	}

	@Override
	public void setY(int y) {
		int dif = this.getY() - y; // Fix inversion: absolute child tracking translation
		super.setY(y);
		updateChildrenYPositions(dif);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) { // GLFW_KEY_ESCAPE
			closeAndSave(); // Esc key guarantees clean escape and bypasses local focused locks [3]
			return true;
		}
		
		// Only route keyboard presses directly to the currently focused child [3]
		GuiEventListener activeFocus = this.getFocused();
		if (activeFocus != null && activeFocus.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	/**
	 * Overridden to bubble character typing events to active, focused sub-children.
	 *
	 * @param codePoint the unicode character code 
	 * @param modifiers typing keyboard modifiers 
	 * @return true if the character is consumed 
	 */
	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		// Only route character typing directly to the currently focused child [3]
		GuiEventListener activeFocus = this.getFocused();
		if (activeFocus != null && activeFocus.charTyped(codePoint, modifiers)) {
			return true;
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
				this.setFocused(child); // Align container-focus mapping cleanly 
				this.setDragging(true); // Force active dragging state 
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
			this.focusedChild = null; // Clear focused widget candidate 
		}
		this.setDragging(false); // Reset dragging state 
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
                // Fix: Evaluate individual child visibilities instead of overwriting them 
                if (widget.visible) {
                    widget.active = this.active;
                    widget.render(guiGraphics, mouseX, mouseY, partialTick);
                }
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
		NineSliceUtil.drawDefault(guiGraphics, getX(), getY(), width, height);
	}
    
    /**
	 * Consolidated helper to retrieve the selected ConnectionBlock for targeted components.
	 */
	@Nullable
	protected ConnectionBlock getSelectedInventory() {
		if (component instanceof IInventoryTarget target) {
			int selectedId = target.getInventoryId();
			if (selectedId != -1) {
				for (ConnectionBlock block : parentScreen.getMenu().getManagerBlockEntity().getInventories()) {
					if (block.getId() == selectedId) {
						return block;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Consolidated helper to evaluate if a block face dynamically supports a capability.
	 * Accommodates special-handling for active directions on custom cluster cards.
	 */
	protected boolean sideSupportsCapability(Level level, @Nullable ConnectionBlock inv, @Nullable Direction side, ResourceLocation capabilityId) {
		if (level == null || inv == null || side == null) {
			return false;
		}
		BlockPos pos = inv.getBlockPos();
		
		// If targeting a cluster card, support only the card's active direction face
		if (inv.getSlotIndex() >= 0) {
			return inv.getDirection() == side;
		}
		
		var flowCap = FlowCapabilityRegistry.get(capabilityId);
		if (flowCap != null) {
			return flowCap.isPresent(level, pos, level.getBlockState(pos), level.getBlockEntity(pos), side);
		}
		return false;
	}

	/**
	 * Consolidated helper to serialize and push component updates to the server.
	 */
	protected void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
	}
}