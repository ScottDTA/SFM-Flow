package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.BlockPreview3DWidget;
import dta.sfmflow.api.client.widget.InventorySelectorWidget;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ObserverTriggerComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ObserverTriggerSettingsOverlay extends NodeSettingsOverlay {
	private final InventorySelectorWidget selectorWidget;
	private final BlockPreview3DWidget previewWidget;

	public ObserverTriggerSettingsOverlay(ManagerScreen parentScreen, ObserverTriggerComponent component) {
		super(parentScreen, component);
		this.width = 300;
		this.height = 360;
		this.setX((parentScreen.width - 300) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		parentScreen.getMenu().setActiveComponent(component);
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId()));

		// 1. Force-sync frontFacing to the actual block's in-world facing on initial
		// load
		Level initialLevel = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		ConnectionBlock initialInv = getSelectedInventory();
		if (initialLevel != null && initialInv != null) {
			BlockState state = initialLevel.getBlockState(initialInv.getBlockPos());
			if (state.hasProperty(BlockStateProperties.FACING)) {
				component.setFrontFacing(state.getValue(BlockStateProperties.FACING));
			}
		}

		// The sideSupportChecker only returns true for the front facing face.
		// Combined with isSideActive, this renders a static green marker on front, and
		// red Xs on other faces.
		this.previewWidget = new BlockPreview3DWidget(getX() + 25, getY() + 78, 250, 190,
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, component,
				face -> face == component.getFrontFacing(), parentScreen, () -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component,
				ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"), parentScreen, block -> {
					Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
					if (level != null) {
						return level.getBlockState(block.getBlockPos()).is(ModBlocks.OBSERVER_CABLE_BLOCK.get());
					}
					return true;
				}, newInv -> {
					// Dynamically query blockstate on target selection
					Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
					if (level != null) {
						BlockState state = level.getBlockState(newInv.getBlockPos());
						if (state.hasProperty(BlockStateProperties.FACING)) {
							component.setFrontFacing(state.getValue(BlockStateProperties.FACING));
						}
					}
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
		int selectedId = ((ObserverTriggerComponent) component).getInventoryId();
		if (selectedId != -1) {
			for (ConnectionBlock block : parentScreen.getMenu().getManagerBlockEntity().getInventories()) {
				if (block.getId() == selectedId) {
					return block;
				}
			}
		}
		return null;
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