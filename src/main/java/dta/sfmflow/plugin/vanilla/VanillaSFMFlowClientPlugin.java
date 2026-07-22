package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.client.FlowOverlayRegistry;
import dta.sfmflow.api.client.SideConfigPopupRegistry;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.client.DataComponentOverlayRegistry;
import dta.sfmflow.api.client.WorkspaceValidatorRegistry;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.helper.WorkspaceValidator;
import dta.sfmflow.client.screen.widgets.AdvancedFluidFilterVariableSettingsOverlay;
import dta.sfmflow.client.screen.widgets.AdvancedItemFilterVariableSettingsOverlay;
import dta.sfmflow.client.screen.widgets.CollectorSettingsOverlay;
import dta.sfmflow.client.screen.widgets.DamageComponentSettingsModal;
import dta.sfmflow.client.screen.widgets.EnchantmentsComponentSettingsModal;
import dta.sfmflow.client.screen.widgets.EnergyConditionalSettingsOverlay;
import dta.sfmflow.client.screen.widgets.EnergySideConfigModalPopup;
import dta.sfmflow.client.screen.widgets.EnergyTransferSettingsOverlay;
import dta.sfmflow.client.screen.widgets.FluidConditionalSettingsOverlay;
import dta.sfmflow.client.screen.widgets.FluidTransferSettingsOverlay;
import dta.sfmflow.client.screen.widgets.IntervalTriggerSettingsOverlay;
import dta.sfmflow.client.screen.widgets.ItemConditionalSettingsOverlay;
import dta.sfmflow.client.screen.widgets.ItemTransferSettingsOverlay;
import dta.sfmflow.client.screen.widgets.ObserverTriggerSettingsOverlay;
import dta.sfmflow.client.screen.widgets.RedstoneConditionalSettingsOverlay;
import dta.sfmflow.client.screen.widgets.RedstoneEmitterSettingsOverlay;
import dta.sfmflow.client.screen.widgets.RedstoneEmitterSideConfigModalPopup;
import dta.sfmflow.client.screen.widgets.RedstoneSideConfigModalPopup;
import dta.sfmflow.client.screen.widgets.RedstoneTriggerSettingsOverlay;
import dta.sfmflow.client.screen.widgets.SculkTriggerSettingsOverlay;
import dta.sfmflow.client.screen.widgets.SculkTriggerSideConfigModalPopup;
import dta.sfmflow.client.screen.widgets.SignUpdaterSettingsOverlay;
import dta.sfmflow.client.screen.widgets.SlotLayoutModalPopup;
import dta.sfmflow.client.screen.widgets.SplitterSettingsOverlay;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.CollectorComponent;
import dta.sfmflow.flowcomponents.EnergyConditionalComponent;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
import dta.sfmflow.flowcomponents.GroupComponent;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemConditionalComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.flowcomponents.ObserverTriggerComponent;
import dta.sfmflow.flowcomponents.RedstoneConditionalComponent;
import dta.sfmflow.flowcomponents.RedstoneEmitterComponent;
import dta.sfmflow.flowcomponents.RedstoneTriggerComponent;
import dta.sfmflow.flowcomponents.SculkTriggerComponent;
import dta.sfmflow.flowcomponents.SignUpdaterComponent;
import dta.sfmflow.flowcomponents.SplitterComponent;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.flowcomponents.FluidConditionalComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/**
 * Built-in vanilla client plugin configuring clientbound properties
 * side-safely.
 */
@OnlyIn(Dist.CLIENT)
public class VanillaSFMFlowClientPlugin {

	/**
	 * Scans connections to verify if a targeted component ID participates in active
	 * visual execution chains.
	 */
	private static boolean hasActiveConnections(ManagerScreen screen, UUID id) {
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

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.REDSTONE_TRIGGER.get(), (screen, component) -> {
			if (component instanceof RedstoneTriggerComponent trigger) {
				return new RedstoneTriggerSettingsOverlay(screen, trigger);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.REDSTONE_EMITTER.get(), (screen, component) -> {
			if (component instanceof RedstoneEmitterComponent emitter) {
				return new RedstoneEmitterSettingsOverlay(screen, emitter);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.OBSERVER_TRIGGER.get(), (screen, component) -> {
			if (component instanceof ObserverTriggerComponent trigger) {
				return new ObserverTriggerSettingsOverlay(screen, trigger);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.ITEM_CONDITIONAL.get(), (screen, component) -> {
			if (component instanceof ItemConditionalComponent conditional) {
				return new ItemConditionalSettingsOverlay(screen, conditional);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.FLUID_CONDITIONAL.get(), (screen, component) -> {
			if (component instanceof FluidConditionalComponent conditional) {
				return new FluidConditionalSettingsOverlay(screen, conditional);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.ENERGY_CONDITIONAL.get(), (screen, component) -> {
			if (component instanceof EnergyConditionalComponent conditional) {
				return new EnergyConditionalSettingsOverlay(screen, conditional);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.REDSTONE_CONDITIONAL.get(), (screen, component) -> {
			if (component instanceof RedstoneConditionalComponent conditional) {
				return new RedstoneConditionalSettingsOverlay(screen, conditional);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.SPLITTER.get(), (screen, component) -> {
			if (component instanceof SplitterComponent splitter) {
				return new SplitterSettingsOverlay(screen, splitter);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.COLLECTOR.get(), (screen, component) -> {
			if (component instanceof CollectorComponent collector) {
				return new CollectorSettingsOverlay(screen, collector);
			}
			return null;
		});

		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.SCULK_TRIGGER.get(), (screen, component) -> {
			if (component instanceof SculkTriggerComponent trigger) {
				return new SculkTriggerSettingsOverlay(screen, trigger);
			}
			return null;
		});
		
		FlowOverlayRegistry.register(VanillaSFMFlowPlugin.SIGN_UPDATER.get(), (screen, component) -> {
			return new SignUpdaterSettingsOverlay(screen, component);
		});

		// 2. Sided Configuration Popups
		SideConfigPopupRegistry.register(EnergyTransferComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new EnergySideConfigModalPopup(screen, (EnergyTransferComponent) sideModel, face, pos, onChanged);
		});

		SideConfigPopupRegistry.register(RedstoneTriggerComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new RedstoneSideConfigModalPopup(screen, (RedstoneTriggerComponent) sideModel, face, pos, onChanged);
		});

		SideConfigPopupRegistry.register(RedstoneEmitterComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new RedstoneEmitterSideConfigModalPopup(screen, (RedstoneEmitterComponent) sideModel, face, pos,
					onChanged);
		});

		SideConfigPopupRegistry.register(ItemConditionalComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new SlotLayoutModalPopup(screen, sideModel, face, pos, onChanged);
		});

		SideConfigPopupRegistry.register(FluidConditionalComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new SlotLayoutModalPopup(screen, sideModel, face, pos, onChanged);
		});

		SideConfigPopupRegistry.register(RedstoneConditionalComponent.class,
				(screen, sideModel, face, pos, onChanged) -> {
					return new RedstoneSideConfigModalPopup(screen, (RedstoneConditionalComponent) sideModel, face, pos,
							onChanged);
				});

		SideConfigPopupRegistry.register(SculkTriggerComponent.class, (screen, sideModel, face, pos, onChanged) -> {
			return new SculkTriggerSideConfigModalPopup(screen, (SculkTriggerComponent) sideModel, face, pos,
					onChanged);
		});

		DataComponentOverlayRegistry.register(DataComponents.DAMAGE, DamageComponentSettingsModal::new);
		DataComponentOverlayRegistry.register(DataComponents.ENCHANTMENTS, EnchantmentsComponentSettingsModal::new);

		// =========================================================================
		// WORKSPACE VALIDATION REGISTRATIONS
		// =========================================================================

		// 1. Items Validation
		WorkspaceValidatorRegistry.register(ItemTransferComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<ItemTransferComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, ItemTransferComponent transfer) {
						if (isInventoryUnboundOrSleeping(screen, transfer.getInventoryId())) {
							return hasActiveConnections(screen, transfer.getId());
						}
						if (transfer.isWhitelist() && isWhitelistEmpty(transfer.getFilterItems())) {
							return hasActiveConnections(screen, transfer.getId());
						}
						if (transfer.getActiveSidesMask() == 0) {
							return hasActiveConnections(screen, transfer.getId());
						}
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen, ItemTransferComponent transfer) {
						if (isInventoryUnboundOrSleeping(screen, transfer.getInventoryId())) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						if (transfer.isWhitelist() && isWhitelistEmpty(transfer.getFilterItems())) {
							return Component.translatable("gui.sfmflow.error.empty_whitelist");
						}
						if (transfer.getActiveSidesMask() == 0) {
							return Component.translatable("gui.sfmflow.error.no_active_sides");
						}
						return null;
					}
				});

		// 2. Fluids Validation
		WorkspaceValidatorRegistry.register(FluidTransferComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<FluidTransferComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, FluidTransferComponent transfer) {
						if (isInventoryUnboundOrSleeping(screen, transfer.getInventoryId())) {
							return hasActiveConnections(screen, transfer.getId());
						}
						if (transfer.isWhitelist() && isWhitelistEmpty(transfer.getFilterItems())) {
							return hasActiveConnections(screen, transfer.getId());
						}
						if (transfer.getActiveSidesMask() == 0) {
							return hasActiveConnections(screen, transfer.getId());
						}
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen, FluidTransferComponent transfer) {
						if (isInventoryUnboundOrSleeping(screen, transfer.getInventoryId())) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						if (transfer.isWhitelist() && isWhitelistEmpty(transfer.getFilterItems())) {
							return Component.translatable("gui.sfmflow.error.empty_whitelist");
						}
						if (transfer.getActiveSidesMask() == 0) {
							return Component.translatable("gui.sfmflow.error.no_active_sides");
						}
						return null;
					}
				});

		// 3. Energy Validation
		WorkspaceValidatorRegistry.register(EnergyTransferComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<EnergyTransferComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, EnergyTransferComponent transfer) {
						if (isInventoryUnboundOrSleeping(screen, transfer.getInventoryId())) {
							return hasActiveConnections(screen, transfer.getId());
						}
						if (transfer.getActiveSidesMask() == 0) {
							return hasActiveConnections(screen, transfer.getId());
						}
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen, EnergyTransferComponent transfer) {
						if (isInventoryUnboundOrSleeping(screen, transfer.getInventoryId())) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						if (transfer.getActiveSidesMask() == 0) {
							return Component.translatable("gui.sfmflow.error.no_active_sides");
						}
						return null;
					}
				});

		// 4. Redstone Trigger Validation
		WorkspaceValidatorRegistry.register(RedstoneTriggerComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<RedstoneTriggerComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, RedstoneTriggerComponent transfer) {
						return isInventoryUnboundOrSleeping(screen, transfer.getInventoryId());
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							RedstoneTriggerComponent transfer) {
						if (hasError(screen, transfer)) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						return null;
					}
				});

		// 5. Variables Warnings
		WorkspaceValidatorRegistry.register(AdvancedItemFilterVariableComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<AdvancedItemFilterVariableComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, AdvancedItemFilterVariableComponent component) {
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							AdvancedItemFilterVariableComponent component) {
						return null;
					}

					@Override
					public boolean hasWarning(ManagerScreen screen, AdvancedItemFilterVariableComponent component) {
						return component.getFilterStack().isEmpty();
					}

					@Override
					public @Nullable Component getWarningTooltip(ManagerScreen screen,
							AdvancedItemFilterVariableComponent component) {
						return Component.translatable("gui.sfmflow.warning.empty_filter_variable");
					}
				});

		WorkspaceValidatorRegistry.register(AdvancedFluidFilterVariableComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<AdvancedFluidFilterVariableComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, AdvancedFluidFilterVariableComponent component) {
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							AdvancedFluidFilterVariableComponent component) {
						return null;
					}

					@Override
					public boolean hasWarning(ManagerScreen screen, AdvancedFluidFilterVariableComponent component) {
						return component.getFilterFluid().isEmpty();
					}

					@Override
					public @Nullable Component getWarningTooltip(ManagerScreen screen,
							AdvancedFluidFilterVariableComponent component) {
						return Component.translatable("gui.sfmflow.warning.empty_filter_variable");
					}
				});

		// 6. Redstone Emitter Validation
		WorkspaceValidatorRegistry.register(RedstoneEmitterComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<RedstoneEmitterComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, RedstoneEmitterComponent transfer) {
						return isInventoryUnboundOrSleeping(screen, transfer.getInventoryId());
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							RedstoneEmitterComponent transfer) {
						if (hasError(screen, transfer)) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						return null;
					}
				});

		WorkspaceValidatorRegistry.register(ObserverTriggerComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<ObserverTriggerComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, ObserverTriggerComponent transfer) {
						return isInventoryUnboundOrSleeping(screen, transfer.getInventoryId());
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							ObserverTriggerComponent transfer) {
						if (hasError(screen, transfer)) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						return null;
					}
				});

		WorkspaceValidatorRegistry.register(ItemConditionalComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<ItemConditionalComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, ItemConditionalComponent component) {
						if (isInventoryUnboundOrSleeping(screen, component.getInventoryId())) {
							return hasActiveConnections(screen, component.getId());
						}
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							ItemConditionalComponent component) {
						if (isInventoryUnboundOrSleeping(screen, component.getInventoryId())) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						return null;
					}
				});

		WorkspaceValidatorRegistry.register(FluidConditionalComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<FluidConditionalComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, FluidConditionalComponent component) {
						if (isInventoryUnboundOrSleeping(screen, component.getInventoryId())) {
							return hasActiveConnections(screen, component.getId());
						}
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							FluidConditionalComponent component) {
						if (isInventoryUnboundOrSleeping(screen, component.getInventoryId())) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						return null;
					}
				});

		WorkspaceValidatorRegistry.register(EnergyConditionalComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<EnergyConditionalComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, EnergyConditionalComponent component) {
						if (isInventoryUnboundOrSleeping(screen, component.getInventoryId())) {
							return hasActiveConnections(screen, component.getId());
						}
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							EnergyConditionalComponent component) {
						if (isInventoryUnboundOrSleeping(screen, component.getInventoryId())) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						return null;
					}
				});

		WorkspaceValidatorRegistry.register(RedstoneConditionalComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<RedstoneConditionalComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, RedstoneConditionalComponent component) {
						if (isInventoryUnboundOrSleeping(screen, component.getInventoryId())) {
							return hasActiveConnections(screen, component.getId());
						}
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							RedstoneConditionalComponent component) {
						if (isInventoryUnboundOrSleeping(screen, component.getInventoryId())) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						return null;
					}
				});

		// Splitter Validator with Chain-Limit Checks
		WorkspaceValidatorRegistry.register(SplitterComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<SplitterComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, SplitterComponent component) {
						int maxAllowed = ServerConfig.MAX_CHAINED_SPLITTERS.get();
						return getChainedSplitterDepth(screen, component) > maxAllowed;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen, SplitterComponent component) {
						int maxAllowed = ServerConfig.MAX_CHAINED_SPLITTERS.get();
						if (getChainedSplitterDepth(screen, component) > maxAllowed) {
							return Component.translatable("gui.sfmflow.error.splitter_chain_limit", maxAllowed);
						}
						return null;
					}

					private int getChainedSplitterDepth(ManagerScreen screen, SplitterComponent component) {
						var components = screen.getMenu().getManagerBlockEntity().getFlowComponents();
						var connections = screen.getMenu().getManagerBlockEntity().getFlowConnections();

						int maxDepth = 1;
						Queue<SplitterPathNode> queue = new ArrayDeque<>();
						queue.add(new SplitterPathNode(component.getId(), 1));

						while (!queue.isEmpty()) {
							SplitterPathNode current = queue.poll();
							if (current.depth() > maxDepth) {
								maxDepth = current.depth();
							}

							for (var conn : connections) {
								if (conn.getTargetComponentId().equals(current.id())) {
									UUID parentId = conn.getSourceComponentId();
									var parentComp = components.get(parentId);
									if (parentComp instanceof SplitterComponent) {
										queue.add(new SplitterPathNode(parentId, current.depth() + 1));
									}
								}
							}
						}
						return maxDepth;
					}

					class SplitterPathNode {
						final UUID id;
						final int depth;

						SplitterPathNode(UUID id, int depth) {
							this.id = id;
							this.depth = depth;
						}

						int depth() {
							return depth;
						}

						UUID id() {
							return id;
						}
					}
				});

		WorkspaceValidatorRegistry.register(SculkTriggerComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<SculkTriggerComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, SculkTriggerComponent component) {
						return false;
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen, SculkTriggerComponent component) {
						return null;
					}
				});
		
		WorkspaceValidatorRegistry.register(SignUpdaterComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<SignUpdaterComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, SignUpdaterComponent transfer) {
						return isInventoryUnboundOrSleeping(screen, transfer.getInventoryId());
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen,
							SignUpdaterComponent transfer) {
						if (hasError(screen, transfer)) {
							return Component.translatable("gui.sfmflow.error.unbound_inventory");
						}
						return null;
					}
				});
		

		// 7. Group Nodes Recursive Error/Warning Bubbling
		WorkspaceValidatorRegistry.register(GroupComponent.class,
				new WorkspaceValidatorRegistry.INodeValidator<GroupComponent>() {
					@Override
					public boolean hasError(ManagerScreen screen, GroupComponent group) {
						// Bubble up any nested errors recursively
						return hasNestedErrorRecursive(screen, group.getId());
					}

					@Override
					public @Nullable Component getErrorTooltip(ManagerScreen screen, GroupComponent group) {
						if (hasError(screen, group)) {
							return Component.translatable("gui.sfmflow.error.nested_group_error");
						}
						return null;
					}

					@Override
					public boolean hasWarning(ManagerScreen screen, GroupComponent group) {
						// Bubble up any nested warnings recursively
						return hasNestedWarningRecursive(screen, group.getId());
					}

					@Override
					public @Nullable Component getWarningTooltip(ManagerScreen screen, GroupComponent group) {
						if (hasWarning(screen, group)) {
							return Component.translatable("gui.sfmflow.warning.nested_group_warning");
						}
						return null;
					}

					// Recursive helper checking child nodes for errors
					private boolean hasNestedErrorRecursive(ManagerScreen s, UUID groupId) {
						var comps = s.getMenu().getManagerBlockEntity().getFlowComponents().values();
						for (AbstractFlowComponent comp : comps) {
							if (isGroupDescendantOf(s, comp.getParentGroupId(), groupId)) {
								if (!(comp instanceof GroupComponent)) {
									if (WorkspaceValidator.hasUnboundInventoryError(s, comp)) {
										return true;
									}
								} else {
									if (hasNestedErrorRecursive(s, comp.getId())) {
										return true;
									}
								}
							}
						}
						return false;
					}

					// Recursive helper checking child nodes for warnings
					private boolean hasNestedWarningRecursive(ManagerScreen s, UUID groupId) {
						var comps = s.getMenu().getManagerBlockEntity().getFlowComponents().values();
						for (AbstractFlowComponent comp : comps) {
							if (isGroupDescendantOf(s, comp.getParentGroupId(), groupId)) {
								if (!(comp instanceof GroupComponent)) {
									if (WorkspaceValidator.hasEmptyFilterVariableWarning(s, comp)) {
										return true;
									}
								} else {
									if (hasNestedWarningRecursive(s, comp.getId())) {
										return true;
									}
								}
							}
						}
						return false;
					}

					// Helper determining if a sub-group is nested inside a target parent group
					private boolean isGroupDescendantOf(ManagerScreen s, @Nullable UUID queryGroupId,
							UUID targetGroupId) {
						if (queryGroupId == null)
							return false;
						if (queryGroupId.equals(targetGroupId))
							return true;
						UUID current = queryGroupId;
						var comps = s.getMenu().getManagerBlockEntity().getFlowComponents();
						while (current != null) {
							var comp = comps.get(current);
							if (comp != null) {
								current = comp.getParentGroupId();
								if (targetGroupId.equals(current)) {
									return true;
								}
							} else {
								break;
							}
						}
						return false;
					}
				});

	}

	/**
	 * Shared helper determining if an inventory ID is completely unselected,
	 * missing, or in an unloaded chunk.
	 */
	private static boolean isInventoryUnboundOrSleeping(ManagerScreen screen, int inventoryId) {
		if (inventoryId == -1) {
			return true;
		}
		for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
			if (block.getId() == inventoryId && !block.isSleeping()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Shared helper identifying if a registered whitelist layout contains zero
	 * items.
	 */
	private static boolean isWhitelistEmpty(List<ItemStack> filterItems) {
		for (ItemStack stack : filterItems) {
			if (stack != null && !stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}
}