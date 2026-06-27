package dta.sfmflow.client.network;

import dta.sfmflow.networking.IPacketHandler;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-only implementation of the common packet handler contract [3].
 * Delegates the processing of clientbound delta updates to the safe ClientPayloadHandler [3].
 */
public class ClientPacketHandlerImpl implements IPacketHandler<SyncComponentDeltaPacket>
 {
  @Override
  public void handle(SyncComponentDeltaPacket payload, IPayloadContext context)
   {
    ClientPayloadHandler.handleSyncComponentDelta(payload, context);
   }
 }