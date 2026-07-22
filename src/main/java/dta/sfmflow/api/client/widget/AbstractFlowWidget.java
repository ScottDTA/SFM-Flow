package dta.sfmflow.api.client.widget;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Public API client-only base class representing a custom visual flowchart
 * element. Implements ContainerEventHandler to support vanilla recursive
 * hit checks and input forwarding.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractFlowWidget extends AbstractWidget implements ContainerEventHandler {
	protected final List<GuiEventListener> children = new ArrayList<>();
	private final WidgetTooltipHolder customTooltip = new WidgetTooltipHolder();
	private boolean showCustomTooltip = false;
	protected boolean customIsHovered = false;

	private GuiEventListener focused = null;
	private boolean isDragging = false;

	public AbstractFlowWidget(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		renderComponent(guiGraphics, mouseX, mouseY, partialTick);
		if (this.visible && showCustomTooltip) {
			this.customTooltip.refreshTooltipForNextRenderPass(this.isHovered(), this.isFocused(), this.getRectangle());
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		// Reserved for accessibility narration configurations
	}

	protected boolean actuallyHovered(int mouseX, int mouseY) {
		return mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
	}

	protected void updateChildrenXPositions(int dif) {
		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widet) {
				widet.setX(widet.getX() - dif);
			}
		}
	}

	@Override
	public void playDownSound(SoundManager handler) {
		// Default empty implementation allowing customized click sound playback
		// configurations
	}

	protected void updateChildrenYPositions(int dif) {
		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.setY(widget.getY() - dif);
			}
		}
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return children;
	}

	@Override
	public boolean isDragging() {
		return isDragging;
	}

	@Override
	public void setDragging(boolean dragging) {
		this.isDragging = dragging;
	}

	@Override
	@Nullable
	public GuiEventListener getFocused() {
		return focused;
	}

	@Override
	public void setFocused(@Nullable GuiEventListener focused) {
		if (this.focused != focused) {
			if (this.focused != null) {
				this.focused.setFocused(false); 
			}
			this.focused = focused;
			if (focused != null) {
				focused.setFocused(true); 
			}
		}
	}

	public void setCustomTooltip(@Nullable Tooltip tooltip) {
		customTooltip.set(tooltip);
	}

	@Nullable
	public Tooltip getCustomTooltip() {
		return customTooltip.get();
	}

	public void setShowCustomTooltip(boolean b) {
		this.showCustomTooltip = b;
	}

	protected abstract void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

	public void setIsHovered(boolean b) {
		this.customIsHovered = b;
	}
}