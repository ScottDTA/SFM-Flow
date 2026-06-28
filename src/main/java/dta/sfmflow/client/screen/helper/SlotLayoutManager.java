package dta.sfmflow.client.screen.helper;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.layout.SlotLayout;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only reload listener that reads data-driven slot layouts from assets [3].
 */
public final class SlotLayoutManager extends SimpleJsonResourceReloadListener {
	private static final Map<ResourceLocation, SlotLayout> LAYOUTS = new HashMap<>();

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
				LAYOUTS.put(id, layout);
			} catch (Exception e) {
				SFMFlow.LOGGER.error("Failed to parse slot layout JSON for {}", id, e);
			}
		}
	}

	/**
	 * Retrieves the custom layout configured for a specific block registry key [3].
	 *
	 * @param blockId block identifier [3]
	 * @return the SlotLayout, or null if using fallback [3]
	 */
	public static @Nullable SlotLayout getLayout(ResourceLocation blockId) {
		return LAYOUTS.get(blockId);
	}
}