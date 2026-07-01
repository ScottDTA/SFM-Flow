# Canvas and Workspace Layout

The Manager Block features a standardized compact flowchart workspace. You can arrange components, connect execution paths, and configure deep internal settings through centered overlay panels.

---

## Graphical Workspace Layout

The interface uses a category-based sliding hover submenu overlay system, keeping creator nodes grouped under sidebar categories:

```
 0,0  ┌──────────────────────────────────────────────────────────────┐
      │[ ]  (X:4, Y:4) Sidebar Category Buttons                      │
      │[T]  ┌──────────────────────────────────────────────────────┐ │
      │[I]  │ (X:22, Y:4)                                          │ │
      │[O]  │                                                      │ │
      │[L]  │         SLIDING OVERLAY SUBMENU (Hover)              │ │
      │[V]  │        ┌─────────────────────────┐                   │ │
      │[U]  │        │   [   Title Label   ]   │ (X:508)           │ │
      │     │        │ ┌───┐ ┌───┐ ┌───┐ ┌───┐ │                   │ │
      │     │        │ │   │ │   │ │   │ │   │ │                   │ │
      │     │        │ └───┘ └───┘ └───┘ └───┘ │         (Y:240)   │ │
      │     └────────┴─────────────────────────┴───────────────────┘ │
      │ Commands: 0                                                  │
      └──────────────────────────────────────────────────────────────┘
                                                                512,256
```

### Canvas Boundary Rules
All flowchart nodes are physically confined within the viewport boundary box. Drag-and-drop operations are clamped to:
*   **Minimum Workspace Bounds**: `X:22`, `Y:4`
*   **Maximum Workspace Bounds**: `X:508`, `Y:240`

---

## Standardized Compact Node Sizing
To maintain a high layout density, all nodes on the canvas are locked to compact viewing dimensions:
*   **Compact Node Background** (`64x20px`): Renders with color-gradient masks based on custom user color configuration choices.
    *   *Texture File*: ![Minimized Component BG](../src/main/resources/assets/sfmflow/textures/gui/flowcomponents/component_min_bg.png)
*   **Interactive Node Sub-Widgets**:
    *   **Move Handle** (`6x6px`): Drag to reposition the node.
        *   *Texture File*: ![Move Button](../src/main/resources/assets/sfmflow/textures/gui/flowcomponents/move_button.png)
    *   **Logic Input Connection Node** (`6x6px` button bounds, `6x12px` texture sheet): Located on the top edge of input-capable components. Uses flipped textures.
    *   **Logic Output Connection Node** (`6x6px` button bounds, `6x12px` texture sheet): Located on the bottom edge of output-capable components.
        *   *Texture File*: ![Output Node](../src/main/resources/assets/sfmflow/textures/gui/flowcomponents/output_node.png)

---

## Workspace Navigation & Controls

### 1. Left Sidebar Category Buttons
Positioned along the left sidebar starting at coordinate `X:4`. These represent logical node groups and are stacked vertically at `16px` intervals:

*   `[T]` **Trigger Category** (`Y:4`): Houses starting logic execution nodes (such as the periodic Interval Trigger).
    *   *Icon*: ![Trigger Icon](../src/main/resources/assets/sfmflow/textures/gui/menu_buttons/trigger_button.png)
*   `[I]` **Input Category** (`Y:20`): Queries data or items from external blocks.
    *   *Icon*: ![Input Icon](../src/main/resources/assets/sfmflow/textures/gui/menu_buttons/input_button.png)
*   `[O]` **Output Category** (`Y:36`): Pushes data or items into external blocks.
    *   *Icon*: ![Output Icon](../src/main/resources/assets/sfmflow/textures/gui/menu_buttons/output_button.png)
*   `[L]` **Logic Category** (`Y:52`): Evaluates comparisons and branches execution paths.
    *   *Icon*: ![Condition Icon](../src/main/resources/assets/sfmflow/textures/gui/menu_buttons/condition_button.png)
*   `[V]` **Variable Category** (`Y:68`): Manages local storage buffers, filters, and variables.
    *   *Icon*: ![Variable Icon](../src/main/resources/assets/sfmflow/textures/gui/menu_buttons/variable_button.png)
*   `[U]` **Utility Category** (`Y:84`): Coordinates groups, signage, camouflage, and crafters.
    *   *Icon*: ![Command Group Icon](../src/main/resources/assets/sfmflow/textures/gui/menu_buttons/command_group_button.png)

Hovering over any of these buttons opens a sliding submenu overlay aligned to the button's midpoint (`getX() + 7`). Moving your mouse cursor away closes the overlay.

---

### 2. Output and Input Wire Connections
To connect two nodes:
1.  Left-click and hold your mouse over a bottom **Output Pin** of a source node.
2.  Drag the cursor to a top **Input Pin** of a destination node.
3.  Release the mouse button to draw a Bezier connection wire.
4.  *To delete a connection*: Hold `Shift` and left-click anywhere along the connection curve.

---

### 3. Context Dropdown Menu (Right-Click)
Right-clicking any node on the canvas opens a compact context menu (`DropdownMenuWidget` scaled at 66%) providing these actions:
*   **Rename Node**: Opens a text modal to assign a custom nickname.
*   **Node Color**: Opens a symmetrical 16-color palette to apply custom gradient color masks to the node.
*   **Settings**: Opens the node's parameters configuration overlay.
*   **Copy Node**: Clones the node and its settings, offset by `+10px`.
*   **Delete Node**: Deletes the node and any connected wires.

---

### 4. Double-Click & Centered Settings Overlays
Double-clicking a node instantly opens its symmetrically centered **Settings Overlay** (`NodeSettingsOverlay`). This action dims the background canvas, focusing your interactions:

*   **Interval Trigger Settings Overlay**:
    *   *Time Unit Cycle Button*: Toggles between `Ticks`, `Seconds`, or `Minutes` as the base duration scale.
    *   *Duration Slider*: Set the periodic trigger rate. Sliding is silent for a smoother feel. Hover and use the scroll wheel to adjust by `1` unit increments.
*   **Item Transfer Settings Overlay**:
    *   *Inventory Selector List*: A scrollable list showing all connected inventory blocks on your network, displaying their block names, icons, and precise coordinates.
    *   *3D Block Preview*: Renders a 3D isometric view of the targeted inventory and neighboring blocks. Hold **Right-click and drag** to orbit/rotate. **Left-click** any of the three visible faces to toggle whether the Manager should access items from that face.
    *   *Slot Layout Configurator*: **Shift-Left-click** any face in the 3D preview to open this modal. Active slots are outlined in neon green, inactive in red, and unaccessible/blocked slots are grayed out with a red X. The layouts are custom-styled to match furnaces, hoppers, crafters, brewing stands, droppers, and dispensers.
    *   *Item Filter Widget*: Toggles Whitelist/Blacklist modes and exposes a 1x12 ghost slot grid where you can drag and drop items from your player inventory to filter operations.

---

## Connector Spacing Offsets

The horizontal layout spacing of pins dynamically adjusts according to the number of input/output paths (1 to 5):

```
  Minimized Layout (Base Width: 64px)
  ┌────────────────────────────────────────────────────────┐
  │ 1 Node:  [29]                                          │
  │ 2 Nodes: [15]       [43]                               │
  │ 3 Nodes: [4]        [29]        [54]                   │
  │ 4 Nodes: [5]   [21]      [37]   [53]                   │
  │ 5 Nodes: [3]  [16]  [29]  [42]  [55]                   │
  └────────────────────────────────────────────────────────┘
```