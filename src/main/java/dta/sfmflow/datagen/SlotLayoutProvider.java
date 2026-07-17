package dta.sfmflow.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dta.sfmflow.api.client.layout.SlotLayout;
import dta.sfmflow.api.client.layout.LayoutKey;
import dta.sfmflow.datagen.layout.ISlotLayoutSubProvider;
import dta.sfmflow.datagen.layout.VanillaSlotLayouts;
import dta.sfmflow.datagen.layout.MekanismSlotLayouts;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Programmatic datagen provider that serializes data-driven slot layouts to
 * disk under capability-specific subfolders.
 * Coordinates modular registrations using sub-provider classes.
 */
public class SlotLayoutProvider implements DataProvider {
	protected final PackOutput packOutput;
	protected final Map<LayoutKey, SlotLayout> layouts = new HashMap<>();
	private final List<ISlotLayoutSubProvider> subProviders = new ArrayList<>();

	public SlotLayoutProvider(PackOutput packOutput) {
		this.packOutput = packOutput;
		registerSubProviders();
		buildLayouts();
	}

	/**
	 * Registers all active modular layout sub-providers.
	 */
	private void registerSubProviders() {
		this.subProviders.add(new VanillaSlotLayouts());
		this.subProviders.add(new MekanismSlotLayouts());
	}

	/**
	 * Populates the layout map by executing all registered sub-providers.
	 */
	protected void buildLayouts() {
		for (ISlotLayoutSubProvider subProvider : subProviders) {
			subProvider.register(this::addLayout);
		}
	}

	/**
	 * Registers a custom slot layout configuration to be generated as a JSON asset.
	 */
	protected final void addLayout(ResourceLocation blockId, ResourceLocation capabilityId, SlotLayout layout) {
		if (blockId != null && capabilityId != null && layout != null) {
			this.layouts.put(new LayoutKey(blockId, capabilityId), layout);
		}
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		CompletableFuture<?>[] futures = layouts.entrySet().stream().map(entry -> {
			LayoutKey key = entry.getKey();
			SlotLayout layout = entry.getValue();
			JsonElement json = SlotLayout.CODEC.encodeStart(JsonOps.INSTANCE, layout)
					.getOrThrow(IllegalStateException::new);

			// Output path: block_namespace/slot_layouts/capability_path/block_path.json
			Path path = packOutput.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
					.resolve(key.blockId().getNamespace() + "/slot_layouts/" + key.capabilityId().getPath() + "/" + key.blockId().getPath() + ".json");
			return DataProvider.saveStable(cache, json, path);
		}).toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures);
	}

	@Override
	public String getName() {
		return "SFM-Flow Slot Layouts Data Provider";
	}
}