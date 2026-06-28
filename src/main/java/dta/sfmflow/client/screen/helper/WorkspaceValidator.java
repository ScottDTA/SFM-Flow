package dta.sfmflow.client.screen.helper;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Validates flowchart component states and rules on the clientbound UI [3].
 */
@OnlyIn(Dist.CLIENT)
public final class WorkspaceValidator {

	private WorkspaceValidator() {
	}

	/**
	 * Evaluates if a given component has an unbound connected inventory error [3].
	 * Verifies that the selected inventory ID actually exists on the network [3].
	 *
	 * @param screen    active screen manager [3]
	 * @param component flow component query [3]
	 * @return true if the component has an unbound inventory error [3]
	 */
	public static boolean hasUnboundInventoryError(ManagerScreen screen, AbstractFlowComponent component) {
		if (component instanceof ItemTransferComponent transfer) {
			// 1. Check if bound inventory is actively connected to the network [3]
			boolean foundBoundInventory = false;
			if (transfer.getInventoryId() != -1) {
				for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
					if (block.getId() == transfer.getInventoryId() && !block.isSleeping()) {
						foundBoundInventory = true;
						break;
					}
				}
			}

			// Flag error if unassigned or if the bound chest is disconnected [3]
			if (transfer.getInventoryId() == -1 || !foundBoundInventory) {
				var connections = screen.getMenu().getManagerBlockEntity().getFlowConnections();
				for (var conn : connections) {
					if (conn.getSourceComponentId().equals(transfer.getId())
							|| conn.getTargetComponentId().equals(transfer.getId())) {
						return true;
					}
				}
			}

			// 2. Empty Whitelist validation check [3]
			if (transfer.isWhitelist()) {
				boolean empty = true;
				for (ItemStack stack : transfer.getFilterItems()) {
					if (stack != null && !stack.isEmpty()) {
						empty = false;
						break;
					}
				}
				if (empty) {
					var connections = screen.getMenu().getManagerBlockEntity().getFlowConnections();
					for (var conn : connections) {
						if (conn.getSourceComponentId().equals(transfer.getId())
								|| conn.getTargetComponentId().equals(transfer.getId())) {
							return true;
						}
					}
				}
			}

			// 3. No active sides error check [3]
			if (transfer.getActiveSidesMask() == 0) {
				var connections = screen.getMenu().getManagerBlockEntity().getFlowConnections();
				for (var conn : connections) {
					if (conn.getSourceComponentId().equals(transfer.getId())
							|| conn.getTargetComponentId().equals(transfer.getId())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}