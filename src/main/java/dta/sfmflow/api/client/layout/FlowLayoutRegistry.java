package dta.sfmflow.api.client.layout;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Public client-only API registry allowing addon developers to register custom
 * visual slot layouts programmatically.
 */
@OnlyIn(Dist.CLIENT)
public final class FlowLayoutRegistry {
	private static final Map<LayoutKey, SlotLayout> REGISTRY = new HashMap<>();

	private FlowLayoutRegistry() {}

	/**
	 * Registers a custom slot layout programmatically for a specific block and capability.
	 *
	 * @param key    the composite layout key
	 * @param layout the custom slot layout details
	 */
	public static void register(LayoutKey key, SlotLayout layout) {
		if (key != null && layout != null) {
			REGISTRY.put(key, layout);
		}
	}

	/**
	 * Convenience overload to register programmatically with raw resource locations.
	 */
	public static void register(ResourceLocation blockId, ResourceLocation capabilityId, SlotLayout layout) {
		if (blockId != null && capabilityId != null && layout != null) {
			REGISTRY.put(new LayoutKey(blockId, capabilityId), layout);
		}
	}

	/**
	 * Retrieves the programmatically registered slot layout for a specific composite key.
	 *
	 * @param key the composite layout key
	 * @return the SlotLayout, or null if unregistered
	 */
	public static @Nullable SlotLayout getLayout(LayoutKey key) {
		return REGISTRY.get(key);
	}

	public static void clear() {
		REGISTRY.clear();
	}
}