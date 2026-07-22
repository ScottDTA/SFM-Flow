package dta.sfmflow.networking.packets.clientbound;

import dta.sfmflow.SFMFlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.List;

public record SyncSignTextPacket(
		BlockPos pos, 
		List<String> frontLines, 
		List<String> backLines, 
		DyeColor frontColor, 
		DyeColor backColor, 
		boolean frontGlow, 
		boolean backGlow, 
		boolean waxed
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncSignTextPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "sync_sign_text"));

	private static final StreamCodec<ByteBuf, DyeColor> DYE_COLOR_CODEC = ByteBufCodecs.idMapper(
			id -> DyeColor.values()[id], DyeColor::ordinal);

	// Custom manual codec bypassing the 6-field composite limit
		public static final StreamCodec<ByteBuf, SyncSignTextPacket> STREAM_CODEC = StreamCodec.of(
				(buf, val) -> {
					BlockPos.STREAM_CODEC.encode(buf, val.pos());
					ByteBufCodecs.stringUtf8(32767).apply(ByteBufCodecs.list()).encode(buf, val.frontLines());
					ByteBufCodecs.stringUtf8(32767).apply(ByteBufCodecs.list()).encode(buf, val.backLines());
					DYE_COLOR_CODEC.encode(buf, val.frontColor());
					DYE_COLOR_CODEC.encode(buf, val.backColor());
					ByteBufCodecs.BOOL.encode(buf, val.frontGlow());
					ByteBufCodecs.BOOL.encode(buf, val.backGlow());
					ByteBufCodecs.BOOL.encode(buf, val.waxed());
				},
				buf -> {
					BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
					List<String> frontLines = ByteBufCodecs.stringUtf8(32767).apply(ByteBufCodecs.list()).decode(buf);
					List<String> backLines = ByteBufCodecs.stringUtf8(32767).apply(ByteBufCodecs.list()).decode(buf);
					DyeColor frontColor = DYE_COLOR_CODEC.decode(buf);
					DyeColor backColor = DYE_COLOR_CODEC.decode(buf);
					boolean frontGlow = ByteBufCodecs.BOOL.decode(buf);
					boolean backGlow = ByteBufCodecs.BOOL.decode(buf);
					boolean waxed = ByteBufCodecs.BOOL.decode(buf);
					return new SyncSignTextPacket(pos, frontLines, backLines, frontColor, backColor, frontGlow, backGlow, waxed);
				}
		);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}