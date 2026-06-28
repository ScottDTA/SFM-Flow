package dta.sfmflow.api.execution;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public API context providing safe access to flowchart components, connections, and
 * the lock-free task ring buffer during asynchronous evaluation runs [3].
 */
public interface FlowchartPlanningContext {

	/**
	 * Retrieves the deep-copied snapshot of connected inventories [3].
	 *
	 * @return the thread-safe snapshot instance [3]
	 */
	ThreadSafeInventorySnapshot getSnapshot();

	/**
	 * Retrieves an unmodifiable map of all registered flowchart components [3].
	 *
	 * @return active components map [3]
	 */
	Map<UUID, AbstractFlowComponent> getComponents();

	/**
	 * Retrieves an unmodifiable list of all wire connections on the active canvas [3].
	 *
	 * @return wire connections list [3]
	 */
	List<FlowComponentConnections> getConnections();

	/**
	 * Retrieves a read-only list of connected inventory blocks on the physical network [3].
	 *
	 * @return the scanned inventories [3]
	 */
	List<ConnectionBlock> getConnectedInventories();

	/**
	 * Enqueues a component ID to be processed next in the planning sequence [3].
	 *
	 * @param componentId the target component UUID [3]
	 */
	void enqueue(UUID componentId);

	/**
	 * Attempts to safely write an execution task to the ring buffer [3].
	 *
	 * @return true if successfully scheduled, false if the queue is full [3]
	 */
	boolean tryWriteTask(BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, ItemStack stack, int amount);
}