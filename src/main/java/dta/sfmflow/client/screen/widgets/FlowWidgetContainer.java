package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.util.NodeCount;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Parent node element widget wrapping standard compact cards and terminal pins.
 * Coordinates dragging bounds, layout translations, and delegates input
 * bounds checks cleanly to visible children.
 */
@OnlyIn(Dist.CLIENT)
public class FlowWidgetContainer extends AbstractFlowWidget {
	private final ManagerScreen parentScreen;
	private final AbstractFlowComponent component;
	private double preciseX;
	private double preciseY;

	public FlowWidgetContainer(ManagerScreen parentScreen, AbstractFlowComponent component, int x, int y) {
		super(x + component.getX(), y + component.getY(), component.getVisualWidth(), component.getVisualHeight(),
				Component.literal("FlowWidgetContainer"));
		this.parentScreen = parentScreen;
		this.component = component;
		this.preciseX = this.getX();
		this.preciseY = this.getY();

		this.children.add(new FlowWidgetBase(this, this.getX(), this.getY(), 64, 20, component.getName()));

		refreshNodes();
	}

	private void refreshNodes() {
		// Only calculate spacings and spawn pins if the component actually has active ports
		if (component.hasInputNodes() && component.getNumInputs() > 0) {
			int baseY = this.getY() - 6;
			NodeCount nodeCount = NodeCount.getForCount(component.getNumInputs());
			int[] spacing = nodeCount.getOffsets(false);
			for (int i = 0; i < component.getNumInputs(); i++) {
				int xOffset = spacing[i];
				int finalX = getX() + xOffset;
				FlowWidgetInputNode pin = new FlowWidgetInputNode(i, this, finalX, baseY);
				Component pinTooltip = component.getInputNodeTooltip(i);
				if (pinTooltip != null) {
					pin.setCustomTooltip(Tooltip.create(pinTooltip));
				}
				this.children.add(pin);
			}
		}

		// Only calculate spacings and spawn pins if the component actually has active ports
		if (component.hasOutputNodes() && component.getNumOutputs() > 0) {
			int baseY = this.getY() + 20;
			NodeCount nodeCount = NodeCount.getForCount(component.getNumOutputs());
			int[] spacing = nodeCount.getOffsets(false);
			for (int i = 0; i < component.getNumOutputs(); i++) {
				int xOffset = spacing[i];
				int finalX = getX() + xOffset;
				FlowWidgetOutputNode pin = new FlowWidgetOutputNode(i, this, finalX, baseY);
				Component pinTooltip = component.getOutputNodeTooltip(i);
				if (pinTooltip != null) {
					pin.setCustomTooltip(Tooltip.create(pinTooltip));
				}
				this.children.add(pin);
			}
		}
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				// Fix: Evaluate individual child visibilities instead of overwriting them
				if (widget.visible) {
					widget.active = this.active;
					widget.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}
		}
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.pose().pushPose();
		// Reverts back to standard sorted Z-level indexing
		guiGraphics.pose().translate(0.0F, 0.0F, this.getZ() * 1.0F);

		int renderMouseX = mouseX;
		int renderMouseY = mouseY;
		if (!this.getParent().isElementHoverable(this)) {
			renderMouseX = -9999;
			renderMouseY = -9999;
		}

		super.renderWidget(guiGraphics, renderMouseX, renderMouseY, partialTick);
		guiGraphics.flush();

		guiGraphics.pose().popPose();
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		if (!this.visible) {
			return false;
		}
		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget && widget.visible && widget.isMouseOver(mouseX, mouseY)) {
				return true;
			}
		}
		return false;
	}

	protected ManagerScreen getParent() {
		return parentScreen;
	}

	public void moveBy(double dragX, double dragY) {
		this.preciseX += dragX;
		this.preciseY += dragY;
		int newX = (int) Math.round(this.preciseX);
		int newY = (int) Math.round(this.preciseY);
		int intChangeX = newX - this.getX();
		int intChangeY = newY - this.getY();
		this.setX(newX);
		this.setY(newY);

		for (GuiEventListener child : this.children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.setX(widget.getX() + intChangeX);
				widget.setY(widget.getY() + intChangeY);
			}
		}
	}

	public int getZ() {
		return component.getZ();
	}

	public AbstractFlowComponent getComponent() {
		return component;
	}

	@Override
	public void setX(int x) {
		int dif = this.getX() - x;
		super.setX(x);
		updateChildrenXPositions(dif);
	}

	@Override
	public void setY(int y) {
		int dif = this.getY() - y;
		super.setY(y);
		updateChildrenYPositions(dif);
	}
}