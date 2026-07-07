package dta.sfmflow.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.layout.SlotLayout;
import dta.sfmflow.api.client.layout.SlotEntry;
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
 * disk [3]. Designed to be safely subclassed by third-party integration addons
 * [3].
 */
public class SlotLayoutProvider implements DataProvider {
	protected final PackOutput packOutput;
	protected final Map<ResourceLocation, SlotLayout> layouts = new HashMap<>();

	public SlotLayoutProvider(PackOutput packOutput) {
		this.packOutput = packOutput;
		buildLayouts();
	}

	/**
	 * Registers a custom slot layout configuration to be generated as a JSON asset [3].
	 *
	 * @param id     the unique Block registry identifier [3]
	 * @param layout the custom slot layout details [3]
	 */
	protected final void addLayout(ResourceLocation id, SlotLayout layout) {
		if (id != null && layout != null) {
			this.layouts.put(id, layout);
		}
	}

	/**
	 * Populates the layout map. Can be overridden by subclasses to register custom layouts [3].
	 */
	protected void buildLayouts() {
		// Furnace layout mimicking vanilla furnace UI (indices: 0 = Input, 1 = Fuel, 2 = Output)
		layouts.put(ResourceLocation.withDefaultNamespace("furnace"),
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"),
						176, 98, List.of(
								new SlotEntry(0, 56, 17), // Input Slot
								new SlotEntry(1, 56, 53), // Fuel Slot
								new SlotEntry(2, 115, 34) // Output Slot
						)));

		// Blast Furnace layout
		layouts.put(ResourceLocation.withDefaultNamespace("blast_furnace"),
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"),
						176, 98, List.of(
								new SlotEntry(0, 56, 17), 
								new SlotEntry(1, 56, 53), 
								new SlotEntry(2, 115, 34)
						)));

		// Smoker layout
		layouts.put(ResourceLocation.withDefaultNamespace("smoker"), 
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/furnace.png"), 
						176, 98, List.of(
								new SlotEntry(0, 56, 17), 
								new SlotEntry(1, 56, 53), 
								new SlotEntry(2, 115, 34)
						)));

		// Brewing Stand layout
		layouts.put(ResourceLocation.withDefaultNamespace("brewing_stand"), 
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/brewing_stand.png"),
						176, 98, List.of(
								new SlotEntry(0, 55, 50),
								new SlotEntry(1, 78, 57),
								new SlotEntry(2, 101, 50),
								new SlotEntry(3, 78, 16),
								new SlotEntry(4, 16, 16)
						)));

		// Dropper layout
		layouts.put(ResourceLocation.withDefaultNamespace("dropper"), 
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/dropper.png"),
						176, 98, List.of(
								new SlotEntry(0, 61, 16),
								new SlotEntry(1, 79, 16),
								new SlotEntry(2, 97, 16),
								new SlotEntry(3, 61, 34),
								new SlotEntry(4, 79, 34),
								new SlotEntry(5, 97, 34),
								new SlotEntry(6, 61, 52),
								new SlotEntry(7, 79, 52),
								new SlotEntry(8, 97, 52)
						)));

		// Dispenser layout
		layouts.put(ResourceLocation.withDefaultNamespace("dispenser"), 
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/dropper.png"),
						176, 98, List.of(
								new SlotEntry(0, 61, 16),
								new SlotEntry(1, 79, 16),
								new SlotEntry(2, 97, 16),
								new SlotEntry(3, 61, 34),
								new SlotEntry(4, 79, 34),
								new SlotEntry(5, 97, 34),
								new SlotEntry(6, 61, 52),
								new SlotEntry(7, 79, 52),
								new SlotEntry(8, 97, 52)
						)));

		// Crafter layout
		layouts.put(ResourceLocation.withDefaultNamespace("crafter"), 
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/crafter.png"),
						176, 98, List.of(
								new SlotEntry(0, 25, 16),
								new SlotEntry(1, 43, 16),
								new SlotEntry(2, 61, 16),
								new SlotEntry(3, 25, 34),
								new SlotEntry(4, 43, 34),
								new SlotEntry(5, 61, 34),
								new SlotEntry(6, 25, 52),
								new SlotEntry(7, 43, 52),
								new SlotEntry(8, 61, 52),
								new SlotEntry(9, 132, 34)
						)));

		// Hopper layout
		layouts.put(ResourceLocation.withDefaultNamespace("hopper"), 
				new SlotLayout(
						ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/slot_layouts/hopper.png"),
						176, 66, List.of(
								new SlotEntry(0, 43, 19),
								new SlotEntry(1, 61, 19),
								new SlotEntry(2, 79, 19),
								new SlotEntry(3, 97, 19),
								new SlotEntry(4, 115, 19)
						)));
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		CompletableFuture<?>[] futures = layouts.entrySet().stream().map(entry -> {
			ResourceLocation id = entry.getKey();
			SlotLayout layout = entry.getValue();
			JsonElement json = SlotLayout.CODEC.encodeStart(JsonOps.INSTANCE, layout)
					.getOrThrow(IllegalStateException::new);

			// Output resolved directly to assets namespace to match resource listener [3]
			Path path = packOutput.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
					.resolve(id.getNamespace() + "/slot_layouts/" + id.getPath() + ".json");
			return DataProvider.saveStable(cache, json, path);
		}).toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures);
	}

	@Override
	public String getName() {
		return "SFM-Flow Slot Layouts Data Provider";
	}
}