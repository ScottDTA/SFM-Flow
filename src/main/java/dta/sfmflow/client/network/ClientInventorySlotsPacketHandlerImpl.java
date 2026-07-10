package dta.sfmflow.client.network;

import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.networking.IPacketHandler;
import dta.sfmflow.networking.packets.clientbound.SyncInventorySlotsPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-only packet handler processing synchronized slot item collections [3].
 */
@OnlyIn(Dist.CLIENT)
public class ClientInventorySlotsPacketHandlerImpl implements IPacketHandler<SyncInventorySlotsPacket> {
	@Override
	public void handle(SyncInventorySlotsPacket payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || payload == null || payload.data() == null) {
				return;
			}

			// Synchronize server-verified slot configurations directly onto the client cache [3]
			ClientInventoryCache.set(payload.pos(), payload.side(), payload.data());

			// Trigger visual update instantly
			if (mc.screen instanceof ManagerScreen screen) {
				screen.refreshWidgetLayout();
			}
		});
	}
}