package dta.sfmflow.client.render;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.FlowWidgetContainer;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.util.NodeCount;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client-only graphic rendering utility class that draws smooth, orthogonal
 * cubic Bezier connection lines (S-curves) between component node input and
 * output pins [3]. Configured to lock all connection wires to a flat sub-card
 * Z-depth of 2.0F, ensuring wires never overlap or obstruct readable card
 * bodies [3].
 */
@OnlyIn(Dist.CLIENT)
public final class VectorWireRenderer {

	private VectorWireRenderer() {
	}

	/**
	 * Loops through the screen flowchart's active wires, resolves node targets,
	 * computes Bezier geometries, blends hexadecimal gradient colors, and draws
	 * dense anti-aliased connection lines [3].
	 *
	 * @param guiGraphics the game rendering canvas [3]
	 * @param screen      the active manager screen interface [3]
	 * @param mouseX      current cursor horizontal offset [3]
	 * @param mouseY      current cursor vertical offset [3]
	 * @param partialTick partial frame ticker [3]
	 */
	public static void renderWires(GuiGraphics guiGraphics, ManagerScreen screen, int mouseX, int mouseY,
			float partialTick) {
		var manager = screen.getMenu().getManagerBlockEntity();
		if (manager == null) {
			return;
		}

		var connections = manager.getFlowConnections();
		if (connections != null && !connections.isEmpty()) {
			for (FlowComponentConnections conn : connections) {
				if (conn == null) {
					continue;
				}

				FlowWidgetContainer srcContainer = findContainer(screen, conn.getSourceComponentId());
				FlowWidgetContainer tgtContainer = findContainer(screen, conn.getTargetComponentId());

				if (srcContainer == null || tgtContainer == null) {
					continue;
				}

				AbstractFlowComponent src = srcContainer.getComponent();
				AbstractFlowComponent tgt = tgtContainer.getComponent();

				int srcPinX = srcContainer.getX() + getOutputOffset(src, conn.getOutputNodeIndex()) + 3;
				int srcPinY = srcContainer.getY() + 23;

				int tgtPinX = tgtContainer.getX() + getInputOffset(tgt, conn.getInputNodeIndex()) + 3;
				int tgtPinY = tgtContainer.getY() - 3;

				int srcColor = src.getColorMask().getHexColor();
				int tgtColor = tgt.getColorMask().getHexColor();

				int r0 = (srcColor >> 16) & 0xFF;
				int g0 = (srcColor >> 8) & 0xFF;
				int b0 = srcColor & 0xFF;

				int r3 = (tgtColor >> 16) & 0xFF;
				int g3 = (tgtColor >> 8) & 0xFF;
				int b3 = tgtColor & 0xFF;

				float dx = (float) (tgtPinX - srcPinX);
				float dy = (float) (tgtPinY - srcPinY);
				float distance = (float) Math.sqrt(dx * dx + dy * dy);

				int steps = Math.max(64, Math.min(400, (int) (distance * 1.5F)));

				float deltaY = Math.max(12.0F, Math.abs(tgtPinY - srcPinY) / 2.0F);

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(0.0F, 0.0F, 10.0F);

				for (int i = 0; i <= steps; i++) {
					float t = (float) i / (float) steps;
					float mt = 1.0F - t;
					float mt2 = mt * mt;
					float mt3 = mt2 * mt;
					float t2 = t * t;
					float t3 = t2 * t;

					float x = mt3 * srcPinX + 3.0F * mt2 * t * srcPinX + 3.0F * mt * t2 * tgtPinX + t3 * tgtPinX;
					float y = mt3 * srcPinY + 3.0F * mt2 * t * (srcPinY + deltaY) + 3.0F * mt * t2 * (tgtPinY - deltaY)
							+ t3 * tgtPinY;

					int r = (int) (mt * r0 + t * r3);
					int g = (int) (mt * g0 + t * g3);
					int b = (int) (mt * b0 + t * b3);
					int color = 0xFF000000 | (r << 16) | (g << 8) | b;

					int drawX = (int) Math.round(x);
					int drawY = (int) Math.round(y);

					guiGraphics.fill(drawX - 1, drawY - 1, drawX + 1, drawY + 1, color);
				}

				guiGraphics.pose().popPose();
			}
		}

		// Render interactive drag-wire under mouse gestures
		var mouseHandler = screen.getMouseHandler();
		if (mouseHandler.isWiring()) {
			var srcNode = mouseHandler.getActiveWiringSource();
			AbstractFlowComponent src = srcNode.getContainer().getComponent();
			int srcPinX = srcNode.getX() + 3;
			int srcPinY = srcNode.getY() + 3;

			int tgtPinX = (int) Math.round(mouseHandler.getWiringCurrentMouseX());
			int tgtPinY = (int) Math.round(mouseHandler.getWiringCurrentMouseY());

			int srcColor = src.getColorMask().getHexColor();
			int r0 = (srcColor >> 16) & 0xFF;
			int g0 = (srcColor >> 8) & 0xFF;
			int b0 = srcColor & 0xFF;

			float dx = (float) (tgtPinX - srcPinX);
			float dy = (float) (tgtPinY - srcPinY);
			float distance = (float) Math.sqrt(dx * dx + dy * dy);

			int steps = Math.max(64, Math.min(400, (int) (distance * 1.5F)));

			float deltaY = Math.max(12.0F, Math.abs(tgtPinY - srcPinY) / 2.0F);

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, 10.0F);

			for (int i = 0; i <= steps; i++) {
				float t = (float) i / (float) steps;
				float mt = 1.0F - t;
				float mt2 = mt * mt;
				float mt3 = mt2 * mt;
				float t2 = t * t;
				float t3 = t2 * t;

				float x = mt3 * srcPinX + 3.0F * mt2 * t * srcPinX + 3.0F * mt * t2 * tgtPinX + t3 * tgtPinX;
				float y = mt3 * srcPinY + 3.0F * mt2 * t * (srcPinY + deltaY) + 3.0F * mt * t2 * (tgtPinY - deltaY)
						+ t3 * tgtPinY;

				int r = (int) (mt * r0 + t * 0xD4);
				int g = (int) (mt * g0 + t * 0xAF);
				int b = (int) (mt * b0 + t * 0x37);
				int color = 0xFA000000 | (r << 16) | (g << 8) | b;

				int drawX = (int) Math.round(x);
				int drawY = (int) Math.round(y);

				guiGraphics.fill(drawX - 1, drawY - 1, drawX + 1, drawY + 1, color);
			}

			guiGraphics.pose().popPose();
		}
	}

	@Nullable
	private static FlowWidgetContainer findContainer(ManagerScreen screen, UUID id) {
		for (var renderable : screen.getRenderables()) {
			if (renderable instanceof FlowWidgetContainer container) {
				if (container.getComponent().getId().equals(id)) {
					return container;
				}
			}
		}
		return null;
	}

	private static int getOutputOffset(AbstractFlowComponent component, int index) {
		if (!component.hasOutputNodes() || index < 0 || index >= component.getNumOutputs()) {
			return 29;
		}
		NodeCount nodeCount = NodeCount.getForCount(component.getNumOutputs());
		int[] spacing = nodeCount.getOffsets(false);
		if (index < spacing.length) {
			return spacing[index];
		}
		return 29;
	}

	private static int getInputOffset(AbstractFlowComponent component, int index) {
		if (!component.hasInputNodes() || index < 0 || index >= component.getNumInputs()) {
			return 29;
		}
		NodeCount nodeCount = NodeCount.getForCount(component.getNumInputs());
		int[] spacing = nodeCount.getOffsets(false);
		if (index < spacing.length) {
			return spacing[index];
		}
		return 29;
	}
}