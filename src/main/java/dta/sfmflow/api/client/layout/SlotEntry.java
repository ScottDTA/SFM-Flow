package dta.sfmflow.api.client.layout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

/**
 * Public API record mapping a specific inventory slot index to its 2x pixel coordinates and size metrics on the UI [3].
 */
public record SlotEntry(
		int index, 
		int x, 
		int y, 
		int width, 
		int height, 
		boolean useGenericTexture, 
		Optional<ResourceLocation> customTexture
) {
	// Traditional constructor mapping standard 18x18 square nodes [3]
	public SlotEntry(int index, int x, int y) {
		this(index, x, y, 18, 18, true, Optional.empty());
	}

	public static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("index").forGetter(SlotEntry::index),
			Codec.INT.fieldOf("x").forGetter(SlotEntry::x),
			Codec.INT.fieldOf("y").forGetter(SlotEntry::y),
			Codec.INT.optionalFieldOf("width", 18).forGetter(SlotEntry::width),
			Codec.INT.optionalFieldOf("height", 18).forGetter(SlotEntry::height),
			Codec.BOOL.optionalFieldOf("useGenericTexture", true).forGetter(SlotEntry::useGenericTexture),
			ResourceLocation.CODEC.optionalFieldOf("customTexture").forGetter(SlotEntry::customTexture)
	).apply(instance, SlotEntry::new));
}