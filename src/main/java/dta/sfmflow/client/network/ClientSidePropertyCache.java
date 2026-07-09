package dta.sfmflow.client.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-only cache holding verified server-side block configurations [3].
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSidePropertyCache {
	private static final Map<PropertyKey, CompoundTag> CACHE = new HashMap<>();

	public record PropertyKey(BlockPos pos, Direction side, ResourceLocation capabilityId) {}

	public static void set(BlockPos pos, Direction side, ResourceLocation capabilityId, CompoundTag tag) {
		CACHE.put(new PropertyKey(pos, side, capabilityId), tag);
	}

	public static CompoundTag get(BlockPos pos, Direction side, ResourceLocation capabilityId) {
		return CACHE.getOrDefault(new PropertyKey(pos, side, capabilityId), new CompoundTag());
	}

	public static void clear() {
		CACHE.clear();
	}
}