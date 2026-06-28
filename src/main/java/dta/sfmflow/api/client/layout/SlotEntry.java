package dta.sfmflow.api.client.layout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Public API record mapping a specific inventory slot index to its 2x pixel coordinates on the UI [3].
 */
public record SlotEntry(int index, int x, int y) {
	public static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("index").forGetter(SlotEntry::index),
			Codec.INT.fieldOf("x").forGetter(SlotEntry::x),
			Codec.INT.fieldOf("y").forGetter(SlotEntry::y)
	).apply(instance, SlotEntry::new));
}