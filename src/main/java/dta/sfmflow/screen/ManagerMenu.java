package dta.sfmflow.screen;

import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.block.entity.FilterGhostSlot;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.item.ModItems;
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
 * The logical container menu for the Manager block interface. Imposes absolute
 * security lockdowns on virtual variable items, tracks item drag origins, and
 * handles dynamic cursor snap-backs cleanly on ghost slot configurations [3].
 */
public class ManagerMenu extends AbstractContainerMenu {
	private final ManagerBlockEntity blockEntity;
	private final Level level;
	private final ContainerData data;

	private @Nullable ItemTransferComponent activeFilterComponent = null;
	private @Nullable AbstractFlowComponent activeComponent = null;

	// Transiently track which inventory slot ID the current cursor drag originated
	// from [3]
	private transient int lastPickedUpSlotId = -1;

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

		// Security: Override player slots to reject placement of virtual variable cards
		// [3]
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				this.addSlot(new Slot(pInv, c + r * 9 + 9, 175 + c * 18, 266 + r * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return !stack.is(ModItems.VARIABLE_CARD.get());
					}
				});
			}
		}

		for (int c = 0; c < 9; c++) {
			this.addSlot(new Slot(pInv, c, 175 + c * 18, 324) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return !stack.is(ModItems.VARIABLE_CARD.get());
				}
			});
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

	public @Nullable AbstractFlowComponent getActiveComponent() {
		return activeComponent;
	}

	public void setActiveFilterComponent(@Nullable ItemTransferComponent comp) {
		setActiveComponent(comp);
	}

	public void setActiveComponent(@Nullable AbstractFlowComponent comp) {
		this.activeComponent = comp;
		this.activeFilterComponent = comp instanceof ItemTransferComponent transfer ? transfer : null;

		int count = comp instanceof IGhostSlotAware aware ? aware.getGhostSlotCount() : 0;

		for (int i = 0; i < 12; i++) {
			Slot slot = this.slots.get(36 + i);
			MenuSlotRepositioner.setSlotPosition(slot, -9999, -9999);
		}
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		// 1. Intercept drops outside the screen to silently delete variable cards [3]
		if (slotId == -999) {
			ItemStack carried = this.getCarried();
			if (carried.is(ModItems.VARIABLE_CARD.get())) {
				this.setCarried(ItemStack.EMPTY); // Silently destroy virtual card to prevent drop exploits [3]
				this.broadcastChanges();
				return;
			}
		}

		// 2. Track original slot index when picking up any real item from player
		// inventory [3]
		if (slotId >= 0 && slotId < 36) {
			if (clickType == ClickType.PICKUP && this.getCarried().isEmpty()) {
				Slot slot = this.slots.get(slotId);
				if (slot.hasItem()) {
					this.lastPickedUpSlotId = slotId; // Record item source coordinate [3]
				}
			}
		}

		// 3. Absolute Security Shield: If carrying a virtual card, reject any click on
		// a non-ghost slot entirely [3]
		if (slotId >= 0 && slotId < this.slots.size()) {
			Slot targetSlot = this.slots.get(slotId);
			if (!(targetSlot instanceof FilterGhostSlot)) {
				ItemStack carried = this.getCarried();
				boolean isHotbarSwapVariable = clickType == ClickType.SWAP
						&& player.getInventory().getItem(button).is(ModItems.VARIABLE_CARD.get());

				if (carried.is(ModItems.VARIABLE_CARD.get()) || isHotbarSwapVariable) {
					this.broadcastChanges();
					return; // Completely block and cancel transaction [3]
				}
			}
		}

		// 4. Intercept and bypass standard clicked validations for FilterGhostSlots [3]
		if (slotId >= 0 && slotId < this.slots.size()) {
			Slot targetSlot = this.slots.get(slotId);
			if (targetSlot instanceof FilterGhostSlot ghostSlot) {
				ItemStack carriedStack = this.getCarried();

				if (clickType == ClickType.PICKUP) {
					if (button == 0) { // Left-click: Clone item to slot [3]
						if (!carriedStack.isEmpty()) {
							ItemStack filterCopy = carriedStack.copyWithCount(1);
							ghostSlot.set(filterCopy);

							// Symmetrical Cursor Clearing & Snap-back: return stack back to its original
							// slot index [3]
							if (carriedStack.is(ModItems.VARIABLE_CARD.get())) {
								this.setCarried(ItemStack.EMPTY); // Variables just dissolve silently
							} else {
								ItemStack returnStack = carriedStack.copy();
								this.setCarried(ItemStack.EMPTY); // Clear the mouse cursor first [3]

								// Symmetrically return items back to the origin slot [3]
								if (lastPickedUpSlotId >= 0 && lastPickedUpSlotId < 36) {
									Slot originalSlot = this.slots.get(lastPickedUpSlotId);
									if (!originalSlot.hasItem()) {
										originalSlot.set(returnStack);
									} else {
										// Secure fallback: place item back anywhere in player inventory safely [3]
										player.getInventory().placeItemBackInInventory(returnStack);
									}
								} else {
									player.getInventory().placeItemBackInInventory(returnStack);
								}
								this.lastPickedUpSlotId = -1; // Reset tracking index [3]
							}
						} else {
							ghostSlot.set(ItemStack.EMPTY);
						}

						if (activeComponent != null) {
							CompoundTag tag = new CompoundTag();
							activeComponent.saveData(tag);
							blockEntity.broadcastDeltaUpdate(new SyncComponentDeltaPacket(blockEntity.getBlockPos(),
									activeComponent.getId(), SyncComponentDeltaPacket.DeltaType.SETTINGS, tag));
						}
					}
				}
				this.broadcastChanges();
				return; // Bypass super.clicked to prevent vanilla transaction mismatches [3]
			}
		}

		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public void removed(Player player) {
		ItemStack carried = this.getCarried();
		// Security check: destroy the card if the menu closes while carrying it [3]
		if (carried.is(ModItems.VARIABLE_CARD.get())) {
			this.setCarried(ItemStack.EMPTY);
		}
		super.removed(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			if (slot instanceof FilterGhostSlot) {
				slot.set(ItemStack.EMPTY);
				if (activeComponent != null) {
					CompoundTag tag = new CompoundTag();
					activeComponent.saveData(tag);
					blockEntity.broadcastDeltaUpdate(new SyncComponentDeltaPacket(blockEntity.getBlockPos(),
							activeComponent.getId(), SyncComponentDeltaPacket.DeltaType.SETTINGS, tag));
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
