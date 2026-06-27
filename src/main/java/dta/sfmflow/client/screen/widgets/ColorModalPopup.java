package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.util.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Symmetrical color selection overlay displaying Minecraft's 16 base colors in an 8x2 grid [3].
 * Employs responsive border-outline highlights to indicate active and hovered options [3].
 */
@OnlyIn(Dist.CLIENT)
public class ColorModalPopup extends AbstractModalPopup
 {
  private final AbstractFlowComponent component;
  private final Color originalColor;
  private Color selectedColor;

  public ColorModalPopup(ManagerScreen parentScreen, FlowWidgetContainer targetContainer)
   {
    super(parentScreen, 110, 56, Component.literal("Node Color"));
    this.component = targetContainer.getComponent();
    this.originalColor = component.getColorMask();
    this.selectedColor = this.originalColor;
   }

  private void saveAndClose()
   {
    component.setColorMask(this.selectedColor);
    CompoundTag nbt = new CompoundTag();
    component.saveData(nbt);
    PacketDistributor.sendToServer(new SaveComponentSettings(
        parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), 
        component.getId(), 
        nbt
    ));
    close();
   }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers)
   {
    if (keyCode == 257 || keyCode == 335)
     {
      saveAndClose();
      return true;
     }
    return super.keyPressed(keyCode, scanCode, modifiers);
   }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button)
   {
    int startGridX = getX() + 15;
    int startGridY = getY() + 16;

    // Row 1 clicks (0-7)
    for (int col = 0; col < 8; col++)
     {
      int cx = startGridX + col * 10;
      int cy = startGridY;
      if (mouseX >= cx && mouseX < cx + 8 && mouseY >= cy && mouseY < cy + 8)
       {
        this.selectedColor = Color.values()[col];
        return true;
       }
     }

    // Row 2 clicks (8-15)
    for (int col = 0; col < 8; col++)
     {
      int cx = startGridX + col * 10;
      int cy = startGridY + 10;
      if (mouseX >= cx && mouseX < cx + 8 && mouseY >= cy && mouseY < cy + 8)
       {
        this.selectedColor = Color.values()[col + 8];
        return true;
       }
     }

    // Save button bounds: 48x14px [3]
    int sx = getX() + 5;
    int sy = getY() + 36;
    if (mouseX >= sx && mouseX < sx + 48 && mouseY >= sy && mouseY < sy + 14)
     {
      saveAndClose();
      return true;
     }

    // Cancel button bounds: 48x14px [3]
    int cx = getX() + 57;
    int cy = getY() + 36;
    if (mouseX >= cx && mouseX < cx + 48 && mouseY >= cy && mouseY < cy + 14)
     {
      component.setColorMask(this.originalColor); // Revert selection
      close();
      return true;
     }

    return false;
   }

  @Override
  protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
   {
    render9SliceBackground(guiGraphics);

    // Section header - Updated to dark gray for high readability on concrete gray backdrops [3]
    guiGraphics.drawString(parentScreen.getFont(), Component.literal("Node Color"), getX() + 5, getY() + 4, 0xFF404040, false);

    // Dynamic grid centering (80px wide) [3]
    int startGridX = getX() + 15;
    int startGridY = getY() + 16;

    // Row 1 colors (0-7)
    for (int i = 0; i < 8; i++)
     {
      int cx = startGridX + i * 10;
      Color color = Color.values()[i];

      // 🔥 Added responsive hover outlines: Selected -> White, Hovered -> Gold, Default -> Black [3]
      boolean isHovered = mouseX >= cx && mouseX < cx + 8 && mouseY >= startGridY && mouseY < startGridY + 8;
      int activeBorderColor = 0xFF000000;
      if (this.selectedColor == color)
       {
        activeBorderColor = 0xFFFFFFFF; // Selected
       }
      else if (isHovered)
       {
        activeBorderColor = 0xFFD4AF37; // Hovered
       }

      guiGraphics.fill(cx, startGridY, cx + 8, startGridY + 8, color.getHexColor() | 0xFF000000);
      guiGraphics.renderOutline(cx, startGridY, 8, 8, activeBorderColor);
     }

    // Row 2 colors (8-15)
    for (int i = 0; i < 8; i++)
     {
      int cx = startGridX + i * 10;
      int cy = startGridY + 10;
      Color color = Color.values()[i + 8];

      // 🔥 Added responsive hover outlines: Selected -> White, Hovered -> Gold, Default -> Black [3]
      boolean isHovered = mouseX >= cx && mouseX < cx + 8 && mouseY >= cy && mouseY < cy + 8;
      int activeBorderColor = 0xFF000000;
      if (this.selectedColor == color)
       {
        activeBorderColor = 0xFFFFFFFF; // Selected
       }
      else if (isHovered)
       {
        activeBorderColor = 0xFFD4AF37; // Hovered
       }

      guiGraphics.fill(cx, cy, cx + 8, cy + 8, color.getHexColor() | 0xFF000000);
      guiGraphics.renderOutline(cx, cy, 8, 8, activeBorderColor);
     }

    // Render Save & Cancel buttons side-by-side [3]
    int sx = getX() + 5;
    int sy = getY() + 36;
    boolean saveHovered = mouseX >= sx && mouseX < sx + 48 && mouseY >= sy && mouseY < sy + 14;
    guiGraphics.fill(sx, sy, sx + 48, sy + 14, saveHovered ? 0xFF555555 : 0xFF222222);
    guiGraphics.renderOutline(sx, sy, 48, 14, 0xFFD4AF37);
    guiGraphics.drawCenteredString(parentScreen.getFont(), "Save", sx + 24, sy + 3, 0xFFFFFFFF);

    int cx = getX() + 57;
    int cy = getY() + 36;
    boolean cancelHovered = mouseX >= cx && mouseX < cx + 48 && mouseY >= cy && mouseY < cy + 14;
    guiGraphics.fill(cx, cy, cx + 48, cy + 14, cancelHovered ? 0xFF555555 : 0xFF222222);
    guiGraphics.renderOutline(cx, cy, 48, 14, 0xFFD4AF37);
    guiGraphics.drawCenteredString(parentScreen.getFont(), "Cancel", cx + 24, cy + 3, 0xFFFFFFFF);
   }

  @Override
  public void setX(int x)
   {
    int dif = this.getX() - x;
    super.setX(x);
    updateChildrenXPositions(dif);
   }

  @Override
  public void setY(int y)
   {
    int dif = this.getY() - y;
    super.setY(y);
    updateChildrenYPositions(dif);
   }
 }