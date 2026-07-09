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
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/**
 * Clientbound packet payload synchronizing items in specific slot coordinates [3].
 */
public record SyncInventorySlotsPacket(BlockPos pos, @Nullable Direction side, CompoundTag data) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncInventorySlotsPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "sync_inventory_slots"));

	private static final StreamCodec<ByteBuf, Direction> DIRECTION_CODEC = ByteBufCodecs.idMapper(
			id -> Direction.values()[id], Direction::ordinal);

	public static final StreamCodec<ByteBuf, SyncInventorySlotsPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, SyncInventorySlotsPacket, BlockPos, Optional<Direction>, CompoundTag>composite(
					BlockPos.STREAM_CODEC, SyncInventorySlotsPacket::pos,
					DIRECTION_CODEC.apply(ByteBufCodecs::optional), p -> Optional.ofNullable(p.side()),
					ByteBufCodecs.COMPOUND_TAG, SyncInventorySlotsPacket::data,
					(pos, opt, data) -> new SyncInventorySlotsPacket(pos, opt.orElse(null), data));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}