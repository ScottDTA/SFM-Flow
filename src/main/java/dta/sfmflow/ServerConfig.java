package dta.sfmflow;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration definition for the SFM-Flow mod [3].
 * Houses configurable performance limits and resource restricting bounds, 
 * allowing server-safe tuning of search depths, canvas entities, and interval triggers.
 * Automatically synchronized to the client upon multiplayer login.
 */
public class ServerConfig
 {
  private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

  /**
   * Maximum allowed connected inventories/containers found when scanning via connected cables [3].
   */
  public static final ModConfigSpec.IntValue MAX_CONNECTED_INVENTORIES;

  /**
   * Maximum cable search depth/distance limit for manager network scanning algorithms [3].
   */
  public static final ModConfigSpec.IntValue MAX_CABLE_LENGTH;

  /**
   * Maximum allowed visual flowchart components/nodes placed on the manager canvas [3].
   */
  public static final ModConfigSpec.IntValue MAX_COMPONENT_AMOUNT;

  /**
   * Minimum duration (in ticks) allowed for Interval Triggers [3].
   */
  public static final ModConfigSpec.IntValue MIN_INTERVAL_TICKS;

  /**
   * Maximum duration (in ticks) allowed for Interval Triggers [3].
   */
  public static final ModConfigSpec.IntValue MAX_INTERVAL_TICKS;

  /**
   * Maximum depth of nested group folders [3].
   */
  public static final ModConfigSpec.IntValue MAX_NESTED_GROUP_DEPTH;

  /**
   * Enable verbose diagnostics for cable network pathfinding sweeps [3].
   */
  public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING;

  /**
   * Minimum tick cooldown between consecutive network BFS scans [3].
   */
  public static final ModConfigSpec.IntValue NETWORK_SCAN_COOLDOWN;

  /**
   * The registered ModConfigSpec server-side configuration instance [3].
   */
  public static final ModConfigSpec SPEC;

  static
   {
    BUILDER.comment("Performance limits and workspace bounds")
           .translation("sfmflow.configuration.section.sfmflow.server.toml.limits")
           .push("limits");

    MAX_CONNECTED_INVENTORIES = BUILDER.comment("Max connected inventories found via scanning")
                                       .translation("sfmflow.configuration.maxConnectedInventories")
                                       .defineInRange("maxConnectedInventories", 1023, 1, 4096);

    MAX_CABLE_LENGTH = BUILDER.comment("Max search depth for cable logic")
                              .translation("sfmflow.configuration.maxCableLength")
                              .defineInRange("maxCableLength", 128, 1, 512);

    MAX_COMPONENT_AMOUNT = BUILDER.comment("Max nodes on the canvas")
                                  .translation("sfmflow.configuration.maxComponentAmount")
                                  .defineInRange("maxComponentAmount", 511, 1, 2048);

    MIN_INTERVAL_TICKS = BUILDER.comment("Minimum ticks allowed for periodic execution triggers")
                                .translation("sfmflow.configuration.minIntervalTicks")
                                .defineInRange("minIntervalTicks", 4, 1, 1200);

    MAX_INTERVAL_TICKS = BUILDER.comment("Maximum ticks allowed for periodic execution triggers")
                                .translation("sfmflow.configuration.maxIntervalTicks")
                                .defineInRange("maxIntervalTicks", 72000, 20, 1000000);

    MAX_NESTED_GROUP_DEPTH = BUILDER.comment("Maximum depth of nested group folders")
                                    .translation("sfmflow.configuration.maxNestedGroupDepth")
                                    .defineInRange("maxNestedGroupDepth", 4, 1, 16);

    // Dynamic Debug Logging Config [3]
    ENABLE_DEBUG_LOGGING = BUILDER.comment("Enable verbose diagnostics for cable network pathfinding sweeps.")
                                  .translation("sfmflow.configuration.enableDebugLogging")
                                  .define("enableDebugLogging", false);

    // Throttled scan cooldown to shield server tick performance [3]
    NETWORK_SCAN_COOLDOWN = BUILDER.comment("Minimum tick cooldown between consecutive network BFS scans.")
                                   .translation("sfmflow.configuration.networkScanCooldown")
                                   .defineInRange("networkScanCooldown", 40, 0, 1200);

    BUILDER.pop();

    SPEC = BUILDER.build();
   }
 }