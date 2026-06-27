package dta.sfmflow.api.client;

import java.util.HashMap;
import java.util.Map;
import dta.sfmflow.api.component.FlowComponentType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Public client-only registry managing the visual and descriptive mappings for flowchart nodes [3].
 * Maps registered component types to their custom UI presentation properties [3].
 */
@OnlyIn(Dist.CLIENT)
public class FlowClientRegistry
 {
  private static final Map<FlowComponentType, INodeClientProperties> REGISTRY = new HashMap<>();

  private FlowClientRegistry()
   {
   }

  /**
   * Registers custom client presentation properties for a specific FlowComponentType [3].
   *
   * @param type the component type being registered [3]
   * @param properties the display and category settings for the component [3]
   */
  public static void register(FlowComponentType type, INodeClientProperties properties)
   {
    REGISTRY.put(type, properties);
   }

  /**
   * Retrieves the custom client presentation properties registered for a specific FlowComponentType [3].
   *
   * @param type the component type lookup query [3]
   * @return the assigned INodeClientProperties, or null if unregistered [3]
   */
  public static INodeClientProperties getProperties(FlowComponentType type)
   {
    return REGISTRY.get(type);
   }
 }