package dta.sfmflow.api.variable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.UUID;

/**
 * Data record managing reusable item lists for filter mappings [3]. Employs
 * standard 1.21.1 ItemStack codecs for deep item data components serialization
 * [3].
 */
public record ItemFilterVariable(UUID id, String name, List<ItemStack> items) {
	public static final MapCodec<ItemFilterVariable> CODEC = RecordCodecBuilder
			.mapCodec(
					instance -> instance
							.group(UUIDUtil.CODEC.fieldOf("id").forGetter(ItemFilterVariable::id),
									Codec.STRING.optionalFieldOf("name", "").forGetter(ItemFilterVariable::name),
									ItemStack.CODEC.listOf().optionalFieldOf("items", List.of())
											.forGetter(ItemFilterVariable::items))
							.apply(instance, ItemFilterVariable::new));
}