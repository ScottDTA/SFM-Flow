package dta.sfmflow.api.client.widget;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.AbstractFilterableConditionalComponent;
import dta.sfmflow.api.component.AbstractFilterableTransferComponent;
import dta.sfmflow.api.component.IFilterable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Consolidated base settings overlay managing a 3D preview, network selector, and 12-slot filter grid.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractFilterableTargetSettingsOverlay extends AbstractTargetSettingsOverlay {
	protected final ItemFilterWidget filterWidget;

	public AbstractFilterableTargetSettingsOverlay(ManagerScreen parentScreen, AbstractFlowComponent component, ResourceLocation capabilityId, int height) {
		super(parentScreen, component, capabilityId, height);

		this.filterWidget = new ItemFilterWidget(getX() + 30, getY() + 294, (IFilterable) component, parentScreen, () -> {
			parentScreen.getMenu().getManagerBlockEntity().setChanged();
			sendSettingsUpdate();
		});
		this.children.add(this.filterWidget);
	}

	@Override
	protected void onInventorySelected(ConnectionBlock newInv) {
		if (component instanceof AbstractFilterableTransferComponent trans) {
			trans.setActiveSidesMask(0);
			for (Direction dir : Direction.values()) {
				trans.setEnabledSlotsMask(dir, -1L); 
			}
		} else if (component instanceof AbstractFilterableConditionalComponent cond) {
			cond.setActiveSidesMask(0);
			for (Direction dir : Direction.values()) {
				cond.setEnabledSlotsMask(dir, -1L); 
			}
		}
	}
}