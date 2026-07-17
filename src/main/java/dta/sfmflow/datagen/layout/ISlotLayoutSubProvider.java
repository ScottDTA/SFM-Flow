package dta.sfmflow.datagen.layout;

import dta.sfmflow.api.client.layout.SlotLayout;
import net.minecraft.resources.ResourceLocation;

/**
 * Common interface for modular slot layout providers.
 * Allows separating layout registration per-mod for high-performance datagen expansion.
 */
@FunctionalInterface
public interface ISlotLayoutSubProvider {
	/**
	 * Registers custom slot layouts using the provided registrar context.
	 */
	void register(Registrar registrar);

	@FunctionalInterface
	interface Registrar {
		void add(ResourceLocation blockId, ResourceLocation capabilityId, SlotLayout layout);
	}
}