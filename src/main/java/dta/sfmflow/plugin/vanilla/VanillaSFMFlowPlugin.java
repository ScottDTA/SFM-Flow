package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.plugin.ISFMFlowPlugin;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Built-in vanilla plugin registering the core interval triggers and item transfers [3].
 */
public class VanillaSFMFlowPlugin implements ISFMFlowPlugin {
	public static DeferredHolder<FlowComponentType, FlowComponentType> INTERVAL_TRIGGER;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ITEM_INPUT;
	public static DeferredHolder<FlowComponentType, FlowComponentType> ITEM_OUTPUT;

	@Override
	public String getPluginId() {
		return "vanilla";
	}

	@Override
	public void registerComponents(DeferredRegister<FlowComponentType> registry) {
		/* STREAMING_CHUNK:Initializing vanilla component holders */
		INTERVAL_TRIGGER = FlowComponentBuilder
				.create("interval_trigger", IntervalTriggerComponent::new).category(NodeCategory.TRIGGER)
				.icon("textures/gui/menu_buttons/trigger_button.png").displayName("gui.sfmflow.interval_trigger")
				.codec(IntervalTriggerComponent.CODEC).build(registry);

		ITEM_INPUT = FlowComponentBuilder
				.create("item_input", uuid -> new ItemTransferComponent(uuid, true)).category(NodeCategory.INPUT)
				.icon("textures/gui/menu_buttons/input_button.png").displayName("gui.sfmflow.item_input")
				.codec(ItemTransferComponent.INPUT_CODEC).build(registry);

		ITEM_OUTPUT = FlowComponentBuilder
				.create("item_output", uuid -> new ItemTransferComponent(uuid, false)).category(NodeCategory.OUTPUT)
				.icon("textures/gui/menu_buttons/output_button.png").displayName("gui.sfmflow.item_output")
				.codec(ItemTransferComponent.OUTPUT_CODEC).build(registry);
	}
}
