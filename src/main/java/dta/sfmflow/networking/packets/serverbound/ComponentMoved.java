package dta.sfmflow.networking.packets.serverbound;

import java.util.List;
import java.util.UUID;
import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf; // REQUIRED: For core buffer stream mapping compatibility
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serverbound packet payload sent when visual flow components are moved on the
 * canvas. Synchronizes visual coordinates and layering indexes (Z-ranks) of all
 * active elements.
 * 
 * @param pos       the coordinates of the Manager block being accessed
 * @param entries   the list of coordinate entries for all active components
 * @param draggedId the UUID of the component that was manually moved in this
 *                  drag operation
 */
public record ComponentMoved(BlockPos pos, List<ComponentMoved.Entry> entries, UUID draggedId)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ComponentMoved> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "component_moved"));

	/**
	 * Represents the layout state of an individual flow component on the client
	 * workspace.
	 */
	public record Entry(UUID id, int x, int y, int z) {
		public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec
				.<ByteBuf, Entry, UUID, Integer, Integer, Integer>composite(UUIDUtil.STREAM_CODEC, Entry::id,
						ByteBufCodecs.VAR_INT, Entry::x, ByteBufCodecs.VAR_INT, Entry::y, ByteBufCodecs.VAR_INT,
						Entry::z, Entry::new);
	}

	public static final StreamCodec<ByteBuf, ComponentMoved> STREAM_CODEC = StreamCodec.<ByteBuf, ComponentMoved, BlockPos, List<ComponentMoved.Entry>, UUID>composite(
			BlockPos.STREAM_CODEC, ComponentMoved::pos, Entry.CODEC.apply(ByteBufCodecs.list()),
			ComponentMoved::entries, UUIDUtil.STREAM_CODEC, ComponentMoved::draggedId, ComponentMoved::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
