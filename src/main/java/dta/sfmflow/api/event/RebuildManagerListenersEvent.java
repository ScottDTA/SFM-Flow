package dta.sfmflow.api.event;

import dta.sfmflow.block.entity.ManagerBlockEntity;
import net.neoforged.bus.api.Event;

/**
 * Fired on the NeoForge event bus when a Manager Block Entity's active environmental
 * listeners or structural configurations are being rebuilt (e.g. on load, rescan, or layout edits).
 * Allows addon developers to register or update their own custom environmental triggers.
 */
public class RebuildManagerListenersEvent extends Event {
	private final ManagerBlockEntity manager;

	public RebuildManagerListenersEvent(ManagerBlockEntity manager) {
		this.manager = manager;
	}

	public ManagerBlockEntity getManager() {
		return this.manager;
	}
}