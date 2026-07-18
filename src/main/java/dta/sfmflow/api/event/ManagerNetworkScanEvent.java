package dta.sfmflow.api.event;

import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.util.ConnectionBlock;
import net.neoforged.bus.api.Event;
import java.util.List;

/**
 * Fired on the NeoForge event bus after the Manager's physical network scan finishes.
 * Allows addons to inspect, filter, or inject custom virtual ConnectionBlocks.
 */
public class ManagerNetworkScanEvent extends Event {
	private final ManagerBlockEntity manager;
	private final List<ConnectionBlock> scannedInventories;

	public ManagerNetworkScanEvent(ManagerBlockEntity manager, List<ConnectionBlock> scannedInventories) {
		this.manager = manager;
		this.scannedInventories = scannedInventories;
	}

	public ManagerBlockEntity getManager() {
		return manager;
	}

	/**
	 * Returns the mutable list of scanned inventories.
	 * Addons can modify this list directly to filter or inject virtual endpoints.
	 */
	public List<ConnectionBlock> getScannedInventories() {
		return scannedInventories;
	}
}