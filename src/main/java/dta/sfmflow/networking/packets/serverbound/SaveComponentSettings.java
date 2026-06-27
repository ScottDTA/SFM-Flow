package dta.sfmflow.networking.packets.serverbound;

import java.util.UUID;
import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf; // REQUIRED: For core buffer stream mapping compatibility
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serverbound packet payload sent when a flow component's configuration
 * settings are modified. Saves and synchronizes serialized component data onto
 * the backing block entity on the server.
 */
public record SaveComponentSettings(BlockPos pos, UUID componentId, CompoundTag settings)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SaveComponentSettings> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "save_component_settings"));

	// 1. FIXED RUNTIME NPE: Compact constructor strips out unexpected stream null
	// parameters
	public SaveComponentSettings {
		if (pos == null) {
			pos = BlockPos.ZERO;
		}
		if (componentId == null) {
			componentId = new UUID(0L, 0L);
		}
		if (settings == null) {
			settings = new CompoundTag(); // Corrects ByteBufCodecs.COMPOUND_TAG returning raw null
		}
	}

	// 2. FIXED COMPILER & RUNTIME ASSIGNMENT ERROR: Shifted from FriendlyByteBuf to
	// ByteBuf
	// This allows parameters to map directly onto the baseline buffer matrix
	// without generic calculation flaws.
	public static final StreamCodec<ByteBuf, SaveComponentSettings> STREAM_CODEC = StreamCodec
			.<ByteBuf, SaveComponentSettings, BlockPos, UUID, CompoundTag>composite(BlockPos.STREAM_CODEC,
					SaveComponentSettings::pos, UUIDUtil.STREAM_CODEC, SaveComponentSettings::componentId,
					ByteBufCodecs.COMPOUND_TAG, SaveComponentSettings::settings, SaveComponentSettings::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
