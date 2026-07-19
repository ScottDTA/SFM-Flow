package dta.sfmflow.datagen.layout;

import dta.sfmflow.api.client.layout.SlotLayout;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

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
		/**
		 * Core single-block layout registration mapping.
		 */
		void add(ResourceLocation blockId, ResourceLocation capabilityId, SlotLayout layout);

		/**
		 * Overloaded helper allowing clean registration of a single layout across a List of blocks.
		 */
		default void add(List<ResourceLocation> blockIds, ResourceLocation capabilityId, SlotLayout layout) {
			for (ResourceLocation blockId : blockIds) {
				add(blockId, capabilityId, layout);
			}
		}

		/**
		 * Varargs helper allowing clean registration of a single layout across a variable array of blocks.
		 */
		default void add(ResourceLocation capabilityId, SlotLayout layout, ResourceLocation... blockIds) {
			for (ResourceLocation blockId : blockIds) {
				add(blockId, capabilityId, layout);
			}
		}
	}
}