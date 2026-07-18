package dta.sfmflow.api.client.widget;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.AbstractConditionalComponent;
import dta.sfmflow.api.component.AbstractTransferComponent;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Consolidated base settings overlay managing a standard 3D preview and network selector list.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractTargetSettingsOverlay extends NodeSettingsOverlay {
	protected final InventorySelectorWidget selectorWidget;
	protected final BlockPreview3DWidget previewWidget;
	protected final ResourceLocation capabilityId;

	public AbstractTargetSettingsOverlay(ManagerScreen parentScreen, AbstractFlowComponent component, ResourceLocation capabilityId, int height) {
		super(parentScreen, component);
		this.width = 300;
		this.height = height;
		this.setX((parentScreen.width - 300) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));
		this.capabilityId = capabilityId;

		parentScreen.getMenu().setActiveComponent(component);
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId()));

		this.previewWidget = new BlockPreview3DWidget(
				getX() + 25, 
				getY() + 78, 
				250, 
				height - 170, 
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, 
				(ISideConfigurable) component,
				face -> sideSupportsCapability(parentScreen.getMenu().getManagerBlockEntity().getLevel(), getSelectedInventory(), face, capabilityId),
				parentScreen, 
				() -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}
		);

		this.selectorWidget = new InventorySelectorWidget(
				getX() + 20, 
				getY() + 28, 
				(IInventoryTarget) component, 
				capabilityId,
				parentScreen,
				this::onInventoryFilter,
				newInv -> {
					onInventorySelected(newInv);
					if (this.previewWidget != null) {
						this.previewWidget.updateHighlightState();
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}
		);

		this.children.add(this.previewWidget);
		this.children.add(this.selectorWidget);
	}

	protected boolean onInventoryFilter(ConnectionBlock block) {
		return true;
	}

	/**
	 * Dynamic selection callback that resets active face-masks safely.
	 */
	protected void onInventorySelected(ConnectionBlock newInv) {
		if (component instanceof AbstractConditionalComponent cond) {
			cond.setActiveSidesMask(0);
		} else if (component instanceof AbstractTransferComponent trans) {
			trans.setActiveSidesMask(0);
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