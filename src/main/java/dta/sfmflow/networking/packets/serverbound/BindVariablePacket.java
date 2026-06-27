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
 * Serverbound packet payload sent to bind an inventory group or item filter
 * variable to a canvas card [3].
 */
public record BindVariablePacket(BlockPos pos, UUID componentId, UUID variableId, boolean isGroupVariable)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<BindVariablePacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "bind_variable"));

	public static final StreamCodec<ByteBuf, BindVariablePacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, BindVariablePacket, BlockPos, UUID, UUID, Boolean>composite(BlockPos.STREAM_CODEC,
					BindVariablePacket::pos, UUIDUtil.STREAM_CODEC, BindVariablePacket::componentId,
					UUIDUtil.STREAM_CODEC, BindVariablePacket::variableId, ByteBufCodecs.BOOL,
					BindVariablePacket::isGroupVariable, BindVariablePacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}