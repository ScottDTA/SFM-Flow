package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFilterableTargetSettingsOverlay;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Settings overlay enabling visual configuration of item transfer input and output blocks.
 */
@OnlyIn(Dist.CLIENT)
public class ItemTransferSettingsOverlay extends AbstractFilterableTargetSettingsOverlay {
	public ItemTransferSettingsOverlay(ManagerScreen parentScreen, ItemTransferComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "item"), 360);
		component.setUseAll(false);
		component.setTargetSlot(-1);
	}
}