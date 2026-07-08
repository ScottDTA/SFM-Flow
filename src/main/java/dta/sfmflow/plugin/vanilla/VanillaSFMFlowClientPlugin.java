package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.client.FlowOverlayRegistry;
import dta.sfmflow.api.client.SideConfigPopupRegistry;
import dta.sfmflow.api.client.DataComponentOverlayRegistry;
import dta.sfmflow.client.screen.widgets.AdvancedFluidFilterVariableSettingsOverlay;
import dta.sfmflow.client.screen.widgets.AdvancedItemFilterVariableSettingsOverlay;
import dta.sfmflow.client.screen.widgets.DamageComponentSettingsModal;
import dta.sfmflow.client.screen.widgets.EnchantmentsComponentSettingsModal;
import dta.sfmflow.client.screen.widgets.EnergySideConfigModalPopup;
import dta.sfmflow.client.screen.widgets.EnergyTransferSettingsOverlay;
import dta.sfmflow.client.screen.widgets.FluidTransferSettingsOverlay;
import dta.sfmflow.client.screen.widgets.IntervalTriggerSettingsOverlay;
import dta.sfmflow.client.screen.widgets.ItemTransferSettingsOverlay;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
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

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.FLUID_INPUT.get(), (screen, component) -> {
			if (component instanceof FluidTransferComponent transfer) {
				return new FluidTransferSettingsOverlay(screen, transfer);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.FLUID_OUTPUT.get(), (screen, component) -> {
			if (component instanceof FluidTransferComponent transfer) {
				return new FluidTransferSettingsOverlay(screen, transfer);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.ADVANCED_FLUID_FILTER_VARIABLE.get(), (screen, component) -> {
			if (component instanceof AdvancedFluidFilterVariableComponent advancedVar) {
				return new AdvancedFluidFilterVariableSettingsOverlay(screen, advancedVar);
			}
			return null;
		});
		
		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.ENERGY_INPUT.get(), (screen, component) -> {
			if (component instanceof EnergyTransferComponent transfer) {
				return new EnergyTransferSettingsOverlay(screen, transfer);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.ENERGY_OUTPUT.get(), (screen, component) -> {
			if (component instanceof EnergyTransferComponent transfer) {
				return new EnergyTransferSettingsOverlay(screen, transfer);
			}
			return null;
		});
		
		SideConfigPopupRegistry.register(EnergyTransferComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new EnergySideConfigModalPopup(screen, (EnergyTransferComponent) sideModel, face, pos, onChanged);
		});
		

		// Register the custom modal popup for standard vanilla damage values [3]
		DataComponentOverlayRegistry.register(DataComponents.DAMAGE,
				(screen, stack) -> new DamageComponentSettingsModal(screen, stack));
		// Register the custom modal popup for standard vanilla enchantment value
		// configurations [3]
		DataComponentOverlayRegistry.register(DataComponents.ENCHANTMENTS,
				(screen, stack) -> new EnchantmentsComponentSettingsModal(screen, stack));
	}
}