package dta.sfmflow.api.plugin;

import dta.sfmflow.api.component.FlowComponentType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Common side-safe extension interface representing an SFM-Flow logic plugin
 * [3].
 */
public interface ISFMFlowPlugin {
	/**
	 * Unique namespace identifier for this plugin [3].
	 */
	String getPluginId();

	/**
	 * Called during the common registration phase to register custom flowchart
	 * components [3].
	 *
	 * @param registry the active DeferredRegister instance for FlowComponentType
	 *                 [3]
	 */
	void registerComponents(DeferredRegister<FlowComponentType> registry);
}
