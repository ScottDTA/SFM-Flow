package dta.sfmflow.client.screen.widgets;

import java.util.ArrayList;
import java.util.List;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.serverbound.CreateNodePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A sliding hover submenu displaying creator nodes mapped to a hovered sidebar category [3].
 * Features 9-slice background scaling, centered and dynamically scaled FlowWidgetText headers [3],
 * OpenGL scissor masking, customizable width/column structures, and button hover states [3].
 */
@OnlyIn(Dist.CLIENT)
public class CategoryHoverSubmenu extends AbstractFlowWidget
 {
  private static final ResourceLocation SUBMENU_BG = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/submenu_bg.png");
  
  private final NodeCategory category;
  private final ManagerScreen parentScreen;
  private final List<FlowComponentType> matchedTypes = new ArrayList<>();
  private double scrollOffset = 0;
  private final int rows;
  private final int numColumns;
  private final int gridWidth;
  private final int scrollbarWidth;
  private final int startXOffset;
  
  /**
   * Submenu title label, powered by FlowWidgetText for scaled ellipsis support [3].
   */
  private final FlowWidgetText titleWidget;

  public CategoryHoverSubmenu(NodeCategory category, int x, int y, ManagerScreen parentScreen)
   {
    super(x, y, 68, 24, Component.literal(category.name()));
    this.category = category;
    this.parentScreen = parentScreen;

    // Filter registry for enabled nodes matching the target category
    for (FlowComponentType type : FlowComponentType.REGISTRY)
     {
      INodeClientProperties props = FlowClientRegistry.getProperties(type);
      if (props != null && props.getCategory() == category && props.isEnabled().get())
       {
        this.matchedTypes.add(type);
       }
     }

    // Dynamically calculate grid columns and scroll boundaries
    this.numColumns = Math.max(1, Math.min(4, matchedTypes.size()));
    this.gridWidth = numColumns * 14;
    this.scrollbarWidth = (matchedTypes.size() > 16) ? 8 : 0;
    
    // Scale width dynamically (min 48px to prevent extreme title squeezing)
    this.width = Math.max(48, 6 + gridWidth + scrollbarWidth + 6);

    // Center grid columns horizontally if they don't occupy full 4-column space
    int availableGridSpace = this.width - 12 - scrollbarWidth;
    this.startXOffset = 6 + (availableGridSpace - gridWidth) / 2;

    this.rows = (matchedTypes.size() + 3) / 4;
    int gridHeight = Math.min(56, rows * 14);
    this.height = 6 + 12 + gridHeight + 6;

    // Ensure layout coordinates clamp perfectly inside vertical viewport boundaries to prevent cutoff
    int maxY = parentScreen.height - this.height - 4;
    this.setY(Math.max(4, Math.min(y, maxY)));

    // Set up dynamically scaled FlowWidgetText for the category name header (positioned 4px below top edge)
    Component titleText = Component.translatable("gui.sfmflow.menu." + category.name().toLowerCase(java.util.Locale.ROOT));
    int titleWidth = parentScreen.getFont().width(titleText);
    float titleScale = 0.8F;
    
    // Width set to exactly 8px less of total width (leaving 4px side paddings)
    int availableTitleWidth = this.width - 8;
    int titleX = this.getX() + 4;
    int titleY = this.getY() + 4;

    if (titleWidth * titleScale > availableTitleWidth)
     {
      titleScale = (float) availableTitleWidth / titleWidth;
      titleScale = Math.max(0.4F, titleScale); // Clamp to a minimum of 40% scale
     }

    this.titleWidget = new FlowWidgetText(parentScreen.getFont(),
                                          titleX,
                                          titleY,
                                          availableTitleWidth,
                                          10,
                                          titleText,
                                          titleScale,
                                          true // Centered!
                                         );    
   }

  /**
   * Retrieves the logical category this hover submenu displays [3].
   *
   * @return the active NodeCategory enum value [3]
   */
  public NodeCategory getCategory()
   {
    return category;
   }

  /**
   * Checks if the mouse coordinates hover over the submenu area [3].
   */
  public boolean isHoveredOrFocused(double mouseX, double mouseY)
   {
    return mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
   }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
   {
    if (this.visible && isHoveredOrFocused(mouseX, mouseY) && maxScroll() > 0)
     {
      this.scrollOffset = Mth.clamp(this.scrollOffset - scrollY * 6, 0, maxScroll());
      return true;
     }
    return false;
   }

  private int maxScroll()
   {
    return Math.max(0, rows * 14 - 56);
   }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button)
   {
    if (this.visible && isHoveredOrFocused(mouseX, mouseY))
     {
      // Identify cell click matches inside the customized column layout
      int gridX = getX() + startXOffset;
      int gridY = getY() + 18;

      if (mouseX >= gridX && mouseX < gridX + gridWidth && mouseY >= gridY && mouseY < gridY + 56)
       {
        int col = (int) ((mouseX - gridX) / 14);
        int row = (int) ((mouseY - gridY + scrollOffset) / 14);
        int index = row * numColumns + col;

        if (index >= 0 && index < matchedTypes.size())
         {
          FlowComponentType clickedType = matchedTypes.get(index);
          ResourceLocation typeLoc = FlowComponentType.REGISTRY.getKey(clickedType);
          if (typeLoc != null)
           {
            // Dynamic registry resolution supports dynamic third-party registrations [3]
            PacketDistributor.sendToServer(new CreateNodePacket(
                parentScreen.getMenu().getManagerBlockEntity().getBlockPos(),
                typeLoc
            ));
           }
          return true;
         }
       }
     }
    return false;
   }

  @Override
  protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
   {
    // 1. Draw 9-slice background texture (submenu_bg.png)
    render9SliceBackground(guiGraphics);

    // 2. Render centered category title using FlowWidgetText
    this.titleWidget.render(guiGraphics, mouseX, mouseY, partialTick);

    // 3. Grid cell render sequence
    int gridX = getX() + startXOffset;
    int gridY = getY() + 18;

    // Enable hardware OpenGL scissor mask to clamp grid items cleanly within their 56x56 view bounding box
    guiGraphics.enableScissor(getX() + 6, gridY, getX() + 6 + 56, gridY + 56);

    for (int i = 0; i < matchedTypes.size(); i++)
     {
      int col = i % numColumns;
      int row = i / numColumns;

      int itemX = gridX + col * 14;
      int itemY = gridY + row * 14 - (int) scrollOffset;

      FlowComponentType type = matchedTypes.get(i);
      INodeClientProperties props = FlowClientRegistry.getProperties(type);
      if (props != null)
       {
        // Button textures files are a 14x14 stacked texture. V-offset is 14 if hovered, 0 otherwise [3].
        int vOffset = 0;
        if (mouseX >= itemX && mouseX < itemX + 14 && mouseY >= itemY && mouseY < itemY + 14)
         {
          vOffset = 14;
         }
        guiGraphics.blit(props.getIconTexture(), itemX, itemY, 0, vOffset, 14, 14, 14, 28);
       }
     }

    guiGraphics.disableScissor();

    // 4. Scrollbar rendering
    if (maxScroll() > 0)
     {
      int scrollbarX = getX() + width - 6 - 2;
      int scrollbarY = getY() + 18;
      int scrollbarHeight = 56;

      // Track background path
      guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x40000000);

      // Scrollbar thumb size and vertical scroll position calculations
      int thumbHeight = (int) ((56.0 / (rows * 14.0)) * 56.0);
      thumbHeight = Math.max(10, Math.min(56, thumbHeight));
      int thumbY = scrollbarY + (int) ((scrollOffset / maxScroll()) * (56 - thumbHeight));

      guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFF8B8B8B);
     }

    // 5. Grid hovering cell tooltip rendering pass
    if (mouseX >= gridX && mouseX < gridX + gridWidth && mouseY >= gridY && mouseY < gridY + 56)
     {
      int col = (mouseX - gridX) / 14;
      int row = (int) ((mouseY - gridY + scrollOffset) / 14);
      int index = row * numColumns + col;

      if (index >= 0 && index < matchedTypes.size())
       {
        FlowComponentType hoveredType = matchedTypes.get(index);
        INodeClientProperties props = FlowClientRegistry.getProperties(hoveredType);
        if (props != null)
         {
          guiGraphics.renderTooltip(parentScreen.getFont(), props.getDisplayName(), mouseX, mouseY);
         }
       }
     }
   }

  /**
   * Performs the 9-slice background matrix stretching calculations on the submenu background textures [3].
   */
  private void render9SliceBackground(GuiGraphics guiGraphics)
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