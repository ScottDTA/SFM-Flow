package dta.sfmflow.datagen;

import dta.sfmflow.SFMFlow;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
	public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, SFMFlow.MODID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		// basicItem(ModItems.TEST_ITEM.get());

	}

}