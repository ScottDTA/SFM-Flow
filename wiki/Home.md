# SFM-Flow Wiki

Welcome to the official wiki for **SFM-Flow** (`sfmflow`), a high-performance flowchart-based automation mod built for NeoForge 1.21.1.

SFM-Flow provides a visual logic design canvas where you can configure and arrange automation routines, connect blocks, and manage resources over a physical network.

---

## Workspace Architecture

```
        [ Cable Network ] ───► [ Dynamic Capabilities (FlowCapabilityRegistry) ]
               │
               ▼
  [ Machine Inventory Manager ] ───► [ Symmetrically Centered Settings Overlays ]
```

---

## Wiki Navigation Directory

The wiki is organized into two primary tracks based on your needs:

### 1. Player & Automator Guides
*   **[Machine Inventory Manager & Blocks](Player-Guide-Manager-Block.md)**: Details the physical manager block, crafting recipes, cables, observers, hatches, card clusters, and server performance limits.
*   **[Canvas and Workspace Layout](Player-Guide-Canvas-and-UI.md)**: Explains the standardized compact canvas boundaries, input/output node spacing, context dropdown menus, symmetrically centered settings overlays, 3D block previews, and slot layouts.

### 2. Developer & Addon Creator API (under `dta.sfmflow.api`)
*   **[API and Component Registries](Developer-API-Overview.md)**: Learn how to register custom flowchart components using the fluent `FlowComponentBuilder`, integrate with the logical plugin system, map client visual properties, and extend dynamic capabilities.
*   **[Creating Custom Nodes](Developer-Custom-Nodes.md)**: Implement custom component classes utilizing the Mojaang Codec serialization system, configure BaseProperties NBT wrappers, design client settings widgets, and adapt vanilla components with `ApiWidgetAdapter`.
*   **[Network Protocol and UI Rendering](Developer-Network-and-Sync.md)**: Explore the asynchronous execution kernel, ring buffers, circuit breakers, client delta strategy routing, gradient blitting, 3D viewport rendering, and scissor masking.