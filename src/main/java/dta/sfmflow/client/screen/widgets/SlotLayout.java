package dta.sfmflow.client.screen.widgets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/**
 * Data record defining custom visual background and slot layouts for specific inventories [3].
 */
public record SlotLayout(ResourceLocation background, int width, int height, List<SlotEntry> slots) {
	public static final Codec<SlotLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("background").forGetter(SlotLayout::background),
			Codec.INT.optionalFieldOf("width", 176).forGetter(SlotLayout::width),
			Codec.INT.optionalFieldOf("height", 166).forGetter(SlotLayout::height),
			SlotEntry.CODEC.listOf().fieldOf("slots").forGetter(SlotLayout::slots)
	).apply(instance, SlotLayout::new));
}