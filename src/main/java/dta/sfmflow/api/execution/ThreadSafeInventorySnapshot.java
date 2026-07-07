package dta.sfmflow.api.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.util.ConnectionBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable thread-safe snapshot container holding deep copies of target
 * inventory, fluid tank, and energy configurations [3]. Instantiated strictly on the main server
 * thread to prevent concurrency collisions during evaluation runs [3].
 */
public final class ThreadSafeInventorySnapshot {

	public record SlotSnapshot(ItemStack stack, int slotLimit, int mainSlotIndex) {
		public SlotSnapshot {
			stack = stack.copy(); // Ensure a deep copy of the ItemStack [3]
		}
	}

	public record InventorySnapshot(Map<Integer, SlotSnapshot> slots) {
		public InventorySnapshot(Map<Integer, SlotSnapshot> slots) {
			this.slots = new HashMap<>(slots);
		}
	}

	// Capture fluid tank data thread-safely [3]
	public record TankSnapshot(FluidStack stack, int capacity) {
		public TankSnapshot {
			stack = stack.copy(); // Ensure a deep copy of the FluidStack [3]
		}
	}

	public record FluidInventorySnapshot(Map<Integer, TankSnapshot> tanks) {
		public FluidInventorySnapshot(Map<Integer, TankSnapshot> tanks) {
			this.tanks = new HashMap<>(tanks);
		}
	}

	// Capture Forge Energy parameters thread-safely [3]
	public record EnergySnapshot(int energyStored, int maxEnergyStored, boolean canExtract, boolean canReceive) {}

	// Composite key mapping coordinates and active sides to their respective slots [3]
	public record SnapshotKey(BlockPos pos, @Nullable Direction side) {
	}

	private final Map<SnapshotKey, InventorySnapshot> snapshotMap;
	private final Map<SnapshotKey, FluidInventorySnapshot> fluidSnapshotMap;
	private final Map<SnapshotKey, EnergySnapshot> energySnapshotMap;
	private final List<ConnectionBlock> capturedInventories;

	private ThreadSafeInventorySnapshot(Map<SnapshotKey, InventorySnapshot> snapshotMap,
			Map<SnapshotKey, FluidInventorySnapshot> fluidSnapshotMap,
			Map<SnapshotKey, EnergySnapshot> energySnapshotMap,
			List<ConnectionBlock> capturedInventories) {
		this.snapshotMap = Collections.unmodifiableMap(new HashMap<>(snapshotMap));
		this.fluidSnapshotMap = Collections.unmodifiableMap(new HashMap<>(fluidSnapshotMap));
		this.energySnapshotMap = Collections.unmodifiableMap(new HashMap<>(energySnapshotMap));
		this.capturedInventories = Collections.unmodifiableList(new ArrayList<>(capturedInventories));
	}

	/**
	 * Creates a thread-safe deep copy snapshot of all loaded inventories connected
	 * to the manager block entity [3].
	 *
	 * @param manager the managing block entity [3]
	 * @return a complete immutable side-specific snapshot [3]
	 */
	public static ThreadSafeInventorySnapshot create(ManagerBlockEntity manager) {
		Map<SnapshotKey, InventorySnapshot> map = new HashMap<>();
		Map<SnapshotKey, FluidInventorySnapshot> fluidMap = new HashMap<>();
		Map<SnapshotKey, EnergySnapshot> energyMap = new HashMap<>();
		List<ConnectionBlock> capturedList = new ArrayList<>();
		Level level = manager.getLevel();
		if (level != null && !level.isClientSide()) {
			for (ConnectionBlock block : manager.getInventories()) {
				capturedList.add(block);
				BlockPos pos = block.getBlockPos();
				if (level.hasChunkAt(pos)) {
					BlockEntity be = level.getBlockEntity(pos);
					
					// Snapshot Item Handlers (non-directional)
					IItemHandler nullHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
					if (nullHandler == null) {
						nullHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.ItemHandler.BLOCK,
								level, pos, level.getBlockState(pos), null);
					}
					if (nullHandler != null) {
						map.put(new SnapshotKey(pos, null), createInventorySnapshot(nullHandler, be, null));
					}
					
					// Snapshot Fluid Handlers (non-directional)
					IFluidHandler nullFluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
					if (nullFluidHandler == null) {
						nullFluidHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.FluidHandler.BLOCK,
								level, pos, level.getBlockState(pos), null);
					}
					if (nullFluidHandler != null) {
						fluidMap.put(new SnapshotKey(pos, null), createFluidInventorySnapshot(nullFluidHandler));
					}

					// Snapshot Energy Storages (non-directional)
					IEnergyStorage nullEnergy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
					if (nullEnergy == null) {
						nullEnergy = SpecialBlockCapabilityRegistry.getCapability(Capabilities.EnergyStorage.BLOCK,
								level, pos, level.getBlockState(pos), null);
					}
					if (nullEnergy != null) {
						energyMap.put(new SnapshotKey(pos, null), new EnergySnapshot(nullEnergy.getEnergyStored(), nullEnergy.getMaxEnergyStored(), nullEnergy.canExtract(), nullEnergy.canReceive()));
					}

					// Index all 6 active directions independently [3]
					for (Direction dir : Direction.values()) {
						// Item Snapshots
						IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
						if (handler == null) {
							handler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.ItemHandler.BLOCK,
									level, pos, level.getBlockState(pos), dir);
						}
						if (handler != null) {
							map.put(new SnapshotKey(pos, dir), createInventorySnapshot(handler, be, dir));
						}
						
						// Fluid Snapshots
						IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, dir);
						if (fluidHandler == null) {
							fluidHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.FluidHandler.BLOCK,
									level, pos, level.getBlockState(pos), dir);
						}
						if (fluidHandler != null) {
							fluidMap.put(new SnapshotKey(pos, dir), createFluidInventorySnapshot(fluidHandler));
						}
						
						// Energy Snapshots
						IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
						if (energy == null) {
							energy = SpecialBlockCapabilityRegistry.getCapability(Capabilities.EnergyStorage.BLOCK,
									level, pos, level.getBlockState(pos), dir);
						}
						if (energy != null) {
							energyMap.put(new SnapshotKey(pos, dir), new EnergySnapshot(energy.getEnergyStored(), energy.getMaxEnergyStored(), energy.canExtract(), energy.canReceive()));
						}
					}
				}
			}
		}
		return new ThreadSafeInventorySnapshot(map, fluidMap, energyMap, capturedList);
	}

	private static InventorySnapshot createInventorySnapshot(IItemHandler handler, @Nullable BlockEntity be,
			@Nullable Direction side) {
		Map<Integer, SlotSnapshot> slots = new HashMap<>();
		int count = handler.getSlots();
		int[] faceSlots = null;

		if (be instanceof WorldlyContainer worldly && side != null) {
			faceSlots = worldly.getSlotsForFace(side);
		}

		for (int i = 0; i < count; i++) {
			ItemStack stack = handler.getStackInSlot(i);
			int mainSlotIndex = i;
			if (faceSlots != null && i >= 0 && i < faceSlots.length) {
				mainSlotIndex = faceSlots[i];
			}
			slots.put(i, new SlotSnapshot(stack, handler.getSlotLimit(i), mainSlotIndex));
		}
		return new InventorySnapshot(slots);
	}

	private static FluidInventorySnapshot createFluidInventorySnapshot(IFluidHandler handler) {
		Map<Integer, TankSnapshot> tanks = new HashMap<>();
		int count = handler.getTanks();
		for (int i = 0; i < count; i++) {
			FluidStack stack = handler.getFluidInTank(i);
			tanks.put(i, new TankSnapshot(stack, handler.getTankCapacity(i)));
		}
		return new FluidInventorySnapshot(tanks);
	}

	public @Nullable InventorySnapshot getInventory(BlockPos pos, @Nullable Direction side) {
		InventorySnapshot direct = snapshotMap.get(new SnapshotKey(pos, side));
		if (direct != null) {
			return direct;
		}
		return snapshotMap.get(new SnapshotKey(pos, null));
	}

	public @Nullable FluidInventorySnapshot getFluidInventory(BlockPos pos, @Nullable Direction side) {
		FluidInventorySnapshot direct = fluidSnapshotMap.get(new SnapshotKey(pos, side));
		if (direct != null) {
			return direct;
		}
		return fluidSnapshotMap.get(new SnapshotKey(pos, null));
	}

	public @Nullable EnergySnapshot getEnergy(BlockPos pos, @Nullable Direction side) {
		EnergySnapshot direct = energySnapshotMap.get(new SnapshotKey(pos, side));
		if (direct != null) {
			return direct;
		}
		return energySnapshotMap.get(new SnapshotKey(pos, null));
	}

	public List<ConnectionBlock> getCapturedInventories() {
		return this.capturedInventories;
	}
}