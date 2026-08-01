package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveManagerAccessPacket(BlockPos pos, int accessLevelOrdinal) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SaveManagerAccessPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "save_manager_access"));

	public static final StreamCodec<ByteBuf, SaveManagerAccessPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, SaveManagerAccessPacket, BlockPos, Integer>composite(
					BlockPos.STREAM_CODEC, SaveManagerAccessPacket::pos,
					ByteBufCodecs.VAR_INT, SaveManagerAccessPacket::accessLevelOrdinal,
					SaveManagerAccessPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}