package dta.sfmflow.client.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-only cache holding synchronized inventory item arrays dynamically indexed by block face [3].
 */
@OnlyIn(Dist.CLIENT)
public final class ClientInventoryCache {
	public record CacheKey(BlockPos pos, @Nullable Direction side) {}

	private static final Map<CacheKey, ItemStack[]> CACHE = new HashMap<>();

	private ClientInventoryCache() {}

	public static void set(BlockPos pos, @Nullable Direction side, ItemStack[] stacks) {
		CACHE.put(new CacheKey(pos, side), stacks);
	}

	public static ItemStack[] get(BlockPos pos, @Nullable Direction side) {
		return CACHE.get(new CacheKey(pos, side));
	}

	public static void clear() {
		CACHE.clear();
	}
}