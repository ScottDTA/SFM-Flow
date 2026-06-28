6-28-2026

### [Added]
* Added an upgraded in-world highlight system that renders the precise 3D block shape of targeted chests and machines instead of generic wireframe boxes.
* Added a fully interactive, draggable 3D block preview component to the Item Input and Output settings screens.

### [Changed]
* Rebuilt the Item Input and Item Output configuration panels into modular, responsive layout elements to improve readability.
* Integrated public API interfaces (`IFilterable`, `IInventoryTarget`, `ISideConfigurable`) allowing third-party addon developers to reuse standard search, filter, and side configuration widgets.

### [Fixed]
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
