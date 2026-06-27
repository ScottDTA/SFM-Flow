6-27-2026

[Added]
* Enforced the Single Controller Constraint. Trying to bridge two independent Machine Inventory Manager networks with cables will now block placement, pop the cable as an item, and display a warning.
* Added a chunk-safety filter for scanned containers. Inventories situated in unloaded chunks are put to "sleep" automatically to prevent machinery operations from causing game crashes.

[Changed]
* Network cable layouts are now saved directly in world save data as a flat array. This allows large cable networks to load instantly without requiring full rediscovery sweeps when the world boots up.
* Cable block placements and unlinking are now optimized using an incremental graph update model. Placing or breaking network cables no longer triggers a full network rescan, eliminating tick-lag and server freezes on large automated setups.
* Rebuilt the backend network scanning engine with primitive-level memory structures, significantly reducing server CPU overhead when mapping complex factory networks.

[Fixed]
* Added an automatic first-tick sanity check on world load to clear out any stale or broken cable references from the saved network cache.
