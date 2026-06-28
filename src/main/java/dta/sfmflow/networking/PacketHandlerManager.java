package dta.sfmflow.networking;

import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket;
import dta.sfmflow.networking.packets.clientbound.SyncInventorySlotsPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Supplier;

/**
 * Common side-safe network manager routing clientbound packet processing [3].
 * Shields the Dedicated Server JVM classloader from loading client-only classes
 * during boot [3].
 */
public final class PacketHandlerManager {
	private PacketHandlerManager() {
	}

	private static final Supplier<IPacketHandler<SyncComponentDeltaPacket>> DELTA_HANDLER = () -> {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			return new dta.sfmflow.client.network.ClientPacketHandlerImpl();
		}
		return (payload, context) -> {
		};
	};

	private static final Supplier<IPacketHandler<SyncConnectionsPacket>> CONNECTIONS_HANDLER = () -> {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			return new dta.sfmflow.client.network.ClientConnectionsPacketHandlerImpl();
		}
		return (payload, context) -> {
		};
	};

	private static final Supplier<IPacketHandler<SyncInventorySlotsPacket>> SLOTS_HANDLER = () -> {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			return new dta.sfmflow.client.network.ClientInventorySlotsPacketHandlerImpl();
		}
		return (payload, context) -> {
		};
	};

	public static void handleSyncComponentDelta(final SyncComponentDeltaPacket payload, final IPayloadContext context) {
		DELTA_HANDLER.get().handle(payload, context);
	}

	public static void handleSyncConnections(final SyncConnectionsPacket payload, final IPayloadContext context) {
		CONNECTIONS_HANDLER.get().handle(payload, context);
	}

	public static void handleSyncInventorySlots(final SyncInventorySlotsPacket payload, final IPayloadContext context) {
		SLOTS_HANDLER.get().handle(payload, context);
	}
}