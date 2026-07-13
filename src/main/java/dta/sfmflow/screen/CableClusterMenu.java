package dta.sfmflow.screen;

import dta.sfmflow.block.entity.CableClusterBlockEntity;
import dta.sfmflow.registry.ModTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The container menu coordinating cluster card and player inventory slots.
 * Configured with dynamic slot baselines to prevent overlaps inside Advanced
 * layouts.
 */
public class CableClusterMenu extends AbstractContainerMenu {
	private final CableClusterBlockEntity blockEntity;

	/**
	 * Client-side container loader.
	 */
	public CableClusterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
		this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	/**
	 * Server/Common container initializer.
	 */
	public CableClusterMenu(int containerId, Inventory playerInventory, BlockEntity entity) {
		super(ModMenuTypes.CABLE_CLUSTER_MENU.get(), containerId);
		if (entity instanceof CableClusterBlockEntity cluster) {
			this.blockEntity = cluster;
		} else {
			throw new IllegalStateException("Block entity is not a Cable Cluster!");
		}

		int numSlots = this.blockEntity.getNumSlots();

		// Dynamic slot allocation based on vertical layout columns
		for (int i = 0; i < numSlots; i++) {
			int col = i % 3;
			int row = i / 3;
			int slotX = 8 + col * 54;
			int slotY = 14 + row * 18;

			this.addSlot(new SlotItemHandler(this.blockEntity.getInventory(), i, slotX, slotY));
		}

		// Dynamic player inventory positioning based on the corrected cluster size
		int startY = (numSlots == 18) ? 132 : 78;
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				this.addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, startY + r * 18));
			}
		}

		// Hotbar
		for (int c = 0; c < 9; c++) {
			this.addSlot(new Slot(playerInventory, c, 8 + c * 18, startY + 58));
		}
	}

	public CableClusterBlockEntity getBlockEntity() {
		return this.blockEntity;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player,
				blockEntity.getBlockState().getBlock());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		int numSlots = this.blockEntity.getNumSlots();

		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();

			if (index < numSlots) {
				if (!this.moveItemStackTo(itemstack1, numSlots, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				if (!itemstack1.is(ModTags.CLUSTER_COMPATIBLE)
						|| !this.moveItemStackTo(itemstack1, 0, numSlots, false)) {
					return ItemStack.EMPTY;
				}
			}

			if (itemstack1.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return itemstack;
	}
}