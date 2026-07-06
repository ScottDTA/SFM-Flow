package dta.sfmflow.api.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Public API registry allowing addon developers to register custom capability bridges
 * for standard blocks that do not natively implement NeoForge capabilities [3].
 */
public final class SpecialBlockCapabilityRegistry {

	@FunctionalInterface
	public interface ISpecialBlockCapabilityProvider<T> {
		@Nullable T getCapability(Level level, BlockPos pos, BlockState state, @Nullable Direction side);
	}

	private static final Map<BlockCapability<?, Direction>, Map<Block, ISpecialBlockCapabilityProvider<?>>> REGISTRY = new HashMap<>();

	private SpecialBlockCapabilityRegistry() {
	}

	/**
	 * Registers a custom capability bridge for a specific Block [3].
	 *
	 * @param capability the target block capability [3]
	 * @param block      the block to bridge [3]
	 * @param provider   the capability provider [3]
	 */
	public static <T> void register(BlockCapability<T, Direction> capability, Block block, ISpecialBlockCapabilityProvider<T> provider) {
		if (capability != null && block != null && provider != null) {
			REGISTRY.computeIfAbsent(capability, k -> new HashMap<>()).put(block, provider);
		}
	}

	/**
	 * Resolves a registered custom capability bridge for a given block state [3].
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable <T> T getCapability(BlockCapability<T, Direction> capability, Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
		Map<Block, ISpecialBlockCapabilityProvider<?>> blockMap = REGISTRY.get(capability);
		if (blockMap != null) {
			ISpecialBlockCapabilityProvider<?> provider = blockMap.get(state.getBlock());
			if (provider != null) {
				return (T) provider.getCapability(level, pos, state, side);
			}
		}
		return null;
	}
}