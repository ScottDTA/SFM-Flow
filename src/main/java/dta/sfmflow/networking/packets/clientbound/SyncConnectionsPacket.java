package dta.sfmflow.networking.packets.clientbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Clientbound packet payload sent to synchronize the active connection wires instantly on the client flowchart [3].
 */
public record SyncConnectionsPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncConnectionsPacket> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "sync_connections")
    );

    public static final StreamCodec<ByteBuf, SyncConnectionsPacket> STREAM_CODEC = StreamCodec.<ByteBuf, SyncConnectionsPacket, BlockPos, CompoundTag>composite(
        BlockPos.STREAM_CODEC, SyncConnectionsPacket::pos,
        ByteBufCodecs.COMPOUND_TAG, SyncConnectionsPacket::data,
        SyncConnectionsPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}