package dta.sfmflow.datagen;

import java.util.Set;
import dta.sfmflow.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

/**
 * Generates blocks' loot tables and validates drop rules during programmatic
 * data runs.
 */
public class ModBlockLootTableProvider extends BlockLootSubProvider {

	protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
		super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
	}

	@Override
	protected void generate() {
		dropSelf(ModBlocks.MANAGER_BLOCK.get());
		dropSelf(ModBlocks.CABLE_BLOCK.get());
		dropSelf(ModBlocks.HARDENED_CABLE_BLOCK.get());
		dropSelf(ModBlocks.REDSTONE_EMITTER_BLOCK.get());
		dropSelf(ModBlocks.REDSTONE_RECEIVER_BLOCK.get());
		dropSelf(ModBlocks.OBSERVER_CABLE_BLOCK.get());
		dropSelf(ModBlocks.ITEM_EJECTOR_VALVE_BLOCK.get());
		dropSelf(ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get());
		dropSelf(ModBlocks.FLUID_HATCH_CABLE_BLOCK.get());
		dropSelf(ModBlocks.CABLE_CLUSTER_BLOCK.get());
		dropSelf(ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get());
	}

	@Override
	protected Iterable<Block> getKnownBlocks() {
		return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
	}
}