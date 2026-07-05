package dta.sfmflow.api.logging;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import java.util.Locale;

/**
 * Utility logger class offering garbage-free diagnostics for pathfinding sweeps
 * [3].
 */
public final class FlowLogger {
	private FlowLogger() {
	}

	/**
	 * Logs pathfinding diagnostics under active debug configurations [3]. Resolves
	 * standard printf-style string formatting safely before SLF4J handoff [3].
	 *
	 * @param message format string [3]
	 * @param args    message parameters [3]
	 */
	public static void pathfinder(String message, Object... args) {
		try {
			if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
				// Safely resolve formatting parameters first [3]
				String formatted = String.format(Locale.ROOT, message, args);
				SFMFlow.LOGGER.info("[SFM-Flow] [Pathfinder] {}", formatted);
			}
		} catch (IllegalStateException e) {
			// Fallback for pre-config registration triggers during bootstrapping [3]
			String formatted = String.format(Locale.ROOT, message, args);
			SFMFlow.LOGGER.info("[SFM-Flow] [Pathfinder] {}", formatted);
		}
	}

	/**
	 * Logs flowchart planning, ticking, and UI execution diagnostics under active
	 * debug configurations [3].
	 *
	 * @param message format string [3]
	 * @param args    message parameters [3]
	 */
	public static void execution(String message, Object... args) {
		try {
			if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
				String formatted = String.format(Locale.ROOT, message, args);
				SFMFlow.LOGGER.info("[SFM-Flow] [Execution] {}", formatted);
			}
		} catch (IllegalStateException e) {
			String formatted = String.format(Locale.ROOT, message, args);
			SFMFlow.LOGGER.info("[SFM-Flow] [Execution] {}", formatted);
		}
	}
}