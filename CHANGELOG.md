6-27-2026



#### [Added]
* **Multi-Threaded Planning Engine:** Flowchart logic evaluations and transfer path planning now run asynchronously on a background thread pool, significantly reducing server tick lag when handling complex layout sheets.
* **Smart Circuit Breakers:** Added a 1000-node safety limit per processing cycle to prevent infinite loops and stack overflows from hanging the server if cyclic connections are created.
* **Thread-Safe Inventories:** The system now captures deep-copied snapshots of containers to calculate upcoming item transfers safely without risking cross-thread memory corruption.
* Implemented a lock-free circular execution ring buffer engine. This Disruptor-style pipeline manages complex item transfers on pre-allocated memory slots, completely eliminating micro-stutters and garbage collection lag spikes during massive routing cycles.
* Upgraded metadata safety during high-speed item transfers. The sorting system now strictly compares item components, ensuring custom tags (e.g. enchantments, names, or durability) are preserved.
* Integrated NeoForge capability caching. Automated transfers now cache item and fluid connections in memory rather than querying them on every operation, significantly reducing lag in large networks.
* Dynamic chunk unloader integration. Cable nodes and scanned inventories situated inside unloaded chunks are put to "sleep" dynamically, protecting the manager from off-thread processing crashes.
* Enforced the Single Controller Constraint. Trying to bridge two independent Machine Inventory Manager networks with cables will now block placement, pop the cable as an item, and display a warning.
* Added a chunk-safety filter for scanned containers. Inventories situated in unloaded chunks are put to "sleep" automatically to prevent machinery operations from causing game crashes.


#### [Changed]
* **Synchronized Task Submissions:** Layout planning tasks are now throttled to dispatch every 10 ticks (twice a second) to balance execution responsiveness and CPU performance.
* Refined inventory scanning with a double-pass safety check to defensively skip sleeping nodes and unloaded chunk regions before processing automated routes.
* Network cable layouts are now saved directly in world save data as a flat array. This allows large cable networks to load instantly without requiring full rediscovery sweeps when the world boots up.
* Cable block placements and unlinking are now optimized using an incremental graph update model. Placing or breaking network cables no longer triggers a full network rescan, eliminating tick-lag and server freezes on large automated setups.
* Rebuilt the backend network scanning engine with primitive-level memory structures, significantly reducing server CPU overhead when mapping complex factory networks.


#### [Fixed]
* **Connection ID Indexing:** Fixed an internal coordinate reference bug where scanned target blocks returned a duplicate ID value of 0, resolving selection mapping issues.
* Added an automatic first-tick sanity check on world load to clear out any stale or broken cable references from the saved network cache.
