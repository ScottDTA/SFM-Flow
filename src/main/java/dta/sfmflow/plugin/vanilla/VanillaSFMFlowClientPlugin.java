package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.client.FlowOverlayRegistry;
import dta.sfmflow.api.client.DataComponentOverlayRegistry;
import dta.sfmflow.client.screen.widgets.AdvancedItemFilterVariableSettingsOverlay;
import dta.sfmflow.client.screen.widgets.DamageComponentSettingsModal;
import dta.sfmflow.client.screen.widgets.EnchantmentsComponentSettingsModal;
import dta.sfmflow.client.screen.widgets.IntervalTriggerSettingsOverlay;
import dta.sfmflow.client.screen.widgets.ItemTransferSettingsOverlay;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.minecraft.core.component.DataComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Built-in vanilla client plugin configuring clientbound properties side-safely
 * [3].
 */
@OnlyIn(Dist.CLIENT)
public class VanillaSFMFlowClientPlugin {
	public void registerClientProperties() {
		// Register settings overlays for vanilla components [3]
		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.INTERVAL_TRIGGER.get(), (screen, component) -> {
			if (component instanceof IntervalTriggerComponent trigger) {
				return new IntervalTriggerSettingsOverlay(screen, trigger);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.ITEM_INPUT.get(), (screen, component) -> {
			if (component instanceof ItemTransferComponent transfer) {
				return new ItemTransferSettingsOverlay(screen, transfer);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.ITEM_OUTPUT.get(), (screen, component) -> {
			if (component instanceof ItemTransferComponent transfer) {
				return new ItemTransferSettingsOverlay(screen, transfer);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.ADVANCED_ITEM_FILTER_VARIABLE.get(), (screen, component) -> {
			if (component instanceof AdvancedItemFilterVariableComponent advancedVar) {
				return new AdvancedItemFilterVariableSettingsOverlay(screen, advancedVar);
			}
			return null;
		});

		// Register the custom modal popup for standard vanilla damage values [3]
		DataComponentOverlayRegistry.register(DataComponents.DAMAGE,
				(screen, stack) -> new DamageComponentSettingsModal(screen, stack));
		// Register the custom modal popup for standard vanilla enchantment value configurations [3]
		DataComponentOverlayRegistry.register(DataComponents.ENCHANTMENTS,
				(screen, stack) -> new EnchantmentsComponentSettingsModal(screen, stack));
	}
}