package dta.sfmflow.client.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.GradientBlitUtil;
import dta.sfmflow.util.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only flowchart widget representing an input execution terminal along the top edge [3].
 * Employs standard pin textures rendered vertically flipped (upside down) [3].
 */
@OnlyIn(Dist.CLIENT)
public class FlowWidgetInputNode extends AbstractFlowWidget
 {
  private static final ResourceLocation INPUT_NODE = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/flowcomponents/output_node.png");
  private final FlowWidgetContainer container;
  private final int id;

  public FlowWidgetInputNode(int id, FlowWidgetContainer container, int x, int y)
   {
    super(x, y, 6, 6, Component.literal("FlowComponentInputNode"));
    this.id = id;
    this.container = container;
   }

  /**
   * Retrieves the relative terminal index of this pin on the flowchart component [3].
   *
   * @return the pin index [3]
   */
  public int getPinIndex() {
      return this.id;
  }

  @Override
  protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
   {
    AbstractFlowComponent comp = container.getComponent();
    Color mask = comp.getColorMask();

    if (mask != null && mask != Color.BLACK)
     {
      float[] colors = GradientBlitUtil.getBottomColorComponents(mask);
      RenderSystem.setShaderColor(colors[0], colors[1], colors[2], 1.0F);
     }
    else
     {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
     }

    int vOffset = 0;
    if (visible && active && actuallyHovered(mouseX, mouseY))
     {
      vOffset = 6;
     }

    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderTexture(0, INPUT_NODE);

    guiGraphics.blit(INPUT_NODE, 
                     getX(), 
                     getY(), 
                     6,
                     6,
                     0.0F,
                     (float) (vOffset + 6),
                     6,
                     -6,
                     6,
                     12
                    );

    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

  public FlowWidgetContainer getContainer()
   {
    return container;
   }
 }