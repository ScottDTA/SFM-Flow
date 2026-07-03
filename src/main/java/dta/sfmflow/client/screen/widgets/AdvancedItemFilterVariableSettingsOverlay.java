package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.helper.MenuSlotRepositioner;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Settings overlay enabling visual configuration of a single-slot item variable
 * [3].
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedItemFilterVariableSettingsOverlay extends NodeSettingsOverlay {
	private static final ResourceLocation FILTER_SLOT_TEXTURE = ResourceLocation
			.fromNamespaceAndPath(dta.sfmflow.SFMFlow.MODID, "textures/gui/flowcomponents/filter_slot.png");

	private final AdvancedItemFilterVariableComponent component;
	private final Button toggleQtyBtn;
	private final EditBox qtyEdit;

	public AdvancedItemFilterVariableSettingsOverlay(ManagerScreen parentScreen,
			AdvancedItemFilterVariableComponent component) {
		super(parentScreen, component);
		this.component = component;
		this.width = 200;
		this.height = 140;
		this.setX((parentScreen.width - this.width) / 2);
		this.setY((256 - this.height) / 2);

		parentScreen.getMenu().setActiveComponent(component);
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId()));

		repositionGhostSlot();

		// 1. Initialize qtyEdit first to resolve Java "blank final field capture"
		// compilation issues [3]
		this.qtyEdit = new EditBox(parentScreen.getFont(), getX() + 20, getY() + 90, 160, 20,
				Component.literal("Quantity"));
		this.qtyEdit.setValue(String.valueOf(component.getQuantity()));
		this.qtyEdit.setEditable(component.isUseQuantity());
		this.qtyEdit.setFilter(text -> text.matches("\\d*")); // Accept integers only
		this.qtyEdit.setResponder(text -> {
			try {
				int val = Integer.parseInt(text);
				if (val > 0) {
					component.setQuantity(val);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}
			} catch (NumberFormatException ignored) {
			}
		});

		// 2. Initialize toggleQtyBtn afterward now that qtyEdit is safely initialized
		// and can be captured [3]
		this.toggleQtyBtn = Button
				.builder(Component.literal("Specific Qty: " + (component.isUseQuantity() ? "ON" : "OFF")), btn -> {
					component.setUseQuantity(!component.isUseQuantity());
					btn.setMessage(Component.literal("Specific Qty: " + (component.isUseQuantity() ? "ON" : "OFF")));
					qtyEdit.setEditable(component.isUseQuantity());
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}).pos(getX() + 20, getY() + 65).size(160, 20).build();

		this.children.add(new ApiWidgetAdapter<>(this.toggleQtyBtn));
		this.children.add(new ApiWidgetAdapter<>(this.qtyEdit));
	}

	private void repositionGhostSlot() {
		int slotX = getX() + 91;
		int slotY = getY() + 35;
		Slot slot = parentScreen.getMenu().slots.get(36);
		MenuSlotRepositioner.setSlotPosition(slot, slotX - parentScreen.getLeftPos(), slotY - parentScreen.getTopPos());
	}

	private void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
	}

	@Override
	public void setX(int x) {
		int dif = this.getX() - x;
		super.setX(x);
		updateChildrenXPositions(dif);
		repositionGhostSlot();
	}

	@Override
	public void setY(int y) {
		int dif = this.getY() - y;
		super.setY(y);
		updateChildrenYPositions(dif);
		repositionGhostSlot();
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderComponent(guiGraphics, mouseX, mouseY, partialTick);

		int slotX = getX() + 90;
		int slotY = getY() + 34;

		boolean hasItem = !component.getFilterStack().isEmpty();
		int vOffset = hasItem ? 18 : 0;

		guiGraphics.blit(FILTER_SLOT_TEXTURE, slotX, slotY, 0, vOffset, 18, 18, 18, 36);

		if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
			guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF8B8B8B);
		}

		if (hasItem) {
			guiGraphics.renderItem(component.getFilterStack(), slotX + 1, slotY + 1);
			guiGraphics.renderItemDecorations(parentScreen.getFont(), component.getFilterStack(), slotX + 1, slotY + 1);
		}
	}

	@Override
	public void closeAndSave() {
		parentScreen.getMenu().setActiveComponent(null);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}
}
