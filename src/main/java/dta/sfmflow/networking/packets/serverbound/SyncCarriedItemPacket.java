package dta.sfmflow.networking.packets.serverbound;

import dta.sfmflow.SFMFlow;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Serverbound packet payload sent to synchronize the player's dragged cursor
 * stack securely [3]. Utilizes RegistryFriendlyByteBuf to support serializing
 * complex item data components safely [3].
 */
public record SyncCarriedItemPacket(ItemStack carried) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncCarriedItemPacket> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "sync_carried_item"));

	// Typo fixed: changed ByteBuf generic type arguments to RegistryFriendlyByteBuf
	// [3]
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncCarriedItemPacket> STREAM_CODEC = StreamCodec
			.<RegistryFriendlyByteBuf, SyncCarriedItemPacket, ItemStack>composite(ItemStack.OPTIONAL_STREAM_CODEC,
					SyncCarriedItemPacket::carried, SyncCarriedItemPacket::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
