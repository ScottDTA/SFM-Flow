package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.BlockPreview3DWidget;
import dta.sfmflow.api.client.widget.InventorySelectorWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
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
 * Settings overlay enabling visual configuration of energy input and output blocks [3].
 */
@OnlyIn(Dist.CLIENT)
public class EnergyTransferSettingsOverlay extends NodeSettingsOverlay {
	private final InventorySelectorWidget selectorWidget;
	private final BlockPreview3DWidget previewWidget;

	public EnergyTransferSettingsOverlay(ManagerScreen parentScreen, EnergyTransferComponent component) {
		super(parentScreen, component);
		this.width = 300;
		this.height = 310;
		this.setX((parentScreen.width - 300) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		parentScreen.getMenu().setActiveComponent(component);

		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId()));

		// Height increased from 160 to 210 to prevent projection overflow [3]
		this.previewWidget = new BlockPreview3DWidget(getX() + 25, getY() + 78, 250, 210,
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, component,
				face -> sideSupportsEnergy(parentScreen.getMenu().getManagerBlockEntity().getLevel(),
						getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, face),
				parentScreen, () -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component,
				ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"),
				parentScreen, newInv -> {
					component.setActiveSidesMask(0); // Reset side selection mask to 0 [3]
					if (this.previewWidget != null) {
						this.previewWidget.updateHighlightState();
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.children.add(this.previewWidget);
		this.children.add(this.selectorWidget);
	}

	private ConnectionBlock getSelectedInventory() {
		int selectedId = ((EnergyTransferComponent) component).getInventoryId();
		if (selectedId != -1) {
			for (ConnectionBlock block : parentScreen.getMenu().getManagerBlockEntity().getInventories()) {
				if (block.getId() == selectedId) {
					return block;
				}
			}
		}
		return null;
	}

	private boolean sideSupportsEnergy(Level level, BlockPos pos, Direction side) {
		if (level == null || pos == null) {
			return false;
		}
		var flowCap = FlowCapabilityRegistry.get(ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"));
		if (flowCap != null) {
			return flowCap.isPresent(level, pos, level.getBlockState(pos), level.getBlockEntity(pos), side);
		}
		return false;
	}

	private void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId(), nbt));
	}

	@Override
	public void closeAndSave() {
		parentScreen.getMenu().setActiveComponent(null);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}
}