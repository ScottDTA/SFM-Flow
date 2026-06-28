package dta.sfmflow.api.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.util.ConnectionBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable thread-safe snapshot container holding deep copies of target
 * inventory slot configurations [3]. Instantiated strictly on the main server
 * thread to prevent concurrency collisions during evaluation runs [3]. Upgraded
 * to query block capabilities using standard sided contexts [3].
 */
public final class ThreadSafeInventorySnapshot {

	public record SlotSnapshot(ItemStack stack, int slotLimit) {
		public SlotSnapshot {
			stack = stack.copy(); // Ensure a deep copy of the ItemStack [3]
		}
	}

	public record InventorySnapshot(Map<Integer, SlotSnapshot> slots) {
		public InventorySnapshot(Map<Integer, SlotSnapshot> slots) {
			this.slots = new HashMap<>(slots);
		}
	}

	private final Map<BlockPos, InventorySnapshot> snapshotMap;

	private ThreadSafeInventorySnapshot(Map<BlockPos, InventorySnapshot> snapshotMap) {
		this.snapshotMap = Collections.unmodifiableMap(new HashMap<>(snapshotMap));
	}

	/**
	 * Resolves the inventory capability safely, utilizing sided contexts for
	 * vanilla container compatibility [3].
	 */
	public static @Nullable IItemHandler getItemHandler(Level level, BlockPos pos) {
		// Query with Direction.UP to ensure standard containers return their capability
		// [3]
		IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
		if (handler != null) {
			return handler;
		}
		return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
	}

	/**
	 * Creates a thread-safe deep copy snapshot of all loaded inventories connected
	 * to the manager block entity [3].
	 *
	 * @param manager the managing block entity [3]
	 * @return a complete immutable snapshot containing deep copies [3]
	 */
	public static ThreadSafeInventorySnapshot create(ManagerBlockEntity manager) {
		Map<BlockPos, InventorySnapshot> map = new HashMap<>();
		var level = manager.getLevel();
		if (level != null && !level.isClientSide()) {
			for (ConnectionBlock block : manager.getInventories()) {
				BlockPos pos = block.getBlockPos();
				if (level.hasChunkAt(pos)) {
					IItemHandler handler = getItemHandler(level, pos);
					if (handler != null) {
						Map<Integer, SlotSnapshot> slots = new HashMap<>();
						int count = handler.getSlots();
						for (int i = 0; i < count; i++) {
							ItemStack stack = handler.getStackInSlot(i);
							slots.put(i, new SlotSnapshot(stack, handler.getSlotLimit(i)));
						}
						map.put(pos, new InventorySnapshot(slots));
					}
				}
			}
		}
		return new ThreadSafeInventorySnapshot(map);
	}

	public @Nullable InventorySnapshot getInventory(BlockPos pos) {
		return snapshotMap.get(pos);
	}

	public Map<BlockPos, InventorySnapshot> getSnapshotMap() {
		return snapshotMap;
	}
}