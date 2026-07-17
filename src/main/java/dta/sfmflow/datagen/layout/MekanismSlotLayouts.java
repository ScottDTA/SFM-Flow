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

		registrar.add(
				ResourceLocation.fromNamespaceAndPath("mekanism", "basic_fluid_tank"), 
				fluidCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/mek_tank.png"),
						176, 98, List.of(
								// Tall non-square vertical bar representing the chemical fluid level tank
								new SlotEntry(0, 49, 19, 64, 48, false, noCustomTex)
						)));

		registrar.add(
				ResourceLocation.fromNamespaceAndPath("mekanism", "basic_fluid_tank"), 
				itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/mek_tank.png"),
						176, 98, List.of(
								new SlotEntry(0, 146, 19, 16, 16, false, noCustomTex), // Input Slot
								new SlotEntry(1, 146, 51, 16, 16, false, noCustomTex)  // Output Slot
						)));
	}
}