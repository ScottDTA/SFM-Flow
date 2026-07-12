package dta.sfmflow.registry;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.FluidHatchCableBlock;
import dta.sfmflow.block.ItemEjectorValveBlock;
import dta.sfmflow.block.ItemVacuumValveBlock;
import dta.sfmflow.block.entity.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Common capabilities registration subscriber routing automated NeoForge
 * capability queries.
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public class ModCapabilities {

	/**
	 * Subscribes to RegisterCapabilitiesEvent to expose item and fluid capacities
	 * side-safely. Exposes capabilities only on the facing side of functional valve
	 * blocks.
	 *
	 * @param event capabilities registration event
	 */
	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		// Restrict Item Ejector capability exposure exclusively to its in-world facing
		// mouth
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ITEM_EJECTOR_HATCH_BE.get(),
				(be, side) -> {
					BlockState state = be.getBlockState();
					if (state.hasProperty(ItemEjectorValveBlock.FACING)
							&& side == state.getValue(ItemEjectorValveBlock.FACING)) {
						return be.getItemHandler(side);
					}
					return null;
				});

		// Restrict Item Vacuum capability exposure exclusively to its in-world facing
		// mouth
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ITEM_VACUUM_HATCH_BE.get(),
				(be, side) -> {
					BlockState state = be.getBlockState();
					if (state.hasProperty(ItemVacuumValveBlock.FACING)
							&& side == state.getValue(ItemVacuumValveBlock.FACING)) {
						return be.getItemHandler(side);
					}
					return null;
				});

		// Restrict Fluid Hatch capability exposure exclusively to its in-world facing
		// mouth
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FLUID_HATCH_CABLE_BE.get(),
				(be, side) -> {
					BlockState state = be.getBlockState();
					if (state.hasProperty(FluidHatchCableBlock.FACING)
							&& side == state.getValue(FluidHatchCableBlock.FACING)) {
						return be.getFluidHandler(side);
					}
					return null;
				});

		// Card clusters remain side-configurable across all directions
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CABLE_CLUSTER_BE.get(),
				(be, side) -> be.getItemHandler(side));

		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.CABLE_CLUSTER_BE.get(),
				(be, side) -> be.getFluidHandler(side));
	}
}