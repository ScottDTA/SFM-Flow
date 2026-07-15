package dta.sfmflow.client.screen.helper;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.layout.SlotLayout;
import dta.sfmflow.api.client.layout.LayoutKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only reload listener that reads data-driven slot layouts from assets under capability-specific subfolders.
 */
public final class SlotLayoutManager extends SimpleJsonResourceReloadListener {
	private static final Map<LayoutKey, SlotLayout> LAYOUTS = new HashMap<>();

	public SlotLayoutManager() {
		super(new GsonBuilder().create(), "slot_layouts");
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
		LAYOUTS.clear();
		for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
			ResourceLocation id = entry.getKey();
			try {
				SlotLayout layout = SlotLayout.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
						.getOrThrow(IllegalStateException::new);
				
				String path = id.getPath(); // e.g., "item/chemical_crystallizer"
				int slashIdx = path.indexOf('/');
				
				if (slashIdx != -1) {
					// Split subfolder and file name
					String capPath = path.substring(0, slashIdx);
					String blockPath = path.substring(slashIdx + 1);
					
					// Resolve composite coordinates
					ResourceLocation capabilityId = ResourceLocation.fromNamespaceAndPath("sfmflow", capPath);
					ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), blockPath);
					
					LAYOUTS.put(new LayoutKey(blockId, capabilityId), layout);
				} else {
					// Backward Compatibility Fallback: flat layouts default to item capability
					ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
					LAYOUTS.put(new LayoutKey(id, itemCapId), layout);
				}
			} catch (Exception e) {
				SFMFlow.LOGGER.error("Failed to parse slot layout JSON for {}", id, e);
			}
		}
	}

	/**
	 * Retrieves the custom layout configured for a specific composite layout key.
	 *
	 * @param key the composite layout key
	 * @return the SlotLayout, or null if using fallback
	 */
	public static @Nullable SlotLayout getLayout(LayoutKey key) {
		return LAYOUTS.get(key);
	}
}