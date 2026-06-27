package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/**
 * Serverbound packet payload sent to remove an existing wire connection from
 * the flowchart [3].
 */
public record RemoveConnectionPacket(BlockPos pos, UUID sourceId, int outputIdx, UUID targetId, int inputIdx)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<RemoveConnectionPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "remove_connection"));

	public static final StreamCodec<ByteBuf, RemoveConnectionPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, RemoveConnectionPacket, BlockPos, UUID, Integer, UUID, Integer>composite(BlockPos.STREAM_CODEC,
					RemoveConnectionPacket::pos, UUIDUtil.STREAM_CODEC, RemoveConnectionPacket::sourceId,
					ByteBufCodecs.VAR_INT, RemoveConnectionPacket::outputIdx, UUIDUtil.STREAM_CODEC,
					RemoveConnectionPacket::targetId, ByteBufCodecs.VAR_INT, RemoveConnectionPacket::inputIdx,
					RemoveConnectionPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}