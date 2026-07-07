package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.compat.MekanismCompat;
import dta.sfmflow.api.capability.EnergyTransferParams;
import dta.sfmflow.api.capability.FlowCapability;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.capability.FluidTransferParams;
import dta.sfmflow.api.capability.ItemTransferParams;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Built-in vanilla plugin registering the core interval triggers, item
 * transfers, and cauldron capability bridges [3].
 */
public class VanillaSFMFlowPlugin {
	public static DeferredHolder<FlowComponentType, FlowComponentType> INTERVAL_TRIGGER;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ITEM_INPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ITEM_OUTPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ADVANCED_ITEM_FILTER_VARIABLE;
	public static DeferredHolder<FlowComponentType, FlowComponentType> FLUID_INPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> FLUID_OUTPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ADVANCED_FLUID_FILTER_VARIABLE;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ENERGY_INPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ENERGY_OUTPUT;

	public void registerComponents(DeferredRegister<FlowComponentType> registry) {
		// Register capabilities natively [3]
		registerItemCapability();
		registerFluidCapability();
		registerEnergyCapability();
		registerChemicalCapability();
		registerRedstoneCapability();
		registerCauldronBridges();

		INTERVAL_TRIGGER = FlowComponentBuilder.create("interval_trigger", IntervalTriggerComponent::new)
				.category(NodeCategory.TRIGGER).icon("textures/gui/menu_buttons/trigger_button.png")
				.displayName("gui.sfmflow.interval_trigger").codec(IntervalTriggerComponent.CODEC).build(registry);

		ITEM_INPUT = FlowComponentBuilder.create("item_input", uuid -> new ItemTransferComponent(uuid, true))
				.category(NodeCategory.INPUT).icon("textures/gui/menu_buttons/input_button.png")
				.displayName("gui.sfmflow.item_input").codec(ItemTransferComponent.INPUT_CODEC).build(registry);

		ITEM_OUTPUT = FlowComponentBuilder.create("item_output", uuid -> new ItemTransferComponent(uuid, false))
				.category(NodeCategory.OUTPUT).icon("textures/gui/menu_buttons/output_button.png")
				.displayName("gui.sfmflow.item_output").codec(ItemTransferComponent.OUTPUT_CODEC).build(registry);

		ADVANCED_ITEM_FILTER_VARIABLE = FlowComponentBuilder
				.create("advanced_item_filter_variable", AdvancedItemFilterVariableComponent::new)
				.category(NodeCategory.VARIABLE).icon("textures/gui/menu_buttons/variable_button.png")
				.displayName("gui.sfmflow.advanced_item_filter_variable")
				.codec(AdvancedItemFilterVariableComponent.CODEC).build(registry);

		FLUID_INPUT = FlowComponentBuilder.create("fluid_input", uuid -> new FluidTransferComponent(uuid, true))
				.category(NodeCategory.INPUT).icon("textures/gui/menu_buttons/input_button.png")
				.displayName("gui.sfmflow.fluid_input").codec(FluidTransferComponent.INPUT_CODEC).build(registry);

		FLUID_OUTPUT = FlowComponentBuilder.create("fluid_output", uuid -> new FluidTransferComponent(uuid, false))
				.category(NodeCategory.OUTPUT).icon("textures/gui/menu_buttons/output_button.png")
				.displayName("gui.sfmflow.fluid_output").codec(FluidTransferComponent.OUTPUT_CODEC).build(registry);
		
		ADVANCED_FLUID_FILTER_VARIABLE = FlowComponentBuilder
				.create("advanced_fluid_filter_variable", AdvancedFluidFilterVariableComponent::new)
				.category(NodeCategory.VARIABLE).icon("textures/gui/menu_buttons/variable_button.png")
				.displayName("gui.sfmflow.advanced_fluid_filter_variable")
				.codec(AdvancedFluidFilterVariableComponent.CODEC).build(registry);
		
		ENERGY_INPUT = FlowComponentBuilder.create("energy_input", uuid -> new EnergyTransferComponent(uuid, true))
				.category(NodeCategory.INPUT).icon("textures/gui/menu_buttons/input_button.png")
				.displayName("gui.sfmflow.energy_input").codec(EnergyTransferComponent.INPUT_CODEC).build(registry);

		ENERGY_OUTPUT = FlowComponentBuilder.create("energy_output", uuid -> new EnergyTransferComponent(uuid, false))
				.category(NodeCategory.OUTPUT).icon("textures/gui/menu_buttons/output_button.png")
				.displayName("gui.sfmflow.energy_output").codec(EnergyTransferComponent.OUTPUT_CODEC).build(registry);
	}

	private void registerEnergyCapability() {
		ResourceLocation energyCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "energy");
		FlowCapabilityRegistry
				.register(new FlowCapability<>(energyCapId, Capabilities.EnergyStorage.BLOCK, "gui.sfmflow.type_energy"));

		// Registered Transfer task executor to execute actual Forge Energy modifications on the server thread [3]
		FlowCapabilityRegistry.registerTransfer(energyCapId, (level, src, srcSide, dest, destSide, params) -> {
			if (params instanceof EnergyTransferParams task) {
				var source = level.getCapability(Capabilities.EnergyStorage.BLOCK, src, srcSide);
				var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, dest, destSide);

				if (source == null) {
					source = level.getCapability(Capabilities.EnergyStorage.BLOCK, src, null);
				}
				if (target == null) {
					target = level.getCapability(Capabilities.EnergyStorage.BLOCK, dest, null);
				}

				if (source != null && target != null) {
					int simDrained = source.extractEnergy(task.maxAmount(), true);
					if (simDrained > 0) {
						int accepted = target.receiveEnergy(simDrained, true);
						if (accepted > 0) {
							int realDrain = source.extractEnergy(accepted, false);
							if (realDrain > 0) {
								target.receiveEnergy(realDrain, false);
								return true;
							}
						}
					}
				}
			}
			return false;
		});
	}

	private void registerFluidCapability() {
		ResourceLocation fluidCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid");
		FlowCapabilityRegistry
				.register(new FlowCapability<>(fluidCapId, Capabilities.FluidHandler.BLOCK, "gui.sfmflow.type_fluid"));

		FlowCapabilityRegistry.registerTransfer(fluidCapId, (level, src, srcSide, dest, destSide, params) -> {
			if (params instanceof FluidTransferParams task) {
				var source = level.getCapability(Capabilities.FluidHandler.BLOCK, src, srcSide);
				var target = level.getCapability(Capabilities.FluidHandler.BLOCK, dest, destSide);

				if (source == null) {
					source = level.getCapability(Capabilities.FluidHandler.BLOCK, src, null);
				}
				if (target == null) {
					target = level.getCapability(Capabilities.FluidHandler.BLOCK, dest, null);
				}

				if (source != null && target != null) {
					FluidStack simDrained = source.drain(task.maxAmount(), IFluidHandler.FluidAction.SIMULATE);
					if (!simDrained.isEmpty() && FluidStack.isSameFluid(simDrained, task.fluid())) {
						int accepted = target.fill(simDrained, IFluidHandler.FluidAction.SIMULATE);
						if (accepted > 0) {
							FluidStack realDrain = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
							if (!realDrain.isEmpty()) {
								target.fill(realDrain, IFluidHandler.FluidAction.EXECUTE);
								return true;
							}
						}
					}
				}
			}
			return false;
		});
	}

	private void registerItemCapability() {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
		FlowCapabilityRegistry
				.register(new FlowCapability<>(itemCapId, Capabilities.ItemHandler.BLOCK, "gui.sfmflow.type_item"));

		FlowCapabilityRegistry.registerTransfer(itemCapId, (level, src, srcSide, dest, destSide, params) -> {
			if (params instanceof ItemTransferParams task) {
				var source = level.getCapability(Capabilities.ItemHandler.BLOCK, src, srcSide);
				var target = level.getCapability(Capabilities.ItemHandler.BLOCK, dest, destSide);

				if (source == null) {
					source = level.getCapability(Capabilities.ItemHandler.BLOCK, src, null);
				}
				if (target == null) {
					target = level.getCapability(Capabilities.ItemHandler.BLOCK, dest, null);
				}

				if (source != null && target != null) {
					ItemStack simExtracted = source.extractItem(task.srcSlot(), task.count(), true);
					if (ItemStack.isSameItemSameComponents(simExtracted, task.item())) {
						ItemStack targetRemaining;
						if (task.destSlot() != -1) {
							targetRemaining = target.insertItem(task.destSlot(), simExtracted, true);
						} else {
							targetRemaining = ItemHandlerHelper.insertItemStacked(target, simExtracted, true);
						}

						int realTransferCount = simExtracted.getCount() - targetRemaining.getCount();

						if (realTransferCount > 0) {
							ItemStack realExtracted = source.extractItem(task.srcSlot(), realTransferCount, false);
							if (task.destSlot() != -1) {
								target.insertItem(task.destSlot(), realExtracted, false);
							} else {
								ItemHandlerHelper.insertItemStacked(target, realExtracted, false);
							}
							return true;
						}
					}
				}
			}
			return false;
		});
	}

	@SuppressWarnings("unchecked")
	private void registerChemicalCapability() {
		ResourceLocation chemicalCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "chemical");
		BlockCapability<?, Direction> chemCap = MekanismCompat.getChemicalCapability();
		if (chemCap != null) {
			FlowCapabilityRegistry.register(new FlowCapability<>(chemicalCapId,
					(BlockCapability<Object, Direction>) chemCap, "Chemical (Gas)"));
		}
	}

	private void registerRedstoneCapability() {
		ResourceLocation redstoneCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone");
		FlowCapabilityRegistry.register(new FlowCapability<>(redstoneCapId, null, "gui.sfmflow.type_redstone"));
	}

	private void registerCauldronBridges() {
		// Bridge stateless vanilla cauldrons to our capability transfer network [3]
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.WATER_CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.LAVA_CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
	}
}