package dta.sfmflow.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.HashMap;
import java.util.Map;

public final class ClientPacketHandlers {
    private static final Map<CustomPacketPayload.Type<?>, IPacketHandler<?>> HANDLERS = new HashMap<>();

    private ClientPacketHandlers() {}

    /**
     * Registers a client-only packet handler safely from the client-only setup.
     */
    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, IPacketHandler<T> handler) {
        HANDLERS.put(type, handler);
    }

    /**
     * Common delegator that executes the handler if present.
     * Statically safe on both client and server distributions.
     */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void handle(T payload, IPayloadContext context) {
        IPacketHandler<T> handler = (IPacketHandler<T>) HANDLERS.get(payload.type());
        if (handler != null) {
            handler.handle(payload, context);
        }
    }
}