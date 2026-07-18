package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.BlockPreview3DWidget;
import dta.sfmflow.api.client.widget.InventorySelectorWidget;
import dta.sfmflow.api.client.widget.ItemFilterWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen interface mapping slots to directional faces via instant feedback
 * widgets. Redesigned with dynamic panel metrics to eliminate visual
 * overlaps completely.
 */
@OnlyIn(Dist.CLIENT)
public class ItemTransferSettingsOverlay extends NodeSettingsOverlay {
	private final InventorySelectorWidget selectorWidget;
	private final BlockPreview3DWidget previewWidget;
	private final ItemFilterWidget filterWidget;

	public ItemTransferSettingsOverlay(ManagerScreen parentScreen, ItemTransferComponent component) {
		super(parentScreen, component);
		this.width = 300;
		this.height = 360;
		this.setX((parentScreen.width - 300) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		component.setUseAll(false);
		component.setTargetSlot(-1);

		// Activate the ghost slots on the client menu
		parentScreen.getMenu().setActiveFilterComponent(component);

		// Activate the ghost slots on the server menu
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId()));

		this.previewWidget = new BlockPreview3DWidget(getX() + 25, getY() + 78, 250, 210,
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, component,
						face -> sideSupportsCapability(
								parentScreen.getMenu().getManagerBlockEntity().getLevel(),
								getSelectedInventory(),
								face,
								ResourceLocation.fromNamespaceAndPath("sfmflow", "item")
						), 
				parentScreen, () -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component,
				ResourceLocation.fromNamespaceAndPath("sfmflow", "item"),
				parentScreen, newInv -> {
					// Reset side selection settings to default when binding a different inventory
					component.setActiveSidesMask(0); // Reset side selection mask to 0 (all sides disabled) 
					for (Direction dir : Direction.values()) {
						component.setEnabledSlotsMask(dir, -1L); // Reset per-side slot exclusions to all enabled 
					}
					if (this.previewWidget != null) {
						this.previewWidget.updateHighlightState();
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.children.add(this.previewWidget);
		this.children.add(this.selectorWidget);

		this.filterWidget = new ItemFilterWidget(getX() + 30, getY() + 294, component, parentScreen, () -> {
			parentScreen.getMenu().getManagerBlockEntity().setChanged();
			sendSettingsUpdate();
		});
		this.children.add(this.filterWidget);
	}

	@Override
	public void closeAndSave() {
		// Reset active filter component on the client menu
		parentScreen.getMenu().setActiveFilterComponent(null);

		// Reset active filter component on the server menu
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}
}