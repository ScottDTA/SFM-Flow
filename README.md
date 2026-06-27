# SFM-Flow

SFM-Flow (`sfmflow`) is a flowchart-based automation mod built for **NeoForge 1.21.1**. It provides an interactive graphical interface to design, execute, and monitor automated logistical networks using cables and nodes.

---

## What SFM-Flow Offers

### For Players (Automators)
*   **Central Processor Block**: The Machine Inventory Manager coordinates network scanning, inventory indexing, and flowchart logic execution.
*   **Cable Network Scanning**: Automatically maps connected blocks using a breadth-first search (BFS) scan down adjacent cables, supporting chest containers, tanks, energy cells, and machines.
*   **Visual Logic Workspace**: An interactive UI layout (coordinates clamped within `X:22–508`, `Y:4–240`) containing sliding category menus, copy/delete hotzones, expanded sub-panels, and output pin distributions.
*   **Hardware Depth Ordering**: Employs OpenGL depth testing and matrix translations to maintain clean layer boundaries between overlapping nodes.
*   **Configurable Boundaries**: Server-side configuration rules (`sfmflow-server.toml`) define scanning and workspace limits to safeguard server tick times.
*   **Capability Integration**: Supports item handlers, fluid handlers, Forge Energy (FE), and Mekanism chemical storage options when Mekanism is installed.

### For Developers (Addon Creators)
*   **Modular Component Registry**: Declare custom logic, input, or output nodes under the NeoForge registry `sfmflow:flow_component_type`.
*   **Decoupled Architecture**: Implement `AbstractFlowComponent` or `AbstractTriggerComponent` for backend processing, while binding client-side graphics separately using `FlowClientRegistry`.
*   **Custom UI Extensions**: Build nested controls using the custom visual base `AbstractFlowWidget` or center-aligned, auto-scaling labels via `FlowWidgetText`.
*   **Vanilla Control Adapter**: Wrap standard Minecraft widgets inside `ApiWidgetAdapter` to coordinate dragging position offsets and preserve correct mouse click focus.
*   **Network Synchronization Rules**: Built-in network logic with discrete sync packets (`SaveComponentSettings`) structured to transmit on mouse releases or scroll increments, minimizing packet flooding.

---

## Repository Structure

*   `src/`: Primary codebase files for client-side and server-side components.
*   `wiki/`: Technical documentation library containing guides for players and developers.
    *   [Wiki Home](wiki/Home.md)
    *   [Machine Inventory Manager Guide](wiki/Player-Guide-Manager-Block.md)
    *   [Canvas and Workspace Guide](wiki/Player-Guide-Canvas-and-UI.md)
    *   [Developer Registry & API Overview](wiki/Developer-API-Overview.md)
    *   [Custom Node Implementation Guide](wiki/Developer-Custom-Nodes.md)
    *   [Network Protocol & Rendering Details](wiki/Developer-Network-and-Sync.md)

---

## Setup and Compilation

To set up the development workspace or build the mod, clone the repository and run the standard Gradle tasks:

```bash
# Set up the workspace
./gradlew genSources

# Compile the mod
./gradlew build
```

---

## Licensing

SFM-Flow uses a dual-licensing structure to address both the compiled code updates and the original visual works:

*   **Codebase**: Released under the **MIT License** (Copyright © 2026 ScottDTA).
*   **Assets & Textures**: Original visual assets, textures, and concepts are the property of the original creator, **Vswe**. In alignment with Vswe's public licensing terms, these assets are distributed, modified, and released under an **Attribution policy**, requiring that credit is provided to Vswe for the original work.