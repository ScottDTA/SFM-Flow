package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.capability.FlowCapability;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.capability.ItemTransferParams;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Built-in vanilla plugin registering the core interval triggers and item
 * transfers [3].
 */
public class VanillaSFMFlowPlugin {
	public static DeferredHolder<FlowComponentType, FlowComponentType> INTERVAL_TRIGGER;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ITEM_INPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ITEM_OUTPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ADVANCED_ITEM_FILTER_VARIABLE;

	public void registerComponents(DeferredRegister<FlowComponentType> registry) {
		// Register the vanilla item capability transfer behavior to our public registry [3]
		registerItemCapability();

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
	}

	private void registerItemCapability() {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
		FlowCapabilityRegistry.register(
				new FlowCapability<>(itemCapId, Capabilities.ItemHandler.BLOCK, "gui.sfmflow.type_item")
		);

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
}