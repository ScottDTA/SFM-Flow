7-5-2026

### [Added]
* Dedicated settings sub-menus for item Damage (durability) and Enchantment properties.
* Tool durability checks now support filtering based on raw values or custom percentage ranges (such as matching only pickaxes with less than 20% durability).
* Enchantment checks now support advanced matching profiles: match any enchanted items, completely un-enchanted items, items containing specific enchantments with level bounds, or items containing exact matches.
* Highlighted checklist entries in settings menus now display their full path in a tooltip when hovered.
* Variable cards can now filter items based on their attached Data Components (such as matching items with specific damage values, enchantments, or custom names).
* Added a component configuration system allowing developers to register custom settings menus for individual data components.
* Added a scrollable list inside the variable settings panel to choose which item data components are validated.
* Truncated UI text labels now display their full text in a popup tooltip when hovered.

### [Changed]
* Sub-menus now properly capture input focus, resolving an issue where clicking text boxes inside configuration popups would fail to accept keyboard entries.
* Obscured tooltips in the background are now muted while a settings panel or pop-up modal is actively open in the foreground.
* Visual overlays now strictly check the visibility status of sub-elements before rendering them.
* Redesigned the variable card settings panel to arrange the tag selector and the new data component list side-by-side for a more balanced layout.
* Updated variable files saving to safely recycle system threads on server reloads.


7-4-2026

### [Added]
* Advanced Item Filter cards can now match items by Mod ID (matching all items belonging to a specific mod) or by Item Tag (such as `#minecraft:planks`).
* Scrollable tag selector within the filter card settings panel to choose matching tags dynamically.
* Cycling item preview animations on cards configured for tags, cycling once per second through all items associated with the selected tag.
* Visual "MID" text overlay on filter card slots when they are configured to match by Mod ID.
* Custom tooltip lines on filter cards to describe whether they are matching by a specific item, mod namespace, or tag.
* Introduced a slot configuration developer API (`ISlotConfigurable`) that allows third-party add-on creators to reuse the compact block slot selector menu on their own custom nodes.
* Added a dynamic error-handling API (`INodeClientProperties#getErrorTooltip`) that enables custom flowchart blocks to display custom validation errors and warning card tooltips in the interface.

### [Changed]
* Aligned quantity configuration fields to the right of the item slot in the variable card overlay, making room for color and tag controls.
* Refactored network code into a separate processing module to clean up the mod's initial boot sequence.
* Re-organized how client-side settings, screens, and keybinds register themselves during game startup.
* Refactored slot selector menus to support configurations on any compatible custom inventory node rather than restricting them exclusively to the default item input and output blocks.
* Moved visual node error message logic out of main system rendering loops and into modular client registries.

### [Fixed]
* Isolated client-only code paths to prevent crash loop errors when hosting or running dedicated multiplayer servers.


7-4-2026 Hotfix

### [Added]
* Background saving pipeline that offloads layout writes to a separate thread, preventing performance stutters during auto-saves.
* Unified client-side configuration screen registration, making in-game mod options accessible directly from the mod list.

### [Changed]
* Gated console trigger execution traces behind the server debug config option to reduce standard log spam.
* Refactored settings panels to securely inherit translation positioning math directly from parent classes.

### [Fixed]
* Resolved coordinate inversion calculations that caused custom layout selections and item filters to drift when moved.
* Fixed an NBT color parsing issue where older uppercase values in card configurations would fail to load.
* Solved a color bleeding bug by resetting graphics shader settings before drawing category menus.

### [Removed]
* Redundant inline configuration code structures, delegating menu coordinate translation math directly to the base widget classes.

7-3-2026

###[Added]
* Variable Card 3D Models: Variable Cards now render as detailed 3D models displaying their custom dye color and a miniature version of their filtered item stack.
* Detailed Variable Tooltips: Hovering over a Variable Card now displays its target filter, quantity limit settings, and dye color.
* Card Tint Customizer: Added a color selector to the Advanced Item Filter settings overlay, allowing players to dye variable cards independently of their node color.

###[Changed]
* Expanded Variables Search: Extended search queries inside the Variable Drawer; players can now search for variables using their dye color name or target item name.

###[Fixed]
* Typing Conflict Keybinds: Fixed a conflict where pressing the default inventory key ("E") while typing inside a node's search or text box would close the interface.
* Text Box Shift Offsets: Fixed a layout coordinate bug where searching for inventories would shift search boxes to incorrect screen offsets.
* Drawer Performance Boost: Optimized drawer panel rendering to skip cards scrolled out of view, improving frame rates.
* Tooltip Depth Clipping: Corrected tooltip rendering layers to prevent item details from clipping underneath active popups and settings overlays.

7-2-2026

###[Added]
* Drawer Sliding Animations: Added smooth, frame-rate independent sliding animations when opening or closing the Variable Drawer.
* Interactive Handle Labels: Added a vertical "Item Vars" title label and hover tooltips directly onto the drawer's handle.
* Advanced Item Filter Nodes: Players can now instantiate a customizable variable node to save, name, and link reusable item filter configurations across the network.
* Variables Drawer UI: Added a sliding side panel containing a searchable, scrollable 3x3 grid of active variables for instant drag-and-drop capability mapping.
* Visual Warning Indicators: Canvas nodes now highlight in yellow and display custom warning tooltips when configured with empty or unassigned filter variables.
* Variable Card Item: Introduced a physical "Variable Card" carrying custom data signatures to link canvas configurations dynamically.

###[Changed]
* Centered Workspace Layout: Centered the main flowchart canvas within the screen while dynamically positioning player inventory slots to prevent layout overlaps.
* Polished Variable Drawer: Redesigned the Variable Drawer panel to use themed backgrounds, custom slots, and a tactile vertical handle.
* Extensible Panel Mappings: Upgraded the overlay menu system to support clean third-party add-on settings panel registrations.
* NBT Serialization Upgrades: Refactored behind-the-scenes save systems to safely preserve modern item components and properties.

###[Fixed]
* Coordinate Drag Drift: Fixed drag-and-drop calculation drifts that occurred when running the manager interface under custom or forced GUI scales.
* Drawer Click Blocking: Restricted the closed Variable Drawer's collision boundaries so players can interact with canvas nodes behind it without obstruction.
* Component Overlaps: Corrected inventory slot positions to ensure they never overlap with the workspace canvas or sliding drawer.
* Inventory Security Exploits: Fixed duplication vulnerabilities where virtual variable cards could be placed into player inventories or dropped as actual physical items in the world.
* Filter Serialization Recovery: Resolved an issue where variable cards would fail to persist inside external manager files when the world was reloaded.
* Item Stack Matching Desyncs: Corrected calculation issues where item transfers would occasionally fail to match due to dynamic component changes.

6-30-2026

###[Added]
* Modular Plugin API: Introduced a backend plugin system (ISFMFlowPlugin / ISFMFlowClientPlugin) that allows third-party developers to register custom flowchart nodes, settings panels, and capabilities side-safely.
* Extensible Network Capabilities: Rewrote the network routing system to support custom capabilities (FlowCapability). Cable networks can now support fluid, energy, or chemical transfer integrations seamlessly in the future.
* Flowchart Wire Buffers: Connection wires now virtually queue items (FlowItemBuffer) as they travel along S-curves, preserving their source block coordinate metadata for downstream evaluation.
* Server Diagnostic Profiler: Added a /sfmflow profile command for administrators to audit active Manager blocks, displaying performance execution times, queued backlog counts, and click-to-teleport actions.
* Execution Time Budgets: Added a configurable execution time budget (limits.maxExecutionBudgetUs, defaulting to 1000 microseconds) to clamp the time spent executing transfers per tick, protecting server performance.
* External Data Storage: Flowchart layouts and configurations are now stored in dedicated .dat files within the world save folder (<world_folder>/sfmflow/managers/) instead of inside the block's NBT. This mitigates chunk NBT bloating, reduces save overhead,     and keeps world save sizes stable.
* Automated Save Cleanup: When a Machine Inventory Manager block is broken, its corresponding external save file is automatically deleted from disk to prevent unreferenced data buildup.

###[Changed]

* Modular Core Layout: Decoupled built-in triggers and transfers, registering them through the new Vanilla plugin modules.
* API Promoted Widgets: Elevated BlockPreview3DWidget, InventorySelectorWidget, and ItemFilterWidget to public API packages for developers to reuse.
* Storage Architecture: Loading and saving processes now fetch layout data asynchronously from the filesystem once the world is loaded.
* Initial Client Synchronization: Data-sync tags continue to package layouts dynamically on chunk load to ensure visual flowchart canvases update immediately when logging in.

###[Fixed]
* Backward Compatibility: Legacy worlds storing layout data directly inside chunk NBT are automatically parsed, loaded, and migrated to the new external format upon first load.

###[Removed]
* Hardcoded Routines: Removed hardcoded item handler operations from the manager block entity's ticking loop in favor of the registry-backed capability executor loops.


6-29-2026

### [Added]
* New Right-Click Actions: Context dropdown menus now include "Copy Node" and "Delete Node" options for faster flowchart canvas management.
* Ghost Item Slots: Filters now use interactive ghost slots, allowing you to drag and drop items from your player inventory to define filters.
* Added an extensible logic planning API, enabling future addon developers to build custom logic blocks, sensors, and conditional triggers that integrate directly with the background planning engine.
* Added authentic, high-fidelity slot layout screens for vanilla Furnaces, Smokers, Blast Furnaces, Brewing Stands, Crafters, Droppers, Dispensers, and Hoppers that replicate their original inventory styles.
* Added support for custom JSON-driven slot layouts, allowing pack makers and developers to define custom background textures and slot alignments for any block.
* Added warning tooltips on slot layout screens to indicate exactly why a slot is inaccessible from a specific block face (e.g., trying to access a furnace output from the top).
* Added specialized visual fallbacks in the 3D block preview scene to render Chests, Trapped Chests, and Ender Chests cleanly without missing textures or model glitches.

### [Changed]
* Compact Context Menus: The right-click node dropdown menu has been scaled down to 66% size and updated with standard grey styling for a more compact and streamlined appearance.
* Face Marker Blending: The 3D preview's face markers are now rendered with 50% opacity to improve text and orientation visibility underneath.
* Dimming Adjustments: Canvas dimming now correctly applies beneath active inventory slots when a settings overlay is open, keeping player slots fully readable.
* Refactored the core flowchart evaluation system to process nodes polymorphically, improving server-side background execution performance and code stability.
* Streamlined internal ticking routines for Card Clusters and Hatches to reduce server tick overhead.
* Relocated core slot layout systems to the public API, enabling third-party integration mods to register their own visual inventory screens programmatically.

### [Fixed]
* Depth Sorting Glitches: Resolved rendering issues where 3D previews could overlap or clip into settings overlays.
* Overlay Input Conflicts: Fixed settings overlays swallowing mouse clicks meant for active inventory slots.

6-28-2026

### [Added]
* Added a new slot layout configuration screen, accessible by shift-clicking any side of the 3D block preview, allowing you to toggle exactly which inventory slots are active for automated transfers.
* Added full slot-mapping support for worldly containers (such as Furnaces, Brewing Stands, and Smelters) to respect face-locked slot layouts.
* Added live item previews inside slot layout screens to show which items currently reside in each slot of a connected container.
* Added an upgraded in-world highlight system that renders the precise 3D block shape of targeted chests and machines instead of generic wireframe boxes.
* Added a fully interactive, draggable 3D block preview component to the Item Input and Output settings screens.

### [Changed]
* Rebuilt the item filter slots with a custom texture design showing distinct empty and filled visual states.
* Optimized screen drawing layers to prevent overlay menus and dropdowns from clipping or rendering behind the 3D block preview.
* Rebuilt the Item Input and Item Output configuration panels into modular, responsive layout elements to improve readability.
* Integrated public API interfaces (`IFilterable`, `IInventoryTarget`, `ISideConfigurable`) allowing third-party addon developers to reuse standard search, filter, and side configuration widgets.

### [Fixed]
* Fixed an issue where items held on the cursor would be lost when clicking "Save & Close" inside configuration screens; held items are now safely returned to your inventory.
* Consolidated internal interface layouts to improve overall user interface stability and menu loading times.
* Fixed mouse dragging and scroll conflicts when adjusting settings inside the node configuration overlays.


6-27-2026

#### [Added]
* **In-World Container Outlines:** Added an "In-World Highlight" option inside the Item Transfer settings panel. Toggling this checkbox places a glowing golden border around the selected inventory block in the game world, visible through solid walls.
* **Highlight Clear Hotkey & Command:** Pressing `H` (by default) or typing `/sfmflow highlight clear` instantly clears all targeted highlights in the world.
* **Sided Capabilities Support:** Item transfers now respect side-specific capabilities (e.g., extracting from the top slot of a furnace or depositing into its side).
* **Player Inventory Graphic Backing:** Added a beveled dark grey background card sheet at the bottom of the screen to neatly house the player's inventory and hotbar slots.

#### [Changed]
* **Precise Route Planning:** Upgraded the off-thread logic simulator to evaluates slots on a per-face basis rather than ignoring side-restricted inventory access.
* **Precise Interval Tick Scheduling:** Network planning runs are now strictly dispatched per-node only when their corresponding timer interval has elapsed, rather than on a flat global interval.
* **Enhanced Visual Sorting:** Connection wires are now drawn directly on the background canvas, placing them behind interactive card bodies to prevent overlaps with buttons and terminals.
* **Vanilla Double-Chest Compatibility:** Upgraded capability scanners to search with vertical side-contexts, enabling standard double-chests and vanilla containers to resolve properly.
* **Expanded Interface Layout:** Increased the Manager console interface height from 256px to 352px to neatly integrate the variables drawers.
* **Centered Player Inventory:** Re-aligned the player inventory and hotbar slots inside the Manager block screen to sit centrally within the new bottom panel layout, eliminating overlapping lists.
* **Synchronized Task Submissions:** Layout planning tasks are now throttled to dispatch every 10 ticks (twice a second) to balance execution responsiveness and CPU performance.
* Refined inventory scanning with a double-pass safety check to defensively skip sleeping nodes and unloaded chunk regions before processing automated routes.
* Network cable layouts are now saved directly in world save data as a flat array. This allows large cable networks to load instantly without requiring full rediscovery sweeps when the world boots up.
* Cable block placements and unlinking are now optimized using an incremental graph update model. Placing or breaking network cables no longer triggers a full network rescan, eliminating tick-lag and server freezes on large automated setups.
* Rebuilt the backend network scanning engine with primitive-level memory structures, significantly reducing server CPU overhead when mapping complex factory networks.

#### [Fixed]
* **Overlay Viewport Clipping:** Fixed issues where opening settings overlays over 256px in height cropped or hid panel elements by dynamically scaling the canvas viewport.
* **Hot Reload Safety:** Fixed potential thread-pool hangs on world reloads by lazily re-initializing the asynchronous daemon executor pool.
* **Item Mutation Isolation:** Planned transfer frames now copy item stacks before processing, isolating multithreaded simulations from in-flight inventory changes.
* **Memory Leak Protection:** Stale network cable registries are now cleanly purged upon network path recalculations.
* **Connection ID Indexing:** Fixed an internal coordinate reference bug where scanned target blocks returned a duplicate ID value of 0, resolving selection mapping issues.
* Added an automatic first-tick sanity check on world load to clear out any stale or broken cable references from the saved network cache.
