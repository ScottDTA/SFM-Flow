package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFilterableTargetSettingsOverlay;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Settings overlay enabling visual configuration of fluid input and output blocks.
 */
@OnlyIn(Dist.CLIENT)
public class FluidTransferSettingsOverlay extends AbstractFilterableTargetSettingsOverlay {
	public FluidTransferSettingsOverlay(ManagerScreen parentScreen, FluidTransferComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"), 360);
		component.setUseAll(false);
		component.setTargetSlot(-1);
	}
}