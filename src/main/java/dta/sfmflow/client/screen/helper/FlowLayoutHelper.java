package dta.sfmflow.client.screen.helper;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.FlowWidgetContainer;
import dta.sfmflow.flowcomponents.ForEachComponent;
import dta.sfmflow.util.NodeCount;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * Consolidates canvas coordinate calculations, widget searches, and parent-child
 * hierarchy traversal algorithms to eliminate duplicate code.
 */
@OnlyIn(Dist.CLIENT)
public final class FlowLayoutHelper {

    private FlowLayoutHelper() {}

    /**
     * Finds a widget container matching a target component ID.
     *
     * @param screen active screen manager
     * @param id     the UUID of the component being searched
     * @return the associated FlowWidgetContainer, or null if unregistered
     */
    @Nullable
    public static FlowWidgetContainer findContainer(ManagerScreen screen, UUID id) {
        if (screen == null || id == null) {
            return null;
        }
        for (var renderable : screen.getRenderables()) {
            if (renderable instanceof FlowWidgetContainer container) {
                if (container.getComponent().getId().equals(id)) {
                    return container;
                }
            }
        }
        return null;
    }

    /**
     * Resolves the horizontal pin output offset for rendering or hit testing.
     */
    public static int getOutputOffset(AbstractFlowComponent component, int index) {
        if (!component.hasOutputNodes() || index < 0 || index >= component.getNumOutputs() || component.getNumOutputs() <= 0) {
            return 29;
        }
        if (component.isRightOutput(index)) {
            return 61; // Right edge offset
        }

        // Dynamically calculate spacing using only the bottom outputs
        int totalBottomOutputs = component.getNumOutputs() - (component.hasRightOutput() ? 1 : 0);
        if (totalBottomOutputs <= 0) {
            return 29;
        }

        NodeCount nodeCount = NodeCount.getForCount(totalBottomOutputs);
        int[] spacing = nodeCount.getOffsets(false);
        if (index < spacing.length) {
            return spacing[index];
        }
        return 29;
    }

    public static int getOutputYOffset(AbstractFlowComponent component, int index) {
        if (component.isRightOutput(index)) {
            return 7; // Vertically centered on the right edge of the card
        }
        return 20; // Default bottom edge y-offset
    }
    
    /**
     * Resolves the horizontal pin input offset for rendering or hit testing.
     */
    public static int getInputOffset(AbstractFlowComponent component, int index) {
        // Added component.getNumInputs() <= 0 safety guard
        if (!component.hasInputNodes() || index < 0 || index >= component.getNumInputs() || component.getNumInputs() <= 0) {
            return 29;
        }
        NodeCount nodeCount = NodeCount.getForCount(component.getNumInputs());
        int[] spacing = nodeCount.getOffsets(false);
        if (index < spacing.length) {
            return spacing[index];
        }
        return 29;
    }

    /**
     * Recursively verifies if a parent element is an ancestor of a target element.
     *
     * @param parent the potential ancestor widget
     * @param target the target event listener
     * @return true if target is nested inside parent's children tree, false otherwise
     */
    public static boolean isAncestorOf(GuiEventListener parent, GuiEventListener target) {
        if (parent == target) {
            return true;
        }
        if (parent instanceof AbstractFlowWidget flowWidget) {
            for (GuiEventListener child : flowWidget.children()) {
                if (isAncestorOf(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }
    
	/**
	 * Verifies if a given widget belongs to the active, top-most interaction panel.
	 * Used to block background tooltips from firing when modals or overlays are open.
	 */
	public static boolean isWidgetActiveAndOnTop(GuiEventListener widget, ManagerScreen screen) {
		if (screen.getActiveModalPopup() != null && screen.getActiveModalPopup().visible) {
			return isAncestorOf(screen.getActiveModalPopup(), widget);
		}
		if (screen.getActiveSettingsOverlay() != null && screen.getActiveSettingsOverlay().visible) {
			return isAncestorOf(screen.getActiveSettingsOverlay(), widget);
		}
		return true;
	}
}