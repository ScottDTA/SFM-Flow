
package dta.sfmflow.datagen;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
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
		// Generate standard flat item model layout for our dynamic variables item card
		// [3]
		basicItem(ModItems.VARIABLE_CARD.get());
	}
}
