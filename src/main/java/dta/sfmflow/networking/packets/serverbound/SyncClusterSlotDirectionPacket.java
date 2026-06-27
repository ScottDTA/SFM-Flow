package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serverbound sync packet sending selected directional ordinals to Cable
 * Cluster Block Entities [3].
 *
 * @param pos              block coordinates [3]
 * @param slotIndex        index target [3]
 * @param directionOrdinal enum ordinal (-1 representing NONE) [3]
 */
public record SyncClusterSlotDirectionPacket(BlockPos pos, int slotIndex, int directionOrdinal)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncClusterSlotDirectionPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "sync_cluster_slot_direction"));

	public static final StreamCodec<ByteBuf, SyncClusterSlotDirectionPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, SyncClusterSlotDirectionPacket, BlockPos, Integer, Integer>composite(BlockPos.STREAM_CODEC,
					SyncClusterSlotDirectionPacket::pos, ByteBufCodecs.VAR_INT,
					SyncClusterSlotDirectionPacket::slotIndex, ByteBufCodecs.VAR_INT,
					SyncClusterSlotDirectionPacket::directionOrdinal, SyncClusterSlotDirectionPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}