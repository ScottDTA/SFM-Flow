package dta.sfmflow.datagen;

import dta.sfmflow.SFMFlow;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Automated localization provider managing translation data generation for
 * SFM-Flow [3]. Populates localization mappings for blocks, UI menus, creative
 * tabs, and configurations.
 */
public class ModLanguageProvider extends LanguageProvider {
	public ModLanguageProvider(PackOutput output, String locale) {
		super(output, SFMFlow.MODID, locale);
	}

	@Override
	protected void addTranslations() {
		add("block.sfmflow.advanced_cable_cluster_block", "Advanced Card Cluster");
		add("block.sfmflow.cable_block", "Network Cable");
		add("block.sfmflow.cable_cluster_block", "Standard Card Cluster");
		add("block.sfmflow.fluid_hatch_cable_block", "Fluid Extraction Hatch");
		add("block.sfmflow.hardened_cable_block", "Hardened Network Cable");
		add("block.sfmflow.item_ejector_hatch_block", "Item Ejection Hatch");
		add("block.sfmflow.item_vacuum_hatch_block", "Item Vacuum Hatch");
		add("block.sfmflow.manager_block", "Machine Inventory Manager");
		add("block.sfmflow.observer_cable_block", "Observer Cable");
		add("block.sfmflow.redstone_emitter_block", "Redstone Network Emitter");
		add("block.sfmflow.redstone_receiver_block", "Redstone Network Receiver");
		add("container.sfmflow.advanced_cable_cluster", "Advanced Card Cluster");
		add("container.sfmflow.cable_cluster", "Card Cluster");

		add("gui.sfmflow.commands", "Commands: %1$s");
		add("gui.sfmflow.error.empty_whitelist", "Active whitelist cannot be completely empty!");
		add("gui.sfmflow.error.no_active_sides", "At least one active inventory side must be selected!");
		add("gui.sfmflow.errors", "Errors: %1$s");
		add("gui.sfmflow.interval_trigger", "Interval Trigger");
		add("gui.sfmflow.item_input", "Item Input");
		add("gui.sfmflow.item_output", "Item Output");
		add("gui.sfmflow.advanced_item_filter_variable", "Advanced Item Filter");
		add("gui.sfmflow.fluid_input", "Fluid Input");
		add("gui.sfmflow.fluid_output", "Fluid Output");
		add("gui.sfmflow.energy_input", "Energy Input");
		add("gui.sfmflow.energy_output", "Energy Output");
		add("gui.sfmflow.advanced_fluid_filter_variable", "Advanced Fluid Filter");
		add("gui.sfmflow.loading", "%1$s REQUESTS LOADING...");

		add("gui.sfmflow.menu.camo", "Camouflage");
		add("gui.sfmflow.menu.command_group", "Command Group");
		add("gui.sfmflow.menu.condition", "Condition");
		add("gui.sfmflow.menu.copy", "Copy");
		add("gui.sfmflow.menu.crafter", "Crafter");
		add("gui.sfmflow.menu.delete", "Delete");
		add("gui.sfmflow.menu.flow_control", "Flow Control");
		add("gui.sfmflow.menu.foreach", "For Each");
		add("gui.sfmflow.menu.group_node", "Group Node");
		add("gui.sfmflow.menu.input", "Input");
		add("gui.sfmflow.menu.logic", "Logic");
		add("gui.sfmflow.menu.output", "Output");
		add("gui.sfmflow.menu.sign", "Sign");
		add("gui.sfmflow.menu.trigger", "Trigger");
		add("gui.sfmflow.menu.utility", "Utility");
		add("gui.sfmflow.menu.variable", "Variable");

		add("gui.sfmflow.time_unit", "Time Unit");
		add("gui.sfmflow.type_inventory", "Inventory");
		add("gui.sfmflow.type_redstone", "Redstone");

		add("item.sfmflow.test_item", "Test Item");
		add("itemGroup.sfmflow", "SFM-Flow");

		add("sfmflow.configuration.background", "Background Mask Hex");
		add("sfmflow.configuration.background.button", "Edit Background Color");
		add("sfmflow.configuration.background.tooltip",
				"The hexadecimal color string (#RRGGBB) used for background panels and borders.");

		add("sfmflow.configuration.black", "Black");
		add("sfmflow.configuration.black.button", "Black Settings");
		add("sfmflow.configuration.black.tooltip", "Custom color overrides for the black mask.");

		add("sfmflow.configuration.blue", "Blue");
		add("sfmflow.configuration.blue.button", "Blue Settings");
		add("sfmflow.configuration.blue.tooltip", "Custom color overrides for the blue mask.");

		add("sfmflow.configuration.brown", "Brown");
		add("sfmflow.configuration.brown.button", "Brown Settings");
		add("sfmflow.configuration.brown.tooltip", "Custom color overrides for the brown mask.");

		add("sfmflow.configuration.colors", "Component Color Masks");
		add("sfmflow.configuration.colors.button", "Configure Colors");
		add("sfmflow.configuration.colors.tooltip",
				"Customize the hexadecimal background and label colors of your canvas nodes.");

		add("sfmflow.configuration.cyan", "Cyan");
		add("sfmflow.configuration.cyan.button", "Cyan Settings");
		add("sfmflow.configuration.cyan.tooltip", "Custom color overrides for the cyan mask.");

		add("sfmflow.configuration.enableDebugLogging", "Enable Pathfinder Debug Logging");
		add("sfmflow.configuration.enableDebugLogging.tooltip",
				"Enables verbose diagnostic logging of physical network BFS scans inside the server console.");

		add("sfmflow.configuration.forceGuiScale", "Force GUI Scale");
		add("sfmflow.configuration.forceGuiScale.tooltip",
				"Force a specific GUI scale when opening the Machine Inventory Manager screen. Set to 0 to use standard adaptive auto-scaling.");

		add("sfmflow.configuration.gray", "Gray");
		add("sfmflow.configuration.gray.button", "Gray Settings");
		add("sfmflow.configuration.gray.tooltip", "Custom color overrides for the gray mask.");

		add("sfmflow.configuration.green", "Green");
		add("sfmflow.configuration.green.button", "Green Settings");
		add("sfmflow.configuration.green.tooltip", "Custom color overrides for the green mask.");

		add("sfmflow.configuration.light_blue", "Light Blue");
		add("sfmflow.configuration.light_blue.button", "Light Blue Settings");
		add("sfmflow.configuration.light_blue.tooltip", "Custom color overrides for the light_blue mask.");

		add("sfmflow.configuration.light_gray", "Light Gray");
		add("sfmflow.configuration.light_gray.button", "Light Gray Settings");
		add("sfmflow.configuration.light_gray.tooltip", "Custom color overrides for the light_gray mask.");

		add("sfmflow.configuration.lime", "Lime");
		add("sfmflow.configuration.lime.button", "Lime Settings");
		add("sfmflow.configuration.lime.tooltip", "Custom color overrides for the lime mask.");

		add("sfmflow.configuration.magenta", "Magenta");
		add("sfmflow.configuration.magenta.button", "Magenta Settings");
		add("sfmflow.configuration.magenta.tooltip", "Custom color overrides for the magenta mask.");

		add("sfmflow.configuration.maxCableLength", "Max Cable Search Depth");
		add("sfmflow.configuration.maxCableLength.tooltip",
				"The maximum distance (in cable blocks) that the network scanner is allowed to search from the Manager block.");

		add("sfmflow.configuration.maxComponentAmount", "Max Component Workspace Nodes");
		add("sfmflow.configuration.maxComponentAmount.tooltip",
				"The maximum number of flow control components/nodes that can be placed on a single Manager layout canvas.");

		add("sfmflow.configuration.maxConnectedInventories", "Max Connected Inventories");
		add("sfmflow.configuration.maxConnectedInventories.tooltip",
				"The absolute maximum number of chest/container inventories that a Manager block is allowed to connect to via cable scanning.");

		add("sfmflow.configuration.maxIntervalTicks", "Max Interval Ticks");
		add("sfmflow.configuration.maxIntervalTicks.tooltip",
				"The absolute maximum duration (in ticks) allowed for Interval Triggers.");

		add("sfmflow.configuration.maxNestedGroupDepth", "Max Nested Group Depth");
		add("sfmflow.configuration.maxNestedGroupDepth.tooltip",
				"The maximum allowable folder hierarchy nesting depth inside the manager.");

		add("sfmflow.configuration.minIntervalTicks", "Min Interval Ticks");
		add("sfmflow.configuration.minIntervalTicks.tooltip",
				"The absolute minimum duration (in ticks) allowed for Interval Triggers.");

		add("sfmflow.configuration.networkScanCooldown", "Network Scan Cooldown (Ticks)");
		add("sfmflow.configuration.networkScanCooldown.tooltip",
				"The minimum tick duration/cooldown required between consecutive physical cable network scans.");

		add("sfmflow.configuration.orange", "Orange");
		add("sfmflow.configuration.orange.button", "Orange Settings");
		add("sfmflow.configuration.orange.tooltip", "Custom color overrides for the orange mask.");

		add("sfmflow.configuration.pink", "Pink");
		add("sfmflow.configuration.pink.button", "Pink Settings");
		add("sfmflow.configuration.pink.tooltip", "Custom color overrides for the pink mask.");

		add("sfmflow.configuration.purple", "Purple");
		add("sfmflow.configuration.purple.button", "Purple Settings");
		add("sfmflow.configuration.purple.tooltip", "Custom color overrides for the purple mask.");

		add("sfmflow.configuration.red", "Red");
		add("sfmflow.configuration.red.button", "Red Settings");
		add("sfmflow.configuration.red.tooltip", "Custom color overrides for the red mask.");

		add("sfmflow.configuration.section.sfmflow.client.toml", "SFM-Flow Client Configurations");
		add("sfmflow.configuration.section.sfmflow.client.toml.title", "SFM-Flow Client Configurations");
		add("sfmflow.configuration.section.sfmflow.server.toml", "SFM-Flow Server Configs");
		add("sfmflow.configuration.section.sfmflow.server.toml.limits", "Performance Limits");
		add("sfmflow.configuration.section.sfmflow.server.toml.limits.button", "Performance Limit Options");
		add("sfmflow.configuration.section.sfmflow.server.toml.limits.tooltip",
				"Configure scanning thresholds and workspace restrictions to protect server performance.");
		add("sfmflow.configuration.section.sfmflow.server.toml.title", "SFM-Flow Server Configs");
		add("sfmflow.configuration.general", "General Client Settings");
		add("sfmflow.configuration.general.button", "Configure General Settings");
		add("sfmflow.configuration.general.tooltip", 
				"Configure core client UI rendering and scaling behaviors.");
		
		add("sfmflow.configuration.limits", "Performance Limits");
		add("sfmflow.configuration.limits.button", "Configure Limits");
		add("sfmflow.configuration.limits.tooltip", 
				"Configure scanning thresholds and workspace restrictions to protect server performance.");

		// Execution Budget Option [3]
		add("sfmflow.configuration.maxExecutionBudgetUs", "Max Execution Budget (Microseconds)");
		add("sfmflow.configuration.maxExecutionBudgetUs.tooltip", 
				"The maximum time budget (in microseconds) per Manager block per tick to execute transfer tasks.");
		

		add("sfmflow.configuration.text", "Label Text Hex");
		add("sfmflow.configuration.text.button", "Edit Text Color");
		add("sfmflow.configuration.text.tooltip",
				"The hexadecimal color string (#RRGGBB) used for text labels and node headers.");

		add("sfmflow.configuration.title", "SFM-Flow Client Configurations");

		add("sfmflow.configuration.white", "White");
		add("sfmflow.configuration.white.button", "White Settings");
		add("sfmflow.configuration.white.tooltip", "Custom color overrides for the white mask.");

		add("sfmflow.configuration.yellow", "Yellow");
		add("sfmflow.configuration.yellow.button", "Yellow Settings");
		add("sfmflow.configuration.yellow.tooltip", "Custom color overrides for the yellow mask.");

		// Slot Layout entries [3]
		add("gui.sfmflow.error.slot_not_accessible", "Slot not accessible on selected side");

		// Warnings [3]
		add("gui.sfmflow.warnings", "Warnings: %1$s");
		add("gui.sfmflow.warning.empty_filter_variable", "Active filter variable cannot be empty!");
	}
}