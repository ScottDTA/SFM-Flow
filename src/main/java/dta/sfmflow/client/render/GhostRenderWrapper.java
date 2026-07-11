package dta.sfmflow.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.Locale;

/**
 * Handles color blending and alpha configurations for neighboring ghost blocks [3].
 */
@OnlyIn(Dist.CLIENT)
public final class GhostRenderWrapper {

	private GhostRenderWrapper() {}

	@OnlyIn(Dist.CLIENT)
	public static class GhostBufferSource implements MultiBufferSource {
		private final MultiBufferSource delegate;
		private final float alpha;

		public GhostBufferSource(MultiBufferSource delegate, float alpha) {
			this.delegate = delegate;
			this.alpha = alpha;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			// Symmetrically delegate to the entity-based translucent shader to remain completely immune to world light levels [3]
			return new GhostVertexConsumer(delegate.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS)), alpha);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class GhostEntityBufferSource implements MultiBufferSource {
		private final MultiBufferSource delegate;
		private final float alpha;

		public GhostEntityBufferSource(MultiBufferSource delegate, float alpha) {
			this.delegate = delegate;
			this.alpha = alpha;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			RenderType actualType = renderType;
			String str = renderType.toString();
			ResourceLocation texture = null;
			int startIdx = str.indexOf("texture[");
			if (startIdx != -1) {
				int endIdx = str.indexOf("]", startIdx);
				if (endIdx != -1) {
					String sub = str.substring(startIdx + 8, endIdx);
					if (sub.startsWith("Optional[")) {
						sub = sub.substring(9, sub.length() - 1);
					}
					if (!sub.startsWith("Optional.empty")) {
						texture = ResourceLocation.tryParse(sub);
					}
				}
			}

			if (texture != null) {
				String name = renderType.toString().toLowerCase(Locale.ROOT);
				if (name.contains("entity_solid") || name.contains("entity_cutout")
						|| name.contains("entity_cutout_no_mips")) {
					actualType = RenderType.entityTranslucentCull(texture);
				}
			}
			return new GhostVertexConsumer(delegate.getBuffer(actualType), alpha);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class GhostVertexConsumer implements VertexConsumer {
		private final VertexConsumer delegate;
		private final float ghostAlpha;

		public GhostVertexConsumer(VertexConsumer delegate, float ghostAlpha) {
			this.delegate = delegate;
			this.ghostAlpha = ghostAlpha;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			delegate.setColor(r, g, b, (int) (a * ghostAlpha));
			return this;
		}

		@Override
		public VertexConsumer setColor(int color) {
			int a = (color >> 24) & 0xFF;
			int r = (color >> 16) & 0xFF;
			int g = (color >> 8) & 0xFF;
			int b = color & 0xFF;
			int newA = (int) (a * ghostAlpha);
			int newColor = (newA << 24) | (r << 16) | (g << 8) | b;
			delegate.setColor(newColor);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(float r, float g, float b, float a) {
			delegate.setColor(r, g, b, a * ghostAlpha);
			return this;
		}

		@Override
		public VertexConsumer setLight(int light) {
			delegate.setLight(light);
			return this;
		}

		@Override
		public VertexConsumer setOverlay(int overlay) {
			delegate.setOverlay(overlay);
			return this;
		}
	}
}