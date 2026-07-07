package dta.sfmflow.flowcomponents;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.execution.FlowFluidBuffer;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Common, stateless helper consolidating fluid transfer simulation, extraction, and
 * deposition planning routines utilizing thread-safe snapshots [3].
 */
public final class FluidTransferPlanner {

	/**
	 * Unique key tracking a specific fluid tank coordinate during the simulation sweep [3].
	 */
	public record TankKey(BlockPos pos, @Nullable Direction side, int tankIndex) {
	}

	private FluidTransferPlanner() {
	}

	@SuppressWarnings("unchecked")
	private static Map<TankKey, FluidStack> getSimulatedTanks(FlowchartPlanningContext context) {
		ResourceLocation fluidTanksKey = ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid_tanks_snapshot");
		Object obj = context.getPipelineBuffer(new UUID(0, 0), fluidTanksKey);
		if (obj instanceof Map) {
			return (Map<TankKey, FluidStack>) obj;
		}
		Map<TankKey, FluidStack> map = new HashMap<>();
		context.setPipelineBuffer(new UUID(0, 0), fluidTanksKey, map);
		return map;
	}

	private static FluidStack getSimulatedFluid(ThreadSafeInventorySnapshot.FluidInventorySnapshot snapshotVal, BlockPos pos, @Nullable Direction side,
			int tankIndex, Map<TankKey, FluidStack> simulatedTanks) {
		TankKey key = new TankKey(pos, side, tankIndex);
		return simulatedTanks.computeIfAbsent(key, k -> {
			var tank = snapshotVal.tanks().get(tankIndex);
			return tank != null ? tank.stack().copy() : FluidStack.EMPTY;
		});
	}

	private static FluidStack simulateDrain(ThreadSafeInventorySnapshot.FluidInventorySnapshot snapshotVal, BlockPos pos, @Nullable Direction side,
			int tankIndex, int maxDrain, Map<TankKey, FluidStack> simulatedTanks) {
		TankKey key = new TankKey(pos, side, tankIndex);
		FluidStack simulated = simulatedTanks.computeIfAbsent(key, k -> {
			var tank = snapshotVal.tanks().get(tankIndex);
			return tank != null ? tank.stack().copy() : FluidStack.EMPTY;
		});
		if (simulated.isEmpty() || maxDrain <= 0) {
			return FluidStack.EMPTY;
		}
		int toDrain = Math.min(simulated.getAmount(), maxDrain);
		FluidStack drained = simulated.copy();
		drained.setAmount(toDrain);

		simulated.shrink(toDrain);
		return drained;
	}

	private static int simulateFill(ThreadSafeInventorySnapshot.FluidInventorySnapshot snapshotVal, BlockPos pos, @Nullable Direction side, int tankIndex,
			FluidStack resource, Map<TankKey, FluidStack> simulatedTanks) {
		if (resource.isEmpty())
			return 0;
		TankKey key = new TankKey(pos, side, tankIndex);
		FluidStack simulated = simulatedTanks.computeIfAbsent(key, k -> {
			var tank = snapshotVal.tanks().get(tankIndex);
			return tank != null ? tank.stack().copy() : FluidStack.EMPTY;
		});

		var tank = snapshotVal.tanks().get(tankIndex);
		int capacity = tank != null ? tank.capacity() : 0;

		if (simulated.isEmpty()) {
			int toFill = Math.min(capacity, resource.getAmount());
			FluidStack filled = resource.copy();
			filled.setAmount(toFill);
			simulatedTanks.put(key, filled);
			return toFill;
		}

		if (FluidStack.isSameFluid(simulated, resource)) {
			int space = capacity - simulated.getAmount();
			int toFill = Math.min(space, resource.getAmount());
			simulated.grow(toFill);
			return toFill;
		}

		return 0;
	}

	public static void planInput(FlowchartPlanningContext context, FluidTransferComponent component) {
		FlowFluidBuffer myOutputBuffer = new FlowFluidBuffer();

		FlowFluidBuffer myInputBuffer = context.getFluidComponentBuffer(component.getId());
		if (!myInputBuffer.isEmpty()) {
			for (FlowFluidBuffer.BufferedFluid fluid : myInputBuffer.getFluids()) {
				myOutputBuffer.add(fluid.srcPos(), fluid.srcSlot(), fluid.srcSide(), fluid.stack().copy());
			}
		}

		extractFluidsIntoBuffer(context, component, myOutputBuffer);

		if (!myOutputBuffer.isEmpty()) {
			for (var conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(component.getId())) {
					UUID targetId = conn.getTargetComponentId();
					FlowFluidBuffer targetInputBuffer = context.getFluidComponentBuffer(targetId);

					for (FlowFluidBuffer.BufferedFluid fluid : myOutputBuffer.getFluids()) {
						targetInputBuffer.add(fluid.srcPos(), fluid.srcSlot(), fluid.srcSide(), fluid.stack().copy());
					}
					context.enqueue(targetId);
				}
			}
		}
	}

	public static void planOutput(FlowchartPlanningContext context, FluidTransferComponent component) {
		FlowFluidBuffer myInputBuffer = context.getFluidComponentBuffer(component.getId());

		if (!myInputBuffer.isEmpty()) {
			FlowFluidBuffer myOutputBuffer = new FlowFluidBuffer();
			depositFluidsFromBuffer(context, component, myInputBuffer, myOutputBuffer);

			for (var conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(component.getId())) {
					UUID targetId = conn.getTargetComponentId();
					FlowFluidBuffer targetInputBuffer = context.getFluidComponentBuffer(targetId);

					for (FlowFluidBuffer.BufferedFluid fluid : myOutputBuffer.getFluids()) {
						targetInputBuffer.add(fluid.srcPos(), fluid.srcSlot(), fluid.srcSide(), fluid.stack().copy());
					}
					context.enqueue(targetId);
				}
			}
		}
	}

	private static void extractFluidsIntoBuffer(FlowchartPlanningContext context, FluidTransferComponent component,
			FlowFluidBuffer buffer) {
		var inventories = context.getConnectedInventories();
		ConnectionBlock srcInventory = null;

		for (var block : inventories) {
			if (block.getId() == component.getInventoryId() && !block.isSleeping()) {
				srcInventory = block;
				break;
			}
		}

		if (srcInventory == null) {
			return;
		}

		BlockPos srcPos = srcInventory.getBlockPos();
		List<Direction> activeSrcSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (component.isSideActive(dir)) {
				activeSrcSides.add(dir);
			}
		}
		if (activeSrcSides.isEmpty()) {
			activeSrcSides.add(null);
		}

		Map<TankKey, FluidStack> simulatedTanks = getSimulatedTanks(context);

		for (Direction srcSide : activeSrcSides) {
			var srcInv = context.getSnapshot().getFluidInventory(srcPos, srcSide);
			if (srcInv == null) {
				continue;
			}

			int tankCount = srcInv.tanks().size();
			for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
				if (component.getTargetSlot() != -1 && component.getTargetSlot() != tankIndex) {
					continue;
				}

				if (!component.isSlotEnabled(srcSide, tankIndex)) {
					continue;
				}

				FluidStack fluidInTank = getSimulatedFluid(srcInv, srcPos, srcSide, tankIndex, simulatedTanks);
				if (fluidInTank.isEmpty()) {
					continue;
				}

				if (!matchesFilter(context, component, fluidInTank)) {
					continue;
				}

				int totalInSource = fluidInTank.getAmount();
				int srcLimit = getFilterLimit(context, component, fluidInTank);
				int srcRemaining = totalInSource;

				if (component.isWhitelist()) {
					if (srcLimit > 0) {
						srcRemaining = Math.min(srcRemaining, srcLimit);
					}
				} else {
					if (srcLimit > 0) {
						int availableOverLimit = totalInSource - srcLimit;
						if (availableOverLimit <= 0) {
							continue;
						}
						srcRemaining = Math.min(srcRemaining, availableOverLimit);
					}
				}

				if (srcRemaining > 0) {
					FluidStack extracted = simulateDrain(srcInv, srcPos, srcSide, tankIndex, srcRemaining, simulatedTanks);
					if (!extracted.isEmpty()) {
						FlowLogger.execution("Simulated Extracting Fluid from Tank %d: Side=%s, Amount=%d", tankIndex,
								srcSide, extracted.getAmount());
						buffer.add(srcPos, tankIndex, srcSide, extracted);
					}
				}
			}
		}
	}

	private static void depositFluidsFromBuffer(FlowchartPlanningContext context, FluidTransferComponent component,
			FlowFluidBuffer inputBuffer, FlowFluidBuffer outputBuffer) {
		var inventories = context.getConnectedInventories();
		ConnectionBlock tgtInventory = null;

		for (var block : inventories) {
			if (block.getId() == component.getInventoryId() && !block.isSleeping()) {
				tgtInventory = block;
				break;
			}
		}

		if (tgtInventory == null) {
			for (FlowFluidBuffer.BufferedFluid fluid : inputBuffer.getFluids()) {
				outputBuffer.add(fluid.srcPos(), fluid.srcSlot(), fluid.srcSide(), fluid.stack().copy());
			}
			return;
		}

		BlockPos tgtPos = tgtInventory.getBlockPos();
		List<Direction> activeTgtSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (component.isSideActive(dir)) {
				activeTgtSides.add(dir);
			}
		}
		if (activeTgtSides.isEmpty()) {
			activeTgtSides.add(null);
		}

		Map<TankKey, FluidStack> simulatedTanks = getSimulatedTanks(context);

		for (FlowFluidBuffer.BufferedFluid incomingFluid : inputBuffer.getFluids()) {
			FluidStack incoming = incomingFluid.stack();
			if (incoming.isEmpty())
				continue;

			if (!matchesFilter(context, component, incoming)) {
				outputBuffer.add(incomingFluid.srcPos(), incomingFluid.srcSlot(), incomingFluid.srcSide(),
						incoming.copy());
				continue;
			}

			int remainingToDeposit = incoming.getAmount();
			int tgtLimit = getFilterLimit(context, component, incoming);

			for (Direction tgtSide : activeTgtSides) {
				if (remainingToDeposit <= 0)
					break;

				var tgtInv = context.getSnapshot().getFluidInventory(tgtPos, tgtSide);
				if (tgtInv == null) {
					continue;
				}

				int tankCount = tgtInv.tanks().size();
				for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
					if (remainingToDeposit <= 0)
						break;

					if (component.getTargetSlot() != -1 && component.getTargetSlot() != tankIndex) {
						continue;
					}

					if (!component.isSlotEnabled(tgtSide, tankIndex)) {
						continue;
					}

					FluidStack fluidInTank = getSimulatedFluid(tgtInv, tgtPos, tgtSide, tankIndex, simulatedTanks);
					int totalInTarget = fluidInTank.getAmount();
					int maxToDeposit = incoming.getAmount();

					if (component.isWhitelist()) {
						if (tgtLimit > 0) {
							maxToDeposit = Math.max(0, tgtLimit - totalInTarget);
							if (maxToDeposit <= 0) {
								continue;
							}
						}
					}

					FluidStack sample = incoming.copy();
					sample.setAmount(Math.min(remainingToDeposit, maxToDeposit));

					int filled = simulateFill(tgtInv, tgtPos, tgtSide, tankIndex, sample, simulatedTanks);
					if (filled > 0) {
						boolean success = context.tryWriteFluidTask(incomingFluid.srcPos(), incomingFluid.srcSlot(),
								incomingFluid.srcSide(), tgtPos, tankIndex, tgtSide, incoming,
								filled);
						if (success) {
							FlowLogger.execution("Simulated Depositing Fluid to Tank %d: Side=%s, Amount=%d", tankIndex,
									tgtSide, filled);
							remainingToDeposit -= filled;
						}
					}
				}
			}

			if (remainingToDeposit > 0) {
				FluidStack remainingStack = incoming.copy();
				remainingStack.setAmount(remainingToDeposit);
				outputBuffer.add(incomingFluid.srcPos(), incomingFluid.srcSlot(), incomingFluid.srcSide(),
						remainingStack);
			}
		}
	}

	private static boolean matchesFilter(FlowchartPlanningContext context, FluidTransferComponent component, FluidStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		int limit = -1;
		boolean found = false;

		if (component.getBoundFilterVariableId() != null) {
			AbstractFlowComponent boundComp = context.getComponents().get(component.getBoundFilterVariableId());
			if (boundComp instanceof AdvancedFluidFilterVariableComponent varComp) {
				if (AdvancedFluidFilterVariableComponent.matchesVariableFilter(varComp, stack)) {
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

				if (filter.getItem() == ModItems.VARIABLE_CARD.get() || filter.is(ModItems.VARIABLE_CARD.get())) {
					CompoundTag tag = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
					if (tag.contains("VariableId")) {
						UUID varId = tag.getUUID("VariableId");
						AbstractFlowComponent varComp = context.getComponents().get(varId);
						if (varComp instanceof AdvancedFluidFilterVariableComponent advancedVar) {
							if (AdvancedFluidFilterVariableComponent.matchesVariableFilter(advancedVar, stack)) {
								found = true;
								limit = advancedVar.isUseQuantity() ? advancedVar.getQuantity() : -1;
								break;
							}
						}
					}
				} else {
					FluidStack filterFluid = getFluidFromItem(filter);
					if (!filterFluid.isEmpty() && FluidStack.isSameFluid(stack, filterFluid)) {
						found = true;
						limit = i < component.getFilterLimits().size() ? component.getFilterLimits().get(i) : -1;
						break;
					}
				}
			}
		}

		if (component.isWhitelist()) {
			return found;
		} else {
			return !found || limit > 0;
		}
	}

	private static int getFilterLimit(FlowchartPlanningContext context, FluidTransferComponent component, FluidStack stack) {
		if (component.getBoundFilterVariableId() != null) {
			AbstractFlowComponent boundComp = context.getComponents().get(component.getBoundFilterVariableId());
			if (boundComp instanceof AdvancedFluidFilterVariableComponent varComp) {
				if (AdvancedFluidFilterVariableComponent.matchesVariableFilter(varComp, stack)) {
					return varComp.isUseQuantity() ? varComp.getQuantity() : -1;
				}
			}
			return -1;
		}

		for (ItemStack filter : component.getFilterItems()) {
			if (filter == null || filter.isEmpty())
				continue;

			if (filter.getItem() == ModItems.VARIABLE_CARD.get() || filter.is(ModItems.VARIABLE_CARD.get())) {
				CompoundTag tag = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
				if (tag.contains("VariableId")) {
					UUID varId = tag.getUUID("VariableId");
					AbstractFlowComponent varComp = context.getComponents().get(varId);
					if (varComp instanceof AdvancedFluidFilterVariableComponent advancedVar) {
						if (AdvancedFluidFilterVariableComponent.matchesVariableFilter(advancedVar, stack)) {
							int resolvedLimit = advancedVar.isUseQuantity() ? advancedVar.getQuantity() : -1;
							FlowLogger.execution("getFilterLimit Resolved Variable Card: ID=%s, Fluid=%s, UseQty=%b, Limit=%d",
									varId, stack.getHoverName().getString(), advancedVar.isUseQuantity(), resolvedLimit);
							return resolvedLimit;
						}
					}
				}
			} else {
				FluidStack filterFluid = getFluidFromItem(filter);
				if (!filterFluid.isEmpty() && FluidStack.isSameFluid(stack, filterFluid)) {
					return component.getFilterLimit(filter);
				}
			}
		}

		return -1;
	}

	public static FluidStack getFluidFromItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return FluidStack.EMPTY;
		}
		var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
		if (handler != null) {
			return handler.getFluidInTank(0);
		}
		return FluidStack.EMPTY;
	}
}