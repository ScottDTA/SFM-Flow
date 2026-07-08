package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.BlockPreview3DWidget;
import dta.sfmflow.api.client.widget.InventorySelectorWidget;
import dta.sfmflow.api.client.widget.ItemFilterWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.api.capability.FlowCapabilityRegistry; // Added import [3]
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

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
		this.setY(parentScreen.getOverlayTargetY(this.height));

		component.setUseAll(false);
		component.setTargetSlot(-1);

		// Activate the ghost slots on the client menu [3]
		parentScreen.getMenu().setActiveFilterComponent(component);

		// Activate the ghost slots on the server menu [3]
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId()));

		this.previewWidget = new BlockPreview3DWidget(getX() + 25, getY() + 78, 250, 210,
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, component,
				face -> sideSupportsItems(parentScreen.getMenu().getManagerBlockEntity().getLevel(),
						getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, face),
				parentScreen, () -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component,
				ResourceLocation.fromNamespaceAndPath("sfmflow", "item"),
				parentScreen, newInv -> {
					// Reset side selection settings to default when binding a different inventory [3]
					component.setActiveSidesMask(0); // Reset side selection mask to 0 (all sides disabled) [3]
					for (Direction dir : Direction.values()) {
						component.setEnabledSlotsMask(dir, -1L); // Reset per-side slot exclusions to all enabled [3]
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
		// Symmetrically check the registered FlowCapability to support bridges natively [3]
		var flowCap = FlowCapabilityRegistry.get(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"));
		if (flowCap != null) {
			return flowCap.isPresent(level, pos, level.getBlockState(pos), level.getBlockEntity(pos), side);
		}
		return false;
	}

	private void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
	}

	@Override
	public void closeAndSave() {
		// Reset active filter component on the client menu [3]
		parentScreen.getMenu().setActiveFilterComponent(null);

		// Reset active filter component on the server menu [3]
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}
}