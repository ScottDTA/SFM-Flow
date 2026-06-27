package dta.sfmflow.block.entity;

import dta.sfmflow.block.ItemVacuumHatchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * Backing BlockEntity for the Item Vacuum Hatch block [3]. Vacuums items in a
 * 3x3x3 volume directly in front of it, respecting pickup delays [3].
 */
public class ItemVacuumHatchBlockEntity extends BlockEntity {

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

	/**
	 * Instantiates a new ItemVacuumHatchBlockEntity [3].
	 *
	 * @param pos   block coordinates [3]
	 * @param state block state properties [3]
	 */
	public ItemVacuumHatchBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ITEM_VACUUM_HATCH_BE.get(), pos, state);
	}

	/**
	 * Staggers tick processing every 10 ticks across different block positions [3].
	 *
	 * @param level level instance [3]
	 * @param pos   block coordinates [3]
	 * @param state block state properties [3]
	 * @param be    Hatch block entity [3]
	 */
	public static void tick(Level level, BlockPos pos, BlockState state, ItemVacuumHatchBlockEntity be) {
		if (level.isClientSide()) {
			return;
		}

		int tickOffset = Math.abs(pos.hashCode()) % 10;
		if ((level.getGameTime() + tickOffset) % 10 != 0) {
			return;
		}

		be.performVacuumIngestion(level, pos, state);
	}

	/**
	 * Vacuum-ingests items in a 3x3x3 area directly in front of the hatch [3].
	 * Respects standard pickup delays to prevent stealing active player drops [3].
	 *
	 * @param level level context [3]
	 * @param pos   block position [3]
	 * @param state block state properties [3]
	 */
	public void performVacuumIngestion(Level level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(ItemVacuumHatchBlock.FACING);
		BlockPos mouthPos = pos.relative(facing);

		// Construct a 3x3x3 suction boundary centered directly on the block in front of
		// the hatch [3]
		AABB suctionBox = new AABB(mouthPos).inflate(1.0);

		List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, suctionBox);
		for (ItemEntity itemEntity : entities) {
			if (itemEntity.isAlive() && !itemEntity.hasPickUpDelay()) {
				ItemStack stack = itemEntity.getItem();
				ItemStack remaining = ItemHandlerHelper.insertItemStacked(this.itemHandler, stack, false);
				if (remaining.isEmpty()) {
					itemEntity.discard();
				} else {
					itemEntity.setItem(remaining);
				}
				this.setChanged();
			}
		}
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