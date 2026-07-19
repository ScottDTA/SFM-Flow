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
 * Serverbound packet payload sent when a request to instantiate a registered flowchart node is triggered.
 */
public record CreateNodePacket(BlockPos pos, ResourceLocation componentTypeLoc, @Nullable UUID parentGroupId) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CreateNodePacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "create_node_packet"));

	public static final StreamCodec<ByteBuf, CreateNodePacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, CreateNodePacket, BlockPos, ResourceLocation, Optional<UUID>>composite(
					BlockPos.STREAM_CODEC, CreateNodePacket::pos, 
					ResourceLocation.STREAM_CODEC, CreateNodePacket::componentTypeLoc,
					UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), p -> Optional.ofNullable(p.parentGroupId()),
					(pos, loc, opt) -> new CreateNodePacket(pos, loc, opt.orElse(null)));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}