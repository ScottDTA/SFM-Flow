package dta.sfmflow.api.client.widget;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import dta.sfmflow.client.GradientBlitUtil;
import dta.sfmflow.util.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Public client-only API widget adapter representing a wrapper for vanilla
 * Minecraft UI controls. Synchronizes visible/active states, delegates
 * coordinate modifications, and routes input focus, clicks, drags, releases,
 * and keys directly to the wrapped vanilla component.
 *
 * @param <T> the type of vanilla AbstractWidget being wrapped
 */
@OnlyIn(Dist.CLIENT)
public class ApiWidgetAdapter<T extends AbstractWidget> extends AbstractFlowWidget {
	private final T vanillaWidget;
	private Supplier<Color> colorSupplier = () -> null;

	public ApiWidgetAdapter(T vanillaWidget) {
		super(vanillaWidget.getX(), vanillaWidget.getY(), vanillaWidget.getWidth(), vanillaWidget.getHeight(),
				vanillaWidget.getMessage());
		this.vanillaWidget = vanillaWidget;
	}

	public ApiWidgetAdapter(T vanillaWidget, Supplier<Color> colorSupplier) {
		this(vanillaWidget);
		this.colorSupplier = colorSupplier;
	}

	public T getVanillaWidget() {
		return vanillaWidget;
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		this.vanillaWidget.setX(x);
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		this.vanillaWidget.setY(y);
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.vanillaWidget.visible = this.visible;
		this.vanillaWidget.active = this.active;

		Color mask = colorSupplier.get();

		if (mask != null && mask != Color.BLACK) {
			float[] colors = GradientBlitUtil.getBottomColorComponents(mask);
			GuiGraphics tintedGraphics = new TintedGuiGraphics(guiGraphics, colors[0], colors[1], colors[2]);
			this.vanillaWidget.render(tintedGraphics, mouseX, mouseY, partialTick);
			guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		} else {
			this.vanillaWidget.render(guiGraphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.visible && this.active && this.vanillaWidget.isMouseOver(mouseX, mouseY)) {
			boolean clicked = this.vanillaWidget.mouseClicked(mouseX, mouseY, button);
			if (clicked) {
				this.vanillaWidget.setFocused(true);
			}
			return clicked;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.visible && this.active) {
			return this.vanillaWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.visible && this.active) {
			return this.vanillaWidget.mouseReleased(mouseX, mouseY, button);
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.visible && this.active) {
			return this.vanillaWidget.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (this.visible && this.active) {
			return this.vanillaWidget.charTyped(codePoint, modifiers);
		}
		return false;
	}

	/**
	 * Defensive bounds verification added to prevent scroll conflicts between
	 * stacked components.
	 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.visible && this.active && this.vanillaWidget.isMouseOver(mouseX, mouseY)) {
			return this.vanillaWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		return false;
	}
	
	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		this.vanillaWidget.setFocused(focused); 
	}

	@Override
	public boolean isFocused() {
		return this.vanillaWidget.isFocused();
	}

	@OnlyIn(Dist.CLIENT)
	private static class TintedGuiGraphics extends GuiGraphics {
		private final GuiGraphics original;
		private final float red;
		private final float green;
		private final float blue;

		private float lastR;
		private float lastG;
		private float lastB;
		private float lastA;

		public TintedGuiGraphics(GuiGraphics original, float red, float green, float blue) {
			super(Minecraft.getInstance(), original.bufferSource());
			this.original = original;
			this.red = red;
			this.green = green;
			this.blue = blue;

			this.pose().last().pose().set(original.pose().last().pose());
			this.pose().last().normal().set(original.pose().last().normal());

			this.lastR = this.red;
			this.lastG = this.green;
			this.lastB = this.blue;
			this.lastA = 1.0F;

			this.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		}

		@Override
		public void setColor(float r, float g, float b, float a) {
			this.lastR = r * this.red;
			this.lastG = g * this.green;
			this.lastB = b * this.blue;
			this.lastA = a;
			super.setColor(this.lastR, this.lastG, this.lastB, this.lastA);
		}

		private void suspendTint() {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}

		private void resumeTint() {
			RenderSystem.setShaderColor(this.lastR, this.lastG, this.lastB, this.lastA);
		}

		@Override
		public int drawString(Font font, @Nullable String text, int x, int y,
				int color) {
			suspendTint();
			int res = super.drawString(font, text, x, y, color);
			resumeTint();
			return res;
		}

		@Override
		public int drawString(Font font, @Nullable String text, int x, int y,
				int color, boolean dropShadow) {
			suspendTint();
			int res = super.drawString(font, text, x, y, color, dropShadow);
			resumeTint();
			return res;
		}

		@Override
		public int drawString(Font font, @Nullable String text, float x,
				float y, int color, boolean dropShadow) {
			suspendTint();
			int res = super.drawString(font, text, x, y, color, dropShadow);
			resumeTint();
			return res;
		}

		@Override
		public int drawString(Font font, FormattedCharSequence text, int x,
				int y, int color) {
			suspendTint();
			int res = super.drawString(font, text, x, y, color);
			resumeTint();
			return res;
		}

		@Override
		public int drawString(Font font, FormattedCharSequence text, int x,
				int y, int color, boolean dropShadow) {
			suspendTint();
			int res = super.drawString(font, text, x, y, color, dropShadow);
			resumeTint();
			return res;
		}

		@Override
		public int drawString(Font font, FormattedCharSequence text,
				float x, float y, int color, boolean dropShadow) {
			suspendTint();
			int res = super.drawString(font, text, x, y, color, dropShadow);
			resumeTint();
			return res;
		}

		@Override
		public int drawString(Font font, Component text, int x, int y, int color) {
			suspendTint();
			int res = super.drawString(font, text, x, y, color);
			resumeTint();
			return res;
		}

		@Override
		public int drawString(Font font, Component text, int x, int y, int color,
				boolean dropShadow) {
			suspendTint();
			int res = super.drawString(font, text, x, y, color, dropShadow);
			resumeTint();
			return res;
		}
	}
}