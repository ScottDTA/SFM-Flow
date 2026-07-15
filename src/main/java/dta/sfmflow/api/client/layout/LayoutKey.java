package dta.sfmflow.api.client.layout;

import net.minecraft.resources.ResourceLocation;

/**
 * Public API composite key pairing a block ID to a capability ID to enable multiple layouts per block.
 */
public record LayoutKey(ResourceLocation blockId, ResourceLocation capabilityId) {}