package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Visual sidebar button representing a specific flowchart node category [3].
 * Positioned along the manager console sidebar, hovering triggers the dynamic creation
 * of its associated category hovering submenu [3].
 */
@OnlyIn(Dist.CLIENT)
public class CategoryButton extends AbstractFlowWidget
 {
  private final NodeCategory category;
  private final ResourceLocation texture;
  private final ManagerScreen parentScreen;

  public CategoryButton(NodeCategory category, int x, int y, ManagerScreen parentScreen)
   {
    super(x, y, 14, 14, Component.literal(category.name()));
    this.category = category;
    this.parentScreen = parentScreen;
    this.texture = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, 
        "textures/gui/menu_buttons/" + getTextureName(category) + "_button.png");
    // Explicitly clear default hover tooltips as per the technical layout specification
    this.setTooltip(null);
   }

  private static String getTextureName(NodeCategory category)
   {
    return switch (category)
     {
      case TRIGGER -> "trigger";
      case INPUT -> "input";
      case OUTPUT -> "output";
      case LOGIC -> "condition";
      case VARIABLE -> "variable";
      case UTILITY -> "command_group";
     };
   }

  /**
   * Retrieves the logical category this sidebar button represents [3].
   *
   * @return the associated NodeCategory enum value [3]
   */
  public NodeCategory getCategory()
   {
    return category;
   }

  @Override
  protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
   {
    int vOffset = 0;
    
    // Check if the submenu overlay corresponding to this category button is currently opened and active
    boolean isSubmenuOpenForMe = this.parentScreen.getActiveSubmenu() != null 
        && this.parentScreen.getActiveSubmenu().getCategory() == this.category;

    if (this.visible && this.active && (actuallyHovered(mouseX, mouseY) || isSubmenuOpenForMe))
     {
      vOffset = 14; 
     }    
    guiGraphics.blit(texture, getX(), getY(), 0, vOffset, 14, 14, 14, 28);   
   }
 }