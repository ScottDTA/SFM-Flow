package dta.sfmflow.api.client.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowClientPlugin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only registry managing visual plugin setups [3].
 */
@OnlyIn(Dist.CLIENT)
public final class SFMFlowClientPluginRegistry {
	private static final List<ISFMFlowClientPlugin> CLIENT_PLUGINS = new ArrayList<>();

	static {
		// Statically register the vanilla client plugin [3]
		registerClient(new VanillaSFMFlowClientPlugin());
	}

	private SFMFlowClientPluginRegistry() {}

	/**
	 * Registers a custom clientbound plugin to participate in client initialization [3].
	 */
	public static void registerClient(ISFMFlowClientPlugin plugin) {
		if (plugin != null) {
			CLIENT_PLUGINS.add(plugin);
		}
	}

	/**
	 * Runs visual layout and properties setups across all clientbound plugins [3].
	 */
	public static void initAllClientProperties() {
		for (ISFMFlowClientPlugin plugin : CLIENT_PLUGINS) {
			plugin.registerClientProperties();
		}
	}

	public static List<ISFMFlowClientPlugin> getClientPlugins() {
		return Collections.unmodifiableList(CLIENT_PLUGINS);
	}
}
