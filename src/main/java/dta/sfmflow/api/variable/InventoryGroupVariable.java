package dta.sfmflow.api.variable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import java.util.List;
import java.util.UUID;

/**
 * Data record managing reusable inventory selection groups mapped by block
 * coordinates. Provides standard codecs for serialization across level NBT saves.
 */
public record InventoryGroupVariable(UUID id, String name, List<BlockPos> selections) {
	public static final Codec<InventoryGroupVariable> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(UUIDUtil.CODEC.fieldOf("id").forGetter(InventoryGroupVariable::id),
					Codec.STRING.optionalFieldOf("name", "").forGetter(InventoryGroupVariable::name),
					BlockPos.CODEC.listOf().optionalFieldOf("selections", List.of())
							.forGetter(InventoryGroupVariable::selections))
			.apply(instance, InventoryGroupVariable::new));
}