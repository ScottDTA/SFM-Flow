package dta.sfmflow.datagen;

import java.util.concurrent.CompletableFuture;
import dta.sfmflow.block.ModBlocks;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

/**
 * Generates custom shaped and shapeless crafting recipes during datagen phases.
 */
public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
	public ModRecipeProvider(PackOutput output, CompletableFuture<Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void buildRecipes(RecipeOutput recipeOutput) {
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANAGER_BLOCK.get()).pattern("III").pattern("IRI")
				.pattern("SPS").define('I', Items.IRON_INGOT).define('R', Blocks.REDSTONE_BLOCK)
				.define('S', Blocks.STONE).define('P', Blocks.PISTON)
				.unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).unlockedBy("has_redstone", has(Items.REDSTONE))
				.save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CABLE_BLOCK.get(), 8).pattern("GPG").pattern("IRI")
				.pattern("GPG").define('R', Items.REDSTONE).define('G', Items.GLASS).define('I', Items.IRON_INGOT)
				.define('P', Items.LIGHT_WEIGHTED_PRESSURE_PLATE).unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
				.save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HARDENED_CABLE_BLOCK.get(), 4).pattern("OCO")
				.pattern("COC").pattern("OCO").define('C', ModBlocks.CABLE_BLOCK.get()).define('O', Blocks.OBSIDIAN)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.REDSTONE_EMITTER_BLOCK.get(), 1).pattern("ITI")
				.pattern("RCR").pattern("ITI").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('T', Items.REDSTONE_TORCH).define('R', Items.REDSTONE)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.REDSTONE_RECEIVER_BLOCK.get(), 1).pattern("IPI")
				.pattern("RCR").pattern("IPI").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('P', Items.REPEATER).define('R', Items.REDSTONE)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.OBSERVER_CABLE_BLOCK.get(), 1).pattern("IOI")
				.pattern("CCC").pattern("III").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('O', Blocks.OBSERVER).unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get()))
				.save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get(), 1).pattern("IHI")
				.pattern("CKC").pattern("IHI").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('H', Items.HOPPER).define('K', Items.CHEST)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ITEM_EJECTOR_VALVE_BLOCK.get(), 1).pattern("IDI")
				.pattern("CKC").pattern("IDI").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('D', Items.DROPPER).define('K', Items.CHEST)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FLUID_EJECTOR_VALVE_BLOCK.get(), 1).pattern("IDI")
				.pattern("CGC").pattern("IBI").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('B', Items.BUCKET).define('G', Items.GLASS).define('D', Items.DROPPER)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get(), 1).pattern("IHI")
				.pattern("CGC").pattern("IBI").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('B', Items.BUCKET).define('G', Items.GLASS).define('H', Items.HOPPER)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		// Cable Cluster Block (4x Cables, 4x Iron Ingots, 1x Chest)
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CABLE_CLUSTER_BLOCK.get(), 1).pattern("ICI")
				.pattern("CKC").pattern("ICI").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('K', Items.CHEST).unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		// Advanced Cable Cluster (Cable Cluster + 4x Hardened Cables + 4x Diamonds)
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get(), 1).pattern("HDH")
				.pattern("DKD").pattern("HDH").define('K', ModBlocks.CABLE_CLUSTER_BLOCK.get())
				.define('H', ModBlocks.HARDENED_CABLE_BLOCK.get()).define('D', Items.DIAMOND)
				.unlockedBy("has_cluster", has(ModBlocks.CABLE_CLUSTER_BLOCK.get())).save(recipeOutput);

		// Item Relay (classic 1.7.10 SFM Inventory Relay recipe)
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ITEM_RELAY_BLOCK.get(), 1).pattern("IHI")
				.pattern("CGC").pattern("IKI").define('I', Items.IRON_INGOT).define('H', Items.HOPPER)
				.define('C', ModBlocks.CABLE_BLOCK.get()).define('G', Items.GOLD_INGOT).define('K', Items.CHEST)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		// Sculk Trigger Cable shaped crafting recipe
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SCULK_TRIGGER_CABLE_BLOCK.get(), 1).pattern("IAI")
				.pattern("CSC").pattern("III").define('C', ModBlocks.CABLE_BLOCK.get()).define('I', Items.IRON_INGOT)
				.define('S', Blocks.SCULK_SENSOR) // Acoustic detection
				.define('A', Items.AMETHYST_SHARD) // Calibrated directional shielding
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		// Fluid Relay (classic 1.7.10 SFM Liquid Relay recipe)
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FLUID_RELAY_BLOCK.get(), 1).pattern("IGI")
				.pattern("CDC").pattern("IBI").define('I', Items.IRON_INGOT).define('G', Items.GLASS)
				.define('C', ModBlocks.CABLE_BLOCK.get()).define('D', Items.GOLD_INGOT).define('B', Items.BUCKET)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

		// Energy Relay (consistently styled classic SFM recipe)
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ENERGY_RELAY_BLOCK.get(), 1).pattern("IPI")
				.pattern("CDC").pattern("IRI").define('I', Items.IRON_INGOT).define('P', Items.COPPER_INGOT)
				.define('C', ModBlocks.CABLE_BLOCK.get()).define('D', Items.GOLD_INGOT)
				.define('R', Blocks.REDSTONE_BLOCK).unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get()))
				.save(recipeOutput);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.SIGN_UPDATER_CABLE_BLOCK.get())
				.requires(ModBlocks.CABLE_BLOCK.get()).requires(ItemTags.SIGNS)
				.unlockedBy("has_cable", has(ModBlocks.CABLE_BLOCK.get())).save(recipeOutput);

	}
}