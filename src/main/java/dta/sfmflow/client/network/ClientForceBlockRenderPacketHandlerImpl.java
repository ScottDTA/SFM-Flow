package dta.sfmflow.client.network;

import dta.sfmflow.networking.IPacketHandler;
import dta.sfmflow.networking.packets.clientbound.ForceBlockRenderPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class ClientForceBlockRenderPacketHandlerImpl implements IPacketHandler<ForceBlockRenderPacket> {
	@Override
	public void handle(ForceBlockRenderPacket payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null && payload.pos() != null) {
				BlockPos pos = payload.pos();
				
				// Enqueue onto the primary client render thread context to prevent packet racing
				mc.execute(() -> {
					if (mc.level != null) {
						BlockState state = mc.level.getBlockState(pos);
						
						// 1. Trigger vanilla block update notification on the client level
						mc.level.sendBlockUpdated(pos, state, state, 3);
						
						// 2. Invalidate the specific 16x16x16 section of the map mesh to clear compiled texture caches
						mc.levelRenderer.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
					}
				});
			}
		});
	}
}