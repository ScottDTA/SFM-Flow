package dta.sfmflow.client.network;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only strategy contract for executing specific flowchart delta updates
 * [3].
 */
@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface IDeltaStrategy {
	/**
	 * Executes the UI transformation for this specific delta update [3].
	 *
	 * @param screen         the active manager screen interface [3]
	 * @param packet         the received delta sync packet [3]
	 * @param localComponent the client-side component model, or null if
	 *                       adding/removed [3]
	 */
	void execute(ManagerScreen screen, SyncComponentDeltaPacket packet, AbstractFlowComponent localComponent);
}