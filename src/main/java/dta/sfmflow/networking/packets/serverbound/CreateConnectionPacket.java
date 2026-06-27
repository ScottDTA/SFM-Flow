package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/**
 * Serverbound packet payload sent to register a new logical connection on the server flowchart [3].
 */
public record CreateConnectionPacket(BlockPos pos, UUID sourceId, int outputIdx, UUID targetId, int inputIdx) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CreateConnectionPacket> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "create_connection")
    );

    public static final StreamCodec<ByteBuf, CreateConnectionPacket> STREAM_CODEC = StreamCodec.<ByteBuf, CreateConnectionPacket, BlockPos, UUID, Integer, UUID, Integer>composite(
        BlockPos.STREAM_CODEC, CreateConnectionPacket::pos,
        UUIDUtil.STREAM_CODEC, CreateConnectionPacket::sourceId,
        ByteBufCodecs.VAR_INT, CreateConnectionPacket::outputIdx,
        UUIDUtil.STREAM_CODEC, CreateConnectionPacket::targetId,
        ByteBufCodecs.VAR_INT, CreateConnectionPacket::inputIdx,
        CreateConnectionPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}