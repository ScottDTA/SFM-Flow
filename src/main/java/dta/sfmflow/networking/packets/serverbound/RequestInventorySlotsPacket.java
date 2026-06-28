package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serverbound packet payload requesting slot inventory contents from the server [3].
 */
public record RequestInventorySlotsPacket(BlockPos pos) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<RequestInventorySlotsPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "request_inventory_slots"));

	public static final StreamCodec<ByteBuf, RequestInventorySlotsPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, RequestInventorySlotsPacket, BlockPos>composite(
					BlockPos.STREAM_CODEC, RequestInventorySlotsPacket::pos,
					RequestInventorySlotsPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}