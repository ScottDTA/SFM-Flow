package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.client.FlowOverlayRegistry;
import dta.sfmflow.client.screen.ManagerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only factory managing the instantiation of custom Settings Overlays
 * for nodes [3]. Delegates entirely to the extensible FlowOverlayRegistry [3].
 */
@OnlyIn(Dist.CLIENT)
public final class NodeSettingsOverlayFactory {
	private NodeSettingsOverlayFactory() {
	}

	/**
	 * Resolves the component type and returns its appropriate configuration overlay
	 * panel [3].
	 *
	 * @param screen    the parent manager screen context [3]
	 * @param component the logical component model instance [3]
	 * @return the configured NodeSettingsOverlay instance, or a Generic fallback
	 *         [3]
	 */
	public static NodeSettingsOverlay create(ManagerScreen screen, AbstractFlowComponent component) {
		var provider = FlowOverlayRegistry.getProvider(component.getType());
		if (provider != null) {
			return provider.create(screen, component);
		}
		return new GenericNodeSettingsOverlay(screen, component);
	}
}