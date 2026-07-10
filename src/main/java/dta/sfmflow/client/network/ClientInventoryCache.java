package dta.sfmflow.client.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-only cache holding synchronized inventory structures dynamically indexed by block face [3].
 */
@OnlyIn(Dist.CLIENT)
public final class ClientInventoryCache {
	public record CacheKey(BlockPos pos, @Nullable Direction side) {}

	private static final Map<CacheKey, CompoundTag> CACHE = new HashMap<>();

	private ClientInventoryCache() {}

	public static void set(BlockPos pos, @Nullable Direction side, CompoundTag tag) {
		CACHE.put(new CacheKey(pos, side), tag);
	}

	public static CompoundTag get(BlockPos pos, @Nullable Direction side) {
		return CACHE.getOrDefault(new CacheKey(pos, side), new CompoundTag());
	}

	public static void clear() {
		CACHE.clear();
	}
}