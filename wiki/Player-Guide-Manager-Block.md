# Machine Inventory Manager & Blocks

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

## Expanded Network Block Array

Beyond the Manager, SFM-Flow provides several specialized physical blocks to expand your automation layouts:

1.  **Network Cables** (`cable_block`): The standard routing medium connecting inventory blocks to the Manager.
2.  **Hardened Cables** (`hardened_cable_block`): A blast-resistant network cable designed to protect cable runs in hazardous environments.
3.  **Observer Cables** (`observer_cable_block`): Directional network monitors that extend vanilla Observers to detect block updates safely without emitting external redstone signals.
4.  **Redstone Network Emitters** (`redstone_emitter_block`): Analog redstone output devices supporting independent multi-sided signal control from `0` to `15`, configured via your flowcharts.
5.  **Redstone Network Receivers** (`redstone_receiver_block`): Analog input monitors caching incoming redstone signals on all six faces independently.
6.  **Item Vacuum Hatches** (`item_vacuum_hatch_block`): Vacuums ground item entities in a 3x3x3 volume in front of its face directly into its internal buffer every 10 ticks.
7.  **Item Ejection Hatches** (`item_ejector_hatch_block`): Ejects items from its internal buffer out into the world as floating item entities every 4 ticks.
8.  **Fluid Extraction Hatches** (`fluid_hatch_cable_block`): Fluid vacuumer and ejector hatch that extracts fluid source blocks or deposits them into the world every 10 ticks.
9.  **Standard & Advanced Card Clusters** (`cable_cluster_block`, `advanced_cable_cluster_block`): Consolidated network sub-chassis holding multiple hardware execution cards (Standard holds 9 slots, Advanced holds 18 slots). They accept cables, emitters, receivers, vacuums, ejectors, and fluid hatches to run proxy actions.

---

## Network Scanning & Performance Limits

Whenever the block is placed, removed, or receives adjacent block updates, it initiates a breadth-first search (BFS) scan of adjacent cables and containers. To preserve server stability and performance, this scanning process respects several configurable bounds managed via the server-side configuration file (`sfmflow-server.toml`):

### Server-Safe Performance Constraints

*   **Maximum Connected Inventories** (`maxConnectedInventories`): 
    *   *Default*: `1023` (Range: `1` to `4096`)
    *   Limits the maximum number of chest/container blocks the manager can register under its active inventory registry.
*   **Maximum Cable Search Depth** (`maxCableLength`): 
    *   *Default*: `128` (Range: `16` to `512`)
    *   Limits how far (in block units) the scanner search will travel along connecting cables.
*   **Maximum Workspace Components** (`maxComponentAmount`): 
    *   *Default*: `100` (Range: `10` to `500`)
    *   Limits the absolute number of flowchart nodes that can be created on the manager's canvas interface.
*   **Minimum Interval Trigger Duration** (`minIntervalTicks`): 
    *   *Default*: `5` (Range: `1` to `100`)
    *   The absolute minimum period (in tick units) allowed for periodic execution triggers.
*   **Maximum Interval Trigger Duration** (`maxIntervalTicks`): 
    *   *Default*: `72000` (Range: `20` to `1000000`)
    *   The absolute maximum period (in tick units) allowed for periodic execution triggers.
*   **Network Scan Cooldown** (`networkScanCooldown`):
    *   *Default*: `40` (Range: `0` to `1200`)
    *   The minimum tick duration required between consecutive physical cable network scans to prevent performance spikes.
*   **Maximum Execution Budget** (`maxExecutionBudgetUs`):
    *   *Default*: `1000` (Range: `100` to `10000`)
    *   The maximum time budget (in microseconds) allocated per Manager block per tick to execute transfer tasks.

---

## Integration Capability Support

SFM-Flow employs a registry-based capability pattern. During a network scan, the Manager queries block capabilities on discovered containers, mapping these handlers:

1.  **Item Storage** (`sfmflow:item`): Detects chest blocks and automated machine inventories (`Capabilities.ItemHandler.BLOCK`).
2.  **Fluid Storage** (`sfmflow:fluid`): Detects tanks and pipes (`Capabilities.FluidHandler.BLOCK`).
3.  **Forge Energy (FE)**: Detects energy cells and power generators (`Capabilities.EnergyStorage.BLOCK`).
4.  **Mekanism Chemical Handlers**: If Mekanism is present, the scanner checks the custom chemical capability (`mekanism:chemical_handler`) for gases, slurries, and infusions.