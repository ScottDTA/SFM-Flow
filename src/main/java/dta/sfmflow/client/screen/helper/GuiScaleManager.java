package dta.sfmflow.client.screen.helper;

import dta.sfmflow.ClientConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Manages adaptive GUI scale overrides for the flowchart workspace [3].
 */
@OnlyIn(Dist.CLIENT)
public final class GuiScaleManager {

	private GuiScaleManager() {}

	/**
	 * Configures adaptive or forced scaling states for the workspace [3].
	 * Returns true if the scale changed and the display needs to resize [3].
	 *
	 * @param mc               Minecraft client instance [3]
	 * @param width            screen width [3]
	 * @param height           screen height [3]
	 * @param originalScale    the initial scale option value [3]
	 * @param requiredWidth    required screen width [3]
	 * @param requiredHeight   required screen height [3]
	 * @return true if scaling changes were applied [3]
	 */
	public static boolean applyOverrides(Minecraft mc, int width, int height, int originalScale, int requiredWidth, int requiredHeight) {
		int currentScale = mc.options.guiScale().get();
		int rawWidth = mc.getWindow().getWidth();
		int rawHeight = mc.getWindow().getHeight();
		boolean scaleApplied = false;

		int forcedScale = ClientConfig.FORCE_GUI_SCALE.get();
		if (forcedScale > 0) {
			int testWidth = rawWidth / forcedScale;
			int testHeight = rawHeight / forcedScale;

			if (testWidth >= requiredWidth && testHeight >= requiredHeight) {
				if (currentScale != forcedScale) {
					mc.options.guiScale().set(forcedScale);
					mc.resizeDisplay();
					return true;
				}
				scaleApplied = true;
			}
		}

		if (!scaleApplied) {
			int actualScale = (int) mc.getWindow().getGuiScale();

			if ((width < requiredWidth || height < requiredHeight) && actualScale > 1) {
				int targetScale = (currentScale == 0) ? (actualScale - 1) : (currentScale - 1);
				if (currentScale != targetScale) {
					mc.options.guiScale().set(targetScale);
					mc.resizeDisplay();
					return true;
				}
			}

			int maxPossibleScale = Math.max(1, Math.min(rawWidth / 320, rawHeight / 240));
			int maxScaleLimit = (originalScale == 0) ? maxPossibleScale : originalScale;

			if (currentScale > 0 && currentScale < maxScaleLimit) {
				int nextScale = currentScale + 1;
				int testWidth = rawWidth / nextScale;
				int testHeight = rawHeight / nextScale;

				if (testWidth >= requiredWidth && testHeight >= requiredHeight) {
					int targetScale = (nextScale == maxScaleLimit && originalScale == 0) ? 0 : nextScale;
					if (currentScale != targetScale) {
						mc.options.guiScale().set(targetScale);
						mc.resizeDisplay();
						return true;
					}
				}
			}
		}
		return false;
	}
}