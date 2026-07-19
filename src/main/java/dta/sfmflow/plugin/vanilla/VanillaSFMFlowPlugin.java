package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.compat.MekanismCompat;
import dta.sfmflow.api.capability.FlowCapability;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.capability.FluidTransferParams;
import dta.sfmflow.api.capability.ItemTransferParams;
import dta.sfmflow.api.capability.EnergyTransferParams;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.api.capability.ClusterCardCapabilityRegistry;
import dta.sfmflow.api.capability.FlowCapabilityPresenceRegistry;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.block.FluidVacuumValveBlock;
import dta.sfmflow.block.ItemVacuumValveBlock;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.block.entity.RedstoneEmitterBlockEntity;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.CollectorComponent;
import dta.sfmflow.flowcomponents.EnergyConditionalComponent;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
import dta.sfmflow.flowcomponents.GroupComponent;
import dta.sfmflow.flowcomponents.GroupInputComponent;
import dta.sfmflow.flowcomponents.GroupOutputComponent;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemConditionalComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.flowcomponents.RedstoneEmitterComponent;
import dta.sfmflow.flowcomponents.RedstoneTriggerComponent;
import dta.sfmflow.flowcomponents.SculkTriggerComponent;
import dta.sfmflow.flowcomponents.SplitterComponent;
import dta.sfmflow.flowcomponents.EnergyTransferComponent;
import dta.sfmflow.flowcomponents.FluidConditionalComponent;
import dta.sfmflow.flowcomponents.ObserverTriggerComponent;
import dta.sfmflow.flowcomponents.RedstoneConditionalComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
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
	public static DeferredHolder<FlowComponentType, FlowComponentType> ITEM_CONDITIONAL;
	public static DeferredHolder<FlowComponentType, FlowComponentType> FLUID_CONDITIONAL;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ENERGY_CONDITIONAL;
	public static DeferredHolder<FlowComponentType, FlowComponentType> REDSTONE_CONDITIONAL;
	public static DeferredHolder<FlowComponentType, FlowComponentType> SPLITTER;
	public static DeferredHolder<FlowComponentType, FlowComponentType> COLLECTOR;
	public static DeferredHolder<FlowComponentType, FlowComponentType> SCULK_TRIGGER;
	public static DeferredHolder<FlowComponentType, FlowComponentType> GROUP_NODE;
	public static DeferredHolder<FlowComponentType, FlowComponentType> GROUP_INPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> GROUP_OUTPUT;

	public void registerComponents(DeferredRegister<FlowComponentType> registry) {
		// Register capabilities natively
		registerItemCapability();
		registerFluidCapability();
		registerEnergyCapability();
		registerChemicalCapability();
		registerRedstoneCapability();
		registerCauldronBridges();
		
		ResourceLocation sculkCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "sculk");
		FlowCapabilityRegistry.register(new FlowCapability<>(sculkCapId, null, "gui.sfmflow.type_sculk"));

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

		ITEM_CONDITIONAL = FlowComponentBuilder.create("item_conditional", ItemConditionalComponent::new)
				.category(NodeCategory.LOGIC).icon("textures/gui/menu_buttons/condition_button.png")
				.displayName("gui.sfmflow.item_conditional").codec(ItemConditionalComponent.CODEC).build(registry);
		
		FLUID_CONDITIONAL = FlowComponentBuilder.create("fluid_conditional", FluidConditionalComponent::new)
				.category(NodeCategory.LOGIC)
				.icon("textures/gui/menu_buttons/fluid_condition_button.png") 
				.displayName("gui.sfmflow.fluid_conditional")
				.codec(FluidConditionalComponent.CODEC)
				.build(registry);
		
		ENERGY_CONDITIONAL = FlowComponentBuilder.create("energy_conditional", EnergyConditionalComponent::new)
				.category(NodeCategory.LOGIC)
				.icon("textures/gui/menu_buttons/energy_condition_button.png") 
				.displayName("gui.sfmflow.energy_conditional")
				.codec(EnergyConditionalComponent.CODEC)
				.build(registry);
		
		REDSTONE_CONDITIONAL = FlowComponentBuilder.create("redstone_conditional", RedstoneConditionalComponent::new)
				.category(NodeCategory.LOGIC)
				.icon("textures/gui/menu_buttons/condition_button.png")
				.displayName("gui.sfmflow.redstone_conditional")
				.codec(RedstoneConditionalComponent.CODEC)
				.build(registry);
		
		SPLITTER = FlowComponentBuilder.create("splitter", SplitterComponent::new)
				.category(NodeCategory.LOGIC)
				.icon("textures/gui/menu_buttons/flow_control_button.png") 
				.displayName("gui.sfmflow.splitter")
				.codec(SplitterComponent.CODEC)
				.build(registry);
		
		COLLECTOR = FlowComponentBuilder.create("collector", CollectorComponent::new)
				.category(NodeCategory.LOGIC)
				.icon("textures/gui/menu_buttons/flow_control_button.png") 
				.displayName("gui.sfmflow.collector")
				.codec(CollectorComponent.CODEC)
				.build(registry);
		
		SCULK_TRIGGER = FlowComponentBuilder.create("sculk_trigger", SculkTriggerComponent::new)
				.category(NodeCategory.TRIGGER)
				.icon("textures/gui/menu_buttons/trigger_button.png") 
				.displayName("gui.sfmflow.sculk_trigger")
				.codec(SculkTriggerComponent.CODEC)
				.build(registry);
		
		GROUP_NODE = FlowComponentBuilder.create("group_node", GroupComponent::new)
				.category(NodeCategory.UTILITY).icon("textures/gui/menu_buttons/group_node_button.png")
				.displayName("gui.sfmflow.menu.group_node").codec(GroupComponent.CODEC).build(registry);

		GROUP_INPUT = FlowComponentBuilder.create("group_input", GroupInputComponent::new)
				.category(NodeCategory.UTILITY).icon("textures/gui/menu_buttons/input_button.png")
				.displayName("Group Input").codec(GroupInputComponent.CODEC).build(registry);

		GROUP_OUTPUT = FlowComponentBuilder.create("group_output", GroupOutputComponent::new)
				.category(NodeCategory.UTILITY).icon("textures/gui/menu_buttons/output_button.png")
				.displayName("Group Output").codec(GroupOutputComponent.CODEC).build(registry);
		
		FlowCapabilityRegistry.registerTransfer(ResourceLocation.fromNamespaceAndPath("sfmflow", "splitter_sync"),
				(Level level, BlockPos src, Direction srcSide, BlockPos dest, Direction destSide, Object params) -> {
					if (params instanceof SplitterComponent.SplitterSyncParams task) {
						for (ManagerBlockEntity manager : ManagerBlockEntity.getActiveManagers()) {
							if (manager.getLevel() == level) {
								AbstractFlowComponent comp = manager.getFlowComponents().get(task.componentId());
								if (comp instanceof SplitterComponent splitter) {
									splitter.setLastOutputIndex(task.nextIndex());
									manager.setChanged();
									return true;
								}
							}
						}
					}
					return false;
				});
	}

	/**
	 * Safe common-setup callback executed after all registries are fully bound and frozen.
	 */
	public static void registerCardCapabilities() {
		// 1. Register Card Capability Proxies safely
		ClusterCardCapabilityRegistry.register(Capabilities.ItemHandler.BLOCK, ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get().asItem(), 
				(level, pos, side, slot, be) -> {
					if (be.getSlotDirection(slot) == side) {
						return be.getSlotBuffer(slot);
					}
					return null;
				});

		ClusterCardCapabilityRegistry.register(Capabilities.ItemHandler.BLOCK, ModBlocks.ITEM_EJECTOR_VALVE_BLOCK.get().asItem(), 
				(level, pos, side, slot, be) -> {
					if (be.getSlotDirection(slot) == side) {
						return be.getSlotBuffer(slot);
					}
					return null;
				});

		ClusterCardCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get().asItem(), 
				(level, pos, side, slot, be) -> {
					if (be.getSlotDirection(slot) == side) {
						return be.getFluidBuffer(slot);
					}
					return null;
				});

		ClusterCardCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, ModBlocks.FLUID_EJECTOR_VALVE_BLOCK.get().asItem(), 
				(level, pos, side, slot, be) -> {
					if (be.getSlotDirection(slot) == side) {
						return be.getFluidBuffer(slot);
					}
					return null;
				});

		ClusterCardCapabilityRegistry.register(Capabilities.ItemHandler.BLOCK, ModBlocks.ITEM_RELAY_BLOCK.get().asItem(), 
				(level, pos, side, slot, be) -> {
					return be.getEntityItemHandler(side);
				});

		ClusterCardCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, ModBlocks.FLUID_RELAY_BLOCK.get().asItem(), 
				(level, pos, side, slot, be) -> {
					return be.getEntityFluidHandler(side);
				});

		ClusterCardCapabilityRegistry.register(Capabilities.EnergyStorage.BLOCK, ModBlocks.ENERGY_RELAY_BLOCK.get().asItem(), 
				(level, pos, side, slot, be) -> {
					return be.getEntityEnergyHandler(side);
				});

		// 2. Register Specialty Always-Present scan blocks safely
		FlowCapabilityPresenceRegistry.registerAlwaysPresent(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"), ModBlocks.ITEM_RELAY_BLOCK.get());
		FlowCapabilityPresenceRegistry.registerAlwaysPresent(ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"), ModBlocks.FLUID_RELAY_BLOCK.get());
		FlowCapabilityPresenceRegistry.registerAlwaysPresent(ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"), ModBlocks.ENERGY_RELAY_BLOCK.get());
	}

	private void registerItemCapability() {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
		FlowCapabilityRegistry
				.register(new FlowCapability<>(itemCapId, Capabilities.ItemHandler.BLOCK, "gui.sfmflow.type_item"));

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
						if (level.getBlockState(src).is(ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get())) {
							return executeVacuumTransfer(level, src, dest, destSide, task);
						}

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
						if (level.getBlockState(src).is(ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get())) {
							return executeFluidVacuumTransfer(level, src, dest, destSide, task);
						}
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

	private static boolean executeFluidVacuumTransfer(Level level, BlockPos src, BlockPos dest, Direction destSide,
			FluidTransferParams task) {
		BlockState state = level.getBlockState(src);
		if (!state.hasProperty(FluidVacuumValveBlock.FACING)) {
			return false;
		}
		Direction facing = state.getValue(FluidVacuumValveBlock.FACING);
		BlockPos mouthPos = src.relative(facing);

		FluidState fluidState = level.getFluidState(mouthPos);
		if (fluidState.isSource() && fluidState.getType() == task.fluid().getFluid()) {
			IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, dest, destSide);
			if (target == null) {
				target = level.getCapability(Capabilities.FluidHandler.BLOCK, dest, null);
			}

			if (target != null) {
				FluidStack simDrained = new FluidStack(fluidState.getType(), 1000);
				int accepted = target.fill(simDrained, IFluidHandler.FluidAction.SIMULATE);
				if (accepted == 1000) {
					target.fill(simDrained, IFluidHandler.FluidAction.EXECUTE);
					level.setBlock(mouthPos, Blocks.AIR.defaultBlockState(), 3);
					return true;
				}
			}
		}
		return false;
	}

	private void registerEnergyCapability() {
		ResourceLocation energyCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "energy");
		FlowCapabilityRegistry.register(
				new FlowCapability<>(energyCapId, Capabilities.EnergyStorage.BLOCK, "gui.sfmflow.type_energy"));

		FlowCapabilityRegistry.registerSnapshotter(energyCapId, (IEnergyStorage handler) -> {
			return new ThreadSafeInventorySnapshot.EnergySnapshot(handler.getEnergyStored(),
					handler.getMaxEnergyStored(), handler.canExtract(), handler.canReceive());
		});

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
								level.scheduleTick(dest, level.getBlockState(dest).getBlock(), 1);
							} else {
								emitter.setPowerForSide(destSide, newPower);
								emitter.setPulsed(destSide, false); 
							}
							return true;
						}
					}
					return false;
				});
	}

	private void registerCauldronBridges() {
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.WATER_CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
		SpecialBlockCapabilityRegistry.register(Capabilities.FluidHandler.BLOCK, Blocks.LAVA_CAULDRON,
				(level, pos, state, side) -> new CauldronFluidHandler(level, pos));
	}

	private static int translateSlot(IItemHandler handler, int slotIndex) {
		if (handler == null) {
			return slotIndex;
		}
		String className = handler.getClass().getName();
		try {
			if (className.equals("net.neoforged.neoforge.items.wrapper.SidedInvWrapper")) {
				Field invField = handler.getClass().getDeclaredField("inv");
				Field sideField = handler.getClass().getDeclaredField("side");
				invField.setAccessible(true);
				sideField.setAccessible(true);
				WorldlyContainer inv = (WorldlyContainer) invField.get(handler);
				Direction side = (Direction) sideField.get(handler);
				if (inv != null && side != null) {
					int[] slots = inv.getSlotsForFace(side);
					if (slots != null && slotIndex >= 0 && slotIndex < slots.length) {
						return slots[slotIndex];
					}
				}
			} else if (className.equals("net.neoforged.neoforge.items.wrapper.RangedWrapper")) {
				Field minSlotField = handler.getClass().getDeclaredField("minSlot");
				minSlotField.setAccessible(true);
				int minSlot = minSlotField.getInt(handler);

				Field composeField = handler.getClass().getDeclaredField("compose");
				composeField.setAccessible(true);
				IItemHandler compose = (IItemHandler) composeField.get(handler);

				return translateSlot(compose, minSlot + slotIndex);
			}
		} catch (Exception e) {
		}
		return slotIndex;
	}

	private static boolean executeVacuumTransfer(Level level, BlockPos src, BlockPos dest, Direction destSide,
			ItemTransferParams task) {
		BlockState state = level.getBlockState(src);
		if (!state.hasProperty(ItemVacuumValveBlock.FACING)) {
			return false;
		}
		Direction facing = state.getValue(ItemVacuumValveBlock.FACING);
		BlockPos centerPos = src.relative(facing, 2);
		AABB suctionBox = new AABB(centerPos).inflate(1.0);

		List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, suctionBox);
		for (ItemEntity entity : entities) {
			if (entity.isAlive() && !entity.hasPickUpDelay()) {
				ItemStack groundStack = entity.getItem();
				if (!groundStack.isEmpty() && ItemStack.isSameItemSameComponents(groundStack, task.item())) {
					IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, dest, destSide);
					if (target == null) {
						target = level.getCapability(Capabilities.ItemHandler.BLOCK, dest, null);
					}
					if (target != null) {
						int toExtract = Math.min(groundStack.getCount(), task.count());
						ItemStack simExtracted = groundStack.copyWithCount(toExtract);

						ItemStack targetRemaining;
						if (task.destSlot() != -1) {
							targetRemaining = target.insertItem(task.destSlot(), simExtracted, true);
						} else {
							targetRemaining = ItemHandlerHelper.insertItemStacked(target, simExtracted, true);
						}

						int realTransferCount = simExtracted.getCount() - targetRemaining.getCount();
						if (realTransferCount > 0) {
							ItemStack realInsert = groundStack.copyWithCount(realTransferCount);
							if (task.destSlot() != -1) {
								target.insertItem(task.destSlot(), realInsert, false);
							} else {
								ItemHandlerHelper.insertItemStacked(target, realInsert, false);
							}

							groundStack.shrink(realTransferCount);
							if (groundStack.isEmpty()) {
								entity.discard();
							} else {
								entity.setItem(groundStack);
							}
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}