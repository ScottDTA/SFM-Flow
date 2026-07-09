package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serverbound sync packet requesting verified capability properties from the server [3].
 */
public record RequestSideConfigPropertiesPacket(BlockPos pos, Direction side, ResourceLocation capabilityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RequestSideConfigPropertiesPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "request_side_config_properties"));

	private static final StreamCodec<ByteBuf, Direction> DIRECTION_CODEC = ByteBufCodecs.idMapper(
			id -> Direction.values()[id], Direction::ordinal);

	public static final StreamCodec<ByteBuf, RequestSideConfigPropertiesPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, RequestSideConfigPropertiesPacket, BlockPos, Direction, ResourceLocation>composite(
					BlockPos.STREAM_CODEC, RequestSideConfigPropertiesPacket::pos,
					DIRECTION_CODEC, RequestSideConfigPropertiesPacket::side,
					ResourceLocation.STREAM_CODEC, RequestSideConfigPropertiesPacket::capabilityId,
					RequestSideConfigPropertiesPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}