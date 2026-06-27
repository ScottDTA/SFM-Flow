package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf; // REQUIRED: For core buffer stream mapping compatibility
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serverbound packet payload sent when a request to instantiate a registered flowchart node is triggered.
 *
 * @param pos the coordinates of the Manager block being accessed
 * @param componentTypeLoc the registered type resource location mapping
 */
public record CreateNodePacket(BlockPos pos, ResourceLocation componentTypeLoc) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<CreateNodePacket> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "create_node_packet")
    );

    // FIXED COMPILER & RUNTIME ASSIGNMENT ERROR: Shifted master type variable definition layout from FriendlyByteBuf to ByteBuf
    // Explicit generic type parameters <ByteBuf, CreateNodePacket, ...> added to composite() force the JVM compiler to infer layout data safely.
    public static final StreamCodec<ByteBuf, CreateNodePacket> STREAM_CODEC = StreamCodec.<ByteBuf, CreateNodePacket, BlockPos, ResourceLocation>composite(
        BlockPos.STREAM_CODEC, CreateNodePacket::pos,
        ResourceLocation.STREAM_CODEC, CreateNodePacket::componentTypeLoc,
        CreateNodePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
