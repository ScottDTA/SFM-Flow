package dta.sfmflow.api.capability;

import java.util.HashMap;
import java.util.Map;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.HatchBehaviorHelper;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Public API registry managing ticking behaviors for cluster cards installed inside Cable Clusters.
 */
public final class CableClusterBehaviorRegistry {
	private static final Map<Item, ClusterCardBehavior> BEHAVIORS = new HashMap<>();

	static {
		// Default Item Vacuum Behavior
		register(ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get().asItem(), (level, pos, dir, slot, card, be) -> {
			if ((level.getGameTime() + slot) % 10 == 0) {
				HatchBehaviorHelper.performVacuum(level, pos, dir, be.getSlotBuffer(slot), be::setChanged);
			}
		});

		// Default Item Ejection Behavior
		register(ModBlocks.ITEM_EJECTOR_VALVE_BLOCK.get().asItem(), (level, pos, dir, slot, card, be) -> {
			if ((level.getGameTime() + slot) % 4 == 0) {
				HatchBehaviorHelper.performEjection(level, pos, dir, be.getSlotBuffer(slot), be::setChanged);
			}
		});

		// Default Fluid Ejector Behavior
				register(ModBlocks.FLUID_EJECTOR_VALVE_BLOCK.get().asItem(), (level, pos, dir, slot, card, be) -> {
					if ((level.getGameTime() + slot) % 10 == 0) {
						HatchBehaviorHelper.performFluidEjection(level, pos, dir, be.getFluidBuffer(slot), be::setChanged);
					}
				});
	}

	private CableClusterBehaviorRegistry() {
	}

	/**
	 * Registers a custom ticking behavior for a specific card item.
	 *
	 * @param item     the card item
	 * @param behavior the behavior callback
	 */
	public static void register(Item item, ClusterCardBehavior behavior) {
		if (item != null && behavior != null) {
			BEHAVIORS.put(item, behavior);
		}
	}

	/**
	 * Retrieves the registered behavior for a specific card item.
	 *
	 * @param item the card item
	 * @return the assigned behavior, or null if unregistered
	 */
	@Nullable
	public static ClusterCardBehavior get(Item item) {
		return BEHAVIORS.get(item);
	}
}