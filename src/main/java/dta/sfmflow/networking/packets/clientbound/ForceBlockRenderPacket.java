package dta.sfmflow.networking.packets.clientbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ForceBlockRenderPacket(BlockPos pos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ForceBlockRenderPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "force_block_render"));

	public static final StreamCodec<ByteBuf, ForceBlockRenderPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, ForceBlockRenderPacket, BlockPos>composite(BlockPos.STREAM_CODEC,
					ForceBlockRenderPacket::pos, ForceBlockRenderPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}