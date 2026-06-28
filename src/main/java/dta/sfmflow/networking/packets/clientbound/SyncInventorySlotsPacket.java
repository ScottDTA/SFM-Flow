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
 * Clientbound packet payload synchronizing items in specific slot coordinates [3].
 */
public record SyncInventorySlotsPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncInventorySlotsPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "sync_inventory_slots"));

	public static final StreamCodec<ByteBuf, SyncInventorySlotsPacket> STREAM_CODEC = StreamCodec
			.<ByteBuf, SyncInventorySlotsPacket, BlockPos, CompoundTag>composite(
					BlockPos.STREAM_CODEC, SyncInventorySlotsPacket::pos,
					ByteBufCodecs.COMPOUND_TAG, SyncInventorySlotsPacket::data,
					SyncInventorySlotsPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}