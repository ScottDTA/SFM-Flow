package dta.sfmflow.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dta.sfmflow.util.Color;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Client-only graphic rendering utility for custom textured quads featuring vertical color gradients [3].
 */
@OnlyIn(Dist.CLIENT)
public class GradientBlitUtil
 {
  private GradientBlitUtil()
   {
   }

  /**
   * Safe vertex-colored blit utility that renders a vertical color gradient overlay onto a GUI texture [3].
   * Utilizes the single-L Tesselator and BufferBuilder under the position-texture-color shader configuration [3].
   *
   * @param matrix the current transformation matrix [3]
   * @param texture the texture asset ResourceLocation [3]
   * @param x the destination X coordinate [3]
   * @param y the destination Y coordinate [3]
   * @param width the destination width [3]
   * @param height the destination height [3]
   * @param u0 the starting U coordinate [3]
   * @param v0 the starting V coordinate [3]
   * @param uWidth the width of the texture slice [3]
   * @param vHeight the height of the texture slice [3]
   * @param texWidth the total width of the texture sheet [3]
   * @param texHeight the total height of the texture sheet [3]
   * @param colorMask the custom Color mask applied to the vertices (can be null for white) [3]
   */
  public static void blitWithGradient(Matrix4f matrix, ResourceLocation texture, int x, int y, int width, int height,
                                      float u0, float v0, int uWidth, int vHeight, int texWidth, int texHeight,
                                      @javax.annotation.Nullable Color colorMask)
   {
    // Resolve base color components [3]
    int hex = (colorMask != null) ? colorMask.getHexColor() : 0xFFFFFF;
    float r = ((hex >> 16) & 0xFF) / 255.0F;
    float g = ((hex >> 8) & 0xFF) / 255.0F;
    float b = (hex & 0xFF) / 255.0F;
 // Keep alpha completely solid so the widget stays opaque
    float a = 1.0F; 

    // Mix 40% of the registry color with 60% pure white to "soften" it naturally
    float mixedR = (r * 0.75F) + 0.25F;
    float mixedG = (g * 0.75F) + 0.25F;
    float mixedB = (b * 0.75F) + 0.25F;

    // Now apply your vertical gradient shading to the mixed color
    float topR = Math.min(1.0F, mixedR * 1.15F);
    float topG = Math.min(1.0F, mixedG * 1.15F);
    float topB = Math.min(1.0F, mixedB * 1.15F);

    float botR = Math.min(1.0F, mixedR * 0.75F);
    float botG = Math.min(1.0F, mixedG * 0.75F);
    float botB = Math.min(1.0F, mixedB * 0.75F);
    
    // Calculate normalized texture UV coordinates [3]
    float minU = u0 / (float) texWidth;
    float minV = v0 / (float) texHeight;
    float maxU = (u0 + uWidth) / (float) texWidth;
    float maxV = (v0 + vHeight) / (float) texHeight;

    // Set up RenderSystem states [3]
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, texture);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();

    // Begin Tesselator and BufferBuilder under properties specified for POSITION_TEX_COLOR [3]
    Tesselator tesselator = Tesselator.getInstance(); // 🔥 FIXED: Spelled Tesselator with single 'l' to match Mojang mappings
    BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

    // Quad Vertex 1 (Top Left)
    bufferBuilder.addVertex(matrix, (float) x, (float) y, 0.0F)
                 .setUv(minU, minV)
                 .setColor(topR, topG, topB, a);

    // Quad Vertex 2 (Bottom Left)
    bufferBuilder.addVertex(matrix, (float) x, (float) (y + height), 0.0F)
                 .setUv(minU, maxV)
                 .setColor(botR, botG, botB, a);

    // Quad Vertex 3 (Bottom Right)
    bufferBuilder.addVertex(matrix, (float) (x + width), (float) (y + height), 0.0F)
                 .setUv(maxU, maxV)
                 .setColor(botR, botG, botB, a);

    // Quad Vertex 4 (Top Right)
    bufferBuilder.addVertex(matrix, (float) (x + width), (float) y, 0.0F)
                 .setUv(maxU, minV)
                 .setColor(topR, topG, topB, a);

    // Draw the completed vertex buffer [3]
    BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
   }
  
  /**
   * Helper utility that returns the RGB bottom gradient color components [3].
   *
   * @param colorMask the Color mask to evaluate [3]
   * @return a float array containing the Red, Green, and Blue factors [3]
   */
  public static float[] getBottomColorComponents(@javax.annotation.Nullable Color colorMask)
   {
    int hex = (colorMask != null) ? colorMask.getHexColor() : 0xFFFFFF;
    float r = ((hex >> 16) & 0xFF) / 255.0F;
    float g = ((hex >> 8) & 0xFF) / 255.0F;
    float b = (hex & 0xFF) / 255.0F;
    
    // Simulate the 0.5 Alpha blend against the solid white (1.0F) background pass
    float mixedR = (r * 0.75F) + 0.25F;
    float mixedG = (g * 0.75F) + 0.25F;
    float mixedB = (b * 0.75F) + 0.25F;
    
    return new float[]{
            Math.min(1.0F, mixedR * 0.75F),
            Math.min(1.0F, mixedG * 0.75F),
            Math.min(1.0F, mixedB * 0.75F)
    };
   }
 }