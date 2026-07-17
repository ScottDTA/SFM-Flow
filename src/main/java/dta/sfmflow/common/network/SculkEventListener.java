package dta.sfmflow.common.network;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.flowcomponents.SculkTriggerComponent;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side acoustic subscriber listening globally to environmental vibrations.
 * Employs a high-performance flat inverted lookup cache to bypass nested loops on every event.
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public final class SculkEventListener {

	public record ActiveSculkListener(ManagerBlockEntity manager, SculkTriggerComponent component, BlockPos cablePos) {}

	private static final List<ActiveSculkListener> ACTIVE_SCULK_LISTENERS = new CopyOnWriteArrayList<>();

	@SubscribeEvent
	public static void onVanillaGameEvent(VanillaGameEvent event) {
		Level level = event.getLevel();
		if (level.isClientSide() || ACTIVE_SCULK_LISTENERS.isEmpty()) {
			return;
		}

		Vec3 pos = event.getEventPosition();
		BlockPos eventPos = BlockPos.containing(pos);

		// Single flat loop over registered acoustic nodes
		for (ActiveSculkListener listener : ACTIVE_SCULK_LISTENERS) {
			if (listener.manager().getLevel() == level && !listener.manager().isRemoved()) {
				// Fast fail-safe: Check if the event occurred within standard 16-block sculk boundaries
				if (eventPos.closerThan(listener.cablePos(), 16.0)) {
					listener.component().onGameEvent(listener.cablePos(), event, eventPos);
				}
			}
		}
	}

	/**
	 * Rebuilds the registered sculk listener bounds for a specific manager block entity.
	 */
	public static void rebuildManagerListeners(ManagerBlockEntity manager) {
		// Evict previous bindings [3]
		ACTIVE_SCULK_LISTENERS.removeIf(listener -> listener.manager() == manager);

		if (manager.isRemoved()) {
			return;
		}

		// Compile active sculk triggers from the flowchart
		for (var comp : manager.getFlowComponents().values()) {
			if (comp instanceof SculkTriggerComponent sculkTrigger) {
				int targetId = sculkTrigger.getInventoryId();
				if (targetId != -1) {
					for (ConnectionBlock block : manager.getInventories()) {
						if (block.getId() == targetId && !block.isSleeping()) {
							ACTIVE_SCULK_LISTENERS.add(new ActiveSculkListener(manager, sculkTrigger, block.getBlockPos()));
							break;
						}
					}
				}
			}
		}
	}
}