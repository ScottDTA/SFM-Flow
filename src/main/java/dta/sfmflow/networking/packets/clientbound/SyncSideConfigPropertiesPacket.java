package dta.sfmflow.networking.packets.clientbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Clientbound sync packet delivering verified server capabilities and limits [3].
 */
public record SyncSideConfigPropertiesPacket(BlockPos pos, Direction side, ResourceLocation capabilityId, CompoundTag properties) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncSideConfigPropertiesPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "sync_side_config_properties"));

	private static final StreamCodec<ByteBuf, Direction> DIRECTION_CODEC = ByteBufCodecs.idMapper(
			id -> Direction.values()[id], Direction::ordinal);

	public static final StreamCodec<ByteBuf, SyncSideConfigPropertiesPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, SyncSideConfigPropertiesPacket, BlockPos, Direction, ResourceLocation, CompoundTag>composite(
					BlockPos.STREAM_CODEC, SyncSideConfigPropertiesPacket::pos,
					DIRECTION_CODEC, SyncSideConfigPropertiesPacket::side,
					ResourceLocation.STREAM_CODEC, SyncSideConfigPropertiesPacket::capabilityId,
					ByteBufCodecs.COMPOUND_TAG, SyncSideConfigPropertiesPacket::properties,
					SyncSideConfigPropertiesPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}