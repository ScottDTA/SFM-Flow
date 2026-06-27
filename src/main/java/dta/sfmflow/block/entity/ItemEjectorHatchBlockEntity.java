package dta.sfmflow.block.entity;

import dta.sfmflow.block.ItemEjectorHatchBlock;
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
 * Backing BlockEntity for the Item Ejector Hatch block [3].
 * Ejects item stacks up to 64 items every 4 ticks, checking for item entity overcrowding in front of it [3].
 */
public class ItemEjectorHatchBlockEntity extends BlockEntity {

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
     * Instantiates a new ItemEjectorHatchBlockEntity [3].
     *
     * @param pos block coordinates [3]
     * @param state block state properties [3]
     */
    public ItemEjectorHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_EJECTOR_HATCH_BE.get(), pos, state);
    }

    /**
     * Staggers tick processing every 4 ticks across different block positions [3].
     *
     * @param level level instance [3]
     * @param pos block coordinates [3]
     * @param state block state properties [3]
     * @param be Hatch block entity [3]
     */
    public static void tick(Level level, BlockPos pos, BlockState state, ItemEjectorHatchBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }

        int tickOffset = Math.abs(pos.hashCode()) % 4;
        if ((level.getGameTime() + tickOffset) % 4 != 0) {
            return;
        }

        be.performEjection(level, pos, state);
    }

    /**
     * Spawns a new item entity in front of the hatch unless too many item entities are already present [3].
     *
     * @param level level context [3]
     * @param pos block position [3]
     * @param state block state properties [3]
     */
    public void performEjection(Level level, BlockPos pos, BlockState state) {
        ItemStack stackInSlot = this.itemHandler.getStackInSlot(0);
        if (stackInSlot.isEmpty()) {
            return;
        }

        Direction facing = state.getValue(ItemEjectorHatchBlock.FACING);
        BlockPos mouthPos = pos.relative(facing);
        AABB mouthBox = new AABB(mouthPos);

        // Crowding check: Cancel ejection if there are 8 or more item stacks already floating in front of it [3]
        List<ItemEntity> existingEntities = level.getEntitiesOfClass(ItemEntity.class, mouthBox);
        if (existingEntities.size() >= 8) {
            return;
        }

        int toExtract = Math.min(stackInSlot.getMaxStackSize(), stackInSlot.getCount());
        ItemStack ejectStack = this.itemHandler.extractItem(0, toExtract, false);

        if (!ejectStack.isEmpty()) {
            double spawnX = mouthPos.getX() + 0.5;
            double spawnY = mouthPos.getY() + 0.5;
            double spawnZ = mouthPos.getZ() + 0.5;

            ItemEntity itemEntity = new ItemEntity(level, spawnX, spawnY, spawnZ, ejectStack);
            
            double velX = facing.getStepX() * 0.2;
            double velY = facing.getStepY() * 0.2 + 0.1;
            double velZ = facing.getStepZ() * 0.2;
            itemEntity.setDeltaMovement(velX, velY, velZ);

            level.addFreshEntity(itemEntity);
            this.setChanged();
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