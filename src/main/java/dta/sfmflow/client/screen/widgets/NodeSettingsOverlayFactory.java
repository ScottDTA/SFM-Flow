package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only factory managing the instantiation of custom Settings Overlays
 * for nodes [3].
 */
@OnlyIn(Dist.CLIENT)
public final class NodeSettingsOverlayFactory {
	private NodeSettingsOverlayFactory() {
	}

	/**
	 * Resolves the component type and returns its appropriate configuration overlay
	 * panel [3].
	 *
	 * @param screen    parent screen context [3]
	 * @param component the targeted flow component [3]
	 * @return NodeSettingsOverlay subclass container [3]
	 */
	public static NodeSettingsOverlay create(ManagerScreen screen, AbstractFlowComponent component) {
		if (component instanceof IntervalTriggerComponent trigger) {
			return new IntervalTriggerSettingsOverlay(screen, trigger);
		}
		return new GenericNodeSettingsOverlay(screen, component);
	}
}