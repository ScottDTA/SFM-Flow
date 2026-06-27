package dta.sfmflow.networking.packets.serverbound;

import java.util.UUID;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.action.CanvasAction;
import io.netty.buffer.ByteBuf; // REQUIRED: For core buffer stream mapping compatibility
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serverbound packet payload sent when a standard canvas node operation is
 * executed.
 *
 * @param pos         the coordinates of the Manager block being accessed
 * @param componentId the UUID of the targeted component
 * @param action      the canvas action execution target
 */
public record CanvasActionPacket(BlockPos pos, UUID componentId, CanvasAction action) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CanvasActionPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "canvas_action_packet"));

	// FIXED COMPILER & RUNTIME ASSIGNMENT ERROR: Shifted master type variable
	// definition layout from FriendlyByteBuf to ByteBuf
	// Bypasses the need for unstable .cast() loops by allowing all parameters to
	// match on the base buffer layer.
	// Explicit generic type parameters <ByteBuf, CanvasActionPacket, ...> added to
	// composite() force the JVM compiler to infer layout data safely.
	public static final StreamCodec<ByteBuf, CanvasActionPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, CanvasActionPacket, BlockPos, UUID, CanvasAction>composite(BlockPos.STREAM_CODEC,
					CanvasActionPacket::pos, UUIDUtil.STREAM_CODEC, CanvasActionPacket::componentId,
					CanvasAction.STREAM_CODEC, CanvasActionPacket::action, // FIXED: Removed .cast() hook
					CanvasActionPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
