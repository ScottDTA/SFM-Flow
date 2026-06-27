package dta.sfmflow.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.widgets.*;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import dta.sfmflow.networking.packets.serverbound.CanvasActionPacket;
import dta.sfmflow.networking.packets.serverbound.CreateConnectionPacket;
import dta.sfmflow.networking.packets.serverbound.RemoveConnectionPacket;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Encapsulates, coordinates, and manages all mouse interaction states, coordinate lookups,
 * and element hit-testing recursive routines for the {@link ManagerScreen} [3].
 * Safely separates input-related physics from pure screen rendering loops [3].
 */
@OnlyIn(Dist.CLIENT)
public class ManagerMouseHandler {
    
    private final ManagerScreen screen;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private FlowWidgetContainer activeDragged = null;
    private UUID doubleClickCandidateId = null;
    private long lastClickTime = 0L;
    private GuiEventListener topHoveredElement = null;

    private FlowWidgetOutputNode activeWiringSource = null;
    private double wiringCurrentMouseX = 0;
    private double wiringCurrentMouseY = 0;

    /**
     * Constructs a new ManagerMouseHandler associated with the parent screen [3].
     *
     * @param screen parent screen context [3]
     */
    public ManagerMouseHandler(ManagerScreen screen) {
        this.screen = screen;
    }

    public void startWiringDrag(FlowWidgetOutputNode source) {
        this.activeWiringSource = source;
        this.wiringCurrentMouseX = source.getX() + 3;
        this.wiringCurrentMouseY = source.getY() + 3;
    }

    public boolean isWiring() {
        return this.activeWiringSource != null;
    }

    public FlowWidgetOutputNode getActiveWiringSource() {
        return this.activeWiringSource;
    }

    public double getWiringCurrentMouseX() {
        return this.wiringCurrentMouseX;
    }

    public double getWiringCurrentMouseY() {
        return this.wiringCurrentMouseY;
    }

    public void clearWiring() {
        this.activeWiringSource = null;
    }

    public void updateTopHoveredElement(double mouseX, double mouseY) {
        this.topHoveredElement = getTopElementAt(mouseX, mouseY);
    }

    public GuiEventListener getTopHoveredElement() {
        return this.topHoveredElement;
    }

    public boolean isDragging() {
        return this.activeDragged != null;
    }

    public FlowWidgetContainer getActiveDragged() {
        return this.activeDragged;
    }

    public void clearDragging() {
        this.activeDragged = null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (screen.getActiveSettingsOverlay() != null) {
            screen.getActiveSettingsOverlay().mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (screen.getActiveModalPopup() != null) {
            screen.getActiveModalPopup().mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (screen.getOpenedDropdown() != null) {
            if (screen.getOpenedDropdown().isMouseOver(mouseX, mouseY)) {
                if (screen.getOpenedDropdown().mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            } else {
                screen.setOpenedDropdown(null);
            }
        }

        if (screen.getActiveSubmenu() != null && screen.getActiveSubmenu().isMouseOver(mouseX, mouseY)) {
            if (screen.getActiveSubmenu().mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Proximity wire hit testing pass on Shift-Left-Click [3]
        if (button == 0 && checkWireShiftClick(mouseX, mouseY)) {
            return true;
        }

        FlowWidgetBase clickedBase = getTopBaseAt(mouseX, mouseY);

        if (button == 0 && clickedBase != null) {
            long currentTime = net.minecraft.Util.getMillis();
            UUID clickedId = clickedBase.getContainer().getComponent().getId();

            if (this.doubleClickCandidateId != null && clickedId.equals(this.doubleClickCandidateId) && (currentTime - this.lastClickTime < 500L)) {
                FlowWidgetContainer clickedContainer = clickedBase.getContainer();
                NodeSettingsOverlay settingsOverlay = NodeSettingsOverlayFactory.create(screen, clickedContainer.getComponent());
                screen.setActiveSettingsOverlay(settingsOverlay);
                this.doubleClickCandidateId = null;
                this.activeDragged = null;
                return true;
            }
            this.doubleClickCandidateId = clickedId;
            this.lastClickTime = currentTime;
        } else if (button == 0) {
            this.doubleClickCandidateId = null;
        }

        GuiEventListener topElement = getTopHoveredElement();
        if (topElement != null) {
            FlowWidgetContainer clickedContainer = getContainerOf(topElement);

            if (clickedContainer != null) {
                if (button == 1) { 
                    screen.setOpenedDropdown(new DropdownMenuWidget(clickedContainer, (int) mouseX, (int) mouseY, screen));
                    return true;
                }
            }

            if (topElement.mouseClicked(mouseX, mouseY, button)) {
                screen.setFocused(topElement);

                if (clickedContainer != null) {
                    screen.getRenderables().remove(clickedContainer);
                    screen.getRenderables().add(clickedContainer);
                    
                    int maxZ = 0;
                    for (Renderable r : screen.getRenderables()) {
                        if (r instanceof FlowWidgetContainer other) {
                            if (other.getComponent().getZ() > maxZ) {
                                maxZ = other.getComponent().getZ();
                            }
                        }
                    }
                    clickedContainer.getComponent().setZ(maxZ + 1);
                    screen.setLastClickedContainer(clickedContainer);
                }

                if (button == 0) {
                    if (topElement instanceof FlowWidgetMoveButton flowComponentMoveButton) {
                        this.activeDragged = flowComponentMoveButton.getParent().getContainer();
                        this.dragStartX = mouseX - activeDragged.getX();
                        this.dragStartY = mouseY - activeDragged.getY();
                    }
                }
                return true;
            }
        }
        this.activeDragged = null;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (screen.getActiveSettingsOverlay() != null) {
            screen.getActiveSettingsOverlay().mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }

        if (screen.getActiveModalPopup() != null) {
            screen.getActiveModalPopup().mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }

        if (this.activeWiringSource != null) {
            this.wiringCurrentMouseX = mouseX;
            this.wiringCurrentMouseY = mouseY;
            return true;
        }

        if (this.activeDragged != null) {
            int newX = clampCoordinate((int) (mouseX - this.dragStartX), false, activeDragged);
            int newY = clampCoordinate((int) (mouseY - this.dragStartY), true, activeDragged);
            activeDragged.setX(newX);
            activeDragged.setY(newY);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (screen.getActiveSettingsOverlay() != null) {
            screen.getActiveSettingsOverlay().mouseReleased(mouseX, mouseY, button);
            return true;
        }

        if (screen.getActiveModalPopup() != null) {
            screen.getActiveModalPopup().mouseReleased(mouseX, mouseY, button);
            return true;
        }

        if (button == 0 && this.activeWiringSource != null) {
            GuiEventListener top = getTopElementAt(mouseX, mouseY);
            if (top instanceof FlowWidgetInputNode targetInput) {
                UUID srcId = this.activeWiringSource.getContainer().getComponent().getId();
                int outIdx = this.activeWiringSource.getPinIndex();
                UUID tgtId = targetInput.getContainer().getComponent().getId();
                int inIdx = targetInput.getPinIndex();

                PacketDistributor.sendToServer(new CreateConnectionPacket(
                    screen.getMenu().getManagerBlockEntity().getBlockPos(),
                    srcId, outIdx, tgtId, inIdx
                ));
            }
            this.activeWiringSource = null;
            return true;
        }

        if (button == 0) {
            if (activeDragged != null) {
                if (isMouseOverCopyButton(mouseX, mouseY)) {
                    activeDragged.getComponent().setZ(screen.getRenderables().size() - 1);  
                    PacketDistributor.sendToServer(new CanvasActionPacket(screen.getMenu().getManagerBlockEntity().getBlockPos(), 
                                                                           activeDragged.getComponent().getId(),
                                                                           CanvasAction.COPY));
                    activeDragged = null;
                    screen.setLastClickedContainer(null);
                    return true;
                }		
                
                if (isMouseOverDeleteButton(mouseX, mouseY)) {		  
                    PacketDistributor.sendToServer(new CanvasActionPacket(screen.getMenu().getManagerBlockEntity().getBlockPos(), 
                                                                           activeDragged.getComponent().getId(),
                                                                           CanvasAction.DELETE));
                    activeDragged = null;
                    screen.setLastClickedContainer(null);
                    return true;		  
                }		
                
                int x = (screen.width - screen.getImageWidth()) / 2;	
                int y = (screen.height - screen.getImageHeight()) / 2;
                int newX = clampCoordinate((int) (mouseX - this.dragStartX), false, activeDragged);
                int newY = clampCoordinate((int) (mouseY - this.dragStartY), true, activeDragged);
                activeDragged.getComponent().setX(newX - x);
                activeDragged.getComponent().setY(newY - y);
                activeDragged.getComponent().setZ(screen.getRenderables().size() - 1);
                sendCoordsToServer(activeDragged.getComponent().getId());

                screen.setRefreshCooldown(10);
            } else if (screen.getLastClickedContainer() != null) {
                sendCoordsToServer(screen.getLastClickedContainer().getComponent().getId());
            }
        }	  
        activeDragged = null;
        screen.setLastClickedContainer(null);
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (screen.getActiveSettingsOverlay() != null) {
            screen.getActiveSettingsOverlay().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }

        if (screen.getActiveModalPopup() != null) {
            screen.getActiveModalPopup().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }

        if (screen.getOpenedDropdown() != null) {
            if (screen.getOpenedDropdown().mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }

        if (screen.getActiveSubmenu() != null && screen.getActiveSubmenu().isMouseOver(mouseX, mouseY)) {
            if (screen.getActiveSubmenu().mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }

        GuiEventListener topElement = getTopHoveredElement();
        if (topElement != null && topElement.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return false;
    }

    public GuiEventListener getTopElementAt(double mouseX, double mouseY) {
        if (screen.getActiveSubmenu() != null && screen.getActiveSubmenu().visible && screen.getActiveSubmenu().isMouseOver(mouseX, mouseY)) {
            GuiEventListener hit = findTopElementRecursive(screen.getActiveSubmenu(), mouseX, mouseY);
            if (!(hit instanceof FlowWidgetContainer)) return hit;
        }
        if (screen.getOpenedDropdown() != null && screen.getOpenedDropdown().visible && screen.getOpenedDropdown().isMouseOver(mouseX, mouseY)) {
            GuiEventListener hit = findTopElementRecursive(screen.getOpenedDropdown(), mouseX, mouseY);
            if (!(hit instanceof FlowWidgetContainer)) return hit;
        }
        if (screen.getActiveModalPopup() != null && screen.getActiveModalPopup().visible && screen.getActiveModalPopup().isMouseOver(mouseX, mouseY)) {
            GuiEventListener hit = findTopElementRecursive(screen.getActiveModalPopup(), mouseX, mouseY);
            if (!(hit instanceof FlowWidgetContainer)) return hit;
        }
        if (screen.getActiveSettingsOverlay() != null && screen.getActiveSettingsOverlay().visible && screen.getActiveSettingsOverlay().isMouseOver(mouseX, mouseY)) {
            GuiEventListener hit = findTopElementRecursive(screen.getActiveSettingsOverlay(), mouseX, mouseY);
            if (!(hit instanceof FlowWidgetContainer)) return hit;
        }

        for (int i = screen.getRenderables().size() - 1; i >= 0; i--) {
            var renderable = screen.getRenderables().get(i);
            if (renderable instanceof GuiEventListener listener) {
                if (listener instanceof FlowWidgetContainer container && container.visible) {
                    GuiEventListener hitElement = findTopElementRecursive(container, mouseX, mouseY);
                    if (hitElement != null && hitElement != container && !(hitElement instanceof FlowWidgetContainer)) {
                        return hitElement;
                    }
                } 
                else if (listener instanceof AbstractFlowWidget baseWidget && baseWidget.visible && baseWidget.isMouseOver(mouseX, mouseY)) {
                    GuiEventListener hitElement = findTopElementRecursive(baseWidget, mouseX, mouseY);
                    if (hitElement != null && !(hitElement instanceof FlowWidgetContainer)) {
                        return hitElement;
                    }
                }
            }
        }
        return null;
    }

    private GuiEventListener findTopElementRecursive(GuiEventListener currentElement, double mouseX, double mouseY) {
        if (currentElement instanceof ContainerEventHandler container) {
            List<? extends GuiEventListener> childrenList = container.children();
            for (int i = childrenList.size() - 1; i >= 0; i--) {
                GuiEventListener child = childrenList.get(i);
                
                boolean isVisible = true;
                boolean isMouseOver = false;
                
                if (child instanceof AbstractFlowWidget afw) {
                    isVisible = afw.visible;
                    isMouseOver = afw.isMouseOver(mouseX, mouseY);
                } else if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                    isVisible = widget.visible;
                    isMouseOver = widget.isMouseOver(mouseX, mouseY);
                }

                if (isVisible && isMouseOver) {
                    GuiEventListener deepHit = findTopElementRecursive(child, mouseX, mouseY);
                    if (deepHit != null && !(deepHit instanceof FlowWidgetContainer)) {
                        return deepHit;
                    }
                }
            }
        }
        return currentElement;
    }

    @javax.annotation.Nullable
    public FlowWidgetBase getTopBaseAt(double mouseX, double mouseY) {
        for (int i = screen.getRenderables().size() - 1; i >= 0; i--) {
            var renderable = screen.getRenderables().get(i);
            if (renderable instanceof FlowWidgetContainer container && container.visible) {
                for (GuiEventListener child : container.children()) {
                    if (child instanceof FlowWidgetBase base && base.visible && base.isMouseOver(mouseX, mouseY)) {
                        return base;
                    }
                }
            }
        }
        return null;
    }

    private FlowWidgetContainer getContainerOf(GuiEventListener element) {
        if (element instanceof FlowWidgetContainer container) {
            return container;
        }
        for (Renderable renderable : screen.getRenderables()) {
            if (renderable instanceof FlowWidgetContainer container) {
                if (isAncestorOf(container, element)) {
                    return container;
                }
            }
        }
        return null;
    }

    private FlowWidgetBase getBaseOf(GuiEventListener element) {
        if (element instanceof FlowWidgetBase base) {
            return base;
        }
        for (Renderable renderable : screen.getRenderables()) {
            if (renderable instanceof FlowWidgetContainer container) {
                for (GuiEventListener child : container.children()) {
                    if (child instanceof FlowWidgetBase base) {
                        if (isAncestorOf(base, element)) {
                            return base;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isAncestorOf(GuiEventListener parent, GuiEventListener target) {
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

    public boolean isElementHoverable(GuiEventListener element) {
        if (this.topHoveredElement == null) {
            return false;
        }
        return isAncestorOf(element, this.topHoveredElement);
    }

    private boolean isMouseOverDeleteButton(double mouseX, double mouseY) {
        for (Renderable renderable : screen.getRenderables()) {
            if (renderable instanceof CanvasActionButton deleteButton && deleteButton.getAction() == CanvasAction.DELETE) {
                return deleteButton.isMouseOver(mouseX, mouseY);
            }
        }
        return false;
    }

    private boolean isMouseOverCopyButton(double mouseX, double mouseY) {
        for (Renderable renderable : screen.getRenderables()) {
            if (renderable instanceof CanvasActionButton copyButton && copyButton.getAction() == CanvasAction.COPY) {
                return copyButton.isMouseOver(mouseX, mouseY);
            }
        }
        return false;
    }

    private void sendCoordsToServer(UUID draggedId) {
        List<ComponentMoved.Entry> entries = new ArrayList<>();
        int nodeRank = 0;
        for (int i = 0; i < screen.getRenderables().size(); i++) {
            if (screen.getRenderables().get(i) instanceof FlowWidgetContainer container) {
                AbstractFlowComponent component = container.getComponent();
                entries.add(new ComponentMoved.Entry(component.getId(), component.getX(), component.getY(), nodeRank));
                nodeRank++;
            }
        }
        PacketDistributor.sendToServer(new ComponentMoved(screen.getMenu().getManagerBlockEntity().getBlockPos(), entries, draggedId));
    }

    private int clampCoordinate(int rawPos, boolean isY, FlowWidgetContainer container) {
        int origin = isY ? (screen.height - screen.getImageHeight()) / 2 : (screen.width - screen.getImageWidth()) / 2;
        if (isY) {
            int minY = origin + (container.getComponent().hasInputNodes() ? 10 : 4);
            int maxY = origin + 240 - container.getHeight();
            return net.minecraft.util.Mth.clamp(rawPos, minY, maxY);
        } else {
            int minX = origin + 22;
            int maxX = origin + 508 - container.getWidth();
            return net.minecraft.util.Mth.clamp(rawPos, minX, maxX);
        }
    }

    /**
     * Reconstructs active connections and evaluates if a Shift-Left-Click hit registers on a wire [3].
     * If hit, fires a serverbound RemoveConnectionPacket to delete the wire [3].
     *
     * @param mouseX cursor horizontal offset [3]
     * @param mouseY cursor vertical offset [3]
     * @return true if a wire was successfully hit and deleted [3]
     */
    private boolean checkWireShiftClick(double mouseX, double mouseY) {
        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            return false;
        }

        var manager = screen.getMenu().getManagerBlockEntity();
        if (manager == null) {
            return false;
        }

        var connections = manager.getFlowConnections();
        if (connections == null || connections.isEmpty()) {
            return false;
        }

        for (var conn : connections) {
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

            float dx = (float) (tgtPinX - srcPinX);
            float dy = (float) (tgtPinY - srcPinY);
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            int steps = Math.max(64, Math.min(400, (int) (distance * 1.5F)));
            float deltaY = Math.max(12.0F, Math.abs(tgtPinY - srcPinY) / 2.0F);

            for (int i = 0; i <= steps; i++) {
                float t = (float) i / (float) steps;
                float mt = 1.0F - t;
                float mt2 = mt * mt;
                float mt3 = mt2 * mt;
                float t2 = t * t;
                float t3 = t2 * t;

                float x = mt3 * srcPinX + 3.0F * mt2 * t * srcPinX + 3.0F * mt * t2 * tgtPinX + t3 * tgtPinX;
                float y = mt3 * srcPinY + 3.0F * mt2 * t * (srcPinY + deltaY) + 3.0F * mt * t2 * (tgtPinY - deltaY) + t3 * tgtPinY;

                double distToMouseSq = (x - mouseX) * (x - mouseX) + (y - mouseY) * (y - mouseY);
                // Proximity evaluation: check if cursor resides within 4 pixels of the wire coordinate [3]
                if (distToMouseSq <= 16.0) {
                    PacketDistributor.sendToServer(new RemoveConnectionPacket(
                        screen.getMenu().getManagerBlockEntity().getBlockPos(),
                        conn.getSourceComponentId(), conn.getOutputNodeIndex(),
                        conn.getTargetComponentId(), conn.getInputNodeIndex()
                    ));
                    return true;
                }
            }
        }
        return false;
    }

    @javax.annotation.Nullable
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
        dta.sfmflow.util.NodeCount nodeCount = dta.sfmflow.util.NodeCount.getForCount(component.getNumOutputs());
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
        dta.sfmflow.util.NodeCount nodeCount = dta.sfmflow.util.NodeCount.getForCount(component.getNumInputs());
        int[] spacing = nodeCount.getOffsets(false);
        if (index < spacing.length) {
            return spacing[index];
        }
        return 29;
    }
}