package dta.sfmflow.api.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.FluidStack;

import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.ItemVacuumValveBlock;
import dta.sfmflow.block.FluidVacuumValveBlock;
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
 * inventories, fluid tanks, and energy configurations dynamically.
 */
public final class ThreadSafeInventorySnapshot {

	public record SlotSnapshot(ItemStack stack, int slotLimit, int mainSlotIndex) {
		public SlotSnapshot {
			stack = stack.copy(); // Ensure a deep copy of the ItemStack
		}
	}

	public record InventorySnapshot(Map<Integer, SlotSnapshot> slots) {
		public InventorySnapshot(Map<Integer, SlotSnapshot> slots) {
			this.slots = new HashMap<>(slots);
		}
	}

	public record TankSnapshot(FluidStack stack, int capacity) {
		public TankSnapshot {
			stack = stack.copy(); // Ensure a deep copy of the FluidStack 
		}
	}

	public record FluidInventorySnapshot(Map<Integer, TankSnapshot> tanks) {
		public FluidInventorySnapshot(Map<Integer, TankSnapshot> tanks) {
			this.tanks = new HashMap<>(tanks);
		}
	}

	public record EnergySnapshot(int energyStored, int maxEnergyStored, boolean canExtract, boolean canReceive) {}

	// Composite key mapping coordinates and active sides
	public record SnapshotKey(BlockPos pos, @Nullable Direction side) {
	}

	private final Map<SnapshotKey, Map<ResourceLocation, Object>> snapshotMap;
	private final List<ConnectionBlock> capturedInventories;

	private ThreadSafeInventorySnapshot(Map<SnapshotKey, Map<ResourceLocation, Object>> snapshotMap,
			List<ConnectionBlock> capturedInventories) {
		this.snapshotMap = Collections.unmodifiableMap(new HashMap<>(snapshotMap));
		this.capturedInventories = Collections.unmodifiableList(new ArrayList<>(capturedInventories));
	}

	/**
	 * Creates a thread-safe deep copy snapshot of all loaded registries connected
	 * to the manager block entity dynamically.
	 *
	 * @param manager the managing block entity
	 * @return a complete immutable dynamic snapshot
	 */
	@SuppressWarnings("unchecked")
	public static ThreadSafeInventorySnapshot create(ManagerBlockEntity manager) {
		Map<SnapshotKey, Map<ResourceLocation, Object>> map = new HashMap<>();
		List<ConnectionBlock> capturedList = new ArrayList<>();
		Level level = manager.getLevel();
		if (level != null && !level.isClientSide()) {
			for (ConnectionBlock block : manager.getInventories()) {
				capturedList.add(new ConnectionBlock(block)); // Deep copy to prevent data races
				BlockPos pos = block.getBlockPos();
				if (level.hasChunkAt(pos)) {
					BlockState state = level.getBlockState(pos);

					// 1. Snapshot dynamic non-directional capabilities 
					Map<ResourceLocation, Object> nullCapsMap = new HashMap<>();
					for (var flowCap : FlowCapabilityRegistry.getRegisteredCapabilities().values()) {
						var cap = flowCap.getCapability();
						if (cap != null) {
							Object handler = level.getCapability((BlockCapability<Object, Direction>) cap, pos, null);
							if (handler == null) {
								handler = SpecialBlockCapabilityRegistry.getCapability((BlockCapability<Object, Direction>) cap, level, pos, state, null);
							}
							if (handler != null) {
								var snapshotter = FlowCapabilityRegistry.getSnapshotter(flowCap.getId());
								if (snapshotter != null) {
									nullCapsMap.put(flowCap.getId(), snapshotter.createSnapshot(handler));
								}
							}
						}
					}

					// Overrides snapshotter for the Item Vacuum Valve to represent ground items as slot contents 
					if (state.is(ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get())) {
						nullCapsMap.put(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"),
								createVacuumGroundSnapshot(level, pos, state));
					}

					// Overrides snapshotter for Fluid Vacuum Valve to represent ground fluids as tank contents
					if (state.is(ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get())) {
						nullCapsMap.put(ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"),
								createFluidVacuumGroundSnapshot(level, pos, state));
					}

					if (!nullCapsMap.isEmpty()) {
						map.put(new SnapshotKey(pos, null), nullCapsMap);
					}

					// 2. Snapshot dynamic directional capabilities
					for (Direction dir : Direction.values()) {
						Map<ResourceLocation, Object> dirCapsMap = new HashMap<>();
						for (var flowCap : FlowCapabilityRegistry.getRegisteredCapabilities().values()) {
							var cap = flowCap.getCapability();
							if (cap != null) {
								Object handler = level.getCapability((BlockCapability<Object, Direction>) cap, pos, dir);
								if (handler == null) {
									handler = SpecialBlockCapabilityRegistry.getCapability((BlockCapability<Object, Direction>) cap, level, pos, state, dir);
								}
								if (handler != null) {
									var snapshotter = FlowCapabilityRegistry.getSnapshotter(flowCap.getId());
									if (snapshotter != null) {
										dirCapsMap.put(flowCap.getId(), snapshotter.createSnapshot(handler));
									}
								}
							}
						}

						// Overrides snapshotter for the Item Vacuum Valve to represent ground items as slot contents directionally
						if (state.is(ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get())) {
							dirCapsMap.put(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"),
									createVacuumGroundSnapshot(level, pos, state));
						}

						// Overrides snapshotter for Fluid Vacuum Valve to represent ground fluids as tank contents directionally
						if (state.is(ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get())) {
							dirCapsMap.put(ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"),
									createFluidVacuumGroundSnapshot(level, pos, state));
						}

						if (!dirCapsMap.isEmpty()) {
							map.put(new SnapshotKey(pos, dir), dirCapsMap);
						}
					}
				}
			}
		}
		return new ThreadSafeInventorySnapshot(map, capturedList);
	}

	/**
	 * Scans the ground area in front of the vacuum valve and populates a virtual InventorySnapshot.
	 * Shifted the AABB center to 2 blocks in front of the block to cover the full 3x3x3 scan area.
	 */
	private static ThreadSafeInventorySnapshot.InventorySnapshot createVacuumGroundSnapshot(Level level, BlockPos pos, BlockState state) {
		Map<Integer, SlotSnapshot> slots = new HashMap<>();
		Direction facing = state.getValue(ItemVacuumValveBlock.FACING);
		BlockPos centerPos = pos.relative(facing, 2); // Shifted center to 2 blocks in front
		AABB suctionBox = new AABB(centerPos).inflate(1.0);

		List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, suctionBox);
		int slotIndex = 0;
		for (ItemEntity entity : entities) {
			if (entity.isAlive() && !entity.hasPickUpDelay()) {
				ItemStack stack = entity.getItem();
				if (!stack.isEmpty()) {
					slots.put(slotIndex, new SlotSnapshot(
							stack.copy(),
							stack.getMaxStackSize(),
							slotIndex
					));
					slotIndex++;
				}
			}
		}
		return new InventorySnapshot(slots);
	}

	/**
	 * Scans the block in front of the vacuum valve and populates a virtual FluidInventorySnapshot.
	 */
	private static ThreadSafeInventorySnapshot.FluidInventorySnapshot createFluidVacuumGroundSnapshot(Level level, BlockPos pos, BlockState state) {
		Map<Integer, TankSnapshot> tanks = new HashMap<>();
		Direction facing = state.getValue(FluidVacuumValveBlock.FACING);
		BlockPos mouthPos = pos.relative(facing);

		FluidState fluidState = level.getFluidState(mouthPos);
		if (fluidState.isSource()) {
			tanks.put(0, new TankSnapshot(
					new FluidStack(fluidState.getType(), 1000),
					1000
			));
		}
		return new FluidInventorySnapshot(tanks);
	}

	/**
	 * Dynamic snapshot resolver that retrieves custom snapshots safely for any capability.
	 */
	public @Nullable <T> T getCustomSnapshot(BlockPos pos, @Nullable Direction side, ResourceLocation capabilityId, Class<T> clazz) {
		Map<ResourceLocation, Object> sideMap = snapshotMap.get(new SnapshotKey(pos, side));
		if (sideMap != null) {
			Object snap = sideMap.get(capabilityId);
			if (clazz.isInstance(snap)) {
				return clazz.cast(snap);
			}
		}
		// Fallback to non-directional snapshot if direction-specific capability is missing 
		Map<ResourceLocation, Object> nullMap = snapshotMap.get(new SnapshotKey(pos, null));
		if (nullMap != null) {
			Object snap = nullMap.get(capabilityId);
			if (clazz.isInstance(snap)) {
				return clazz.cast(snap);
			}
		}
		return null;
	}

	public @Nullable InventorySnapshot getInventory(BlockPos pos, @Nullable Direction side) {
		return getCustomSnapshot(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "item"), InventorySnapshot.class);
	}

	public @Nullable FluidInventorySnapshot getFluidInventory(BlockPos pos, @Nullable Direction side) {
		return getCustomSnapshot(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"), FluidInventorySnapshot.class);
	}

	public @Nullable EnergySnapshot getEnergy(BlockPos pos, @Nullable Direction side) {
		return getCustomSnapshot(pos, side, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"), EnergySnapshot.class);
	}

	public List<ConnectionBlock> getCapturedInventories() {
		return this.capturedInventories;
	}
}