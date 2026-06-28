package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.util.ConnectionBlock;
import dta.sfmflow.util.ConnectionBlockType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Screen interface mapping slots to directional faces via instant feedback
 * widgets [3]. Redesigned with dynamic panel metrics to eliminate visual
 * overlaps completely [3].
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
		this.setY(25);

		component.setUseAll(false);
		component.setTargetSlot(-1);

		// 1. Instantiate the 3D preview widget first so its final reference is fully assigned [3]
		this.previewWidget = new BlockPreview3DWidget(
				getX() + 25, getY() + 78, 250, 210,
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null,
				component,
				face -> sideSupportsItems(parentScreen.getMenu().getManagerBlockEntity().getLevel(), getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, face),
				parentScreen,
				() -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}
		);

		// 2. Instantiate the selector widget next, safely capturing the initialized previewWidget [3]
		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component, ConnectionBlockType.ITEM, parentScreen, newInv -> {
			if (this.previewWidget != null) {
				this.previewWidget.updateHighlightState(); // Sync checkbox highlight state smoothly [3]
			}
			parentScreen.getMenu().getManagerBlockEntity().setChanged();
			sendSettingsUpdate(); // Transmit changes immediately over the network [3]
		});

		// 3. Add both widgets to the child hierarchy [3]
		this.children.add(this.previewWidget);
		this.children.add(this.selectorWidget);

		// Reusable Item Filter widget handling toggle states and grid slots cleanly [3]
		this.filterWidget = new ItemFilterWidget(getX() + 30, getY() + 294, component, parentScreen, () -> {
			parentScreen.getMenu().getManagerBlockEntity().setChanged();
			sendSettingsUpdate(); // Transmit changes immediately over the network [3]
		});
		this.children.add(this.filterWidget);
	}

	private ConnectionBlock getSelectedInventory() {
		int selectedId = ((ItemTransferComponent) component).getInventoryId();
		if (selectedId != -1) {
			for (ConnectionBlock block : parentScreen.getMenu().getManagerBlockEntity().getInventories()) {
				if (block.getId() == selectedId) {
					return block;
				}
			}
		}
		return null;
	}

	private boolean sideSupportsItems(Level level, BlockPos pos, Direction side) {
		if (level == null || pos == null) {
			return false;
		}
		return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side) != null;
	}

	private void sendSettingsUpdate() {
		net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
		component.saveData(nbt);
		net.neoforged.neoforge.network.PacketDistributor.sendToServer(
			new dta.sfmflow.networking.packets.serverbound.SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), 
				component.getId(), 
				nbt
			)
		);
	}
}