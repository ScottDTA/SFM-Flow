package dta.sfmflow.client.network;

import dta.sfmflow.networking.IPacketHandler;
import dta.sfmflow.networking.packets.clientbound.SyncInventorySlotsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-only packet handler processing synchronized slot item collections [3].
 */
@OnlyIn(Dist.CLIENT)
public class ClientInventorySlotsPacketHandlerImpl implements IPacketHandler<SyncInventorySlotsPacket> {
	@Override
	public void handle(SyncInventorySlotsPacket payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || payload == null || payload.data() == null) {
				return;
			}

			CompoundTag tag = payload.data();
			int totalSlots = tag.getInt("totalSlots");
			if (totalSlots <= 0) {
				return;
			}

			ItemStack[] items = new ItemStack[totalSlots];
			for (int i = 0; i < totalSlots; i++) {
				items[i] = ItemStack.EMPTY;
			}

			ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag slotTag = list.getCompound(i);
				int slot = slotTag.getInt("slot");
				if (slot >= 0 && slot < totalSlots) {
					items[slot] = ItemStack.parse(mc.level.registryAccess(), slotTag.getCompound("item"))
							.orElse(ItemStack.EMPTY);
				}
			}

			ClientInventoryCache.set(payload.pos(), items);

			// Trigger visual update instantly
			if (mc.screen instanceof dta.sfmflow.client.screen.ManagerScreen screen) {
				screen.refreshWidgetLayout();
			}
		});
	}
}