package dta.sfmflow.api.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Public API registry allowing addon developers to register specialty blocks
 * that should always report a capability as present during pathfinder scans.
 */
public final class FlowCapabilityPresenceRegistry {
	private static final Map<ResourceLocation, Set<Block>> ALWAYS_PRESENT = new HashMap<>();

	private FlowCapabilityPresenceRegistry() {}

	/**
	 * Registers a block to always report a specific capability as present.
	 */
	public static void registerAlwaysPresent(ResourceLocation capabilityId, Block block) {
		if (capabilityId != null && block != null) {
			ALWAYS_PRESENT.computeIfAbsent(capabilityId, k -> new HashSet<>()).add(block);
		}
	}

	/**
	 * Checks if a block is registered to always report the capability as present.
	 */
	public static boolean isAlwaysPresent(ResourceLocation capabilityId, Block block) {
		Set<Block> blocks = ALWAYS_PRESENT.get(capabilityId);
		return blocks != null && blocks.contains(block);
	}
}