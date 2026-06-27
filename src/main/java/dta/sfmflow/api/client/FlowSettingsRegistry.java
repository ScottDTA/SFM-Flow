package dta.sfmflow.api.client;

import java.util.HashMap;
import java.util.Map;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.client.widget.ISettingsWidgetProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only registry managing the registration of custom UI settings panels to flowchart node types [3].
 * Provides a clean interface for developers to link interactive configurations without dedicated server physical loading [3].
 */
@OnlyIn(Dist.CLIENT)
public class FlowSettingsRegistry
 {
  private static final Map<FlowComponentType, ISettingsWidgetProvider> REGISTRY = new HashMap<>();

  private FlowSettingsRegistry()
   {
   }

  /**
   * Registers a custom settings widget provider for a specific FlowComponentType [3].
   *
   * @param type the component type being registered [3]
   * @param provider the settings widget provider factory [3]
   */
  public static void register(FlowComponentType type, ISettingsWidgetProvider provider)
   {
    REGISTRY.put(type, provider);
   }

  /**
   * Retrieves the custom settings widget provider registered for a specific FlowComponentType [3].
   *
   * @param type the component type lookup query [3]
   * @return the assigned ISettingsWidgetProvider, or null if unregistered [3]
   */
  public static ISettingsWidgetProvider getProvider(FlowComponentType type)
   {
    return REGISTRY.get(type);
   }
 }