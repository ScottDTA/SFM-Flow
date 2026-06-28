package dta.sfmflow.api.client.layout;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Public client-only API registry allowing addon developers to register custom
 * visual slot layouts programmatically [3].
 */
@OnlyIn(Dist.CLIENT)
public final class FlowLayoutRegistry {
	private static final Map<ResourceLocation, SlotLayout> REGISTRY = new HashMap<>();

	private FlowLayoutRegistry() {}

	/**
	 * Registers a custom slot layout programmatically for a specific block registry key [3].
	 *
	 * @param blockId the registry identifier of the block [3]
	 * @param layout  the custom slot layout details [3]
	 */
	public static void register(ResourceLocation blockId, SlotLayout layout) {
		if (blockId != null && layout != null) {
			REGISTRY.put(blockId, layout);
		}
	}

	/**
	 * Retrieves the programmatically registered slot layout for a specific block registry key [3].
	 *
	 * @param blockId the registry identifier of the block [3]
	 * @return the SlotLayout, or null if unregistered [3]
	 */
	public static @Nullable SlotLayout getLayout(ResourceLocation blockId) {
		return REGISTRY.get(blockId);
	}

	public static void clear() {
		REGISTRY.clear();
	}
}