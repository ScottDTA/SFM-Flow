package dta.sfmflow.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.layout.SlotLayout;
import dta.sfmflow.api.client.layout.SlotEntry;
import dta.sfmflow.api.client.layout.LayoutKey;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Programmatic datagen provider that serializes data-driven slot layouts to
 * disk under capability-specific subfolders [3].
 */
public class SlotLayoutProvider implements DataProvider {
	protected final PackOutput packOutput;
	protected final Map<LayoutKey, SlotLayout> layouts = new HashMap<>();

	public SlotLayoutProvider(PackOutput packOutput) {
		this.packOutput = packOutput;
		buildLayouts();
	}

	/**
	 * Registers a custom slot layout configuration to be generated as a JSON asset [3].
	 */
	protected final void addLayout(ResourceLocation blockId, ResourceLocation capabilityId, SlotLayout layout) {
		if (blockId != null && capabilityId != null && layout != null) {
			this.layouts.put(new LayoutKey(blockId, capabilityId), layout);
		}
	}

	/**
	 * Populates the layout map. Set useGenericTexture to false for slot positions
	 * baked directly into the custom background art [3].
	 */
	protected void buildLayouts() {
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "item");
		java.util.Optional<ResourceLocation> noCustomTex = java.util.Optional.empty();

		// Furnace layout mimicking vanilla furnace UI
		addLayout(ResourceLocation.withDefaultNamespace("furnace"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"),
						176, 98, List.of(
								new SlotEntry(0, 56, 17, 16, 16, false, noCustomTex), // Input Slot [3]
								new SlotEntry(1, 56, 53, 16, 16, false, noCustomTex), // Fuel Slot [3]
								new SlotEntry(2, 111, 30, 26, 26, false, noCustomTex) // Output Slot [3]
						)));

		// Blast Furnace layout
		addLayout(ResourceLocation.withDefaultNamespace("blast_furnace"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"),
						176, 98, List.of(
								new SlotEntry(0, 56, 17, 16, 16, false, noCustomTex), 
								new SlotEntry(1, 56, 53, 16, 16, false, noCustomTex), 
								new SlotEntry(2, 111, 30, 26, 26, false, noCustomTex)
						)));

		// Smoker layout
		addLayout(ResourceLocation.withDefaultNamespace("smoker"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"), 
						176, 98, List.of(
								new SlotEntry(0, 56, 17, 16, 16, false, noCustomTex), 
								new SlotEntry(1, 56, 53, 16, 16, false, noCustomTex), 
								new SlotEntry(2, 111, 30, 26, 26, false, noCustomTex)
						)));

		// Brewing Stand layout
		addLayout(ResourceLocation.withDefaultNamespace("brewing_stand"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/brewing_stand.png"),
						176, 98, List.of(
								new SlotEntry(0, 56, 50, 16, 16, false, noCustomTex),
								new SlotEntry(1, 79, 57, 16, 16, false, noCustomTex),
								new SlotEntry(2, 102, 50, 16, 16, false, noCustomTex),
								new SlotEntry(3, 78, 16, 18, 18, false, noCustomTex),
								new SlotEntry(4, 17, 17, 16, 16, false, noCustomTex)
						)));

		// Dropper layout
		addLayout(ResourceLocation.withDefaultNamespace("dropper"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/dropper.png"),
						176, 98, List.of(
								new SlotEntry(0, 62, 17, 16, 16, false, noCustomTex),
								new SlotEntry(1, 80, 17, 16, 16, false, noCustomTex),
								new SlotEntry(2, 98, 17, 16, 16, false, noCustomTex),
								new SlotEntry(3, 62, 35, 16, 16, false, noCustomTex),
								new SlotEntry(4, 80, 35, 16, 16, false, noCustomTex),
								new SlotEntry(5, 98, 35, 16, 16, false, noCustomTex),
								new SlotEntry(6, 62, 53, 16, 16, false, noCustomTex),
								new SlotEntry(7, 80, 53, 16, 16, false, noCustomTex),
								new SlotEntry(8, 98, 53, 16, 16, false, noCustomTex)
						)));

		// Dispenser layout
		addLayout(ResourceLocation.withDefaultNamespace("dispenser"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/dropper.png"),
						176, 98, List.of(
								new SlotEntry(0, 62, 17, 16, 16, false, noCustomTex),
								new SlotEntry(1, 80, 17, 16, 16, false, noCustomTex),
								new SlotEntry(2, 98, 17, 16, 16, false, noCustomTex),
								new SlotEntry(3, 62, 35, 16, 16, false, noCustomTex),
								new SlotEntry(4, 80, 35, 16, 16, false, noCustomTex),
								new SlotEntry(5, 98, 35, 16, 16, false, noCustomTex),
								new SlotEntry(6, 62, 53, 16, 16, false, noCustomTex),
								new SlotEntry(7, 80, 53, 16, 16, false, noCustomTex),
								new SlotEntry(8, 98, 53, 16, 16, false, noCustomTex)
						)));

		// Crafter layout
		addLayout(ResourceLocation.withDefaultNamespace("crafter"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/crafter.png"),
						176, 98, List.of(
								new SlotEntry(0, 26, 17, 16, 16, false, noCustomTex),
								new SlotEntry(1, 44, 17, 16, 16, false, noCustomTex),
								new SlotEntry(2, 62, 17, 16, 16, false, noCustomTex),
								new SlotEntry(3, 26, 35, 16, 16, false, noCustomTex),
								new SlotEntry(4, 44, 35, 16, 16, false, noCustomTex),
								new SlotEntry(5, 62, 35, 16, 16, false, noCustomTex),
								new SlotEntry(6, 26, 53, 16, 16, false, noCustomTex),
								new SlotEntry(7, 44, 53, 16, 16, false, noCustomTex),
								new SlotEntry(8, 62, 53, 16, 16, false, noCustomTex),
								new SlotEntry(9, 129, 30, 26, 26, false, noCustomTex)
						)));

		// Hopper layout
		addLayout(ResourceLocation.withDefaultNamespace("hopper"), itemCapId,
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/hopper.png"),
						176, 66, List.of(
								new SlotEntry(0, 44, 20, 16, 16, false, noCustomTex),
								new SlotEntry(1, 62, 20, 16, 16, false, noCustomTex),
								new SlotEntry(2, 80, 20, 16, 16, false, noCustomTex),
								new SlotEntry(3, 98, 20, 16, 16, false, noCustomTex),
								new SlotEntry(4, 116, 20, 16, 16, false, noCustomTex)
						)));
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		CompletableFuture<?>[] futures = layouts.entrySet().stream().map(entry -> {
			LayoutKey key = entry.getKey();
			SlotLayout layout = entry.getValue();
			JsonElement json = SlotLayout.CODEC.encodeStart(JsonOps.INSTANCE, layout)
					.getOrThrow(IllegalStateException::new);

			// Output path: block_namespace/slot_layouts/capability_path/block_path.json [3]
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