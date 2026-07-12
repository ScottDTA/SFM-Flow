package dta.sfmflow.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * Common, stateless helper consolidating vacuum, ejection, and fluid hatch
 * execution logic.
 */
public final class HatchBehaviorHelper {

	private HatchBehaviorHelper() {
	}

	/**
	 * Suctions ground items in a 3x3x3 volume centered on the mouth position into
	 * the target item handler.
	 *
	 * @param level       the world level context
	 * @param pos         the base position of the block performing the action
	 * @param facing      the direction the hatch face is looking
	 * @param itemHandler the destination capability item handler
	 * @param onChange    the callback run to mark changes and stamp dirtiness
	 */
	public static void performVacuum(Level level, BlockPos pos, Direction facing, IItemHandler itemHandler,
			Runnable onChange) {
		BlockPos mouthPos = pos.relative(facing);
		AABB suctionBox = new AABB(mouthPos).inflate(1.0);

		List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, suctionBox);
		boolean changed = false;
		for (ItemEntity itemEntity : entities) {
			if (itemEntity.isAlive() && !itemEntity.hasPickUpDelay()) {
				ItemStack stack = itemEntity.getItem();
				ItemStack remaining = ItemHandlerHelper.insertItemStacked(itemHandler, stack, false);
				if (remaining.isEmpty()) {
					itemEntity.discard();
					changed = true;
				} else if (remaining.getCount() != stack.getCount()) {
					itemEntity.setItem(remaining);
					changed = true;
				}
			}
		}
		if (changed) {
			onChange.run();
		}
	}

	/**
	 * Ejects item stacks out of the first available slot of the target item handler
	 * as floating entities.
	 *
	 * @param level       the world level context
	 * @param pos         the base position of the block performing the action
	 * @param facing      the direction the hatch face is looking
	 * @param itemHandler the source capability item handler
	 * @param onChange    the callback run to mark changes and stamp dirtiness
	 */
	public static void performEjection(Level level, BlockPos pos, Direction facing, IItemHandler itemHandler,
			Runnable onChange) {
		ItemStack stackInSlot = itemHandler.getStackInSlot(0);
		if (stackInSlot.isEmpty()) {
			return;
		}

		BlockPos mouthPos = pos.relative(facing);
		AABB mouthBox = new AABB(mouthPos);

		List<ItemEntity> existingEntities = level.getEntitiesOfClass(ItemEntity.class, mouthBox);
		if (existingEntities.size() >= 8) {
			return;
		}

		int toExtract = Math.min(stackInSlot.getMaxStackSize(), stackInSlot.getCount());
		ItemStack ejectStack = itemHandler.extractItem(0, toExtract, false);

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
			onChange.run();
		}
	}

	/**
	 * Ingests adjacent fluid sources and fills the target fluid handler.
	 *
	 * @param level        the world level context
	 * @param pos          the base position of the block performing the action
	 * @param facing       the direction the hatch face is looking
	 * @param fluidHandler the target capability fluid handler
	 * @param onChange     the callback run to mark changes and stamp dirtiness
	 */
	public static void performFluidVacuum(Level level, BlockPos pos, Direction facing, IFluidHandler fluidHandler,
			Runnable onChange) {
		BlockPos mouthPos = pos.relative(facing);
		FluidState fluidState = level.getFluidState(mouthPos);

		if (fluidState.isSource()) {
			Fluid fluid = fluidState.getType();
			FluidStack sample = new FluidStack(fluid, 1000);
			int accepted = fluidHandler.fill(sample, IFluidHandler.FluidAction.SIMULATE);

			if (accepted == 1000) {
				fluidHandler.fill(sample, IFluidHandler.FluidAction.EXECUTE);
				level.setBlock(mouthPos, Blocks.AIR.defaultBlockState(), 3);
				onChange.run();
			}
		}
	}

	/**
	 * Ejects fluid from the target fluid handler to place as source blocks in the
	 * world.
	 *
	 * @param level        the world level context
	 * @param pos          the base position of the block performing the action
	 * @param facing       the direction the hatch face is looking
	 * @param fluidHandler the source capability fluid handler
	 * @param onChange     the callback run to mark changes and stamp dirtiness
	 */
	public static void performFluidEjection(Level level, BlockPos pos, Direction facing, IFluidHandler fluidHandler,
			Runnable onChange) {
		FluidStack stored = fluidHandler.getFluidInTank(0);
		if (stored.getAmount() >= 1000) {
			BlockPos mouthPos = pos.relative(facing);
			BlockState mouthState = level.getBlockState(mouthPos);

			if (mouthState.isAir() || mouthState.canBeReplaced(stored.getFluid())) {
				BlockState fluidBlockState = stored.getFluid().defaultFluidState().createLegacyBlock();

				if (!fluidBlockState.isAir()) {
					level.setBlock(mouthPos, fluidBlockState, 3);
					fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
					onChange.run();
				}
			}
		}
	}
}