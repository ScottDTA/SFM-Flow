package dta.sfmflow.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.item.ItemStack;

/**
 * Backing BlockEntity for the Item Vacuum Hatch block.
 * Vacuums items only when the connected flowchart schedules and executes a transfer task.
 */
public class ItemVacuumValveBlockEntity extends BlockEntity {

	private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.getCount() <= 64;
		}
	};

	public ItemVacuumValveBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ITEM_VACUUM_HATCH_BE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ItemVacuumValveBlockEntity be) {
		// Standalone Vacuum Valve remains completely passive; items are scanned as ground snapshots during flowchart planning
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("Inventory", this.itemHandler.serializeNBT(registries));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("Inventory")) {
			this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
		}
	}

	public IItemHandler getItemHandler(Direction side) {
		return this.itemHandler;
	}
}