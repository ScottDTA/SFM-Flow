package dta.sfmflow.api.client;

import java.util.function.Supplier;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.widgets.FlowWidgetContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only visual and descriptive properties definition for flowchart component nodes [3].
 * Provides the visual asset paths, display naming routes, categorizations, and active checks
 * required to render submenus and creator widgets dynamically in the flowchart canvas UI [3].
 */
@OnlyIn(Dist.CLIENT)
public interface INodeClientProperties
 {
  /**
   * Retrieves the logical category this node belongs to under the flowchart API [3].
   *
   * @return the assigned NodeCategory enum value [3]
   */
  NodeCategory getCategory();

  /**
   * Retrieves the ResourceLocation pointing to the icon texture asset for this node [3].
   * <p>
   * <strong>Texture Requirements:</strong> The target texture sheet must be formatted as a 
   * vertically stacked texture sheet of 14x28 pixels [3]. The top 14x14 pixel region represents 
   * the default node button state, and the bottom 14x14 pixel region represents its hover state [3].
   * </p>
   *
   * @return the stacked icon texture ResourceLocation path [3]
   */
  ResourceLocation getIconTexture();

  /**
   * Retrieves the localized display name of this node type [3].
   *
   * @return the localized chat Component [3]
   */
  Component getDisplayName();

  /**
   * A check supplier that dictates whether this creator node is active and visible to the client [3].
   *
   * @return a Supplier returning true if enabled, false otherwise [3]
   */
  Supplier<Boolean> isEnabled();

  /**
   * Creates a dynamic settings widget to render within the expanded flowchart node panel [3].
   *
   * @param container the parent UI container widget [3]
   * @param component the logical component data model [3]
   * @return a custom settings widget, or null if this component type has no customizable settings [3]
   */
  @javax.annotation.Nullable
  default AbstractFlowWidget createSettingsWidget(FlowWidgetContainer container, AbstractFlowComponent component)
   {
    return null;
   }
 }