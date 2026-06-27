package dta.sfmflow.client.network;

import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;

/**
 * Handles clientbound polymorphic delta synchronization payloads safely on the
 * client thread.
 */
@OnlyIn(Dist.CLIENT)
public class ClientPayloadHandler {
	private ClientPayloadHandler() {
	}

	/**
	 * Processes a S2C delta synchronization payload on the client thread. Safely
	 * isolates world-load null states to prevent Render thread crashes.
	 *
	 * @param data    the delta synchronization payload
	 * @param context the packet execution context
	 */
	public static void handleSyncComponentDelta(final SyncComponentDeltaPacket data, final IPayloadContext context) {
		// Enqueue the packet logic onto Minecraft's main drawing and ticks thread
		// safely
		context.enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();

			// 🔥 THE INPUT SHIELD: Stop processing immediately if the client world or
			// player object is not ready yet!
			if (mc.level == null || mc.player == null || data == null) {
				return;
			}

			// Safe guard: ensure the active screen is a ManagerScreen
			if (mc.screen instanceof dta.sfmflow.client.screen.ManagerScreen screen) {

				// Safe guard: ensure the menu/container wrapper isn't null
				if (screen.getMenu() != null) {

					var blockEntity = screen.getMenu().getManagerBlockEntity();

					// Safe guard: ensure the underlying block entity reference is active on the
					// client
					if (blockEntity != null && blockEntity.getBlockPos() != null && data.pos() != null) {

						// Coordinate verification matching the incoming payload
						if (blockEntity.getBlockPos().equals(data.pos())) {

							// Safe to process! Both references and coordinates match perfectly.
							ClientDeltaRegistry.handle(screen, data);
						}
					}
				}
			}
		});
	}
}
