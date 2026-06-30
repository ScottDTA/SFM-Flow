package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Serverbound packet payload sent to configure the active filter component in
 * the player's container menu [3].
 */
public record SetActiveFilterComponentPacket(BlockPos pos, @Nullable UUID componentId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SetActiveFilterComponentPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "set_active_filter_component"));

	public static final StreamCodec<ByteBuf, SetActiveFilterComponentPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, SetActiveFilterComponentPacket, BlockPos, Optional<UUID>>composite(BlockPos.STREAM_CODEC,
					SetActiveFilterComponentPacket::pos, UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional),
					p -> Optional.ofNullable(p.componentId()),
					(pos, opt) -> new SetActiveFilterComponentPacket(pos, opt.orElse(null)));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}