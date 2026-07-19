package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Serverbound packet payload sent to migrate a component to a different sub-canvas folder level.
 */
public record MoveComponentGroupPacket(BlockPos pos, UUID componentId, @Nullable UUID targetGroupId) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MoveComponentGroupPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "move_component_group"));

	public static final StreamCodec<ByteBuf, MoveComponentGroupPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, MoveComponentGroupPacket, BlockPos, UUID, Optional<UUID>>composite(
					BlockPos.STREAM_CODEC, MoveComponentGroupPacket::pos,
					UUIDUtil.STREAM_CODEC, MoveComponentGroupPacket::componentId,
					UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), p -> Optional.ofNullable(p.targetGroupId()),
					(pos, id, opt) -> new MoveComponentGroupPacket(pos, id, opt.orElse(null)));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}