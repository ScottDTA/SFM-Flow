package dta.sfmflow.compat;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import mekanism.api.chemical.IChemicalHandler;

public class MekanismCompat {
	private static BlockCapability<IChemicalHandler, Direction> chemicalCapability = null;

	public static BlockCapability<?, Direction> getChemicalCapability() {
		if (!ModList.get().isLoaded("mekanism")) {
			return null;
		}
		if (chemicalCapability == null) {
			// Under the hood, Mekanism exposes this canonical ResourceLocation for blocks
			chemicalCapability = BlockCapability.createSided(
					ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);
		}
		return chemicalCapability;
	}
}