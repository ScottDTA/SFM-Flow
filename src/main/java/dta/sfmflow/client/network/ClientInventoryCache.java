package dta.sfmflow.client.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-only cache holding synchronized inventory item arrays [3].
 */
@OnlyIn(Dist.CLIENT)
public final class ClientInventoryCache {
	private static final Map<BlockPos, ItemStack[]> CACHE = new HashMap<>();

	private ClientInventoryCache() {}

	public static void set(BlockPos pos, ItemStack[] stacks) {
		CACHE.put(pos, stacks);
	}

	public static ItemStack[] get(BlockPos pos) {
		return CACHE.get(pos);
	}

	public static void clear() {
		CACHE.clear();
	}
}