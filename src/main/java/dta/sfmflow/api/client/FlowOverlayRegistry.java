package dta.sfmflow.api.client;

import java.util.HashMap;
import java.util.Map;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.NodeSettingsOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Side-safe clientbound registry managing the mapping of custom modal settings
 * overlay panels to flowchart node types [3].
 */
@OnlyIn(Dist.CLIENT)
public final class FlowOverlayRegistry {
	private static final Map<FlowComponentType, INodeOverlayProvider> REGISTRY = new HashMap<>();

	private FlowOverlayRegistry() {
	}

	/**
	 * Registers a custom settings overlay provider for a specific FlowComponentType
	 * [3].
	 *
	 * @param type     the flowchart component type [3]
	 * @param provider the overlay factory provider [3]
	 */
	public static void register(FlowComponentType type, INodeOverlayProvider provider) {
		if (type != null && provider != null) {
			REGISTRY.put(type, provider);
		}
	}

	/**
	 * Retrieves the custom settings overlay provider registered for a specific
	 * component type [3].
	 *
	 * @param type the component type lookup query [3]
	 * @return the assigned INodeOverlayProvider, or null if unregistered [3]
	 */
	public static INodeOverlayProvider getProvider(FlowComponentType type) {
		return REGISTRY.get(type);
	}

	/**
	 * Functional interface defining the instantiation of a custom modal
	 * configuration overlay [3].
	 */
	@FunctionalInterface
	public interface INodeOverlayProvider {
		/**
		 * Instantiates the custom modal settings overlay [3].
		 *
		 * @param screen    the parent manager screen context [3]
		 * @param component the logical component model instance [3]
		 * @return the configured NodeSettingsOverlay instance [3]
		 */
		NodeSettingsOverlay create(ManagerScreen screen, AbstractFlowComponent component);
	}
}