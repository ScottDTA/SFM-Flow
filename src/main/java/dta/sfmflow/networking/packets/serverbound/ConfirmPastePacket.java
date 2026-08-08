package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfirmPastePacket(BlockPos pos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ConfirmPastePacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "confirm_paste"));

	public static final StreamCodec<ByteBuf, ConfirmPastePacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, ConfirmPastePacket, BlockPos>composite(
					BlockPos.STREAM_CODEC, ConfirmPastePacket::pos,
					ConfirmPastePacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}