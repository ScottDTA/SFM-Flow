package dta.sfmflow.datagen;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dta.sfmflow.SFMFlow;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

/**
 * Programmatic asset and data registry coordinator for SFM-Flow [3].
 * Hooks up the datagen pipeline to generate block state models, recipe schemas, loot tables,
 * tag configurations, item models, and localization asset packages [3].
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public class DataGenerators
 {
  private DataGenerators()
   {
   }

  /**
   * Listens to the mod event bus to orchestrate and append active data providers [3].
   * Registers both client-side visual assets (block models, items, languages) and server-side logic files [3].
   *
   * @param event the data generation context event [3]
   */
  @SubscribeEvent
  public static void gatherData(GatherDataEvent event)
   {
    DataGenerator generator = event.getGenerator();
    PackOutput packOutput = generator.getPackOutput();
    ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
    CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
	
    generator.addProvider(event.includeServer(),
                          new LootTableProvider(packOutput,
                                                Collections.emptySet(),
                                                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new,
                                                                                               LootContextParamSets.BLOCK)),
                                                lookupProvider));
	
    generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));
	
    BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
    generator.addProvider(event.includeServer(), blockTagsProvider);
    generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
	
    generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
    generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));	
    generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "en_us"));
   }
 }