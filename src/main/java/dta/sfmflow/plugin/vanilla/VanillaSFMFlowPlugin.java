package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.compat.MekanismCompat;
import dta.sfmflow.api.capability.FlowCapability;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.capability.FluidTransferParams;
import dta.sfmflow.api.capability.ItemTransferParams;
import dta.sfmflow.api.capability.EnergyTransferParams;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.block.entity.RedstoneEmitterBlockEntity;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.flowcomponents.RedstoneEmitterComponent;
import dta.sfmflow.flowcomponents.RedstoneTriggerComponent;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.flowcomponents.ObserverTriggerComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Built-in vanilla plugin registering the core interval triggers, transfers,
 * and capability snapshotters.
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
	public static DeferredHolder<FlowComponentType, FlowComponentType> REDSTONE_TRIGGER;
	public static DeferredHolder<FlowComponentType, FlowComponentType> REDSTONE_EMITTER;
	public static DeferredHolder<FlowComponentType, FlowComponentType> OBSERVER_TRIGGER;

	public void registerComponents(DeferredRegister<FlowComponentType> registry) {
		// Register capabilities natively
		registerItemCapability();
		registerFluidCapability();
		registerEnergyCapability();
		registerChemicalCapability();
		registerRedstoneCapability();
		registerCauldronBridges();

		INTERVAL_TRIGGER = FlowComponentBuilder.create("interval_trigger", IntervalTriggerComponent::new)
				.category(NodeCategory.TRIGGER).icon("textures/gui/menu_buttons/interval_trigger_button.png")
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
				.category(NodeCategory.INPUT).icon("textures/gui/menu_buttons/fluid_input_button.png")
				.displayName("gui.sfmflow.fluid_input").codec(FluidTransferComponent.INPUT_CODEC).build(registry);

		FLUID_OUTPUT = FlowComponentBuilder.create("fluid_output", uuid -> new FluidTransferComponent(uuid, false))
				.category(NodeCategory.OUTPUT).icon("textures/gui/menu_buttons/fluid_output_button.png")
				.displayName("gui.sfmflow.fluid_output").codec(FluidTransferComponent.OUTPUT_CODEC).build(registry);

		ADVANCED_FLUID_FILTER_VARIABLE = FlowComponentBuilder
				.create("advanced_fluid_filter_variable", AdvancedFluidFilterVariableComponent::new)
				.category(NodeCategory.VARIABLE).icon("textures/gui/menu_buttons/fluid_variable_button.png")
				.displayName("gui.sfmflow.advanced_fluid_filter_variable")
				.codec(AdvancedFluidFilterVariableComponent.CODEC).build(registry);

		ENERGY_INPUT = FlowComponentBuilder.create("energy_input", uuid -> new EnergyTransferComponent(uuid, true))
				.category(NodeCategory.INPUT).icon("textures/gui/menu_buttons/energy_input_button.png")
				.displayName("gui.sfmflow.energy_input").codec(EnergyTransferComponent.INPUT_CODEC).build(registry);

		ENERGY_OUTPUT = FlowComponentBuilder.create("energy_output", uuid -> new EnergyTransferComponent(uuid, false))
				.category(NodeCategory.OUTPUT).icon("textures/gui/menu_buttons/energy_output_button.png")
				.displayName("gui.sfmflow.energy_output").codec(EnergyTransferComponent.OUTPUT_CODEC).build(registry);

		REDSTONE_TRIGGER = FlowComponentBuilder.create("redstone_trigger", RedstoneTriggerComponent::new)
				.category(NodeCategory.TRIGGER).icon("textures/gui/menu_buttons/redstone_trigger_button.png")
				.displayName("gui.sfmflow.redstone_trigger").codec(RedstoneTriggerComponent.CODEC).build(registry);

		REDSTONE_EMITTER = FlowComponentBuilder.create("redstone_emitter", RedstoneEmitterComponent::new)
				.category(NodeCategory.OUTPUT).icon("textures/gui/menu_buttons/redstone_emitter_button.png")
				.displayName("gui.sfmflow.redstone_emitter").codec(RedstoneEmitterComponent.CODEC).build(registry);

		OBSERVER_TRIGGER = FlowComponentBuilder.create("observer_trigger", ObserverTriggerComponent::new)
				.category(NodeCategory.TRIGGER).icon("textures/gui/menu_buttons/observer_trigger_button.png")
				.displayName("gui.sfmflow.observer_trigger").codec(ObserverTriggerComponent.CODEC).build(registry);
	}

	private void registerItemCapability() {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
		FlowCapabilityRegistry
				.register(new FlowCapability<>(itemCapId, Capabilities.ItemHandler.BLOCK, "gui.sfmflow.type_item"));

		// Register standard items snapshotter
		FlowCapabilityRegistry.registerSnapshotter(itemCapId, (IItemHandler handler) -> {
			Map<Integer, ThreadSafeInventorySnapshot.SlotSnapshot> slots = new HashMap<>();
			int count = handler.getSlots();
			for (int i = 0; i < count; i++) {
				int mainSlot = translateSlot(handler, i);
				slots.put(i, new ThreadSafeInventorySnapshot.SlotSnapshot(handler.getStackInSlot(i),
						handler.getSlotLimit(i), mainSlot));
			}
			return new ThreadSafeInventorySnapshot.InventorySnapshot(slots);
		});

		FlowCapabilityRegistry.registerTransfer(itemCapId,
				(Level level, BlockPos src, Direction srcSide, BlockPos dest, Direction destSide, Object params) -> {
					if (params instanceof ItemTransferParams task) {
						IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, src, srcSide);
						IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, dest, destSide);

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
									ItemStack realExtracted = source.extractItem(task.srcSlot(), realTransferCount,
											false);
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

	private void registerFluidCapability() {
		ResourceLocation fluidCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid");
		FlowCapabilityRegistry
				.register(new FlowCapability<>(fluidCapId, Capabilities.FluidHandler.BLOCK, "gui.sfmflow.type_fluid"));

		// Register standard fluids snapshotter
		FlowCapabilityRegistry.registerSnapshotter(fluidCapId, (IFluidHandler handler) -> {
			Map<Integer, ThreadSafeInventorySnapshot.TankSnapshot> tanks = new HashMap<>();
			int count = handler.getTanks();
			for (int i = 0; i < count; i++) {
				tanks.put(i, new ThreadSafeInventorySnapshot.TankSnapshot(handler.getFluidInTank(i),
						handler.getTankCapacity(i)));
			}
			return new ThreadSafeInventorySnapshot.FluidInventorySnapshot(tanks);
		});

		FlowCapabilityRegistry.registerTransfer(fluidCapId,
				(Level level, BlockPos src, Direction srcSide, BlockPos dest, Direction destSide, Object params) -> {
					if (params instanceof FluidTransferParams task) {
						IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, src, srcSide);
						IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, dest, destSide);

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

	private void registerEnergyCapability() {
		ResourceLocation energyCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "energy");
		FlowCapabilityRegistry.register(
				new FlowCapability<>(energyCapId, Capabilities.EnergyStorage.BLOCK, "gui.sfmflow.type_energy"));

		// Register standard energy snapshotter
		FlowCapabilityRegistry.registerSnapshotter(energyCapId, (IEnergyStorage handler) -> {
			return new ThreadSafeInventorySnapshot.EnergySnapshot(handler.getEnergyStored(),
					handler.getMaxEnergyStored(), handler.canExtract(), handler.canReceive());
		});

		// Explicit parameter types prevent JMM-type inference breakdowns
		FlowCapabilityRegistry.registerTransfer(energyCapId,
				(Level level, BlockPos src, Direction srcSide, BlockPos dest, Direction destSide, Object params) -> {
					if (params instanceof EnergyTransferParams task) {
						IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, src, srcSide);
						IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, dest, destSide);

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

		FlowCapabilityRegistry.registerTransfer(ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone_emitter"),
				(Level level, BlockPos src, Direction srcSide, BlockPos dest, Direction destSide, Object params) -> {
					if (params instanceof RedstoneEmitterComponent.RedstoneEmitterParams task && destSide != null) {
						BlockEntity be = level.getBlockEntity(dest);
						if (be instanceof RedstoneEmitterBlockEntity emitter) {
							int currentPower = emitter.getPowerForSide(destSide);
							int newPower = task.operator().apply(currentPower, task.modifierValue(),
									task.rolloverEnabled());

							if (task.isPulse()) {
								emitter.setPowerForSide(destSide, newPower);
								emitter.setPulsed(destSide, true);
								// Schedule 1-tick delay to clear the pulse automatically
								level.scheduleTick(dest, level.getBlockState(dest).getBlock(), 1);
							} else {
								emitter.setPowerForSide(destSide, newPower);
								emitter.setPulsed(destSide, false); // clear pulse state if set
							}
							return true;
						}
					}
					return false;
				});
	}

	private void registerCauldronBridges() {
		// Bridge stateless vanilla cauldrons to our capability transfer network
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.WATER_CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.LAVA_CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
	}

	/**
	 * Resolves the underlying main slot index from nested Capability Wrappers [3].
	 */
	private static int translateSlot(IItemHandler handler, int slotIndex) {
		if (handler == null) {
			return slotIndex;
		}
		String className = handler.getClass().getName();
		try {
			if (className.equals("net.neoforged.neoforge.items.wrapper.SidedInvWrapper")) {
				java.lang.reflect.Field invField = handler.getClass().getDeclaredField("inv");
				java.lang.reflect.Field sideField = handler.getClass().getDeclaredField("side");
				invField.setAccessible(true);
				sideField.setAccessible(true);
				net.minecraft.world.WorldlyContainer inv = (net.minecraft.world.WorldlyContainer) invField.get(handler);
				net.minecraft.core.Direction side = (net.minecraft.core.Direction) sideField.get(handler);
				if (inv != null && side != null) {
					int[] slots = inv.getSlotsForFace(side);
					if (slots != null && slotIndex >= 0 && slotIndex < slots.length) {
						return slots[slotIndex];
					}
				}
			} else if (className.equals("net.neoforged.neoforge.items.wrapper.RangedWrapper")) {
				java.lang.reflect.Field minSlotField = handler.getClass().getDeclaredField("minSlot");
				minSlotField.setAccessible(true);
				int minSlot = minSlotField.getInt(handler);

				java.lang.reflect.Field composeField = handler.getClass().getDeclaredField("compose");
				composeField.setAccessible(true);
				IItemHandler compose = (IItemHandler) composeField.get(handler);

				return translateSlot(compose, minSlot + slotIndex);
			}
		} catch (Exception e) {
			// Fallback on security exceptions or missing fields
		}
		return slotIndex;
	}
}