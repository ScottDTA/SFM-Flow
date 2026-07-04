package dta.sfmflow.datagen;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Programmatic item model compiler for data generation cycles [3].
 */
public class ModItemModelProvider extends ItemModelProvider {
	public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, SFMFlow.MODID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		/* STREAMING_CHUNK:Registering variable card item model */
		// Generates variable_card item model referencing the active dynamic entity template [3]
		getBuilder(ModItems.VARIABLE_CARD.getId().getPath())
				.parent(new ModelFile.UncheckedModelFile(
						ResourceLocation.withDefaultNamespace("builtin/entity")
				))
				.texture("layer0", ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "item/" + ModItems.VARIABLE_CARD.getId().getPath()));

		// Generates the hidden variable_card_flat item model utilized by the BEWLR renderer [3]
		getBuilder(ModItems.VARIABLE_CARD.getId().getPath() + "_flat")
				.parent(new ModelFile.UncheckedModelFile(
						ResourceLocation.withDefaultNamespace("item/generated")
				))
				.texture("layer0", ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "item/" + ModItems.VARIABLE_CARD.getId().getPath()));
	}
}