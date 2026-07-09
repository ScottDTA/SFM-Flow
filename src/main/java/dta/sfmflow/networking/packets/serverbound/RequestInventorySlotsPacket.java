package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/**
 * Serverbound packet payload requesting slot inventory contents from the server [3].
 */
public record RequestInventorySlotsPacket(BlockPos pos, @Nullable Direction side) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<RequestInventorySlotsPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "request_inventory_slots"));

	private static final StreamCodec<ByteBuf, Direction> DIRECTION_CODEC = ByteBufCodecs.idMapper(
			id -> Direction.values()[id], Direction::ordinal);

	public static final StreamCodec<ByteBuf, RequestInventorySlotsPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, RequestInventorySlotsPacket, BlockPos, Optional<Direction>>composite(
					BlockPos.STREAM_CODEC, RequestInventorySlotsPacket::pos,
					DIRECTION_CODEC.apply(ByteBufCodecs::optional), p -> Optional.ofNullable(p.side()),
					(pos, opt) -> new RequestInventorySlotsPacket(pos, opt.orElse(null)));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}