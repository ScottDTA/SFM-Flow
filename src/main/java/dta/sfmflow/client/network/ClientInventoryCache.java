package dta.sfmflow.client.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-only cache holding synchronized inventory structures dynamically indexed by block face and capability type.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientInventoryCache {
	public record CacheKey(BlockPos pos, @Nullable Direction side, ResourceLocation capabilityId) {}

	private static final Map<CacheKey, CompoundTag> CACHE = new HashMap<>();

	private ClientInventoryCache() {}

	public static void set(BlockPos pos, @Nullable Direction side, ResourceLocation capabilityId, CompoundTag tag) {
		CACHE.put(new CacheKey(pos, side, capabilityId), tag);
	}

	public static CompoundTag get(BlockPos pos, @Nullable Direction side, ResourceLocation capabilityId) {
		return CACHE.getOrDefault(new CacheKey(pos, side, capabilityId), new CompoundTag());
	}

	public static void clear() {
		CACHE.clear();
	}
}