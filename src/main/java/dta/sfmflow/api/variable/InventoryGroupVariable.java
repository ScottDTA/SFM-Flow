package dta.sfmflow.api.variable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import java.util.List;
import java.util.UUID;

/**
 * Data record managing reusable inventory selection groups mapped by block
 * coordinates [3]. Provides map codecs for serialization across level NBT saves
 * [3].
 */
public record InventoryGroupVariable(UUID id, String name, List<BlockPos> selections) {
	public static final MapCodec<InventoryGroupVariable> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(UUIDUtil.CODEC.fieldOf("id").forGetter(InventoryGroupVariable::id),
					Codec.STRING.optionalFieldOf("name", "").forGetter(InventoryGroupVariable::name),
					BlockPos.CODEC.listOf().optionalFieldOf("selections", List.of())
							.forGetter(InventoryGroupVariable::selections))
			.apply(instance, InventoryGroupVariable::new));
}