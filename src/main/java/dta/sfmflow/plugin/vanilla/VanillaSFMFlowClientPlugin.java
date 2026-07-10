package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.client.FlowOverlayRegistry;
import dta.sfmflow.api.client.SideConfigPopupRegistry;
import dta.sfmflow.api.client.DataComponentOverlayRegistry;
import dta.sfmflow.api.client.WorkspaceValidatorRegistry; // Added [3]
import dta.sfmflow.client.screen.ManagerScreen; // Added [3]
import dta.sfmflow.client.screen.widgets.AdvancedFluidFilterVariableSettingsOverlay;
import dta.sfmflow.client.screen.widgets.AdvancedItemFilterVariableSettingsOverlay;
import dta.sfmflow.client.screen.widgets.DamageComponentSettingsModal;
import dta.sfmflow.client.screen.widgets.EnchantmentsComponentSettingsModal;
import dta.sfmflow.client.screen.widgets.EnergySideConfigModalPopup;
import dta.sfmflow.client.screen.widgets.EnergyTransferSettingsOverlay;
import dta.sfmflow.client.screen.widgets.FluidTransferSettingsOverlay;
import dta.sfmflow.client.screen.widgets.IntervalTriggerSettingsOverlay;
import dta.sfmflow.client.screen.widgets.ItemTransferSettingsOverlay;
import dta.sfmflow.client.screen.widgets.RedstoneSideConfigModalPopup;
import dta.sfmflow.client.screen.widgets.RedstoneTriggerSettingsOverlay;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.flowcomponents.RedstoneTriggerComponent;
import dta.sfmflow.flowcomponents.EnergyTransferComponent; // Added [3]
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component; // Added [3]
import net.minecraft.world.item.ItemStack; // Added [3]
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * Built-in vanilla client plugin configuring clientbound properties side-safely [3].
 */
@OnlyIn(Dist.CLIENT)
public class VanillaSFMFlowClientPlugin {

	/**
	 * Scans connections to verify if a targeted component ID participates in active visual execution chains [3].
	 */
	private static boolean hasActiveConnections(ManagerScreen screen, java.util.UUID id) {
		var connections = screen.getMenu().getManagerBlockEntity().getFlowConnections();
		for (var conn : connections) {
			if (conn.getSourceComponentId().equals(id) || conn.getTargetComponentId().equals(id)) {
				return true;
			}
		}
		return false;
	}

	public void registerClientProperties() {
		// 1. Settings Overlays Registrations...
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

		// Register the new Redstone Trigger Overlay [3]
		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.REDSTONE_TRIGGER.get(), (screen, component) -> {
			if (component instanceof RedstoneTriggerComponent trigger) {
				return new RedstoneTriggerSettingsOverlay(screen, trigger);
			}
			return null;
		});

		// 2. Sided Configuration Popups [3]
		SideConfigPopupRegistry.register(EnergyTransferComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new EnergySideConfigModalPopup(screen, (EnergyTransferComponent) sideModel, face, pos, onChanged);
		});

		// Register the sided Redstone Trigger popup [3]
		SideConfigPopupRegistry.register(RedstoneTriggerComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new RedstoneSideConfigModalPopup(screen, (RedstoneTriggerComponent) sideModel, face, pos, onChanged);
		});

		DataComponentOverlayRegistry.register(DataComponents.DAMAGE, DamageComponentSettingsModal::new);
		DataComponentOverlayRegistry.register(DataComponents.ENCHANTMENTS, EnchantmentsComponentSettingsModal::new);


		// =========================================================================
		// WORKSPACE VALIDATION REGISTRATIONS [3]
		// =========================================================================

		// 1. Items Validation [3]
		WorkspaceValidatorRegistry.register(ItemTransferComponent.class, new WorkspaceValidatorRegistry.INodeValidator<ItemTransferComponent>() {
			@Override
			public boolean hasError(ManagerScreen screen, ItemTransferComponent transfer) {
				boolean foundBoundInventory = false;
				if (transfer.getInventoryId() != -1) {
					for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
						if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
							foundBoundInventory = true;
							break;
						}
					}
				}

				if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
					if (hasActiveConnections(screen, transfer.getId())) {
						return true;
					}
				}

				if (transfer.isWhitelist()) {
					boolean empty = true;
					for (ItemStack stack : transfer.getFilterItems()) {
						if (stack != null && !stack.isEmpty()) {
							empty = false;
							break;
						}
					}
					if (empty) {
						if (hasActiveConnections(screen, transfer.getId())) {
							return true;
						}
					}
				}

				if (transfer.getActiveSidesMask() == 0) {
					if (hasActiveConnections(screen, transfer.getId())) {
						return true;
					}
				}
				return false;
			}

			@Override
			public @Nullable Component getErrorTooltip(ManagerScreen screen, ItemTransferComponent transfer) {
				boolean foundBoundInventory = false;
				if (transfer.getInventoryId() != -1) {
					for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
						if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
							foundBoundInventory = true;
							break;
						}
					}
				}

				if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
					return Component.translatable("gui.sfmflow.error.unbound_inventory");
				}

				if (transfer.isWhitelist()) {
					boolean empty = true;
					for (ItemStack stack : transfer.getFilterItems()) {
						if (stack != null && !stack.isEmpty()) {
							empty = false;
							break;
						}
					}
					if (empty) {
						return Component.translatable("gui.sfmflow.error.empty_whitelist");
					}
				}

				if (transfer.getActiveSidesMask() == 0) {
					return Component.translatable("gui.sfmflow.error.no_active_sides");
				}
				return null;
			}
		});

		// 2. Fluids Validation [3]
		WorkspaceValidatorRegistry.register(FluidTransferComponent.class, new WorkspaceValidatorRegistry.INodeValidator<FluidTransferComponent>() {
			@Override
			public boolean hasError(ManagerScreen screen, FluidTransferComponent transfer) {
				boolean foundBoundInventory = false;
				if (transfer.getInventoryId() != -1) {
					for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
						if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
							foundBoundInventory = true;
							break;
						}
					}
				}

				if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
					if (hasActiveConnections(screen, transfer.getId())) {
						return true;
					}
				}

				if (transfer.isWhitelist()) {
					boolean empty = true;
					for (ItemStack stack : transfer.getFilterItems()) {
						if (stack != null && !stack.isEmpty()) {
							empty = false;
							break;
						}
					}
					if (empty) {
						if (hasActiveConnections(screen, transfer.getId())) {
							return true;
						}
					}
				}

				if (transfer.getActiveSidesMask() == 0) {
					if (hasActiveConnections(screen, transfer.getId())) {
						return true;
					}
				}
				return false;
			}

			@Override
			public @Nullable Component getErrorTooltip(ManagerScreen screen, FluidTransferComponent transfer) {
				boolean foundBoundInventory = false;
				if (transfer.getInventoryId() != -1) {
					for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
						if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
							foundBoundInventory = true;
							break;
						}
					}
				}

				if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
					return Component.translatable("gui.sfmflow.error.unbound_inventory");
				}

				if (transfer.isWhitelist()) {
					boolean empty = true;
					for (ItemStack stack : transfer.getFilterItems()) {
						if (stack != null && !stack.isEmpty()) {
							empty = false;
							break;
						}
					}
					if (empty) {
						return Component.translatable("gui.sfmflow.error.empty_whitelist");
					}
				}

				if (transfer.getActiveSidesMask() == 0) {
					return Component.translatable("gui.sfmflow.error.no_active_sides");
				}
				return null;
			}
		});

		// 3. Energy Validation [3]
		WorkspaceValidatorRegistry.register(EnergyTransferComponent.class, new WorkspaceValidatorRegistry.INodeValidator<EnergyTransferComponent>() {
			@Override
			public boolean hasError(ManagerScreen screen, EnergyTransferComponent transfer) {
				boolean foundBoundInventory = false;
				if (transfer.getInventoryId() != -1) {
					for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
						if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
							foundBoundInventory = true;
							break;
						}
					}
				}

				if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
					if (hasActiveConnections(screen, transfer.getId())) {
						return true;
					}
				}

				if (transfer.getActiveSidesMask() == 0) {
					if (hasActiveConnections(screen, transfer.getId())) {
						return true;
					}
				}
				return false;
			}

			@Override
			public @Nullable Component getErrorTooltip(ManagerScreen screen, EnergyTransferComponent transfer) {
				boolean foundBoundInventory = false;
				if (transfer.getInventoryId() != -1) {
					for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
						if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
							foundBoundInventory = true;
							break;
						}
					}
				}

				if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
					return Component.translatable("gui.sfmflow.error.unbound_inventory");
				}

				if (transfer.getActiveSidesMask() == 0) {
					return Component.translatable("gui.sfmflow.error.no_active_sides");
				}
				return null;
			}
		});

		// 4. Redstone Trigger Validation [3]
		WorkspaceValidatorRegistry.register(RedstoneTriggerComponent.class, new WorkspaceValidatorRegistry.INodeValidator<RedstoneTriggerComponent>() {
			@Override
			public boolean hasError(ManagerScreen screen, RedstoneTriggerComponent transfer) {
				boolean foundBoundInventory = false;
				if (transfer.getInventoryId() != -1) {
					for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
						if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
							foundBoundInventory = true;
							break;
						}
					}
				}
				return transfer.getInventoryId() == -1 || !foundBoundInventory;
			}

			@Override
			public @Nullable Component getErrorTooltip(ManagerScreen screen, RedstoneTriggerComponent transfer) {
				boolean foundBoundInventory = false;
				if (transfer.getInventoryId() != -1) {
					for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
						if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
							foundBoundInventory = true;
							break;
						}
					}
				}
				if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
					return Component.translatable("gui.sfmflow.error.unbound_inventory");
				}
				return null;
			}
		});

		// 5. Variables Warnings [3]
		WorkspaceValidatorRegistry.register(AdvancedItemFilterVariableComponent.class, new WorkspaceValidatorRegistry.INodeValidator<AdvancedItemFilterVariableComponent>() {
			@Override
			public boolean hasError(ManagerScreen screen, AdvancedItemFilterVariableComponent component) { return false; }
			@Override
			public @Nullable Component getErrorTooltip(ManagerScreen screen, AdvancedItemFilterVariableComponent component) { return null; }

			@Override
			public boolean hasWarning(ManagerScreen screen, AdvancedItemFilterVariableComponent component) {
				return component.getFilterStack().isEmpty();
			}

			@Override
			public @Nullable Component getWarningTooltip(ManagerScreen screen, AdvancedItemFilterVariableComponent component) {
				return Component.translatable("gui.sfmflow.warning.empty_filter_variable");
			}
		});

		WorkspaceValidatorRegistry.register(AdvancedFluidFilterVariableComponent.class, new WorkspaceValidatorRegistry.INodeValidator<AdvancedFluidFilterVariableComponent>() {
			@Override
			public boolean hasError(ManagerScreen screen, AdvancedFluidFilterVariableComponent component) { return false; }
			@Override
			public @Nullable Component getErrorTooltip(ManagerScreen screen, AdvancedFluidFilterVariableComponent component) { return null; }

			@Override
			public boolean hasWarning(ManagerScreen screen, AdvancedFluidFilterVariableComponent component) {
				return component.getFilterFluid().isEmpty();
			}

			@Override
			public @Nullable Component getWarningTooltip(ManagerScreen screen, AdvancedFluidFilterVariableComponent component) {
				return Component.translatable("gui.sfmflow.warning.empty_filter_variable");
			}
		});
	}
}