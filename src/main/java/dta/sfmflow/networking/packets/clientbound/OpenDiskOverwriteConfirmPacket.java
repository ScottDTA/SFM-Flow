package dta.sfmflow.networking.packets.clientbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenDiskOverwriteConfirmPacket(BlockPos pos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenDiskOverwriteConfirmPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "open_disk_overwrite_confirm"));

	public static final StreamCodec<ByteBuf, OpenDiskOverwriteConfirmPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, OpenDiskOverwriteConfirmPacket, BlockPos>composite(
					BlockPos.STREAM_CODEC, OpenDiskOverwriteConfirmPacket::pos,
					OpenDiskOverwriteConfirmPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}