package dta.sfmflow.api.client;

import dta.sfmflow.SFMFlow;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Side-safe clientbound rendering utility for 9-sliced GUI panels [3].
 */
@OnlyIn(Dist.CLIENT)
public final class NineSliceUtil {
	private static final ResourceLocation DEFAULT_SUBMENU_BG = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/submenu_bg.png");

	private NineSliceUtil() {}

	/**
	 * Renders the default standard 22x22px submenu background utilizing 6px corners [3].
	 *
	 * @param guiGraphics the active graphics rendering canvas [3]
	 * @param x           target horizontal screen offset [3]
	 * @param y           target vertical screen offset [3]
	 * @param width       target panel width [3]
	 * @param height      target panel height [3]
	 */
	public static void drawDefault(GuiGraphics guiGraphics, int x, int y, int width, int height) {
		draw(guiGraphics, DEFAULT_SUBMENU_BG, x, y, width, height, 6, 10, 22);
	}

	/**
	 * Renders an arbitrary 9-sliced panel texture resized to dynamic dimension bounds [3].
	 *
	 * @param guiGraphics   the active drawing graphics canvas [3]
	 * @param texture       the asset ResourceLocation of the 9-slice grid [3]
	 * @param x             the target horizontal offset [3]
	 * @param y             the target vertical offset [3]
	 * @param width         the physical panel width [3]
	 * @param height        the physical panel height [3]
	 * @param cornerSize    the pixel size of the non-stretching corner tiles (e.g. 6px) [3]
	 * @param centerSize    the pixel size of the stretching center tiles (e.g. 10px) [3]
	 * @param textureSize   the total dimensions of the source texture sheet (e.g. 22px) [3]
	 */
	public static void draw(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, int cornerSize, int centerSize, int textureSize) {
		int c = cornerSize;
		int m = centerSize;
		int ts = textureSize;

		// 1. Draw Corners [3]
		guiGraphics.blit(texture, x, y, 0, 0, c, c, ts, ts); // Top-Left
		guiGraphics.blit(texture, x + width - c, y, (float)(ts - c), 0.0F, c, c, ts, ts); // Top-Right
		guiGraphics.blit(texture, x, y + height - c, 0.0F, (float)(ts - c), c, c, ts, ts); // Bottom-Left
		guiGraphics.blit(texture, x + width - c, y + height - c, (float)(ts - c), (float)(ts - c), c, c, ts, ts); // Bottom-Right

		// 2. Draw Edges [3]
		guiGraphics.blit(texture, x + c, y, width - 2 * c, c, (float) c, 0.0F, m, c, ts, ts); // Top-Edge
		guiGraphics.blit(texture, x + c, y + height - c, width - 2 * c, c, (float) c, (float)(ts - c), m, c, ts, ts); // Bottom-Edge
		guiGraphics.blit(texture, x, y + c, c, height - 2 * c, 0.0F, (float) c, c, m, ts, ts); // Left-Edge
		guiGraphics.blit(texture, x + width - c, y + c, c, height - 2 * c, (float)(ts - c), (float) c, c, m, ts, ts); // Right-Edge

		// 3. Draw Center Fill [3]
		guiGraphics.blit(texture, x + c, y + c, width - 2 * c, height - 2 * c, (float) c, (float) c, m, m, ts, ts);
	}
}