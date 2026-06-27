package dta.sfmflow.networking.packets.clientbound;

import java.util.UUID;
import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Clientbound packet payload sent to perform surgical synchronization on specific components [3].
 *
 * @param pos the coordinates of the Manager block being accessed [3]
 * @param componentId the UUID of the targeted component [3]
 * @param deltaType the type of delta sync event [3]
 * @param data custom data payload associated with this sync [3]
 */
public record SyncComponentDeltaPacket(BlockPos pos, UUID componentId, DeltaType deltaType, CompoundTag data) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncComponentDeltaPacket> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "sync_component_delta")
    );

    // 1. FIXED RUNTIME NPE: Compact constructor handles potential null parameters over the wire
    public SyncComponentDeltaPacket {
        if (pos == null) {
            pos = BlockPos.ZERO;
        }
        if (componentId == null) {
            componentId = new UUID(0L, 0L);
        }
        if (deltaType == null) {
            deltaType = DeltaType.MOVE;
        }
        if (data == null) {
            data = new CompoundTag(); // Corrects ByteBufCodecs.COMPOUND_TAG returning a raw null
        }
    }

    /**
     * Polymorphic delta types for flowchart surgical synchronization [3].
     */
    public enum DeltaType {
        MOVE,
        NAME_COLOR,
        SETTINGS,
        ADD,
        REMOVE
    }

    private static final StreamCodec<ByteBuf, DeltaType> DELTA_TYPE_CODEC = ByteBufCodecs.idMapper(
        id -> id >= 0 && id < DeltaType.values().length ? DeltaType.values()[id] : DeltaType.MOVE,
        DeltaType::ordinal
    );

    // 2. FIXED COMPILER ERROR: Master stream type shifted from FriendlyByteBuf to ByteBuf
    // Bypasses the need for unstable .cast() loops by allowing all parameters to match on the base buffer layer.
    public static final StreamCodec<ByteBuf, SyncComponentDeltaPacket> STREAM_CODEC = StreamCodec.<ByteBuf, SyncComponentDeltaPacket, BlockPos, UUID, DeltaType, CompoundTag>composite(
        BlockPos.STREAM_CODEC, SyncComponentDeltaPacket::pos,
        UUIDUtil.STREAM_CODEC, SyncComponentDeltaPacket::componentId,
        DELTA_TYPE_CODEC, SyncComponentDeltaPacket::deltaType,
        ByteBufCodecs.COMPOUND_TAG, SyncComponentDeltaPacket::data,
        SyncComponentDeltaPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
