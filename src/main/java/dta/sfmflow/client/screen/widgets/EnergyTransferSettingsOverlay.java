package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractTargetSettingsOverlay;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Settings overlay enabling visual configuration of energy input and output blocks.
 */
@OnlyIn(Dist.CLIENT)
public class EnergyTransferSettingsOverlay extends AbstractTargetSettingsOverlay {
	public EnergyTransferSettingsOverlay(ManagerScreen parentScreen, EnergyTransferComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"), 310);
	}
}