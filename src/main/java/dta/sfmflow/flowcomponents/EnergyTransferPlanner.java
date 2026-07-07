package dta.sfmflow.flowcomponents;

import dta.sfmflow.api.execution.FlowEnergyBuffer;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Common, stateless helper consolidating energy transfer simulation, extraction, and
 * deposition planning routines [3].
 */
public final class EnergyTransferPlanner {

	public record EnergyKey(BlockPos pos, @Nullable Direction side) {}

	private EnergyTransferPlanner() {}

	@SuppressWarnings("unchecked")
	private static Map<EnergyKey, Integer> getSimulatedEnergy(FlowchartPlanningContext context) {
		ResourceLocation energySnapshotKey = ResourceLocation.fromNamespaceAndPath("sfmflow", "energy_snapshot");
		Object obj = context.getPipelineBuffer(new UUID(0, 0), energySnapshotKey);
		if (obj instanceof Map) {
			return (Map<EnergyKey, Integer>) obj;
		}
		Map<EnergyKey, Integer> map = new HashMap<>();
		context.setPipelineBuffer(new UUID(0, 0), energySnapshotKey, map);
		return map;
	}

	private static int getSimulatedEnergyLevel(ThreadSafeInventorySnapshot.EnergySnapshot snapshotVal, BlockPos pos, @Nullable Direction side, Map<EnergyKey, Integer> simulatedEnergy) {
		EnergyKey key = new EnergyKey(pos, side);
		return simulatedEnergy.computeIfAbsent(key, k -> snapshotVal.energyStored());
	}

	public static void planInput(FlowchartPlanningContext context, EnergyTransferComponent component) {
		FlowEnergyBuffer myOutputBuffer = new FlowEnergyBuffer();

		FlowEnergyBuffer myInputBuffer = context.getEnergyComponentBuffer(component.getId());
		if (!myInputBuffer.isEmpty()) {
			for (FlowEnergyBuffer.BufferedEnergy energy : myInputBuffer.getEnergies()) {
				myOutputBuffer.add(energy.srcPos(), energy.srcSide(), energy.amount());
			}
		}

		extractEnergyIntoBuffer(context, component, myOutputBuffer);

		if (!myOutputBuffer.isEmpty()) {
			for (var conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(component.getId())) {
					UUID targetId = conn.getTargetComponentId();
					FlowEnergyBuffer targetInputBuffer = context.getEnergyComponentBuffer(targetId);

					for (FlowEnergyBuffer.BufferedEnergy energy : myOutputBuffer.getEnergies()) {
						targetInputBuffer.add(energy.srcPos(), energy.srcSide(), energy.amount());
					}
					context.enqueue(targetId);
				}
			}
		}
	}

	public static void planOutput(FlowchartPlanningContext context, EnergyTransferComponent component) {
		FlowEnergyBuffer myInputBuffer = context.getEnergyComponentBuffer(component.getId());

		if (!myInputBuffer.isEmpty()) {
			FlowEnergyBuffer myOutputBuffer = new FlowEnergyBuffer();
			depositEnergyFromBuffer(context, component, myInputBuffer, myOutputBuffer);

			for (var conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(component.getId())) {
					UUID targetId = conn.getTargetComponentId();
					FlowEnergyBuffer targetInputBuffer = context.getEnergyComponentBuffer(targetId);

					for (FlowEnergyBuffer.BufferedEnergy energy : myOutputBuffer.getEnergies()) {
						targetInputBuffer.add(energy.srcPos(), energy.srcSide(), energy.amount());
					}
					context.enqueue(targetId);
				}
			}
		}
	}

	private static void extractEnergyIntoBuffer(FlowchartPlanningContext context, EnergyTransferComponent component, FlowEnergyBuffer buffer) {
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

		Map<EnergyKey, Integer> simulatedEnergy = getSimulatedEnergy(context);

		for (Direction srcSide : activeSrcSides) {
			var snap = context.getSnapshot().getEnergy(srcPos, srcSide);
			if (snap == null || !snap.canExtract()) {
				continue;
			}

			int currentEnergy = getSimulatedEnergyLevel(snap, srcPos, srcSide, simulatedEnergy);
			if (currentEnergy <= 0) {
				continue;
			}

			int toExtract = Math.min(currentEnergy, component.getMaxTransferAmount());
			if (toExtract > 0) {
				FlowLogger.execution("Simulating Energy Extract from %s: Side=%s, Amount=%d", srcPos, srcSide, toExtract);

				EnergyKey key = new EnergyKey(srcPos, srcSide);
				simulatedEnergy.put(key, currentEnergy - toExtract);

				buffer.add(srcPos, srcSide, toExtract);
			}
		}
	}

	private static void depositEnergyFromBuffer(FlowchartPlanningContext context, EnergyTransferComponent component,
			FlowEnergyBuffer inputBuffer, FlowEnergyBuffer outputBuffer) {
		var inventories = context.getConnectedInventories();
		ConnectionBlock tgtInventory = null;

		for (var block : inventories) {
			if (block.getId() == component.getInventoryId() && !block.isSleeping()) {
				tgtInventory = block;
				break;
			}
		}

		if (tgtInventory == null) {
			for (FlowEnergyBuffer.BufferedEnergy energy : inputBuffer.getEnergies()) {
				outputBuffer.add(energy.srcPos(), energy.srcSide(), energy.amount());
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

		Map<EnergyKey, Integer> simulatedEnergy = getSimulatedEnergy(context);

		for (FlowEnergyBuffer.BufferedEnergy incomingEnergy : inputBuffer.getEnergies()) {
			int remainingToDeposit = incomingEnergy.amount();
			if (remainingToDeposit <= 0) continue;

			for (Direction tgtSide : activeTgtSides) {
				if (remainingToDeposit <= 0) break;

				var snap = context.getSnapshot().getEnergy(tgtPos, tgtSide);
				if (snap == null || !snap.canReceive()) {
					continue;
				}

				int currentEnergy = getSimulatedEnergyLevel(snap, tgtPos, tgtSide, simulatedEnergy);
				int maxCapacity = snap.maxEnergyStored();
				int space = Math.max(0, maxCapacity - currentEnergy);

				if (space <= 0) {
					continue;
				}

				int toDeposit = Math.min(remainingToDeposit, Math.min(space, component.getMaxTransferAmount()));
				if (toDeposit > 0) {
					boolean success = context.tryWriteEnergyTask(incomingEnergy.srcPos(), incomingEnergy.srcSide(), tgtPos, tgtSide, toDeposit);
					if (success) {
						FlowLogger.execution("Simulating Energy Deposit to %s: Side=%s, Amount=%d", tgtPos, tgtSide, toDeposit);

						EnergyKey key = new EnergyKey(tgtPos, tgtSide);
						simulatedEnergy.put(key, currentEnergy + toDeposit);

						remainingToDeposit -= toDeposit;
					}
				}
			}

			if (remainingToDeposit > 0) {
				outputBuffer.add(incomingEnergy.srcPos(), incomingEnergy.srcSide(), remainingToDeposit);
			}
		}
	}
}