# Network Protocol and UI Rendering

SFM-Flow synchronizes flowchart logic between clients and the server using dedicated custom network payloads and decoupled client widgets.

---

## Network Payloads

Custom payloads are processed on the main server thread using NeoForge's network system:

```
  [ Client UI Drag ] ───► sends ComponentMoved Packet ───► [ Server Work Queue ]
                                                                   │
                                                           Updates Positions
                                                                   │
                                                       Triggers Block Update
```

### 1. `ComponentMoved` (Serverbound)
Dispatched when dragging a node across the canvas viewport. It updates position coordinates and Z-level visual layers on the server.
*   **Payload Fields**:
    *   `pos`: `BlockPos` (The coordinates of the active Manager block).
    *   `entries`: `List<ComponentMoved.Entry>` (A list of visual coordinate entries for all active nodes on the canvas).
    *   `draggedId`: `UUID` (The UUID of the node that was actively dragged).

### 2. `ManagerMenuButtonClick` (Serverbound)
Sent when a button is clicked on the workspace interface. This handles copying, deleting, resizing, and dynamically spawning nodes.
*   **Payload Fields**:
    *   `containerId`: `int` (The ID of the active container menu).
    *   `buttonType`: `MenuButtonType` (The type of menu action or workspace creation requested).
    *   `pos`: `BlockPos` (The coordinates of the active Manager block).
    *   `componentId`: `UUID` (The target component's UUID).

---

## Client Screen Updates

When a manager block entity changes on the server, the server transmits block packet updates to nearby clients. Clients process these updates via `ClientPacketHelper`:

```java
public static void refreshManagerScreen(ManagerBlockEntity be) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.screen instanceof ManagerScreen activeScreen) {
        if (activeScreen.getMenu().getManagerBlockEntity().getBlockPos().equals(be.getBlockPos())) {
            activeScreen.refreshWidgetLayout();
        }
    }
}
```

This method cleans up old widget elements from the screen and parses updated coordinates to construct fresh `FlowWidgetContainer` instances on the client interface.

### Depth Sorting (Z-Level) and Hardware Layering
To draw overlapping nodes correctly, the rendering loop sorts active components using their depth index (`zLevel`), translates them along the Z-axis, and flushes the buffer:

```java
// Inside FlowWidgetContainer's rendering path:
guiGraphics.pose().pushPose();
guiGraphics.pose().translate(0.0F, 0.0F, this.getZ() * 10.0F);

super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

// Forces a buffer flush to apply the depth writing step instantly
guiGraphics.flush();

guiGraphics.pose().popPose();
```

The sorting is resolved during widget compilation:

```java
private void buildComponents(int x, int y) {
    List<FlowWidgetContainer> componentContainers = new ArrayList<>();
    
    for (AbstractFlowComponent component : this.getMenu().getManagerBlockEntity().getFlowComponents().values()) {
        FlowWidgetContainer componentContainer = new FlowWidgetContainer(this, component, x, y);
        componentContainers.add(componentContainer);
    }
    
    // Sort ascending to render lower elements first
    componentContainers.sort(Comparator.comparing(FlowWidgetContainer::getZ));

    for (FlowWidgetContainer container : componentContainers) {
        this.addRenderableWidget(container);
    }
}
```

---

## Custom UI Element Extensions (`AbstractFlowWidget`)

For creating custom client-side sub-panels, text fields, or sliders inside your expanded node, extend the public client API widget base class:
`dta.sfmflow.api.client.widget.AbstractFlowWidget`

This widget handles nested rendering ticks, sound dispatching configurations, hovering checks, and automatic hierarchical position shifts.

### Custom Rendering Typography (`FlowWidgetText`)
For text rendering tasks, the API includes `dta.sfmflow.api.client.widget.FlowWidgetText` as a public client-side widget class. Developers can instantiate it inside their panels to leverage optional text centering, custom matrix scale translations, and automated ellipsis truncation for long localized text values:

```java
package com.example.addon.client.gui;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class CustomAddonPanelWidget extends AbstractFlowWidget {
    
    private final FlowWidgetText centeredHeaderLabel;

    public CustomAddonPanelWidget(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        
        // Instantiate a center-aligned text label running at 80% scale factor
        this.centeredHeaderLabel = new FlowWidgetText(
            font, 
            x + 4, 
            y + 4, 
            width - 8, 
            10, 
            Component.translatable("gui.addon.panel.header"), 
            0.8F, 
            true // Centered horizontal alignment
        );
        this.children.add(centeredHeaderLabel);
    }

    @Override
    protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.visible) {
            this.centeredHeaderLabel.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
```

---

## Advanced UI Rendering in the Category Hover Submenu

Addon creators can integrate custom elements into the category navigation layout. The `CategoryHoverSubmenu` uses several strategies to construct sliding node overlays:

### 1. 9-Slice Background Stretching
The submenu background relies on `submenu_bg.png` (`22x22px` texture sheet). It stretches midsections to fit variable grids while maintaining `6px` border corners:

```
  Source Sheet (22x22px)                  9-Slice Scaling Formula
  ┌───┬──────────┬───┐              ┌──────────┬──────────────┬──────────┐
  │   │  Stretch │   │ 6px Corner   │  Corner  │ Stretched    │  Corner  │
  │   │  (10px)  │   │              ├──────────┼──────────────┼──────────┤
  ├───┼──────────┼───┤              │          │              │          │
  │ S │  Center  │ S │ 10px Mid     │Stretched │ Stretched    │Stretched │
  │ t │  Stretch │ t │              │          │              │          │
  ├───┼──────────┼───┤              ├──────────┼──────────────┼──────────┤
  │   │  Stretch │   │ 6px Corner   │  Corner  │ Stretched    │  Corner  │
  └───┴──────────┴───┘              └──────────┴──────────────└──────────┘
```

### 2. Centered Typography and Scaled Centering
Menu titles are generated using `FlowWidgetText` instances with horizontal alignment centering. If localized names exceed the horizontal viewport boundaries, typography scales dynamically down to a minimum of 40% scale factor (`0.4F`) before applying truncation ellipses:

```java
int availableScaledWidth = (int) (getWidth() / scale);
int titleWidth = font.width(getMessage());

if (titleWidth <= availableScaledWidth) {
    int startX = this.centered ? (availableScaledWidth - titleWidth) / 2 : 0;
    guiGraphics.drawString(font, getMessage(), startX, 0, 4210752, false);		
} else {
    int ellipsisWidth = font.width("...");
    String croppedText = font.getSplitter().plainHeadByWidth(getMessage().getString(), availableScaledWidth - ellipsisWidth, Style.EMPTY);
    int croppedWidth = font.width(croppedText + "...");
    int startX = this.centered ? (availableScaledWidth - croppedWidth) / 2 : 0;
    guiGraphics.drawString(font, croppedText + "...", startX, 0, 4210752, false);
}
```

### 3. Hardware Scissor Masking
Grid items are clamped to a strict visual box to prevent rendering artifacts during scroll offset transitions. Hardware OpenGL scissors restrict the rendering layout coordinates:

```java
// Limit grid cells to a 56x56 view bounding box
guiGraphics.enableScissor(getX() + 6, getY() + 18, getX() + 6 + 56, getY() + 18 + 56);

// Render items at offset coordinates
guiGraphics.blit(props.getIconTexture(), itemX, itemY, 0, vOffset, 14, 14, 14, 28);

guiGraphics.disableScissor();
```