package dta.sfmflow.client.screen.helper;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.FlowWidgetContainer;
import dta.sfmflow.util.NodeCount;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * Consolidates canvas coordinate calculations, widget searches, and parent-child
 * hierarchy traversal algorithms to eliminate duplicate code [3].
 */
@OnlyIn(Dist.CLIENT)
public final class FlowLayoutHelper {

    private FlowLayoutHelper() {}

    /**
     * Finds a widget container matching a target component ID [3].
     *
     * @param screen active screen manager [3]
     * @param id     the UUID of the component being searched [3]
     * @return the associated FlowWidgetContainer, or null if unregistered [3]
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
     * Resolves the horizontal pin output offset for rendering or hit testing [3].
     */
    public static int getOutputOffset(AbstractFlowComponent component, int index) {
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

    /**
     * Resolves the horizontal pin input offset for rendering or hit testing [3].
     */
    public static int getInputOffset(AbstractFlowComponent component, int index) {
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

    /**
     * Recursively verifies if a parent element is an ancestor of a target element [3].
     *
     * @param parent the potential ancestor widget [3]
     * @param target the target event listener [3]
     * @return true if target is nested inside parent's children tree, false otherwise [3]
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
}