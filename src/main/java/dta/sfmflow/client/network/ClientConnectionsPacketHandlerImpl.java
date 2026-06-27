package dta.sfmflow.client.network;

import dta.sfmflow.networking.IPacketHandler;
import dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only implementation of the connections sync packet handler [3].
 */
@OnlyIn(Dist.CLIENT)
public class ClientConnectionsPacketHandlerImpl implements IPacketHandler<SyncConnectionsPacket> {
	@Override
	public void handle(SyncConnectionsPacket payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			if (mc.screen instanceof dta.sfmflow.client.screen.ManagerScreen screen) {
				if (screen.getMenu() != null) {
					var be = screen.getMenu().getManagerBlockEntity();
					if (be != null && be.getBlockPos().equals(payload.pos())) {
						var connectionsList = be.getFlowConnections();
						connectionsList.clear();

						net.minecraft.nbt.ListTag listTag = payload.data().getList("connections",
								net.minecraft.nbt.Tag.TAG_COMPOUND);
						for (int i = 0; i < listTag.size(); i++) {
							connectionsList.add(
									dta.sfmflow.flowcomponents.FlowComponentConnections.load(listTag.getCompound(i)));
						}
						// Refresh client layout instantly
						screen.refreshWidgetLayout();
					}
				}
			}
		});
	}
}