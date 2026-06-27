package dta.sfmflow.registry;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.entity.ModBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Common capabilities registration subscriber routing automated NeoForge capability queries [3].
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public class ModCapabilities {

    /**
     * Subscribes to RegisterCapabilitiesEvent to expose item and fluid capacities side-safely [3].
     *
     * @param event capabilities registration event [3]
     */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
    	event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, 
                ModBlockEntities.ITEM_EJECTOR_HATCH_BE.get(), 
                (be, side) -> be.getItemHandler(side)
            );

            event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, 
                ModBlockEntities.ITEM_VACUUM_HATCH_BE.get(), 
                (be, side) -> be.getItemHandler(side)
            );

        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK, 
            ModBlockEntities.FLUID_HATCH_CABLE_BE.get(), 
            (be, side) -> be.getFluidHandler(side)
        );

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.CABLE_CLUSTER_BE.get(),
            (be, side) -> be.getItemHandler(side)
        );

        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            ModBlockEntities.CABLE_CLUSTER_BE.get(),
            (be, side) -> be.getFluidHandler(side)
        );
    }
}
