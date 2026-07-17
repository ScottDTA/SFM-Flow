package dta.sfmflow.datagen.layout;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.layout.SlotEntry;
import dta.sfmflow.api.client.layout.SlotLayout;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Modular layout provider containing all vanilla Minecraft container configurations.
 */
public class VanillaSlotLayouts implements ISlotLayoutSubProvider {
	@Override
	public void register(Registrar registrar) {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "item");
		Optional<ResourceLocation> noCustomTex = Optional.empty();

		// Furnace layout mimicking vanilla furnace UI
		registrar.add(ResourceLocation.withDefaultNamespace("furnace"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"),
						176, 98, List.of(
								new SlotEntry(0, 56, 17, 16, 16, false, noCustomTex),
								new SlotEntry(1, 56, 53, 16, 16, false, noCustomTex), 
								new SlotEntry(2, 111, 30, 26, 26, false, noCustomTex)
						)));

		// Blast Furnace layout
		registrar.add(ResourceLocation.withDefaultNamespace("blast_furnace"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"),
						176, 98, List.of(
								new SlotEntry(0, 56, 17, 16, 16, false, noCustomTex), 
								new SlotEntry(1, 56, 53, 16, 16, false, noCustomTex), 
								new SlotEntry(2, 111, 30, 26, 26, false, noCustomTex)
						)));

		// Smoker layout
		registrar.add(ResourceLocation.withDefaultNamespace("smoker"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"), 
						176, 98, List.of(
								new SlotEntry(0, 56, 17, 16, 16, false, noCustomTex), 
								new SlotEntry(1, 56, 53, 16, 16, false, noCustomTex), 
								new SlotEntry(2, 111, 30, 26, 26, false, noCustomTex)
						)));

		// Brewing Stand layout
		registrar.add(ResourceLocation.withDefaultNamespace("brewing_stand"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/brewing_stand.png"),
						176, 98, List.of(
								new SlotEntry(0, 56, 50, 16, 16, false, noCustomTex),
								new SlotEntry(1, 79, 57, 16, 16, false, noCustomTex),
								new SlotEntry(2, 102, 50, 16, 16, false, noCustomTex),
								new SlotEntry(3, 78, 16, 18, 18, false, noCustomTex),
								new SlotEntry(4, 17, 17, 16, 16, false, noCustomTex)
						)));

		// Dropper layout
		registrar.add(ResourceLocation.withDefaultNamespace("dropper"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/dropper.png"),
						176, 98, List.of(
								new SlotEntry(0, 62, 17, 16, 16, false, noCustomTex),
								new SlotEntry(1, 80, 17, 16, 16, false, noCustomTex),
								new SlotEntry(2, 98, 17, 16, 16, false, noCustomTex),
								new SlotEntry(3, 62, 35, 16, 16, false, noCustomTex),
								new SlotEntry(4, 80, 35, 16, 16, false, noCustomTex),
								new SlotEntry(5, 98, 35, 16, 16, false, noCustomTex),
								new SlotEntry(6, 62, 53, 16, 16, false, noCustomTex),
								new SlotEntry(7, 80, 53, 16, 16, false, noCustomTex),
								new SlotEntry(8, 98, 53, 16, 16, false, noCustomTex)
						)));

		// Dispenser layout
		registrar.add(ResourceLocation.withDefaultNamespace("dispenser"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/dropper.png"),
						176, 98, List.of(
								new SlotEntry(0, 62, 17, 16, 16, false, noCustomTex),
								new SlotEntry(1, 80, 17, 16, 16, false, noCustomTex),
								new SlotEntry(2, 98, 17, 16, 16, false, noCustomTex),
								new SlotEntry(3, 62, 35, 16, 16, false, noCustomTex),
								new SlotEntry(4, 80, 35, 16, 16, false, noCustomTex),
								new SlotEntry(5, 98, 35, 16, 16, false, noCustomTex),
								new SlotEntry(6, 62, 53, 16, 16, false, noCustomTex),
								new SlotEntry(7, 80, 53, 16, 16, false, noCustomTex),
								new SlotEntry(8, 98, 53, 16, 16, false, noCustomTex)
						)));

		// Crafter layout
		registrar.add(ResourceLocation.withDefaultNamespace("crafter"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/crafter.png"),
						176, 98, List.of(
								new SlotEntry(0, 26, 17, 16, 16, false, noCustomTex),
								new SlotEntry(1, 44, 17, 16, 16, false, noCustomTex),
								new SlotEntry(2, 62, 17, 16, 16, false, noCustomTex),
								new SlotEntry(3, 26, 35, 16, 16, false, noCustomTex),
								new SlotEntry(4, 44, 35, 16, 16, false, noCustomTex),
								new SlotEntry(5, 62, 35, 16, 16, false, noCustomTex),
								new SlotEntry(6, 26, 53, 16, 16, false, noCustomTex),
								new SlotEntry(7, 44, 53, 16, 16, false, noCustomTex),
								new SlotEntry(8, 62, 53, 16, 16, false, noCustomTex),
								new SlotEntry(9, 129, 30, 26, 26, false, noCustomTex)
						)));

		// Hopper layout
		registrar.add(ResourceLocation.withDefaultNamespace("hopper"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/hopper.png"),
						176, 66, List.of(
								new SlotEntry(0, 44, 20, 16, 16, false, noCustomTex),
								new SlotEntry(1, 62, 20, 16, 16, false, noCustomTex),
								new SlotEntry(2, 80, 20, 16, 16, false, noCustomTex),
								new SlotEntry(3, 98, 20, 16, 16, false, noCustomTex),
								new SlotEntry(4, 116, 20, 16, 16, false, noCustomTex)
						)));
	}
}