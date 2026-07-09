package dta.sfmflow.client.network;

import dta.sfmflow.networking.IPacketHandler;
import dta.sfmflow.networking.packets.clientbound.SyncSideConfigPropertiesPacket;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.EnergySideConfigModalPopup;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Syncs incoming server properties onto the client cache and refreshes active limit views [3].
 */
@OnlyIn(Dist.CLIENT)
public class ClientSideConfigPropertiesPacketHandlerImpl implements IPacketHandler<SyncSideConfigPropertiesPacket> {
	@Override
	public void handle(SyncSideConfigPropertiesPacket payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			ClientSidePropertyCache.set(payload.pos(), payload.side(), payload.capabilityId(), payload.properties());

			Minecraft mc = Minecraft.getInstance();
			if (mc.screen instanceof ManagerScreen screen) {
				if (screen.getActiveModalPopup() instanceof EnergySideConfigModalPopup energyPopup) {
					energyPopup.refreshProperties();
				}
			}
		});
	}
}