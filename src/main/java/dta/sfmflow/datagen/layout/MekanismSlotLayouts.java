package dta.sfmflow.datagen.layout;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.layout.SlotEntry;
import dta.sfmflow.api.client.layout.SlotLayout;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Modular layout provider for Mekanism container configurations.
 */
public class MekanismSlotLayouts implements ISlotLayoutSubProvider {
	@Override
	public void register(Registrar registrar) {
		ResourceLocation fluidCapId = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "fluid");
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "item");
		Optional<ResourceLocation> noCustomTex = Optional.empty();

		// Compile the target Mekanism fluid tank block locations
		List<ResourceLocation> fluidTanks = List.of(
				ResourceLocation.fromNamespaceAndPath("mekanism", "basic_fluid_tank"),
				ResourceLocation.fromNamespaceAndPath("mekanism", "advanced_fluid_tank"),
				ResourceLocation.fromNamespaceAndPath("mekanism", "elite_fluid_tank"),
				ResourceLocation.fromNamespaceAndPath("mekanism", "ultimate_fluid_tank"),
				ResourceLocation.fromNamespaceAndPath("mekanism", "creative_fluid_tank")
		);

		// Expose fluid tank level bar on the "fluid" capability context
		registrar.add(fluidTanks, fluidCapId, new SlotLayout(
				ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/mek_tank.png"),
				176, 98, List.of(
						new SlotEntry(0, 49, 19, 64, 48, false, noCustomTex)
				)));

		// Expose item input/output slots on the "item" capability context
		registrar.add(fluidTanks, itemCapId, new SlotLayout(
				ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/mek_tank.png"),
				176, 98, List.of(
						new SlotEntry(0, 146, 19, 16, 16, false, noCustomTex),
						new SlotEntry(1, 146, 51, 16, 16, false, noCustomTex)
				)));
	}
}