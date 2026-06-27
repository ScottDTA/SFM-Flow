# Machine Inventory Manager

The **Machine Inventory Manager** is the primary processor block of your automation network. It functions as the central computer, executing flowchart logic designed within its graphic interface.

---

## Block Visual Profile

The Manager Block features a custom metallic casing and status interfaces on different faces:

| Bottom Face | Side Face | Top Face |
| :---: | :---: | :---: |
| ![Manager Bottom](../src/main/resources/assets/sfmflow/textures/block/manager_bot.png) | ![Manager Side](../src/main/resources/assets/sfmflow/textures/block/manager_side.png) | ![Manager Top](../src/main/resources/assets/sfmflow/textures/block/manager_top.png) |

---

## Crafting Recipe
The Machine Inventory Manager is constructed in a standard crafting table using the following configuration:

*   **Inputs**:
    *   `I`: Iron Ingot
    *   `R`: Redstone Block
    *   `S`: Stone
    *   `P`: Piston
*   **Grid Placement**:

| Row | Left Column | Center Column | Right Column |
|---|---|---|---|
| **Top** | Iron Ingot (`I`) | Iron Ingot (`I`) | Iron Ingot (`I`) |
| **Middle** | Iron Ingot (`I`) | Redstone Block (`R`) | Iron Ingot (`I`) |
| **Bottom** | Stone (`S`) | Piston (`P`) | Stone (`S`) |

---

## Network Scanning & Performance Limits

Whenever the block is placed, removed, or receives adjacent block updates, it initiates a breadth-first search (BFS) scan of adjacent cables and containers. To preserve server stability and performance, this scanning process respects several configurable bounds managed via the server-side configuration file (`sfmflow-server.toml`):

### Server-Safe Performance Constraints

*   **Maximum Connected Inventories** (`maxConnectedInventories`): 
    *   *Default*: `1023` (Range: `1` to `4096`)
    *   Limits the maximum number of chest/container blocks the manager can register under its active inventory registry.
*   **Maximum Cable Search Depth** (`maxCableLength`): 
    *   *Default*: `128` (Range: `1` to `512`)
    *   Limits how far (in block units) the scanner search will travel along connecting cables.
*   **Maximum Workspace Components** (`maxComponentAmount`): 
    *   *Default*: `511` (Range: `1` to `2048`)
    *   Limits the absolute number of flowchart nodes that can be created on the manager's canvas interface.
*   **Minimum Interval Trigger Duration** (`minIntervalTicks`): 
    *   *Default*: `4` (Range: `1` to `1200`)
    *   The absolute minimum period (in tick units) allowed for periodic execution triggers.
*   **Maximum Interval Trigger Duration** (`maxIntervalTicks`): 
    *   *Default*: `72000` (Range: `20` to `1000000`)
    *   The absolute maximum period (in tick units) allowed for periodic execution triggers.

---

## Integration Capability Support

During a network scan, the Manager queries block capabilities on discovered containers. The following handlers are supported:

1.  **Item Storage**: Detects chest blocks and automated machine inventories (`Capabilities.ItemHandler.BLOCK`).
2.  **Fluid Storage**: Detects tanks and pipes (`Capabilities.FluidHandler.BLOCK`).
3.  **Forge Energy (FE)**: Detects energy cells and power generators (`Capabilities.EnergyStorage.BLOCK`).
4.  **Mekanism Chemical Handlers**: If Mekanism is present, the scanner checks the custom chemical capability (`mekanism:chemical_handler`) for gases, slurries, and infusions using SFM-Flow's built-in compatibility bridge.