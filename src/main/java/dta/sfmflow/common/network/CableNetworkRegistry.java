package dta.sfmflow.common.network;

import dta.sfmflow.SFMFlow;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level-scoped static cache mapping physical cable coordinates to their owning
 * controllers [3]. Provides constant-time O(1) checks to enforce network
 * partition boundaries and prevent collisions [3].
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public class CableNetworkRegistry {
	private static final Map<ResourceKey<Level>, Map<BlockPos, BlockPos>> CABLE_TO_CONTROLLER = new ConcurrentHashMap<>();

	private CableNetworkRegistry() {
	}

	/**
	 * Registers a physical cable coordinates mapping to its owning controller
	 * BlockPos [3].
	 *
	 * @param level         level context [3]
	 * @param cablePos      physical cable position [3]
	 * @param controllerPos owning manager block entity position [3]
	 */
	public static void registerCable(Level level, BlockPos cablePos, BlockPos controllerPos) {
		CABLE_TO_CONTROLLER.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(cablePos,
				controllerPos);
	}

	/**
	 * Unregisters a physical cable coordinate cleanly releasing pointers [3].
	 *
	 * @param level    level context [3]
	 * @param cablePos physical cable position [3]
	 */
	public static void unregisterCable(Level level, BlockPos cablePos) {
		Map<BlockPos, BlockPos> levelMap = CABLE_TO_CONTROLLER.get(level.dimension());
		if (levelMap != null) {
			levelMap.remove(cablePos);
		}
	}

	/**
	 * Queries the registered controller position for a specific cable position [3].
	 *
	 * @param level    level context [3]
	 * @param cablePos query cable position [3]
	 * @return owning manager block entity position, or null if unassigned [3]
	 */
	public static BlockPos getController(Level level, BlockPos cablePos) {
		Map<BlockPos, BlockPos> levelMap = CABLE_TO_CONTROLLER.get(level.dimension());
		return levelMap != null ? levelMap.get(cablePos) : null;
	}

	/**
	 * Clears all cached static registration entries for a specific Level dimension
	 * [3].
	 *
	 * @param level level dimension to purge [3]
	 */
	public static void clearLevel(Level level) {
		CABLE_TO_CONTROLLER.remove(level.dimension());
	}

	/**
	 * Static game bus listener that intercepts level unloads to discard dimension
	 * references [3].
	 *
	 * @param event level unload event [3]
	 */
	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof Level level) {
			clearLevel(level);
		}
	}
}