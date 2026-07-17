package dta.sfmflow;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration class managing performance limits, network scan thresholds,
 * and time-budget constraints [3].
 */
public class ServerConfig {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.IntValue MAX_CONNECTED_INVENTORIES;
	public static final ModConfigSpec.IntValue MAX_CABLE_LENGTH;
	public static final ModConfigSpec.IntValue MAX_COMPONENT_AMOUNT;
	public static final ModConfigSpec.IntValue MIN_INTERVAL_TICKS;
	public static final ModConfigSpec.IntValue MAX_INTERVAL_TICKS;
	public static final ModConfigSpec.IntValue MAX_NESTED_GROUP_DEPTH;
	public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING;
	public static final ModConfigSpec.IntValue NETWORK_SCAN_COOLDOWN;
	public static final ModConfigSpec.IntValue MAX_CHAINED_SPLITTERS;

	/**
	 * Maximum time budget (in microseconds) per Manager Block per tick to execute transfer tasks [3].
	 * Protects server performance against execution tick spikes [3].
	 */
	public static final ModConfigSpec.IntValue MAX_EXECUTION_BUDGET_US;

	public static final ModConfigSpec SPEC;

	static {
		BUILDER.comment("Configure scanning thresholds and workspace restrictions to protect server performance.")
				.push("limits");

		MAX_CONNECTED_INVENTORIES = BUILDER.comment("The absolute maximum number of chest/container inventories that a Manager block is allowed to connect to via cable scanning.")
				.translation("sfmflow.configuration.maxConnectedInventories")
				.defineInRange("maxConnectedInventories", 1023, 1, 4096);

		MAX_CABLE_LENGTH = BUILDER.comment("The maximum distance (in cable blocks) that the network scanner is allowed to search from the Manager block.")
				.translation("sfmflow.configuration.maxCableLength")
				.defineInRange("maxCableLength", 128, 16, 512);

		MAX_COMPONENT_AMOUNT = BUILDER.comment("The maximum number of flow control components/nodes that can be placed on a single Manager layout canvas.")
				.translation("sfmflow.configuration.maxComponentAmount")
				.defineInRange("maxComponentAmount", 512, 10, 2048);

		MIN_INTERVAL_TICKS = BUILDER.comment("The absolute minimum duration (in ticks) allowed for Interval Triggers.")
				.translation("sfmflow.configuration.minIntervalTicks")
				.defineInRange("minIntervalTicks", 5, 1, 100);

		MAX_INTERVAL_TICKS = BUILDER.comment("The absolute maximum duration (in ticks) allowed for Interval Triggers.")
				.translation("sfmflow.configuration.maxIntervalTicks")
				.defineInRange("maxIntervalTicks", 72000, 20, 1000000);
		
		MAX_CHAINED_SPLITTERS = BUILDER.comment("The maximum number of consecutive Splitter components that can be chained together in a single execution path to prevent server budget starvation.")
				.translation("sfmflow.configuration.maxChainedSplitters")
				.defineInRange("maxChainedSplitters", 4, 1, 16);

		MAX_NESTED_GROUP_DEPTH = BUILDER.comment("Maximum depth of nested group folders")
				.translation("sfmflow.configuration.maxNestedGroupDepth")
				.defineInRange("maxNestedGroupDepth", 4, 1, 16);

		ENABLE_DEBUG_LOGGING = BUILDER.comment("Enable verbose diagnostics for cable network pathfinding sweeps.")
				.translation("sfmflow.configuration.enableDebugLogging").define("enableDebugLogging", false);

		NETWORK_SCAN_COOLDOWN = BUILDER.comment("Minimum tick cooldown between consecutive network BFS scans.")
				.translation("sfmflow.configuration.networkScanCooldown")
				.defineInRange("networkScanCooldown", 40, 0, 1200);

		MAX_EXECUTION_BUDGET_US = BUILDER.comment("Maximum time budget (in microseconds) per Manager block per tick to execute transfers. Prevents server lag spikes.")
				.translation("sfmflow.configuration.maxExecutionBudgetUs")
				.defineInRange("maxExecutionBudgetUs", 1000, 100, 10000); // 1ms default, min 0.1ms, max 10ms

		BUILDER.pop();

		SPEC = BUILDER.build();
	}
}