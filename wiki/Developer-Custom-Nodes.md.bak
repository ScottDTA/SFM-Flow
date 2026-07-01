# Creating Custom Nodes

Custom nodes in SFM-Flow are created by extending the `dta.sfmflow.api.component.AbstractFlowComponent` or `dta.sfmflow.api.component.AbstractTriggerComponent` classes.

---

## Base Class Hierarchies

Choose the inheritance structure that matches your custom component's functional goals:

```
                  [AbstractFlowComponent] (Generic Base Sizing, Coordinates & NBT)
                             │
                             ├───► [AbstractTriggerComponent] (Output Nodes Boilerplate)
                             │               │
                             │               └───► Custom Event/Periodic Triggers
                             │
                             └───► Custom Input, Output, Logic, or Variable Nodes
```

*   **`AbstractFlowComponent`**: The core API blueprint. Provides position tracking, dimensions, category configurations, and NBT loading/saving interfaces.
*   **`AbstractTriggerComponent`**: A specialized trigger extension. It configures single-output properties (`hasOutputNodes = true` and `numOutputs = 1`) out of the box.

---

## Component Lifecycle

```
  [ Factory Function ] ───► [ Instantiation ] ───► [ loadData (NBT) ] ───► [ Client Widget Rendering ]
```

1.  **Instantiation**: The custom registry factory creates a new component instance using a random UUID.
2.  **State Initialization**: Default positions (`x: 50`, `y: 50`, `z: 0`) and minimized sizing states are applied.
3.  **NBT Deserialization**: The backing block entity calls `loadData(CompoundTag)` to restore position and custom properties from the world file.
4.  **Client UI Wrapping**: The container screen creates a `FlowWidgetContainer` on the client to represent the component.

---

## State & NBT Serialization

Your custom node classes must override the serialization methods to save their layout positions and properties to disk.

```java
@Override
public CompoundTag saveData(CompoundTag compoundTag) {
    super.saveData(compoundTag);
    // Serialize custom component settings
    compoundTag.putInt("TargetSlot", this.targetSlot);
    compoundTag.putBoolean("MatchDamage", this.matchDamage);
    return compoundTag;
}

@Override
public void loadData(CompoundTag compoundTag) {
    super.loadData(compoundTag);
    // Deserialize custom component settings
    this.targetSlot = compoundTag.getInt("TargetSlot");
    this.matchDamage = compoundTag.putBoolean("MatchDamage");
}
```

### Base Serialized Fields
The superclass `AbstractFlowComponent` automatically manages the following spatial NBT values:

| NBT Key | Type | Description |
|---|---|---|
| `xCoord` | `int` | Relative X coordinate position on the workspace canvas. |
| `yCoord` | `int` | Relative Y coordinate position on the workspace canvas. |
| `zLevel` | `int` | Depth rendering layer (Z-rank sorting priority). |
| `IsOpen` | `boolean` | Flag indicating whether the node is expanded/maximized. |
| `XBefore` | `int` | Cached X coordinate stored before the node was expanded. |
| `YBefore` | `int` | Cached Y coordinate stored before the node was expanded. |

---

## Active Configuration Controls (Maximized Panels)

When a node is maximized, its panel renders interactive controls. To configure settings and sync changes cleanly between the client and server, follow these guidelines:

### 1. Integrating Vanilla UI Controls (`ApiWidgetAdapter`)
If your maximized panel uses standard Minecraft UI components (such as buttons, text inputs, sliders, or checkbox controls), wrap them inside the public client wrapper `dta.sfmflow.api.client.widget.ApiWidgetAdapter<T>`:

```java
package com.example.addon.client.gui;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CustomAddonPanelWidget extends AbstractFlowWidget {
    public CustomAddonPanelWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());

        // Create a standard vanilla button
        Button vanillaButton = Button.builder(Component.literal("Click Me"), btn -> {
            // Trigger local action
        }).bounds(x + 12, y + 30, 80, 20).build();

        // Wrap the button inside the adapter to delegate dragging, focus, and inputs
        this.children.add(new ApiWidgetAdapter<>(vanillaButton));
    }

    @Override
    protected void renderComponent(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Child components in the children list render automatically
    }
}
```
*Benefits of `ApiWidgetAdapter`*:
*   **Coordinate Delegation**: Updates the absolute screen position of the wrapped vanilla widget when dragging its parent node across the canvas.
*   **Input Focus Routing**: Correctly routes mouse clicks, drags, releases, keypresses, and scrolling down to the vanilla widget, avoiding potential input interception from container slot drag handlers.

---

### 2. Network Synchronization (`SaveComponentSettings`)
To save and synchronize modifications to the server, dispatch a serverbound `SaveComponentSettings` packet payload containing your serialized NBT data:

```java
private void saveSettingsToServer() {
    CompoundTag nbt = new CompoundTag();
    this.component.saveData(nbt); // Save current state values into NBT

    // Get the BlockPos of the active Machine Inventory Manager block
    BlockPos managerPos = this.container.getParent().getMenu().getManagerBlockEntity().getBlockPos();

    // Transmit the settings packet to the server
    PacketDistributor.sendToServer(new SaveComponentSettings(managerPos, this.component.getId(), nbt));
}
```

#### Network Optimization Tip:
To prevent network bottlenecks and desynchronization loops during continuous operations (such as sliding a duration bar):
*   **Do not** send `SaveComponentSettings` on every pixel change in your slider drag method (`applyValue()`).
*   **Only** dispatch the packet when the user releases the mouse button (`mouseReleased`) or performs a discrete step change via scroll events (`mouseScrolled`).