package dta.sfmflow.item;

import java.util.function.Supplier;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Common registry class managing mod creative mode tab additions [3].
 */
public class ModCreativeModeTabs
 {
  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SFMFlow.MODID);
  
  public static final Supplier<CreativeModeTab> SFMFLOW_ITEMS_TAB = CREATIVE_MODE_TAB.register("sfmflow_items_tab",
                                                                                               () -> CreativeModeTab.builder()
		                                                                                                            .icon(() -> new ItemStack(ModBlocks.MANAGER_BLOCK.get()))
		                                                                                                            .title(Component.translatable("itemGroup.sfmflow"))
		                                                                                                            .displayItems((itemDisplayParameters, output) ->
		                                                                                                             {
		                                                                                                              output.accept(ModBlocks.MANAGER_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.CABLE_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.HARDENED_CABLE_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.REDSTONE_EMITTER_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.REDSTONE_RECEIVER_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.OBSERVER_CABLE_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.ITEM_EJECTOR_HATCH_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.ITEM_VACUUM_HATCH_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.FLUID_HATCH_CABLE_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.CABLE_CLUSTER_BLOCK.get());
		                                                                                                              output.accept(ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get());
		                                                                                                             })
		                                                                                                            .build());
    
  public static void register(IEventBus eventBus)
   {
	CREATIVE_MODE_TAB.register(eventBus);
   }
 }