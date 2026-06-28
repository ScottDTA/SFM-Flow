package dta.sfmflow.datagen;

import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.registry.ModTags;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Automates mining and cable topology tag compilation [3].
 */
public class ModBlockTagProvider extends BlockTagsProvider {
	public ModBlockTagProvider(PackOutput output, CompletableFuture<Provider> lookupProvider,
			@Nullable ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, SFMFlow.MODID, existingFileHelper);
	}

	@Override
	protected void addTags(Provider provider) {
		tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.MANAGER_BLOCK.get(), ModBlocks.CABLE_BLOCK.get(),
				ModBlocks.HARDENED_CABLE_BLOCK.get(), ModBlocks.REDSTONE_EMITTER_BLOCK.get(),
				ModBlocks.REDSTONE_RECEIVER_BLOCK.get(), ModBlocks.OBSERVER_CABLE_BLOCK.get(),
				ModBlocks.ITEM_EJECTOR_HATCH_BLOCK.get(), ModBlocks.ITEM_VACUUM_HATCH_BLOCK.get(),
				ModBlocks.FLUID_HATCH_CABLE_BLOCK.get(), ModBlocks.CABLE_CLUSTER_BLOCK.get(),
				ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get());

		tag(ModTags.CABLES).add(ModBlocks.CABLE_BLOCK.get(), ModBlocks.HARDENED_CABLE_BLOCK.get(),
				ModBlocks.REDSTONE_EMITTER_BLOCK.get(), ModBlocks.REDSTONE_RECEIVER_BLOCK.get(),
				ModBlocks.OBSERVER_CABLE_BLOCK.get(), ModBlocks.ITEM_EJECTOR_HATCH_BLOCK.get(),
				ModBlocks.ITEM_VACUUM_HATCH_BLOCK.get(), ModBlocks.FLUID_HATCH_CABLE_BLOCK.get(),
				ModBlocks.CABLE_CLUSTER_BLOCK.get(), ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get());

		tag(ModTags.REDSTONE_CABLES).add(ModBlocks.REDSTONE_EMITTER_BLOCK.get(),
				ModBlocks.REDSTONE_RECEIVER_BLOCK.get(), ModBlocks.OBSERVER_CABLE_BLOCK.get());

		// Populate 3D special render blocks tag to handle containers programmatically [3]
		tag(ModTags.SPECIAL_3D_RENDERS).add(
				Blocks.CHEST,
				Blocks.TRAPPED_CHEST,
				Blocks.ENDER_CHEST
		);
	}
}