package dta.sfmflow.datagen;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Programmatic item model compiler for data generation cycles.
 */
public class ModItemModelProvider extends ItemModelProvider {
	public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, SFMFlow.MODID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		getBuilder(ModItems.VARIABLE_CARD.getId().getPath())
				.parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("builtin/entity")))
				.texture("layer0", ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
						"item/" + ModItems.VARIABLE_CARD.getId().getPath()));

		getBuilder(ModItems.VARIABLE_CARD.getId().getPath() + "_flat")
				.parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("item/generated")))
				.texture("layer0", ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
						"item/" + ModItems.VARIABLE_CARD.getId().getPath()));

		getBuilder(ModItems.PROGRAM_DISK.getId().getPath())
				.parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("item/generated")))
				.texture("layer0", ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
						"item/" + ModItems.PROGRAM_DISK.getId().getPath()));

	}
}