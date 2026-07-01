package dta.sfmflow.api.client.plugin;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only extension interface to manage visual assets, property bindings,
 * and setting widget associations without dedicated server classloading risks [3].
 */
@OnlyIn(Dist.CLIENT)
public interface ISFMFlowClientPlugin {
	/**
	 * Unique namespace identifier matching the parent ISFMFlowPlugin [3].
	 */
	String getPluginId();

	/**
	 * Called during client setup phase to register client properties and UI providers [3].
	 */
	void registerClientProperties();
}
