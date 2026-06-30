package dta.sfmflow.screen;

import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.block.entity.FilterGhostSlot;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.util.MenuSlotRepositioner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The logical container menu for the Manager block interface. Coordinates
 * inventory transfer mappings and synchronizes active workspace command counts.
 */
public class ManagerMenu extends AbstractContainerMenu {
	private final ManagerBlockEntity blockEntity;
	private final Level level;
	private final ContainerData data;

	private @Nullable ItemTransferComponent activeFilterComponent = null;

	public ManagerMenu(int pContainerId, Inventory pInv, FriendlyByteBuf pExtradata) {
		this(pContainerId, pInv, pInv.player.level().getBlockEntity(pExtradata.readBlockPos()),
				new SimpleContainerData(1));
	}

	public ManagerMenu(int pContainerId, Inventory pInv, BlockEntity pEntity, ContainerData pData) {
		super(ModMenuTypes.MANAGER_MENU.get(), pContainerId);
		if (pEntity instanceof ManagerBlockEntity manager) {
			this.blockEntity = manager;
		} else {
			throw new IllegalStateException("Block entity is not a Manager!");
		}
		this.level = pInv.player.level();
		this.data = pData;

		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				this.addSlot(new Slot(pInv, c + r * 9 + 9, 175 + c * 18, 266 + r * 18));
			}
		}

		for (int c = 0; c < 9; c++) {
			this.addSlot(new Slot(pInv, c, 175 + c * 18, 324));
		}

		// Add 12 Filter Ghost Slots off-screen by default [3]
		for (int i = 0; i < 12; i++) {
			this.addSlot(new FilterGhostSlot(this, i, -9999, -9999));
		}

		addDataSlots(data);
	}

	public @Nullable ItemTransferComponent getActiveFilterComponent() {
		return activeFilterComponent;
	}

	public void setActiveFilterComponent(@Nullable ItemTransferComponent comp) {
		this.activeFilterComponent = comp;
		for (int i = 0; i < 12; i++) {
			Slot slot = this.slots.get(36 + i);
			if (comp != null) {
				MenuSlotRepositioner.setSlotPosition(slot, 30 + i * 20 + 1, 295);
			} else {
				MenuSlotRepositioner.setSlotPosition(slot, -9999, -9999);
			}
		}
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		// Let super.clicked handle the transaction first so that it acknowledges State
		// IDs natively [3]
		super.clicked(slotId, button, clickType, player);

		// Intercept and apply custom ghost slot cloning/clearing after the transaction
		// completes [3]
		if (slotId >= 0 && slotId < this.slots.size()) {
			Slot targetSlot = this.slots.get(slotId);
			if (targetSlot instanceof FilterGhostSlot ghostSlot) {
				ItemStack carriedStack = this.getCarried();

				if (clickType == ClickType.PICKUP) {
					if (button == 0) { // Left-click: Clone item to slot [3]
						if (!carriedStack.isEmpty()) {
							ItemStack filterCopy = carriedStack.copyWithCount(1);
							ghostSlot.set(filterCopy);
						} else {
							ghostSlot.set(ItemStack.EMPTY);
							if (activeFilterComponent != null
									&& ghostSlot.getContainerSlot() < activeFilterComponent.getFilterLimits().size()) {
								activeFilterComponent.getFilterLimits().set(ghostSlot.getContainerSlot(), -1);
							}
						}

						// Broadcast the settings delta update to the client [3]
						if (activeFilterComponent != null) {
							CompoundTag tag = new CompoundTag();
							activeFilterComponent.saveData(tag);
							blockEntity.broadcastDeltaUpdate(new SyncComponentDeltaPacket(blockEntity.getBlockPos(),
									activeFilterComponent.getId(), SyncComponentDeltaPacket.DeltaType.SETTINGS, tag));
						}
					}
				}
				this.broadcastChanges();
			}
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			if (slot instanceof FilterGhostSlot) {
				slot.set(ItemStack.EMPTY);
				if (activeFilterComponent != null
						&& slot.getContainerSlot() < activeFilterComponent.getFilterLimits().size()) {
					activeFilterComponent.getFilterLimits().set(slot.getContainerSlot(), -1);

					// Broadcast the clear update to the client [3]
					CompoundTag tag = new CompoundTag();
					activeFilterComponent.saveData(tag);
					blockEntity.broadcastDeltaUpdate(new SyncComponentDeltaPacket(blockEntity.getBlockPos(),
							activeFilterComponent.getId(), SyncComponentDeltaPacket.DeltaType.SETTINGS, tag));
				}
				this.broadcastChanges();
				return ItemStack.EMPTY;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer,
				ModBlocks.MANAGER_BLOCK.get());
	}

	public int getCommandCount() {
		return this.data.get(0);
	}

	public ManagerBlockEntity getManagerBlockEntity() {
		return blockEntity;
	}
}