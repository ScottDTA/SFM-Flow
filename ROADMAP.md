# Master Development Roadmap - SFM-Flow (`sfmflow`)

## Phase 1: Interaction & UX (Final Polish)

### Milestone 1.14: Translation Automation (Language DataGen) [ACTIVE]
*   **Objective:** Relieve the programmer of manual JSON localization files by setting up an automated client-side `LanguageProvider` datagen task, compiling all translations programmatically in Java.
*   **Assigned Agents:**
    *   **Lead Programmer**: Implement `ModLanguageProvider.java` extending NeoForge's base language provider, register existing localized keys in Java, and integrate it with the main datagen event pipeline.
    *   **Changelog Manager**: Log translation data migration to datagen providers.
*   **Technical Goals:**
    *   Construct `ModLanguageProvider` under the `dta.sfmflow.datagen` namespace.
    *   Migrate all localized text from `en_us.json` into Java provider registrations.
    *   Register the new language provider to the `GatherDataEvent` listener pipeline.

### Milestone 1.15: The Canvas Action API & Enum Purge
*   **Objective:** Completely eliminate the rigid `MenuButtonType` enum, splitting canvas-level operations (COPY, DELETE, TOGGLE_OPEN) into a robust `CanvasAction` API while establishing a fluent builder API and dynamic packet spawning for addon nodes.
*   **Assigned Agents:**
    *   **Lead Programmer**: Purge `MenuButtonType`, build the `CanvasAction` enum and associated `CanvasActionPacket`, implement the serverbound `CreateNodePacket` (referencing dynamic `ResourceLocation` keys), and deploy the fluent `FlowComponentBuilder` API helper.
    *   **Wiki Manager**: Author API guides detailing how to use the fluent `FlowComponentBuilder` to register and localize addon nodes.
    *   **Changelog Manager**: Record the networking protocol restructuring and API builder release.

### Milestone 1.16: Adaptive Node Sizing & Accordion Layouts
*   **Objective:** Support customizable vertical sizing for expanded flowchart nodes using a 3-part vertical slice rendering pass on our existing 124x152 texture, and deploy a collapsible Accordion UI framework to fit dense node configurations cleanly [3].
*   **Assigned Agents:**
    *   **Lead Programmer**: Refactor `getVisualHeight()` in `AbstractFlowComponent` to support dynamic, adaptive values, implement the 3-part vertical slice rendering math in `FlowWidgetBase` using our existing texture asset, and write the collapsible `AbstractAccordionWidget` UI helper.
    *   **Wiki Manager**: Document the vertical slicing math and write developer-facing API guides for the Accordion sub-panel UI widget.
    *   **Changelog Manager**: Log adaptive component sizing and the Accordion UI API release.

### Milestone 1.7: The Spatial Side Selector (3D Holo-Cube)
*   **Objective:** Implement an interactive, rotatable 3D block preview inside the expanded 124x152 component workspace. This allows players to select faces visually instead of using text directions.

### Milestone 1.8: Slot Masking Interface
*   **Objective:** Render the scanned machine's slot contents at 50% scale inside the expanded node panel. This allows players to click specific slot indices to whitelist or blacklist them using a `BitSet` mask.

### Milestone 1.9: Data-Driven Component Layouts (JSON)
*   **Objective:** Allow community and modpack creators to define custom machine layouts using JSON files loaded via the data pack system.

### Milestone 1.10: Developer-Facing Layout DataGen
*   **Objective:** Build a fluent Java builder API inside our `api` package so addon developers can programmatically generate and validate layout JSON files.

### Milestone 1.11: Smart Tag Filter System
*   **Objective:** Allow players to right-click a whitelisted item in an Input/Output node to automatically escalate the match from a specific item stack to any associated Forge/Minecraft Tag (`#`).

---

## Phase 2: The Modern Logic Engine

### Milestone 2.0: The Serialization Evolution (Codecs)
*   **Objective:** Isolate flowchart components and wire data mappings into a dedicated, self-contained `Flowchart` class, and transition save loops to declarative, registry-dispatched Mojang Codecs [3].

### Milestone 2.1: The Fiber Kernel (Tick-Slicing)
*   **Objective:** Structure a cooperative multi-tasking execution loop inside a dedicated `FlowExecutionKernel` class, separating flowchart compile and execution runs from the standard `ManagerBlockEntity` file system container [3].

### Milestone 2.2: Profiler Diagnostics
*   **Objective:** Provide administrative tools to profile active Managers in real-time.

### Milestone 2.3: HUD Diagnostics (Jade & TOP)
*   **Objective:** Expose real-time ownership and tick consumption stats on world-look tooltips (such as Jade or The One Probe).

### Milestone 2.4: Advanced Inventory Interop (Safe Transactions & Adapter API)
*   **Objective:** Prevent duplication exploits, item loss, or network lag when interacting with complex storage mods (e.g., Sophisticated Storage, Storage Drawers) by implementing prioritized interceptor wrappers and a public resolution API.

### Milestone 2.5: Ownership Lock & FTB Teams
*   **Objective:** Prevent players from editing another user's flowchart or network configurations, with support for FTB Teams mapping.

