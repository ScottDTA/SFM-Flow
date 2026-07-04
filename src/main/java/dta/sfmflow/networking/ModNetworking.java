package dta.sfmflow.networking;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.networking.packets.clientbound.*;
import dta.sfmflow.networking.packets.serverbound.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(SFMFlow.MODID);

        // Serverbound Packets (Client to Server)
        registrar.playToServer(CanvasActionPacket.TYPE, CanvasActionPacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleCanvasAction(payload, context));
        registrar.playToServer(CreateNodePacket.TYPE, CreateNodePacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleCreateNode(payload, context));
        registrar.playToServer(ComponentMoved.TYPE, ComponentMoved.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleComponentMoved(payload, context));
        registrar.playToServer(SaveComponentSettings.TYPE, SaveComponentSettings.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleSaveComponentSettings(payload, context));
        registrar.playToServer(SyncClusterSlotDirectionPacket.TYPE, SyncClusterSlotDirectionPacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleSyncClusterSlotDirection(payload, context));
        registrar.playToServer(CreateConnectionPacket.TYPE, CreateConnectionPacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleCreateConnection(payload, context));
        registrar.playToServer(RemoveConnectionPacket.TYPE, RemoveConnectionPacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleRemoveConnection(payload, context));
        registrar.playToServer(BindVariablePacket.TYPE, BindVariablePacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleBindVariable(payload, context));
        registrar.playToServer(RequestInventorySlotsPacket.TYPE, RequestInventorySlotsPacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleRequestInventorySlots(payload, context));
        registrar.playToServer(SetActiveFilterComponentPacket.TYPE, SetActiveFilterComponentPacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleSetActiveFilterComponent(payload, context));
        registrar.playToServer(SyncCarriedItemPacket.TYPE, SyncCarriedItemPacket.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleSyncCarriedItem(payload, context));

        // Clientbound Packets (Server to Client)
        registrar.playToClient(SyncConnectionsPacket.TYPE, SyncConnectionsPacket.STREAM_CODEC,
                PacketHandlerManager::handleSyncConnections);
        registrar.playToClient(SyncComponentDeltaPacket.TYPE, SyncComponentDeltaPacket.STREAM_CODEC,
                PacketHandlerManager::handleSyncComponentDelta);
        registrar.playToClient(SyncInventorySlotsPacket.TYPE, SyncInventorySlotsPacket.STREAM_CODEC,
                PacketHandlerManager::handleSyncInventorySlots);
    }
}
