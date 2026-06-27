package dta.sfmflow.block.entity;

import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.registry.ModTags;
import dta.sfmflow.screen.CableClusterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Backing block entity logic running cluster card proxy sweeps, pre-allocated
 * directional capability routing, staggered logic executions, and adjacent pipe
 * cache flush notifications [3]. Upgraded to utilize 1.21.1 Data Components to
 * check card NBT data safely [3].
 */
public class CableClusterBlockEntity extends BlockEntity implements MenuProvider {
	private final int numSlots;
	private final ItemStackHandler inventory;
	private final Direction[] slotDirections;

	// Direct Pre-Allocated, face-locked capability routing proxies [3]
	private final IItemHandler[] directionalItemProxies = new IItemHandler[6];
	private final IFluidHandler[] directionalFluidProxies = new IFluidHandler[6];

	// Card-Specific Isolated Storage buffers [3]
	private final ItemStackHandler[] slotBuffers;
	private final FluidTank[] fluidBuffers;

	/**
	 * Instantiates the CableClusterBlockEntity and pre-allocates proxy handlers
	 * [3].
	 *
	 * @param pos   coordinate positions [3]
	 * @param state block behavior parameters [3]
	 */
	public CableClusterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CABLE_CLUSTER_BE.get(), pos, state);
		this.numSlots = state.is(ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get()) ? 18 : 9;

		this.inventory = new ItemStackHandler(numSlots) {
			@Override
			public boolean isItemValid(int slot, @NotNull ItemStack stack) {
				return stack.is(ModTags.CLUSTER_COMPATIBLE);
			}

			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				CableClusterBlockEntity.this.setChanged();
				CableClusterBlockEntity.this.notifyNeighbors();
			}
		};

		this.slotDirections = new Direction[numSlots];
		Arrays.fill(this.slotDirections, null); // Null/NONE unassigned by default

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

	/**
	 * Forces immediate cache invalidations across connected networks and pipes [3].
	 */
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

	/**
	 * Cycles the slot configuration's face routing, triggering adjacent pipeline
	 * flush notifications [3].
	 */
	public void setSlotDirection(int slot, int ordinal) {
		if (slot >= 0 && slot < numSlots) {
			Direction dir = (ordinal == -1 || ordinal < 0 || ordinal >= 6) ? null : Direction.values()[ordinal];
			if (this.slotDirections[slot] != dir) {
				this.slotDirections[slot] = dir;
				this.setChanged();
				this.notifyNeighbors();
			}
		}
	}

	public boolean isItemCard(int slot) {
		ItemStack stack = this.inventory.getStackInSlot(slot);
		if (stack.isEmpty())
			return false;
		return stack.is(ModBlocks.ITEM_VACUUM_HATCH_BLOCK.get().asItem())
				|| stack.is(ModBlocks.ITEM_EJECTOR_HATCH_BLOCK.get().asItem());
	}

	public boolean isFluidCard(int slot) {
		ItemStack stack = this.inventory.getStackInSlot(slot);
		if (stack.isEmpty())
			return false;
		return stack.is(ModBlocks.FLUID_HATCH_CABLE_BLOCK.get().asItem());
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

	/**
	 * Ticking loop scanning hardware items inside slots and executing modular sweep
	 * actions [3].
	 */
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
			if (stack.is(ModBlocks.ITEM_VACUUM_HATCH_BLOCK.get().asItem())) {
				be.tickVacuum(i, dir);
			} else if (stack.is(ModBlocks.ITEM_EJECTOR_HATCH_BLOCK.get().asItem())) {
				be.tickEjection(i, dir);
			} else if (stack.is(ModBlocks.FLUID_HATCH_CABLE_BLOCK.get().asItem())) {
				be.tickFluidHatch(i, dir);
			}
		}
	}

	private void tickVacuum(int slotIdx, Direction dir) {
		if ((this.level.getGameTime() + slotIdx) % 10 != 0) {
			return;
		}
		BlockPos targetPos = this.worldPosition.relative(dir);
		net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(targetPos).inflate(1.0);
		List<net.minecraft.world.entity.item.ItemEntity> items = this.level.getEntitiesOfClass(
				net.minecraft.world.entity.item.ItemEntity.class, area,
				item -> item.isAlive() && !item.hasPickUpDelay());
		for (net.minecraft.world.entity.item.ItemEntity item : items) {
			ItemStack stack = item.getItem();
			ItemStack remaining = this.slotBuffers[slotIdx].insertItem(0, stack, false);
			if (remaining.isEmpty()) {
				item.discard();
			} else {
				item.setItem(remaining);
			}
			this.setChanged();
		}
	}

	private void tickEjection(int slotIdx, Direction dir) {
		if ((this.level.getGameTime() + slotIdx) % 4 != 0) {
			return;
		}
		ItemStack stackInSlot = this.slotBuffers[slotIdx].getStackInSlot(0);
		if (stackInSlot.isEmpty()) {
			return;
		}
		BlockPos mouthPos = this.worldPosition.relative(dir);
		net.minecraft.world.phys.AABB mouthBox = new net.minecraft.world.phys.AABB(mouthPos);
		List<net.minecraft.world.entity.item.ItemEntity> existingEntities = this.level
				.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, mouthBox);
		if (existingEntities.size() >= 8) {
			return; // Crowding limit check [3]
		}
		int toExtract = Math.min(stackInSlot.getMaxStackSize(), stackInSlot.getCount());
		ItemStack ejectStack = this.slotBuffers[slotIdx].extractItem(0, toExtract, false);
		if (!ejectStack.isEmpty()) {
			double spawnX = mouthPos.getX() + 0.5;
			double spawnY = mouthPos.getY() + 0.5;
			double spawnZ = mouthPos.getZ() + 0.5;
			net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
					this.level, spawnX, spawnY, spawnZ, ejectStack);
			itemEntity.setDeltaMovement(dir.getStepX() * 0.2, dir.getStepY() * 0.2 + 0.1, dir.getStepZ() * 0.2);
			this.level.addFreshEntity(itemEntity);
			this.setChanged();
		}
	}

	private void tickFluidHatch(int slotIdx, Direction dir) {
		if ((this.level.getGameTime() + slotIdx) % 10 != 0) {
			return;
		}
		ItemStack stack = this.inventory.getStackInSlot(slotIdx);
		String modeStr = "vacuum";

		// Upgraded component checking: query 1.21.1 Custom Data tags [3]
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (tag.contains("mode")) {
			modeStr = tag.getString("mode");
		}

		BlockPos mouthPos = this.worldPosition.relative(dir);
		if ("vacuum".equals(modeStr)) {
			net.minecraft.world.level.material.FluidState fluidState = this.level.getFluidState(mouthPos);
			if (fluidState.isSource()) {
				net.minecraft.world.level.material.Fluid fluid = fluidState.getType();
				net.neoforged.neoforge.fluids.FluidStack sample = new net.neoforged.neoforge.fluids.FluidStack(fluid,
						1000);
				int accepted = this.fluidBuffers[slotIdx].fill(sample, IFluidHandler.FluidAction.SIMULATE);
				if (accepted == 1000) {
					this.fluidBuffers[slotIdx].fill(sample, IFluidHandler.FluidAction.EXECUTE);
					this.level.setBlock(mouthPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
					this.setChanged();
				}
			}
		} else {
			FluidStack stored = this.fluidBuffers[slotIdx].getFluid();
			if (stored.getAmount() >= 1000) {
				BlockState mouthState = this.level.getBlockState(mouthPos);
				if (mouthState.isAir() || mouthState.canBeReplaced(stored.getFluid())) {
					BlockState fluidBlockState = stored.getFluid().defaultFluidState().createLegacyBlock();
					if (!fluidBlockState.isAir()) {
						this.level.setBlock(mouthPos, fluidBlockState, 3);
						this.fluidBuffers[slotIdx].drain(1000, IFluidHandler.FluidAction.EXECUTE);
						this.setChanged();
					}
				}
			}
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
			for (int i = 0; i < this.numSlots; i++) {
				if (i < fluidBuffersList.size()) {
					this.fluidBuffers[i].readFromNBT(registries, fluidBuffersList.getCompound(i));
				}
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
			if (stack.isEmpty())
				return ItemStack.EMPTY;
			ItemStack remaining = stack.copy();
			for (int i = 0; i < parent.numSlots; i++) {
				if (parent.slotDirections[i] == direction && parent.isItemCard(i)) {
					remaining = parent.slotBuffers[i].insertItem(0, remaining, simulate);
					if (remaining.isEmpty()) {
						break;
					}
				}
			}
			return remaining;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (amount <= 0)
				return ItemStack.EMPTY;
			for (int i = 0; i < parent.numSlots; i++) {
				if (parent.slotDirections[i] == direction && parent.isItemCard(i)) {
					ItemStack extracted = parent.slotBuffers[i].extractItem(0, amount, simulate);
					if (!extracted.isEmpty()) {
						return extracted;
					}
				}
			}
			return ItemStack.EMPTY;
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
}