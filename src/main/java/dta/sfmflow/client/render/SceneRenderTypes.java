package dta.sfmflow.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Houses custom RenderTypes used specifically for GUI-based 3D scene layers [3].
 */
@OnlyIn(Dist.CLIENT)
public final class SceneRenderTypes {

	private SceneRenderTypes() {}

	/**
	 * Ignores depth writing and testing so target face markers sit cleanly on top of geometry [3].
	 */
	public static final RenderType MARKER_RENDER_TYPE = RenderType.create("sfm_marker",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
			RenderType.CompositeState.builder()
					.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(new RenderStateShard.CullStateShard(false))
					.setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519))
					.createCompositeState(false));

	/**
	 * Maps a block state's standard chunk RenderType to its corresponding entity-based,
	 * lightmap-independent GUI RenderType to guarantee consistent high-brightness rendering [3].
	 */
	public static RenderType getGuiRenderType(RenderType chunkType) {
		if (chunkType == RenderType.translucent()) {
			return RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
		} else if (chunkType == RenderType.cutout() || chunkType == RenderType.cutoutMipped()) {
			return RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS);
		} else {
			return RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS);
		}
	}
}