package dta.sfmflow.api.client.widget;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.widgets.FlowWidgetContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Dynamic settings widget provider that defines how a custom flow component's setting controls are instantiated on the client [3].
 */
@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface ISettingsWidgetProvider
 {
  /**
   * Creates the custom configuration UI panel widget [3].
   *
   * @param container the parent UI container widget [3]
   * @param component the logical component data model [3]
   * @return the configured settings panel widget [3]
   */
  AbstractFlowWidget createSettingsWidget(FlowWidgetContainer container, AbstractFlowComponent component);
 }