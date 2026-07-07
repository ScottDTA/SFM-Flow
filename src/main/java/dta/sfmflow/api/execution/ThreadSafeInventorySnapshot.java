package dta.sfmflow.api.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry; // Added import [3]
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
 * inventory slot configurations [3]. Instantiated strictly on the main server
 * thread to prevent concurrency collisions during evaluation runs [3]. Upgraded
 * to support side-specific capability indexing and main slot mapping [3].
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

	// Composite key mapping coordinates and active sides to their respective slots
	// [3]
	public record SnapshotKey(BlockPos pos, @Nullable Direction side) {
	}

	private final Map<SnapshotKey, InventorySnapshot> snapshotMap;
	private final List<ConnectionBlock> capturedInventories;

	private ThreadSafeInventorySnapshot(Map<SnapshotKey, InventorySnapshot> snapshotMap,
			List<ConnectionBlock> capturedInventories) {
		this.snapshotMap = Collections.unmodifiableMap(new HashMap<>(snapshotMap));
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
		List<ConnectionBlock> capturedList = new ArrayList<>();
		Level level = manager.getLevel();
		if (level != null && !level.isClientSide()) {
			for (ConnectionBlock block : manager.getInventories()) {
				capturedList.add(block);
				BlockPos pos = block.getBlockPos();
				if (level.hasChunkAt(pos)) {
					BlockEntity be = level.getBlockEntity(pos);
					// Index the block's non-directional state [3]
					IItemHandler nullHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
					if (nullHandler == null) {
						// Check the dynamic capability bridge fallback registry [3]
						nullHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.ItemHandler.BLOCK,
								level, pos, level.getBlockState(pos), null);
					}
					if (nullHandler != null) {
						map.put(new SnapshotKey(pos, null), createInventorySnapshot(nullHandler, be, null));
					}
					// Index all 6 active directions independently [3]
					for (Direction dir : Direction.values()) {
						IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
						if (handler == null) {
							// Check the dynamic capability bridge fallback registry [3]
							handler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.ItemHandler.BLOCK,
									level, pos, level.getBlockState(pos), dir);
						}
						if (handler != null) {
							map.put(new SnapshotKey(pos, dir), createInventorySnapshot(handler, be, dir));
						}
					}
				}
			}
		}
		return new ThreadSafeInventorySnapshot(map, capturedList);
	}

	private static InventorySnapshot createInventorySnapshot(IItemHandler handler, @Nullable BlockEntity be,
			@Nullable Direction side) {
		Map<Integer, SlotSnapshot> slots = new HashMap<>();
		int count = handler.getSlots();
		int[] faceSlots = null;

		// Resolve WorldlyContainer accessible slot indexes for side-specific mapping
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

	public @Nullable InventorySnapshot getInventory(BlockPos pos, @Nullable Direction side) {
		InventorySnapshot direct = snapshotMap.get(new SnapshotKey(pos, side));
		if (direct != null) {
			return direct;
		}
		// Fallback to non-directional if side-specific capability is absent [3]
		return snapshotMap.get(new SnapshotKey(pos, null));
	}

	public List<ConnectionBlock> getCapturedInventories() {
		return this.capturedInventories;
	}
}