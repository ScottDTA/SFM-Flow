package dta.sfmflow.datagen;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.ModBlocks;
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
		// Creative Mode Tab Translation
		add("itemGroup.sfmflow", "SFM-Flow");

		// Block Translations
		add(ModBlocks.MANAGER_BLOCK.get(), "Machine Inventory Manager");
		add(ModBlocks.CABLE_BLOCK.get(), "Network Cable");
		add(ModBlocks.HARDENED_CABLE_BLOCK.get(), "Hardened Network Cable");
		add(ModBlocks.REDSTONE_EMITTER_BLOCK.get(), "Redstone Network Emitter");
		add(ModBlocks.REDSTONE_RECEIVER_BLOCK.get(), "Redstone Network Receiver");
		add(ModBlocks.OBSERVER_CABLE_BLOCK.get(), "Observer Cable");
		add(ModBlocks.ITEM_EJECTOR_HATCH_BLOCK.get(), "Item Ejection Hatch");
		add(ModBlocks.ITEM_VACUUM_HATCH_BLOCK.get(), "Item Vacuum Hatch");
		add(ModBlocks.FLUID_HATCH_CABLE_BLOCK.get(), "Fluid Extraction Hatch");
		add(ModBlocks.CABLE_CLUSTER_BLOCK.get(), "Standard Card Cluster");
		add(ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get(), "Advanced Card Cluster");

		add("container.sfmflow.cable_cluster", "Card Cluster");
		add("container.sfmflow.advanced_cable_cluster", "Advanced Card Cluster");

		// Sidebar Category & Menu Button Translations
		add("gui.sfmflow.menu.trigger", "Trigger");
		add("gui.sfmflow.menu.input", "Input");
		add("gui.sfmflow.menu.output", "Output");
		add("gui.sfmflow.menu.condition", "Condition");
		add("gui.sfmflow.menu.flow_control", "Flow Control");
		add("gui.sfmflow.menu.variable", "Variable");
		add("gui.sfmflow.menu.foreach", "For Each");
		add("gui.sfmflow.menu.command_group", "Command Group");
		add("gui.sfmflow.menu.group_node", "Group Node");
		add("gui.sfmflow.menu.camo", "Camouflage");
		add("gui.sfmflow.menu.sign", "Sign");
		add("gui.sfmflow.menu.crafter", "Crafter");
		add("gui.sfmflow.menu.copy", "Copy");
		add("gui.sfmflow.menu.delete", "Delete");
		add("gui.sfmflow.menu.logic", "Logic");
		add("gui.sfmflow.menu.utility", "Utility");

		// Interval Trigger Configuration UI Labels
		add("gui.sfmflow.time_unit", "Time Unit");
		add("gui.sfmflow.interval_trigger", "Interval Trigger");
		add("gui.sfmflow.commands", "Commands: %1$s");
		add("gui.sfmflow.loading", "%1$s REQUESTS LOADING...");

		// Integration & Type Labels
		add("gui.sfmflow.type_inventory", "Inventory");

		// Item Translations
		add("item.sfmflow.test_item", "Test Item");

		// Milestone 1.3 New Localization Entries [3]
		add("gui.sfmflow.item_input", "Item Input");
		add("gui.sfmflow.item_output", "Item Output");
		add("sfmflow.configuration.maxNestedGroupDepth", "Max Nested Group Depth");
		add("sfmflow.configuration.maxNestedGroupDepth.tooltip",
				"The maximum allowable folder hierarchy nesting depth inside the manager.");

		// Server-side limits and performance configuration translations
		add("sfmflow.configuration.section.sfmflow.server.toml", "SFM-Flow Server Configs");
		add("sfmflow.configuration.section.sfmflow.server.toml.limits", "Performance Limits");
		add("sfmflow.configuration.section.sfmflow.server.toml.limits.tooltip",
				"Configure scanning thresholds and workspace restrictions to protect server performance.");
		add("sfmflow.configuration.maxConnectedInventories", "Max Connected Inventories");
		add("sfmflow.configuration.maxConnectedInventories.tooltip",
				"The absolute maximum number of chest/container inventories that a Manager block is allowed to connect to via cable scanning.");
		add("sfmflow.configuration.maxCableLength", "Max Cable Search Depth");
		add("sfmflow.configuration.maxCableLength.tooltip",
				"The maximum distance (in cable blocks) that the network scanner is allowed to search from the Manager block.");
		add("sfmflow.configuration.maxComponentAmount", "Max Component Workspace Nodes");
		add("sfmflow.configuration.maxComponentAmount.tooltip",
				"The maximum number of flow control components/nodes that can be placed on a single Manager layout canvas.");
		add("sfmflow.configuration.minIntervalTicks", "Min Interval Ticks");
		add("sfmflow.configuration.minIntervalTicks.tooltip",
				"The absolute minimum duration (in ticks) allowed for Interval Triggers.");
		add("sfmflow.configuration.maxIntervalTicks", "Max Interval Ticks");
		add("sfmflow.configuration.maxIntervalTicks.tooltip",
				"The absolute maximum duration (in ticks) allowed for Interval Triggers.");

		// Under Redstone/Capabilities translations inside ModLanguageProvider.java
		add("gui.sfmflow.type_redstone", "Redstone");

		// --- Dynamic Client Configuration Translations [3] ---
		add("sfmflow.configuration.section.sfmflow.client.toml", "SFM-Flow Client Configurations");
		add("sfmflow.configuration.section.sfmflow.client.toml.title", "SFM-Flow Client Configurations");
		add("sfmflow.configuration.title", "SFM-Flow Client Configurations");

		add("sfmflow.configuration.colors", "Component Color Masks");
		add("sfmflow.configuration.colors.tooltip",
				"Customize the hexadecimal background and label colors of your canvas nodes.");
		add("sfmflow.configuration.colors.button", "Configure Colors");

		add("sfmflow.configuration.background", "Background Mask Hex");
		add("sfmflow.configuration.background.tooltip",
				"The hexadecimal color string (#RRGGBB) used for background panels and borders.");
		add("sfmflow.configuration.background.button", "Edit Background Color");

		add("sfmflow.configuration.text", "Label Text Hex");
		add("sfmflow.configuration.text.tooltip",
				"The hexadecimal color string (#RRGGBB) used for text labels and node headers.");
		add("sfmflow.configuration.text.button", "Edit Text Color");

		for (dta.sfmflow.util.Color color : dta.sfmflow.util.Color.values()) {
			String name = color.getSerializedName();
			String capitalized = name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1);

			add("sfmflow.configuration." + name, capitalized);
			add("sfmflow.configuration." + name + ".tooltip", "Custom color overrides for the " + name + " mask.");
			add("sfmflow.configuration." + name + ".button", capitalized + " Settings");
		}

		// --- Milestone 1.4 Config translation keys [3] ---
		add("sfmflow.configuration.networkScanCooldown", "Network Scan Cooldown (Ticks)");
		add("sfmflow.configuration.networkScanCooldown.tooltip",
				"The minimum tick duration/cooldown required between consecutive physical cable network scans.");
		add("sfmflow.configuration.enableDebugLogging", "Enable Pathfinder Debug Logging");
		add("sfmflow.configuration.enableDebugLogging.tooltip",
				"Enables verbose diagnostic logging of physical network BFS scans inside the server console.");
		add("sfmflow.configuration.section.sfmflow.server.toml.title", "SFM-Flow Server Configs");
		add("sfmflow.configuration.section.sfmflow.server.toml.limits.button", "Performance Limit Options");
		add("gui.sfmflow.error.empty_whitelist", "Active whitelist cannot be completely empty!");

		// --- Milestone 1.8 Client-Side Override keys [3] ---
		add("sfmflow.configuration.forceGuiScale", "Force GUI Scale");
		add("sfmflow.configuration.forceGuiScale.tooltip",
				"Force a specific GUI scale when opening the Machine Inventory Manager screen. Set to 0 to use standard adaptive auto-scaling.");

		// --- Symmetrical Variables Drawers and Error translation keys [3] ---
		add("gui.sfmflow.errors", "Errors: %1$s");
	}
}