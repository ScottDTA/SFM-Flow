package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Base modal window popup that centers itself dynamically and intercepts all workspace inputs [3].
 * Employs 9-sliced background panels for visual consistency [3].
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractModalPopup extends AbstractFlowWidget
 {
  protected final ManagerScreen parentScreen;
  protected static final ResourceLocation SUBMENU_BG = ResourceLocation.fromNamespaceAndPath(dta.sfmflow.SFMFlow.MODID, "textures/gui/submenu_bg.png");

  public AbstractModalPopup(ManagerScreen parentScreen, int width, int height, Component title)
   {
    // Dynamically center the modal relative to current game window dimensions [3]
    super((parentScreen.width - width) / 2, (parentScreen.height - height) / 2, width, height, title);
    this.parentScreen = parentScreen;
   }

  /**
   * Safely dismisses the modal and returns active focus to the main canvas [3].
   */
  public void close()
   {
    this.parentScreen.setActiveModalPopup(null);
   }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers)
   {
    if (keyCode == 256) // GLFW_KEY_ESCAPE
     {
      close(); // Cancel and dismiss modal cleanly on ESC [3]
      return true;
     }
    return super.keyPressed(keyCode, scanCode, modifiers);
   }

  /**
   * Performs the 9-slice stretching calculations on the submenu background textures [3].
   *
   * @param guiGraphics the drawing graphics pipeline context [3]
   */
  protected void render9SliceBackground(GuiGraphics guiGraphics)
   {
    int c = 6;  // Corner border thickness in pixels
    int m = 10; // Mid-section stretch dimensions
    int x = getX();
    int y = getY();

    // Corner segments (6x6 px)
    guiGraphics.blit(SUBMENU_BG, x, y, 0, 0, c, c, 22, 22);
    guiGraphics.blit(SUBMENU_BG, x + width - c, y, 16, 0, c, c, 22, 22);
    guiGraphics.blit(SUBMENU_BG, x, y + height - c, 0, 16, c, c, 22, 22);
    guiGraphics.blit(SUBMENU_BG, x + width - c, y + height - c, 16, 16, c, c, 22, 22);

    // Border segments stretched
    guiGraphics.blit(SUBMENU_BG, x + c, y, width - 2 * c, c, (float) c, 0.0F, m, c, 22, 22);
    guiGraphics.blit(SUBMENU_BG, x + c, y + height - c, width - 2 * c, c, (float) c, 16.0F, m, c, 22, 22);
    guiGraphics.blit(SUBMENU_BG, x, y + c, c, height - 2 * c, 0.0F, (float) c, c, m, 22, 22);
    guiGraphics.blit(SUBMENU_BG, x + width - c, y + c, c, height - 2 * c, 16.0F, (float) c, c, m, 22, 22);

    // Stretched central segments
    guiGraphics.blit(SUBMENU_BG, x + c, y + c, width - 2 * c, height - 2 * c, (float) c, (float) c, m, m, 22, 22);
   }
 }