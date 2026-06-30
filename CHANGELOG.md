6-30-2026

### [Added]
* External Data Storage: Flowchart layouts and configurations are now stored in dedicated .dat files within the world save folder (<world_folder>/sfmflow/managers/) instead of inside the block's NBT. This mitigates chunk NBT bloating, reduces save overhead,     and keeps world save sizes stable.
* Automated Save Cleanup: When a Machine Inventory Manager block is broken, its corresponding external save file is automatically deleted from disk to prevent unreferenced data buildup.

### [Changed]
* Storage Architecture: Loading and saving processes now fetch layout data asynchronously from the filesystem once the world is loaded.
* Initial Client Synchronization: Data-sync tags continue to package layouts dynamically on chunk load to ensure visual flowchart canvases update immediately when logging in.

###[Fixed]
* Backward Compatibility: Legacy worlds storing layout data directly inside chunk NBT are automatically parsed, loaded, and migrated to the new external format upon first load.

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
