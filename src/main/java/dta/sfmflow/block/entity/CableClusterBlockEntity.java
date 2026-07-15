package dta.sfmflow.block.entity;

import dta.sfmflow.api.capability.CableClusterBehaviorRegistry;
import dta.sfmflow.api.capability.ClusterCardBehavior;
import dta.sfmflow.block.CableBlock;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.registry.ModTags;
import dta.sfmflow.screen.CableClusterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Backing BlockEntity logic running cluster card proxy sweeps, pre-allocated
 * directional capability routing, staggered logic executions, and adjacent pipe
 * cache flush notifications.
 */
public class CableClusterBlockEntity extends BlockEntity implements MenuProvider {
	private final int numSlots;
	private final ItemStackHandler inventory;
	private final Direction[] slotDirections;

	// Direct Pre-Allocated, face-locked capability routing proxies
	private final IItemHandler[] directionalItemProxies = new IItemHandler[6];
	private final IFluidHandler[] directionalFluidProxies = new IFluidHandler[6];

	// Card-Specific Isolated Storage buffers
	private final ItemStackHandler[] slotBuffers;
	private final FluidTank[] fluidBuffers;

	public CableClusterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CABLE_CLUSTER_BE.get(), pos, state);
		this.numSlots = state.is(ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get()) ? 18 : 9;

		this.inventory = new ItemStackHandler(numSlots) {
			@Override
			public boolean isItemValid(int slot, @NotNull ItemStack stack) {
				if (!stack.is(ModTags.CLUSTER_COMPATIBLE)) {
					return false;
				}
				return isCardInsertionValid(slot, stack);
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}

			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				ItemStack stack = getStackInSlot(slot);
				if (stack.isEmpty()) {
					CableClusterBlockEntity.this.slotDirections[slot] = null;
				} else {
					Direction dir = CableClusterBlockEntity.this.slotDirections[slot];
					if (dir != null && !isDirectionValid(slot, dir)) {
						CableClusterBlockEntity.this.slotDirections[slot] = null;
					}
				}
				CableClusterBlockEntity.this.setChanged();
				CableClusterBlockEntity.this.notifyNeighbors();

				// Flag the nearby cable networks as dirty to force a topology rescan
				if (CableClusterBlockEntity.this.level != null && !CableClusterBlockEntity.this.level.isClientSide()) {
					CableBlock.markNearbyNetworksDirty(CableClusterBlockEntity.this.level,
							CableClusterBlockEntity.this.worldPosition);
				}
			}
		};

		this.slotDirections = new Direction[numSlots];
		Arrays.fill(this.slotDirections, null);

		this.slotBuffers = new ItemStackHandler[numSlots];
		this.fluidBuffers = new FluidTank[numSlots];

		for (int i = 0; i < numSlots; i++) {
			this.slotBuffers[i] = new ItemStackHandler(1) {
				@Override
				protected void onContentsChanged(int slot) {
					CableClusterBlockEntity.this.setChanged();
				}
			};
			this.fluidBuffers[i] = new FluidTank(8000) {
				@Override
				protected void onContentsChanged() {
					CableClusterBlockEntity.this.setChanged();
				}
			};
		}

		for (Direction dir : Direction.values()) {
			this.directionalItemProxies[dir.ordinal()] = new ClusterItemHandlerProxy(this, dir);
			this.directionalFluidProxies[dir.ordinal()] = new ClusterFluidHandlerProxy(this, dir);
		}
	}

	public void notifyNeighbors() {
		if (this.level != null && !this.level.isClientSide()) {
			this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
		}
	}

	public int getNumSlots() {
		return numSlots;
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	@Nullable
	public Direction getSlotDirection(int slot) {
		if (slot >= 0 && slot < numSlots) {
			return slotDirections[slot];
		}
		return null;
	}

	public void setSlotDirection(int slot, int ordinal) {
		if (slot >= 0 && slot < numSlots) {
			Direction dir = (ordinal == -1 || ordinal < 0 || ordinal >= 6) ? null : Direction.values()[ordinal];
			if (isDirectionValid(slot, dir)) {
				if (this.slotDirections[slot] != dir) {
					this.slotDirections[slot] = dir;
					this.setChanged();
					this.notifyNeighbors();

					// Flag the nearby cable networks as dirty to force a topology rescan
					if (this.level != null && !this.level.isClientSide()) {
						CableBlock.markNearbyNetworksDirty(this.level, this.worldPosition);
					}
				}
			}
		}
	}

	/**
	 * Determines if a specific directional value is valid and unconflicted for a
	 * slot card.
	 */
	public boolean isDirectionValid(int slot, @Nullable Direction dir) {
		if (dir == null)
			return true;
		ItemStack stack = inventory.getStackInSlot(slot);
		if (stack.isEmpty())
			return true;

		// Query extensible Tag lists to support third-party addon directional cards
		// natively
		if (stack.is(ModTags.DIRECTIONAL_CLUSTER_CARDS)) {
			Item item = stack.getItem();
			for (int i = 0; i < numSlots; i++) {
				if (i != slot) {
					ItemStack s = inventory.getStackInSlot(i);
					if (!s.isEmpty() && s.is(item) && slotDirections[i] == dir) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Evaluates if a card insertion satisfies standard cables, omni, and valve
	 * capacity rules. Querying extensible ModTags guarantees full compatibility
	 * with addon cards.
	 */
	public boolean isCardInsertionValid(int slot, ItemStack stack) {
		if (stack.isEmpty())
			return true;
		Item item = stack.getItem();

		// 1. Standard or Hardened Cable: maximum of 1 of either allowed total
		boolean isCable = (stack.is(ModBlocks.CABLE_BLOCK.get().asItem())
				|| stack.is(ModBlocks.HARDENED_CABLE_BLOCK.get().asItem()));
		if (isCable) {
			for (int i = 0; i < numSlots; i++) {
				if (i != slot) {
					ItemStack s = inventory.getStackInSlot(i);
					if (!s.isEmpty() && (s.is(ModBlocks.CABLE_BLOCK.get().asItem())
							|| s.is(ModBlocks.HARDENED_CABLE_BLOCK.get().asItem()))) {
						return false;
					}
				}
			}
		}

		// 2. Omni-Directional cables: maximum of 1 of each type allowed
		if (stack.is(ModTags.OMNI_DIRECTIONAL_CLUSTER_CARDS)) {
			for (int i = 0; i < numSlots; i++) {
				if (i != slot) {
					ItemStack s = inventory.getStackInSlot(i);
					if (!s.isEmpty() && s.is(item)) {
						return false;
					}
				}
			}
		}

		// 3. Directional cables: maximum of 6 of each individual type allowed
		if (stack.is(ModTags.DIRECTIONAL_CLUSTER_CARDS)) {
			int count = 0;
			for (int i = 0; i < numSlots; i++) {
				if (i != slot) {
					ItemStack s = inventory.getStackInSlot(i);
					if (!s.isEmpty() && s.is(item)) {
						count++;
					}
				}
			}
			if (count >= 6) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Determines if the network is allowed to route/traverse through this cluster
	 * block.
	 */
	public boolean hasCableCard() {
		for (int i = 0; i < numSlots; i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty() && (stack.is(ModBlocks.CABLE_BLOCK.get().asItem())
					|| stack.is(ModBlocks.HARDENED_CABLE_BLOCK.get().asItem()))) {
				return true;
			}
		}
		return false;
	}

	public boolean isItemCard(int slot) {
		ItemStack stack = this.inventory.getStackInSlot(slot);
		if (stack.isEmpty())
			return false;
		return stack.is(ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get().asItem())
				|| stack.is(ModBlocks.ITEM_EJECTOR_VALVE_BLOCK.get().asItem());
	}

	public boolean isFluidCard(int slot) {
		ItemStack stack = this.inventory.getStackInSlot(slot);
		if (stack.isEmpty())
			return false;
		return stack.is(ModBlocks.FLUID_EJECTOR_VALVE_BLOCK.get().asItem())
				|| stack.is(ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get().asItem());
	}

	public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
		if (side == null) {
			return this.inventory;
		}
		return this.directionalItemProxies[side.ordinal()];
	}

	public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (side == null) {
			return null;
		}
		return this.directionalFluidProxies[side.ordinal()];
	}

	public ItemStackHandler getSlotBuffer(int slot) {
		return this.slotBuffers[slot];
	}

	public FluidTank getFluidBuffer(int slot) {
		return this.fluidBuffers[slot];
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CableClusterBlockEntity be) {
		if (level.isClientSide()) {
			return;
		}
		for (int i = 0; i < be.numSlots; i++) {
			ItemStack stack = be.inventory.getStackInSlot(i);
			if (stack.isEmpty()) {
				continue;
			}
			Direction dir = be.slotDirections[i];
			if (dir == null) {
				continue;
			}
			// Delegate custom ticking to the extensible behavior registry
			ClusterCardBehavior behavior = CableClusterBehaviorRegistry.get(stack.getItem());
			if (behavior != null) {
				behavior.tick(level, pos, dir, i, stack, be);
			}
		}
	}

	@Override
	public void setChanged() {
		super.setChanged();
		// Force server-to-client block entity sync whenever properties, directions, or
		// items modify
		if (this.level != null && !this.level.isClientSide()) {
			BlockState state = this.getBlockState();
			this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("Inventory", this.inventory.serializeNBT(registries));

		int[] dirOrdinals = new int[this.numSlots];
		for (int i = 0; i < this.numSlots; i++) {
			dirOrdinals[i] = (this.slotDirections[i] == null) ? -1 : this.slotDirections[i].ordinal();
		}
		tag.putIntArray("SlotDirections", dirOrdinals);

		ListTag itemBuffersList = new ListTag();
		for (int i = 0; i < this.numSlots; i++) {
			itemBuffersList.add(this.slotBuffers[i].serializeNBT(registries));
		}
		tag.put("ItemBuffers", itemBuffersList);

		ListTag fluidBuffersList = new ListTag();
		for (int i = 0; i < this.numSlots; i++) {
			CompoundTag fluidTag = new CompoundTag();
			this.fluidBuffers[i].writeToNBT(registries, fluidTag);
			fluidBuffersList.add(fluidTag);
		}
		tag.put("FluidBuffers", fluidBuffersList);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("Inventory")) {
			this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
		}
		if (tag.contains("SlotDirections")) {
			int[] dirOrdinals = tag.getIntArray("SlotDirections");
			for (int i = 0; i < this.numSlots; i++) {
				if (i < dirOrdinals.length) {
					int ord = dirOrdinals[i];
					this.slotDirections[i] = (ord == -1 || ord < 0 || ord >= 6) ? null : Direction.values()[ord];
				} else {
					this.slotDirections[i] = null;
				}
			}
		}
		if (tag.contains("ItemBuffers")) {
			ListTag itemBuffersList = tag.getList("ItemBuffers", Tag.TAG_COMPOUND);
			for (int i = 0; i < this.numSlots; i++) {
				if (i < itemBuffersList.size()) {
					this.slotBuffers[i].deserializeNBT(registries, itemBuffersList.getCompound(i));
				}
			}
		}
		if (tag.contains("FluidBuffers")) {
			ListTag fluidBuffersList = tag.getList("FluidBuffers", Tag.TAG_COMPOUND);
			for (int i = 0; i < fluidBuffersList.size() && i < this.numSlots; i++) {
				this.fluidBuffers[i].readFromNBT(registries, fluidBuffersList.getCompound(i));
			}
		}
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new CableClusterMenu(containerId, playerInventory, this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().is(ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get())
				? "container.sfmflow.advanced_cable_cluster"
				: "container.sfmflow.cable_cluster");
	}

	private static class ClusterItemHandlerProxy implements IItemHandler {
		private final CableClusterBlockEntity parent;
		private final Direction direction;

		public ClusterItemHandlerProxy(CableClusterBlockEntity parent, Direction direction) {
			this.parent = parent;
			this.direction = direction;
		}

		@Override
		public int getSlots() {
			return parent.numSlots;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			if (slot >= 0 && slot < parent.numSlots && parent.slotDirections[slot] == direction
					&& parent.isItemCard(slot)) {
				return parent.slotBuffers[slot].getStackInSlot(0);
			}
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			return parent.slotBuffers[slot].insertItem(0, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (amount <= 0) {
				return ItemStack.EMPTY;
			}
			return parent.slotBuffers[slot].extractItem(0, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			if (slot >= 0 && slot < parent.numSlots && parent.slotDirections[slot] == direction
					&& parent.isItemCard(slot)) {
				return parent.slotBuffers[slot].getSlotLimit(0);
			}
			return 0;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (slot >= 0 && slot < parent.numSlots && parent.slotDirections[slot] == direction
					&& parent.isItemCard(slot)) {
				return parent.slotBuffers[slot].isItemValid(0, stack);
			}
			return false;
		}
	}

	private static class ClusterFluidHandlerProxy implements IFluidHandler {
		private final CableClusterBlockEntity parent;
		private final Direction direction;

		public ClusterFluidHandlerProxy(CableClusterBlockEntity parent, Direction direction) {
			this.parent = parent;
			this.direction = direction;
		}

		@Override
		public int getTanks() {
			return parent.numSlots;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			if (tank >= 0 && tank < parent.numSlots && parent.slotDirections[tank] == direction
					&& parent.isFluidCard(tank)) {
				return parent.fluidBuffers[tank].getFluid();
			}
			return FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			if (tank >= 0 && tank < parent.numSlots && parent.slotDirections[tank] == direction
					&& parent.isFluidCard(tank)) {
				return parent.fluidBuffers[tank].getCapacity();
			}
			return 0;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			if (tank >= 0 && tank < parent.numSlots && parent.slotDirections[tank] == direction
					&& parent.isFluidCard(tank)) {
				return parent.fluidBuffers[tank].isFluidValid(stack);
			}
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty())
				return 0;
			FluidStack remaining = resource.copy();
			int totalFilled = 0;
			for (int i = 0; i < parent.numSlots; i++) {
				if (parent.slotDirections[i] == direction && parent.isFluidCard(i)) {
					int filled = parent.fluidBuffers[i].fill(remaining, action);
					totalFilled += filled;
					remaining.shrink(filled);
					if (remaining.isEmpty()) {
						break;
					}
				}
			}
			return totalFilled;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (resource.isEmpty())
				return FluidStack.EMPTY;
			for (int i = 0; i < parent.numSlots; i++) {
				if (parent.slotDirections[i] == direction && parent.isFluidCard(i)) {
					FluidStack drained = parent.fluidBuffers[i].drain(resource, action);
					if (!drained.isEmpty()) {
						return drained;
					}
				}
			}
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			if (maxDrain <= 0)
				return FluidStack.EMPTY;
			for (int i = 0; i < parent.numSlots; i++) {
				if (parent.slotDirections[i] == direction && parent.isFluidCard(i)) {
					FluidStack drained = parent.fluidBuffers[i].drain(maxDrain, action);
					if (!drained.isEmpty()) {
						return drained;
					}
				}
			}
			return FluidStack.EMPTY;
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		this.saveAdditional(tag, registries);
		return tag;
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
			HolderLookup.Provider registries) {
		super.onDataPacket(connection, packet, registries);
	}
}