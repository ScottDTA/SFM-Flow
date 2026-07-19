package dta.sfmflow.api.capability;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Public API registry allowing addon developers to register custom capability proxies for cluster cards.
 */
public final class ClusterCardCapabilityRegistry {

	@FunctionalInterface
	public interface IClusterCardCapabilityProvider<T> {
		/**
		 * Resolves the capability handler exposed by the card installed in the Cable Cluster.
		 *
		 * @param level        the level context
		 * @param pos          the position of the Cable Cluster
		 * @param side         the direction face queried
		 * @param slotIndex    the index of the slot the card is installed in
		 * @param blockEntity  the Cable Cluster block entity
		 * @return the capability handler, or null if unexposed
		 */
		@Nullable T getCapability(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, @Nullable Direction side, int slotIndex, dta.sfmflow.block.entity.CableClusterBlockEntity blockEntity);
	}

	private static final Map<BlockCapability<?, Direction>, Map<Item, IClusterCardCapabilityProvider<?>>> REGISTRY = new HashMap<>();

	private ClusterCardCapabilityRegistry() {}

	/**
	 * Registers a custom capability proxy for a specific Item card.
	 */
	public static <T> void register(BlockCapability<T, Direction> capability, Item item, IClusterCardCapabilityProvider<T> provider) {
		if (capability != null && item != null && provider != null) {
			REGISTRY.computeIfAbsent(capability, k -> new HashMap<>()).put(item, provider);
		}
	}

	/**
	 * Resolves a registered custom capability proxy for the active card.
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable <T> T getCapability(BlockCapability<T, Direction> capability, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, @Nullable Direction side, int slotIndex, dta.sfmflow.block.entity.CableClusterBlockEntity blockEntity) {
		Map<Item, IClusterCardCapabilityProvider<?>> itemMap = REGISTRY.get(capability);
		if (itemMap != null) {
			IClusterCardCapabilityProvider<?> provider = itemMap.get(blockEntity.getInventory().getStackInSlot(slotIndex).getItem());
			if (provider != null) {
				return (T) provider.getCapability(level, pos, side, slotIndex, blockEntity);
			}
		}
		return null;
	}

	/**
	 * Dynamic check to see if the card item exposes the targeted block capability.
	 */
	public static boolean hasCapability(BlockCapability<?, Direction> capability, Item item) {
		Map<Item, IClusterCardCapabilityProvider<?>> itemMap = REGISTRY.get(capability);
		return itemMap != null && itemMap.containsKey(item);
	}
}