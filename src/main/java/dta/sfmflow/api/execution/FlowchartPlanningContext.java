package dta.sfmflow.api.execution;

import dta.sfmflow.api.capability.FluidTransferParams;
import dta.sfmflow.api.capability.ItemTransferParams;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public API context providing safe access to flowchart components, connections, and
 * the lock-free task ring buffer during asynchronous evaluation runs [3].
 */
public interface FlowchartPlanningContext {

	ThreadSafeInventorySnapshot getSnapshot();

	Map<UUID, AbstractFlowComponent> getComponents();

	List<FlowComponentConnections> getConnections();

	List<ConnectionBlock> getConnectedInventories();

	void enqueue(UUID componentId);

	boolean tryWriteTask(ResourceLocation capabilityId, BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, Object taskParams);

	/**
	 * Retrieves a generic, dynamic in-transit wire buffer mapped to a capability ID [3].
	 *
	 * @param componentId the unique ID of the target component [3]
	 * @param capabilityId the unique capability key (e.g., sfmflow:item, sfmflow:fluid) [3]
	 * @return the active buffer Object, or null if empty [3]
	 */
	default @Nullable Object getPipelineBuffer(UUID componentId, ResourceLocation capabilityId) {
		return null;
	}

	/**
	 * Configures a generic, dynamic in-transit wire buffer mapped to a capability ID [3].
	 *
	 * @param componentId the unique ID of the target component [3]
	 * @param capabilityId the unique capability key (e.g., sfmflow:item, sfmflow:fluid) [3]
	 * @param buffer the buffer Object to store [3]
	 */
	default void setPipelineBuffer(UUID componentId, ResourceLocation capabilityId, Object buffer) {}

	/**
	 * Default convenience helper for standard item transfers, delegating to the generic pipeline map [3].
	 */
	default FlowItemBuffer getComponentBuffer(UUID componentId) {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
		Object buffer = getPipelineBuffer(componentId, itemCapId);
		if (buffer instanceof FlowItemBuffer itemBuffer) {
			return itemBuffer;
		}
		FlowItemBuffer newBuffer = new FlowItemBuffer();
		setPipelineBuffer(componentId, itemCapId, newBuffer);
		return newBuffer;
	}

	/**
	 * Default convenience helper for standard item transfers, delegating to the generic pipeline map [3].
	 */
	default void setComponentBuffer(UUID componentId, FlowItemBuffer buffer) {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
		setPipelineBuffer(componentId, itemCapId, buffer);
	}

	default boolean tryWriteTask(BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, ItemStack stack, int amount) {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
		return tryWriteTask(itemCapId, src, srcSlot, srcSide, dest, destSlot, destSide, new ItemTransferParams(srcSlot, destSlot, stack, amount));
	}
	
	/**
	 * Default convenience helper for standard fluid transfers, delegating to the generic pipeline map [3].
	 */
	default FlowFluidBuffer getFluidComponentBuffer(UUID componentId) {
		ResourceLocation fluidCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid");
		Object buffer = getPipelineBuffer(componentId, fluidCapId);
		if (buffer instanceof FlowFluidBuffer fluidBuffer) {
			return fluidBuffer;
		}
		FlowFluidBuffer newBuffer = new FlowFluidBuffer();
		setPipelineBuffer(componentId, fluidCapId, newBuffer);
		return newBuffer;
	}

	/**
	 * Default convenience helper for standard fluid transfers, delegating to the generic pipeline map [3].
	 */
	default void setFluidComponentBuffer(UUID componentId, FlowFluidBuffer buffer) {
		ResourceLocation fluidCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid");
		setPipelineBuffer(componentId, fluidCapId, buffer);
	}

	/**
	 * Default convenience helper for standard fluid transfer tasks, delegating to the generic pipeline map [3].
	 */
	default boolean tryWriteFluidTask(BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, FluidStack stack, int amount) {
		ResourceLocation fluidCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid");
		return tryWriteTask(fluidCapId, src, srcSlot, srcSide, dest, destSlot, destSide, new FluidTransferParams(stack, amount));
	}
}
