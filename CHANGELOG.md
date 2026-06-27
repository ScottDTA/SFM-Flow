6-27-2026

[Added]
* Implemented a lock-free circular execution ring buffer engine. This Disruptor-style pipeline manages complex item transfers on pre-allocated memory slots, completely eliminating micro-stutters and garbage collection lag spikes during massive routing cycles.
* Upgraded metadata safety during high-speed item transfers. The sorting system now strictly compares item components, ensuring custom tags (e.g. enchantments, names, or durability) are preserved.
* Integrated NeoForge capability caching. Automated transfers now cache item and fluid connections in memory rather than querying them on every operation, significantly reducing lag in large networks.
* Dynamic chunk unloader integration. Cable nodes and scanned inventories situated inside unloaded chunks are put to "sleep" dynamically, protecting the manager from off-thread processing crashes.
* Enforced the Single Controller Constraint. Trying to bridge two independent Machine Inventory Manager networks with cables will now block placement, pop the cable as an item, and display a warning.
* Added a chunk-safety filter for scanned containers. Inventories situated in unloaded chunks are put to "sleep" automatically to prevent machinery operations from causing game crashes.

[Changed]
* Refined inventory scanning with a double-pass safety check to defensively skip sleeping nodes and unloaded chunk regions before processing automated routes.
* Network cable layouts are now saved directly in world save data as a flat array. This allows large cable networks to load instantly without requiring full rediscovery sweeps when the world boots up.
* Cable block placements and unlinking are now optimized using an incremental graph update model. Placing or breaking network cables no longer triggers a full network rescan, eliminating tick-lag and server freezes on large automated setups.
* Rebuilt the backend network scanning engine with primitive-level memory structures, significantly reducing server CPU overhead when mapping complex factory networks.

[Fixed]
* Added an automatic first-tick sanity check on world load to clear out any stale or broken cable references from the saved network cache.
