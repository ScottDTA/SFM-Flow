package dta.sfmflow.api.client.widget;

import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Renderable text component supporting custom matrix scale translations,
 * automated ellipsis shortening, optional horizontal alignment centering, and
 * dynamic colors [3].
 */
public class FlowWidgetText extends AbstractFlowWidget {
	private final float scale;
	private final Font font;
	private final boolean centered;
	private Supplier<Integer> colorSupplier = () -> 4210752; // Default dark charcoal gray

	public FlowWidgetText(Font fontRenderer, int x, int y, int width, int height, Component message, float scale) {
		this(fontRenderer, x, y, width, height, message, scale, false);
	}

	public FlowWidgetText(Font fontRenderer, int x, int y, int width, int height, Component message, float scale,
			boolean centered) {
		super(x, y, width, height, message);
		this.scale = scale;
		this.font = fontRenderer;
		this.centered = centered;
	}

	/**
	 * Overloaded constructor supporting dynamic color evaluation [3].
	 *
	 * @param fontRenderer  system font renderer [3]
	 * @param x             coordinate offset [3]
	 * @param y             coordinate offset [3]
	 * @param width         text box width [3]
	 * @param height        text box height [3]
	 * @param message       naming contents [3]
	 * @param scale         size multiplier [3]
	 * @param centered      centering alignment state [3]
	 * @param colorSupplier dynamic integer supplier returning hex colors [3]
	 */
	public FlowWidgetText(Font fontRenderer, int x, int y, int width, int height, Component message, float scale,
			boolean centered, Supplier<Integer> colorSupplier) {
		this(fontRenderer, x, y, width, height, message, scale, centered);
		this.colorSupplier = colorSupplier;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(getX(), getY(), 0);
		guiGraphics.pose().scale(scale, scale, 1.0F);

		int availableScaledWidth = (int) (getWidth() / scale);
		int titleWidth = font.width(getMessage());
		int textColor = this.colorSupplier.get(); // Dynamic color query [3]

		if (titleWidth <= availableScaledWidth) {
			int startX = this.centered ? (availableScaledWidth - titleWidth) / 2 : 0;
			guiGraphics.drawString(font, getMessage(), startX, 0, textColor, false);
		} else {
			int ellipsisWidth = font.width("...");
			String croppedText = font.getSplitter().plainHeadByWidth(getMessage().getString(),
					availableScaledWidth - ellipsisWidth, Style.EMPTY);
			int croppedWidth = font.width(croppedText + "...");
			int startX = this.centered ? (availableScaledWidth - croppedWidth) / 2 : 0;

			guiGraphics.drawString(font, croppedText + "...", startX, 0, textColor, false);
			setCustomTooltip(Tooltip.create(getMessage()));
		}
		guiGraphics.pose().popPose();
	}
}