package dta.sfmflow.plugin.vanilla;

import dta.sfmflow.api.client.FlowSettingsRegistry;
import dta.sfmflow.api.client.plugin.ISFMFlowClientPlugin;
import dta.sfmflow.client.screen.widgets.IntervalTriggerSettingsWidget;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Built-in vanilla client plugin configuring settings widgets side-safely [3].
 */
@OnlyIn(Dist.CLIENT)
public class VanillaSFMFlowClientPlugin implements ISFMFlowClientPlugin {
	@Override
	public String getPluginId() {
		return "vanilla";
	}

	@Override
	public void registerClientProperties() {
		/* STREAMING_CHUNK:Registering settings widgets */
		FlowSettingsRegistry.register(VanillaSFMFlowPlugin.INTERVAL_TRIGGER.get(), (container, component) -> {
			if (component instanceof IntervalTriggerComponent intervalTrigger) {
				return new IntervalTriggerSettingsWidget(container, intervalTrigger);
			}
			return null;
		});
	}
}
