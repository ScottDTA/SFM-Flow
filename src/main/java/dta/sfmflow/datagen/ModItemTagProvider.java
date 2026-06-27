package dta.sfmflow.datagen;

import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Programmatic item tag compiler for data generation cycles [3].
 */
public class ModItemTagProvider extends ItemTagsProvider
 {
  public ModItemTagProvider(PackOutput output,
		                    CompletableFuture<HolderLookup.Provider> lookupProvider,
			                CompletableFuture<TagLookup<Block>> blockTags,
			                @Nullable ExistingFileHelper existingFileHelper)
   {
	super(output, lookupProvider, blockTags, SFMFlow.MODID, existingFileHelper);
   }

  @Override
  protected void addTags(Provider provider)
   {
    tag(ModTags.CLUSTER_COMPATIBLE).add(
        ModBlocks.CABLE_BLOCK.get().asItem(),
        ModBlocks.HARDENED_CABLE_BLOCK.get().asItem(),
        ModBlocks.REDSTONE_EMITTER_BLOCK.get().asItem(),
        ModBlocks.REDSTONE_RECEIVER_BLOCK.get().asItem(),
        ModBlocks.OBSERVER_CABLE_BLOCK.get().asItem(),
        ModBlocks.ITEM_VACUUM_HATCH_BLOCK.get().asItem(),
        ModBlocks.ITEM_EJECTOR_HATCH_BLOCK.get().asItem(),
        ModBlocks.FLUID_HATCH_CABLE_BLOCK.get().asItem()
    );
   }
 }