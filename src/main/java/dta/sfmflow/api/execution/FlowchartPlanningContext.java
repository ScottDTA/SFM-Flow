package dta.sfmflow.api.execution;

import dta.sfmflow.api.capability.EnergyTransferParams;
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
 * the lock-free task ring buffer during asynchronous evaluation runs.
 */
public interface FlowchartPlanningContext {

	ThreadSafeInventorySnapshot getSnapshot();

	Map<UUID, AbstractFlowComponent> getComponents();

	List<FlowComponentConnections> getConnections();

	List<ConnectionBlock> getConnectedInventories();

	void enqueue(UUID componentId);

	boolean tryWriteTask(ResourceLocation capabilityId, BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, Object taskParams);

	/**
	 * Retrieves a generic, dynamic in-transit wire buffer mapped to a capability ID.
	 *
	 * @param componentId the unique ID of the target component
	 * @param capabilityId the unique capability key (e.g., sfmflow:item, sfmflow:fluid)
	 * @return the active buffer Object, or null if empty
	 */
	default @Nullable Object getPipelineBuffer(UUID componentId, ResourceLocation capabilityId) {
		return null;
	}

	/**
	 * Configures a generic, dynamic in-transit wire buffer mapped to a capability ID.
	 *
	 * @param componentId the unique ID of the target component
	 * @param capabilityId the unique capability key (e.g., sfmflow:item, sfmflow:fluid)
	 * @param buffer the buffer Object to store
	 */
	default void setPipelineBuffer(UUID componentId, ResourceLocation capabilityId, Object buffer) {}

	/**
	 * Default convenience helper for standard item transfers, delegating to the generic pipeline map.
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
	 * Default convenience helper for standard item transfers, delegating to the generic pipeline map.
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
	 * Default convenience helper for standard fluid transfers, delegating to the generic pipeline map.
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
	 * Default convenience helper for standard fluid transfers, delegating to the generic pipeline map.
	 */
	default void setFluidComponentBuffer(UUID componentId, FlowFluidBuffer buffer) {
		ResourceLocation fluidCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid");
		setPipelineBuffer(componentId, fluidCapId, buffer);
	}

	/**
	 * Default convenience helper for standard fluid transfer tasks, delegating to the generic pipeline map.
	 */
	default boolean tryWriteFluidTask(BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, FluidStack stack, int amount) {
		ResourceLocation fluidCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid");
		return tryWriteTask(fluidCapId, src, srcSlot, srcSide, dest, destSlot, destSide, new FluidTransferParams(stack, amount));
	}
	
	/**
	 * Default convenience helper for standard energy transfers, delegating to the generic pipeline map.
	 */
	default FlowEnergyBuffer getEnergyComponentBuffer(UUID componentId) {
		ResourceLocation energyCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "energy");
		Object buffer = getPipelineBuffer(componentId, energyCapId);
		if (buffer instanceof FlowEnergyBuffer energyBuffer) {
			return energyBuffer;
		}
		FlowEnergyBuffer newBuffer = new FlowEnergyBuffer();
		setPipelineBuffer(componentId, energyCapId, newBuffer);
		return newBuffer;
	}

	default void setEnergyComponentBuffer(UUID componentId, FlowEnergyBuffer buffer) {
		ResourceLocation energyCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "energy");
		setPipelineBuffer(componentId, energyCapId, buffer);
	}

	default boolean tryWriteEnergyTask(BlockPos src, @Nullable Direction srcSide, BlockPos dest, @Nullable Direction destSide, int amount) {
		ResourceLocation energyCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "energy");
		return tryWriteTask(energyCapId, src, 0, srcSide, dest, 0, destSide, new EnergyTransferParams(amount));
	}
	
	/**
	 * Dynamically copies all active pipeline buffers (including standard and custom addon types) 
	 * from a source component ID to a destination component ID.
	 */
	default void copyPipelineBuffers(UUID srcComponentId, UUID destComponentId) {}
	
}
