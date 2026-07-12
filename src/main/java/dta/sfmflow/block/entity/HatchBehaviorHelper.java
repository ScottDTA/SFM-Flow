package dta.sfmflow.block.entity;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.util.ConnectionBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.ArrayDeque;

/**
 * Common, stateless helper consolidating vacuum, ejection, and fluid hatch
 * execution logic [3].
 */
public final class HatchBehaviorHelper {

	private HatchBehaviorHelper() {
	}

	/**
	 * Suctions ground items in a 3x3x3 volume centered on the mouth position into
	 * the target item handler.
	 *
	 * @param level       the world level context [3]
	 * @param pos         the base position of the block performing the action [3]
	 * @param facing      the direction the hatch face is looking [3]
	 * @param itemHandler the destination capability item handler [3]
	 * @param onChange    the callback run to mark changes and stamp dirtiness [3]
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

				// Restrict suctioning to valid, connected flowchart outputs with available space [3]
				if (!hasFlowchartSpaceAndMatch(level, pos, stack)) {
					continue;
				}

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
	 * as floating entities [3].
	 *
	 * @param level       the world level context [3]
	 * @param pos         the base position of the block performing the action [3]
	 * @param facing      the direction the hatch face is looking [3]
	 * @param itemHandler the source capability item handler [3]
	 * @param onChange    the callback run to mark changes and stamp dirtiness [3]
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
	 * Ingests adjacent fluid sources and fills the target fluid handler [3].
	 *
	 * @param level        the world level context [3]
	 * @param pos          the base position of the block performing the action [3]
	 * @param facing       the direction the hatch face is looking [3]
	 * @param fluidHandler the target capability fluid handler [3]
	 * @param onChange     the callback run to mark changes and stamp dirtiness [3]
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
	 * world [3].
	 *
	 * @param level        the world level context [3]
	 * @param pos          the base position of the block performing the action [3]
	 * @param facing       the direction the hatch face is looking [3]
	 * @param fluidHandler the source capability fluid handler [3]
	 * @param onChange     the callback run to mark changes and stamp dirtiness [3]
	 */
	public static void performFluidEjection(Level level, BlockPos pos, Direction facing, IFluidHandler fluidHandler,
			Runnable onChange) {
		FluidStack stored = fluidHandler.getFluidInTank(0);
		if (stored.getAmount() >= 1000) {
			BlockPos mouthPos = pos.relative(facing);
			BlockState mouthState = level.getBlockState(mouthPos);
			FluidState fluidState = level.getFluidState(mouthPos);

			// Prevent placing the fluid if the target location is already a fluid source block [3]
			if (!fluidState.isSource() && (mouthState.isAir() || mouthState.canBeReplaced(stored.getFluid()))) {
				BlockState fluidBlockState = stored.getFluid().defaultFluidState().createLegacyBlock();

				if (!fluidBlockState.isAir()) {
					level.setBlock(mouthPos, fluidBlockState, 3);
					fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
					onChange.run();
				}
			}
		}
	}

	/**
	 * Scans the connected physical network for any target inventories that have space to put the stack [3].
	 */
	private static boolean hasFlowchartSpaceAndMatch(Level level, BlockPos valvePos, ItemStack stack) {
		BlockPos controllerPos = dta.sfmflow.common.network.CableNetworkRegistry.getController(level, valvePos);
		if (controllerPos == null) {
			return false; // Not connected to any network [3]
		}
		BlockEntity managerBe = level.getBlockEntity(controllerPos);
		if (managerBe instanceof ManagerBlockEntity manager) {
			int valveId = valvePos.hashCode(); // ConnectionBlock ID is pos.hashCode() [3]

			for (AbstractFlowComponent comp : manager.getFlowComponents().values()) {
				if (comp instanceof ItemTransferComponent inputComp && inputComp.isInput() && inputComp.getInventoryId() == valveId) {
					// 1. Check if the item matches the Input Card's filter criteria [3]
					if (!matchesFilterCommon(manager, inputComp, stack)) {
						continue;
					}

					// 2. Traverse downstream to locate connected outputs [3]
					List<ItemTransferComponent> outputs = findDownstreamOutputs(manager, inputComp.getId());
					for (ItemTransferComponent outputComp : outputs) {
						// 3. Check if the item matches the Output Card's filter criteria [3]
						if (!matchesFilterCommon(manager, outputComp, stack)) {
							continue;
						}

						// 4. Find the chest bound to the Item Output [3]
						ConnectionBlock destInv = null;
						for (ConnectionBlock inv : manager.getInventories()) {
							if (inv.getId() == outputComp.getInventoryId() && !inv.isSleeping()) {
								destInv = inv;
								break;
							}
						}

						if (destInv != null) {
							IItemHandler handler = destInv.getItemHandler(null);
							if (handler != null) {
								// 5. Check if there is room in that chest [3]
								ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, stack.copyWithCount(1), true);
								if (remaining.isEmpty()) {
									return true; // Found a valid target with space [3]
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Performs a fast, non-allocating downstream search to locate all Output cards connected to this Input [3].
	 */
	private static List<ItemTransferComponent> findDownstreamOutputs(ManagerBlockEntity manager, UUID startId) {
		List<ItemTransferComponent> outputs = new ArrayList<>();
		Queue<UUID> queue = new ArrayDeque<>();
		Set<UUID> visited = new HashSet<>();

		queue.add(startId);
		visited.add(startId);

		while (!queue.isEmpty()) {
			UUID currentId = queue.poll();

			for (FlowComponentConnections conn : manager.getFlowConnections()) {
				if (conn.getSourceComponentId().equals(currentId)) {
					UUID targetId = conn.getTargetComponentId();
					if (!visited.contains(targetId)) {
						visited.add(targetId);
						AbstractFlowComponent targetComp = manager.getFlowComponents().get(targetId);
						if (targetComp != null) {
							if (targetComp instanceof ItemTransferComponent outputComp && !outputComp.isInput()) {
								outputs.add(outputComp);
							} else {
								// Continue searching down the chain for intermediate nodes (e.g. logic) [3]
								queue.add(targetId);
							}
						}
					}
				}
			}
		}
		return outputs;
	}

	/**
	 * Simplified matchesFilter method that works on live server thread using the manager component map [3].
	 */
	private static boolean matchesFilterCommon(ManagerBlockEntity manager, ItemTransferComponent component, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		int limit = -1;
		boolean found = false;

		if (component.getBoundFilterVariableId() != null) {
			AbstractFlowComponent boundComp = manager.getFlowComponents().get(component.getBoundFilterVariableId());
			if (boundComp instanceof AdvancedItemFilterVariableComponent varComp) {
				if (AdvancedItemFilterVariableComponent.matchesVariableFilter(varComp, stack)) {
					found = true;
					limit = varComp.isUseQuantity() ? varComp.getQuantity() : -1;
				}
			}
		} else {
			for (int i = 0; i < component.getFilterItems().size(); i++) {
				ItemStack filter = component.getFilterItems().get(i);
				if (filter == null || filter.isEmpty()) {
					continue;
				}

				if (filter.is(ModItems.VARIABLE_CARD.get())) {
					CompoundTag tag = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
					if (tag.contains("VariableId")) {
						UUID varId = tag.getUUID("VariableId");
						AbstractFlowComponent varComp = manager.getFlowComponents().get(varId);
						if (varComp instanceof AdvancedItemFilterVariableComponent advancedVar) {
							if (AdvancedItemFilterVariableComponent.matchesVariableFilter(advancedVar, stack)) {
								found = true;
								limit = advancedVar.isUseQuantity() ? advancedVar.getQuantity() : -1;
								break;
							}
						}
					}
				} else if (ItemStack.isSameItemSameComponents(stack, filter)) {
					found = true;
					limit = i < component.getFilterLimits().size() ? component.getFilterLimits().get(i) : -1;
					break;
				}
			}
		}

		if (component.isWhitelist()) {
			return found;
		} else {
			return !found || limit > 0;
		}
	}
}