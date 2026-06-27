**[Added]**
* Added **Vector Wire Patching**, allowing players to visually connect node terminals by left-clicking and dragging a wire from an output pin to an input pin.
* Added a wire deletion mechanic: holding Shift and left-clicking within 4 pixels of any connection wire instantly removes it from the flowchart.
* Added terminal slot limit protection that automatically replaces an existing connection if a new wire is patched onto an already occupied pin.
* Added a **Force GUI Scale** option to client configurations, letting players lock the Machine Inventory Manager interface to a preferred visual scale size rather than relying on automatic screen scaling.
* Added localization translations for the new GUI scale configuration options.
* Added **Standard Card Cluster** (9 slots) and **Advanced Card Cluster** (18 slots) sub-chassis blocks, allowing players to condense multiple functional cards (such as vacuums, ejectors, and fluid hatches) into a single physical block.
* Added an interactive configuration menu for Card Clusters, where players can map each inserted card slot to a specific physical side of the block to route inputs and outputs dynamically.
* Added the `#sfmflow:cluster_compatible` item tag containing all items and hardware cards allowed inside Card Clusters.
* Added balanced crafting recipes, loot tables, and creative mode tab listings for Standard and Advanced Card Clusters.
* Added **Hardened Network Cable**, a blast-resistant cable block designed to protect vital network pathways in hazardous conditions.
* Added **Redstone Network Emitter**, enabling multi-sided analog redstone signal control directly from the flowchart network.
* Added **Redstone Network Receiver**, enabling scannable, independent multi-sided analog input signal monitoring.
* Added **Observer Cable**, a scannable cable block that detects adjacent block updates and notifies the network.
* Added **Item Ejection Hatch**, ejects items into the world with automated crowding checks that halt operation if too many floating item stacks accumulate.
* Added **Item Vacuum Hatch**, pulls dropped items in a 3x3x3 volume directly into the network while respecting standard pickup delays.
* Added **Fluid Extraction Hatch**, supports fluid source vacuum ingestion and legacy fluid block creation with an internal 8-bucket storage capacity.
* Added balanced shaped crafting recipes, loot tables, and creative mode tab listings for all new physical blocks.
* Added localization entries and English translations for the new redstone targets and environmental hatches.

**[Changed]**
* Consolidated **Item Input** and **Item Output** nodes into a unified backend model to improve flowchart file memory efficiency.
* Connection wires now render behind node card bodies, preventing wires from overlapping or blocking interactive buttons and labels.
* Screen scale restoration on exit has been deferred to the next frame tick, resolving potential screen flickering and resizing feedback loops during menu transitions.
* Physical network scanning and routing logic have been updated to recognize and route resources through active Card Cluster blocks.

**[Removed]**
* Removed the redundant, separate backend codebases previously managing inputs and outputs.




**[Added]**
* Double-clicking a flowchart node card now opens a full-screen, dedicated settings configuration overlay panel.
* Introducing scrollable, right-click dropdown context menus to compact node actions.
* Support for third-party expansion add-ons to register custom clickable shortcuts directly into node dropdown menus.

**[Changed]**
* Flowchart components are now permanently compact on the canvas workspace to preserve screen space.
* Adjusting trigger configuration sliders is now completely silent, removing repetitive UI clicking noises.

**[Fixed]**
* Fixed a coordinate collision bug where the invisible margins of overlapping node container boxes could block mouse interactions with nodes located on lower depth layers.
* Resolved potential scroll input conflicts when interacting with stacked vanilla components.

**[Removed]**
* Removed the obsolete node expansion toggle button from the flowchart canvas layout.




#### `[Added]`
* Symmetrical, centrally focused modal popups (`ColorModalPopup` and `RenameModalPopup`) to streamline editing and color-tinting logic nodes [3].
* High-visibility dark background dimming overlays behind active modal menus to focus attention on edits [3].
* Symmetrical 8x2 grid selection menu with responsive gold hover outlines inside the color popup modal [3].
* An airtight modality input-lock system. Keyboard keystrokes, dragging, scroll gestures, and mouse clicks are now completely blocked from affecting the background canvas while a modal is open, preventing accidental workspace edits [3].

#### `[Changed]`
* Refactored the right-click node context menu from a large overlay into a compact, elegant dropdown list containing "Rename Node" and "Node Color" options [3].
* Re-routed the network packet registration sequence through an intermediate common delegation firewall, completely eliminating dedicated server booting crashes.
* Pressing the Escape key while a modal is open now safely dismisses the modal and returns focus to the workspace, eliminating modality escape locks [3].

#### `[Fixed]`
* Fixed an interaction bug where clicking the "Save" or "Cancel" buttons inside popups would sometimes pass through and select text input fields behind them [3].





#### `[Added]`
* Network Cables (`CableBlock`), enabling players to wire together blocks, machines, and storage chests to construct active automation lines [3].
* Shaped crafting recipe for Network Cables, yielding 8 cables per craft using glass, iron, redstone, and pressure plates [3].
* New server configuration parameters: configure a custom tick cooldown limit between active cable scans, and toggle verbose pathfinding diagnostic output inside the server console [3].

#### `[Changed]`
* Decoupled the physical network scanning algorithm from the main block entity into a separate performance-throttled pathway, optimizing tick efficiency.
* Blended logic card background colors with a soft white overlay, resulting in lighter pastel shades that significantly improve label contrast and readability.
* Re-aligned the right-click node color palette to a symmetrical 8x2 grid, replacing the manual clear cell with a default white template.
* Tracing adjacent cables during block breaks is now capped to a conservative search radius of 64 blocks, preventing server TPS stutters.

#### `[Removed]`
* Replaced the Turquoise color mask option with a rich Brown alternative.





#### `[Added]`
* Fully customizable client color configuration file (`sfmflow-client.toml`), letting players customize card backgrounds, borders, and text colors with custom hex codes [3].
* Item Input logic cards, allowing items to be extracted and queried from networked containers [3].
* Item Output logic cards, allowing items to be deposited into targeted inventories [3].
* Input execution terminal pins along the top edge of logic cards, enabling multi-directional connection routing [3].
* Added a server limit setting (`MAX_NESTED_GROUP_DEPTH`) to configure the maximum allowable folder nesting depth [3].
* Interactive right-click context menu for all flowchart workspace nodes [3].
* Customizable node renaming, allowing players to assign custom nicknames directly to any logic card [3].
* Contextual node color tinting. Players can now select from 16 gradient color masks to organize and group workspace setups [3].
* Smart text readability coloring. Labels inside tinted nodes automatically adjust between dark and light colors to stay readable against their chosen backdrop [3].
* An automated save-file recovery barrier. If corrupted layout data is detected during world load, the system isolates the crash safely and clears the workspace instead of bricking the block entity.

#### `[Changed]`
* Expanded logic card click hitboxes dynamically to include both the top input and bottom output pin regions, preventing click-through misses [3].
* Adjusted card dragging and copying bounds to dynamically offset limits if top input pins are present, keeping connection pins from clipping out of the screen boundaries.
* Optimized save files to write block-entity data maps as safe NBT list compounds, improving stability across save sessions.
* Rewrote network stream codecs to align with standard byte-buffers, resolving connection crashes on specific server networks.
* Migrated network packet bootstrap registrations to lazy loading to prevent client classloading errors on dedicated servers.

#### `[Fixed]`
* Fixed a rendering issue where overlapping menus, submenus, and tooltips would occasionally flicker or display behind standard canvas node widgets.





#### `[Added]`
* Active, state-driven multiplayer synchronization protocol. Node movements, settings configurations, additions, and deletions are now updated surgically on nearby observing players' screens without forcing full UI reconstruction cycles.
* Layout refresh cooldown timers and network polling gates to completely prevent the desync-snapping loops and visual jitter caused by delayed packets.
* Dynamic card layering. Clicking on any flowchart panel now automatically raises its visual depth layer to the top foreground.
* Introduced a modular developer API (`FlowComponentBuilder`, `FlowSettingsRegistry`, `ISettingsWidgetProvider`) allowing third-party addon mods to easily register custom logic nodes and configuration menus.
* Programmatic language generator, automating `en_us` localization files during mod compilation.
* Added an automated network synchronization system (`SaveComponentSettings`) to immediately transmit custom settings from the interface to the server on release, preventing desync loops.
* Added a universal widget adapter (`ApiWidgetAdapter`) allowing developers to seamlessly run standard Minecraft UI buttons, sliders, and controls inside custom node layouts.
* Added visual settings controls inside the expanded Interval Trigger node, featuring a custom slider bar to adjust timings and a button to cycle between Ticks, Seconds, and Minutes.
* Added server configuration limits (`minIntervalTicks` and `maxIntervalTicks`) to let administrators clamp trigger frequencies, protecting tick rate stability.
* Added the `AbstractTriggerComponent` API base class to establish a standard structural foundation for all trigger-type nodes.
* Added sidebar category buttons to group and organize flowchart node types logically.
* Added a sliding hover submenu (popup panel) that displays creator tools in a clean, scrollable grid matching the active category.
* Added scrollbar mechanics, 9-slice scaling borders, and smooth scrolling capabilities to the node selection panels.
* Added menu localization strings for "Logic" and "Utility" categories.
* Added a client-side node registry (FlowClientRegistry) allowing the mod to dynamically define icons, translated titles, and menu button properties for each tool.
* Added a workspace sorting category system (NodeCategory) to classify, filter, and group flowchart tools.
* Established a modular public API boundary (dta.sfmflow.api) to allow clean mod integrations and structural separation of core logic.
* Added dynamic GUI scaling to the Machine Inventory Manager workspace, which automatically resizes the interface on low screen resolutions or smaller window sizes.
* Added a dynamic sidebar layout system that automatically collapses vertical empty spaces when tools are disabled.
* Added an automatic screen scaling system for players with smaller monitors or low resolutions. The interface now dynamically shrinks itself if needed to prevent the layout from being cut off.
* Added a dynamic layout system to the manager interface sidebar that automatically collapses empty vertical gaps when specific tools are disabled.

#### `[Changed]`
* **BREAKING CHANGE:** Upgraded the underlying save format for machine layouts and cables to the modern data codec standard. Flowchart canvas configurations created in previous versions are not compatible and will be reset.
* Separated network packet registrations between client and server targets to improve stability on multiplayer dedicated servers.
* Introduced a modular developer API (`FlowComponentBuilder`, `FlowSettingsRegistry`, `ISettingsWidgetProvider`) allowing third-party addon mods to easily register custom logic nodes and configuration menus.
* Programmatic language generator, automating `en_us` localization files during mod compilation.
* Added an automated network synchronization system (`SaveComponentSettings`) to immediately transmit custom settings from the interface to the server on release, preventing desync loops.
* Added a universal widget adapter (`ApiWidgetAdapter`) allowing developers to seamlessly run standard Minecraft UI buttons, sliders, and controls inside custom node layouts.
* Added visual settings controls inside the expanded Interval Trigger node, featuring a custom slider bar to adjust timings and a button to cycle between Ticks, Seconds, and Minutes.
* Added server configuration limits (`minIntervalTicks` and `maxIntervalTicks`) to let administrators clamp trigger frequencies, protecting tick rate stability.
* Added the `AbstractTriggerComponent` API base class to establish a standard structural foundation for all trigger-type nodes.
* Added sidebar category buttons to group and organize flowchart node types logically.
* Added a sliding hover submenu (popup panel) that displays creator tools in a clean, scrollable grid matching the active category.
* Added scrollbar mechanics, 9-slice scaling borders, and smooth scrolling capabilities to the node selection panels.
* Added menu localization strings for "Logic" and "Utility" categories.
* Added a client-side node registry (FlowClientRegistry) allowing the mod to dynamically define icons, translated titles, and menu button properties for each tool.
* Added a workspace sorting category system (NodeCategory) to classify, filter, and group flowchart tools.
* Established a modular public API boundary (dta.sfmflow.api) to allow clean mod integrations and structural separation of core logic.
* Added dynamic GUI scaling to the Machine Inventory Manager workspace, which automatically resizes the interface on low screen resolutions or smaller window sizes.
* Added a dynamic sidebar layout system that automatically collapses vertical empty spaces when tools are disabled.
* Added an automatic screen scaling system for players with smaller monitors or low resolutions. The interface now dynamically shrinks itself if needed to prevent the layout from being cut off.
* Added a dynamic layout system to the manager interface sidebar that automatically collapses empty vertical gaps when specific tools are disabled.

#### `[Fixed]`
* Fixed an issue where cloning a node would sometimes cause coordinate conflicts due to identical internal identifiers.
* Fixed an overlapping interface bug where nodes completely hidden behind other panels on the canvas would still trigger visual hover highlights and pop-up tooltips.
* Fixed visual layering bugs where overlapping text and node background cards would render in the wrong order. Selected and dragged components now cleanly snap to the front layer.
* Fixed slider dragging conflicts where clicking on settings panels would occasionally trigger canvas dragging instead of sliding values.




### ROADMAP.md Status Update

The Master Development Roadmap has been updated. **Milestone 1.4** is now archived as **Completed**, and **Milestone 1.5** is promoted to **Active**.

```markdown
# 🗺️ Master Development Roadmap - SFM-Flow (`sfmflow`)

## Current Status
*   **Active Phase**: Phase 1: Interaction & UX (Final Polish)
*   **Active Milestone**: Milestone 1.5: Advanced Network Hardware [ACTIVE]
*   **Target Version**: `0.1.0-alpha.1`

---

## Archives (Completed Milestones)

### Milestone 1.1: Core Canvas Rendering & Flowchart Foundation
*   **Status**: Completed.

### Milestone 1.2: Right-Click Context Menu, Dynamic Naming & Color Mask Gradients
*   **Status**: Completed.

### Milestone 1.3: Gameplay Logic Foundation (Item Input & Item Output Components)
*   **Status**: Completed.

### Milestone 1.4: Expandable Physical Networks & Decoupled Topology
*   **Status**: Completed (Merged in synced snapshot).

---

## Phase 1: Interaction & UX (Final Polish)

### Milestone 1.5: Advanced Network Hardware [ACTIVE]
*   **Objective:** Implement hardened (blast-resistant) cable nodes, functional Redstone Emitter/Receiver blocks, block-update Observer cables, and vacuum/ejector Hatch and Fluid Hatch cables, deploying an entity-clamping safety shell to prevent server lag [3].
*   **Status:** ACTIVE
*   **Assigned Agents:**
    *   **Lead Programmer:** Implement block classes and block entities for hardened, redstone, observer, and hatch cables, registering them to registries and DataGen tags.
    *   **Wiki Manager:** Author guides mapping Redstone, BUD, Hatch Vacuum/Ejector, and Fluid Hatch placement/suction behaviors.
```

---

# Lead Programmer Handoff Prompt
## Milestone 1.5: Advanced Network Hardware & The Safety Shell

### 1. Technical Specifications

#### A. Explosion-Proof Transmission (`HardenedCableBlock.java`)
*   **Implementation:** Extend `CableBlock`.
*   **Properties:** Set strength and explosion resistance to match blast-resistant structures (strength: `15.0F`, resistance: `1200.0F` to prevent TNT and Wither skull destruction).
*   **Tagging:** Ensure standard DataGen registers this block to the `#sfmflow:cables` tag so `PhysicalNetwork` traverses it natively.

#### B. Redstone Interop Blocks (`RedstoneEmitterBlock.java` & `RedstoneReceiverBlock.java`)
1.  **Redstone Emitter (Active Power Injection):**
    *   Requires a block entity (`RedstoneEmitterBlockEntity`) storing an integer `powerLevel` (range `0` to `15`).
    *   Expose a blockstate integer property `POWER` (`0` to `15`) and update it when the flowchart logic injects a redstone pulse.
    *   Override `getSignal` and `getDirectSignal` on `Block` to emit active analog signals based on the block state property.
2.  **Redstone Receiver (Active Signal Detection):**
    *   Monitor neighbor updates using `neighborChanged`.
    *   If the incoming redstone power level changes compared to its cached value, flag `isDirty = true` on the connected `PhysicalNetwork` or trigger a designated flow execution check on the manager [3].

#### C. Block-Update Observers (`ObserverCableBlock.java`)
*   Implement a directional block (`ObserverCableBlock`) that monitors block state updates on its forward-facing side.
*   Upon detecting an update, flag the local `PhysicalNetwork` as dirty to force an immediate on-demand sweep [3].

#### D. Vacuum/Ejector Hatches & The Entity-Clamping Safety Shell (`HatchCableBlock.java`)
*   **Hatch Cable (Vacuum/Ejector):** Allows direct interaction with dropped item entities in-world.
*   **The Safety Shell (Throttled Scanning):**
    *   Performing `level.getEntitiesOfClass` checks on every server tick is highly expensive and causes server lag.
    *   Enforce a **10-tick scan cooldown** inside the hatching ticking logic.
    *   **Volume Clamping:** Restrict the scan bounding box to a `1x1x1` block volume immediately adjacent to the hatch's open face. Do not scan whole chunks or large search radiuses.
    *   **Ejection Pathing:** When ejecting item stacks, verify if the block position is clear before spawning an `ItemEntity` to prevent item-stack collision physics overload on busy automation lines.

---

### 2. Context Snapshot

#### File Paths & Expected Packages
```text
src/main/java/dta/sfmflow/
├── block/
│   ├── CableBlock.java                 <-- Baseline medium
│   ├── HardenedCableBlock.java         <-- Blast-resistant variant
│   ├── RedstoneEmitterBlock.java       <-- Emits redstone signal on logical updates
│   ├── RedstoneReceiverBlock.java      <-- Receives redstone signal from world
│   ├── ObserverCableBlock.java         <-- Directional update detector
│   ├── HatchCableBlock.java            <-- Vacuum/Ejector item hatch
│   ├── FluidHatchCableBlock.java       <-- Vacuum/Ejector fluid hatch
│   └── entity/
│       ├── RedstoneEmitterBlockEntity.java <-- Caches power levels
│       ├── RedstoneReceiverBlockEntity.java
│       └── HatchCableBlockEntity.java   <-- Manages 10-tick vacuum sweeps & bounding boxes
```

#### Core Class Signature Framework

```java
// ModBlocks.java Registrations (Addendum)
public static final DeferredBlock<Block> HARDENED_CABLE_BLOCK = registerBlock("hardened_cable_block",
    () -> new HardenedCableBlock(BlockBehaviour.Properties.of()
        .strength(15.0F, 1200.0F)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops()
        .pushReaction(PushReaction.BLOCK)));

public static final DeferredBlock<Block> REDSTONE_EMITTER_BLOCK = registerBlock("redstone_emitter_block",
    () -> new RedstoneEmitterBlock(BlockBehaviour.Properties.of()
        .strength(2.0F, 5.0F)
        .sound(SoundType.METAL)));
```

```java
// HatchCableBlockEntity.java (Entity-Clamping Scan Code)
package dta.sfmflow.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class HatchCableBlockEntity extends BlockEntity {
    private int tickCounter = 0;

    public HatchCableBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super(null, pos, state); // Assign correct BlockEntityType in ModBlockEntities
    }

    public void tick(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        tickCounter++;
        if (tickCounter % 10 != 0) {
            return; // 10-tick scan cooldown to prevent TPS lag
        }

        // Clamped Bounding Box: Scan exactly 1 block in front of the hatch's facing direction
        net.minecraft.core.Direction facing = level.getBlockState(pos).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos scanPos = pos.relative(facing);
        AABB clampBox = new AABB(scanPos);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, clampBox);
        if (!items.isEmpty()) {
            // Process item vacuum logic safely
        }
    }
}
```

---

### 3. PM Risk Assessment

*   **Recursion Loop on Redstone Updates:** An Observer cable detecting a redstone state change can trigger a flowchart execution, which then updates a Redstone Emitter cable, creating a circular redstone update loop that crashes the server thread with a `StackOverflowError`.
    *   *Mitigation:* Any redstone changes or observer updates triggered by an SFM-Flow execution must be scheduled for the *next* server tick or decoupled through a non-blocking queue rather than executing immediately in the same call stack [3].
*   **AABB Calculation Leak:** Re-instantiating `AABB` bounding boxes continuously inside ticking blocks can trigger heavy garbage-collection overhead.
    *   *Mitigation:* Cache the local block face bounding boxes during block placement or on face state updates, and reuse the static reference inside `getEntitiesOfClass` loops [3].
*   **Hatch Entity Duplication Hazard:** Vacuuming up an `ItemEntity` and converting it to an internal inventory stack before calling `entity.discard()` can result in item duplication bugs if the transaction is interrupted by chunk unloads or server crashes.
    *   *Mitigation:* Ensure that the item entity's `discard()` or `setRemoved(Entity.RemovalReason.DISCARDED)` method is executed in the same database transaction scope as the inventory item stack insertion, completely verifying successful entity death before saving items.

---

This design specification is finalized. Proceed with implementing Milestone 1.5 when ready.







