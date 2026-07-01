package dta.sfmflow.api.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Common side-safe manager holding registered extensions and triggering setup runs [3].
 */
public final class SFMFlowPluginRegistry {
	private static final List<ISFMFlowPlugin> PLUGINS = new ArrayList<>();

	static {
		// Statically register the core vanilla plugin to ensure early registration [3]
		register(new VanillaSFMFlowPlugin());
	}

	private SFMFlowPluginRegistry() {}

	/**
	 * Registers a custom plugin to participate in the mod lifecycle [3].
	 */
	public static void register(ISFMFlowPlugin plugin) {
		if (plugin != null) {
			PLUGINS.add(plugin);
		}
	}

	/**
	 * Walks through all registered plugins, invoking their component registration tasks [3].
	 */
	public static void registerAllComponents(DeferredRegister<FlowComponentType> registry) {
		for (ISFMFlowPlugin plugin : PLUGINS) {
			plugin.registerComponents(registry);
		}
	}

	public static List<ISFMFlowPlugin> getPlugins() {
		return Collections.unmodifiableList(PLUGINS);
	}
}
