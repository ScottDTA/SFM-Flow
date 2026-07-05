package dta.sfmflow.api.capability;

import java.util.HashMap;
import java.util.Map;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.HatchBehaviorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Public API registry managing ticking behaviors for cluster cards installed inside Cable Clusters [3].
 */
public final class CableClusterBehaviorRegistry {
	private static final Map<Item, ClusterCardBehavior> BEHAVIORS = new HashMap<>();

	static {
		// Default Item Vacuum Behavior
		register(ModBlocks.ITEM_VACUUM_HATCH_BLOCK.get().asItem(), (level, pos, dir, slot, card, be) -> {
			if ((level.getGameTime() + slot) % 10 == 0) {
				HatchBehaviorHelper.performVacuum(level, pos, dir, be.getSlotBuffer(slot), be::setChanged);
			}
		});

		// Default Item Ejection Behavior
		register(ModBlocks.ITEM_EJECTOR_HATCH_BLOCK.get().asItem(), (level, pos, dir, slot, card, be) -> {
			if ((level.getGameTime() + slot) % 4 == 0) {
				HatchBehaviorHelper.performEjection(level, pos, dir, be.getSlotBuffer(slot), be::setChanged);
			}
		});

		// Default Fluid Hatch Behavior
		register(ModBlocks.FLUID_HATCH_CABLE_BLOCK.get().asItem(), (level, pos, dir, slot, card, be) -> {
			if ((level.getGameTime() + slot) % 10 == 0) {
				String modeStr = "vacuum";
				CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
				if (tag.contains("mode")) {
					modeStr = tag.getString("mode");
				}
				if ("vacuum".equals(modeStr)) {
					HatchBehaviorHelper.performFluidVacuum(level, pos, dir, be.getFluidBuffer(slot), be::setChanged);
				} else {
					HatchBehaviorHelper.performFluidEjection(level, pos, dir, be.getFluidBuffer(slot), be::setChanged);
				}
			}
		});
	}

	private CableClusterBehaviorRegistry() {
	}

	/**
	 * Registers a custom ticking behavior for a specific card item [3].
	 *
	 * @param item     the card item [3]
	 * @param behavior the behavior callback [3]
	 */
	public static void register(Item item, ClusterCardBehavior behavior) {
		if (item != null && behavior != null) {
			BEHAVIORS.put(item, behavior);
		}
	}

	/**
	 * Retrieves the registered behavior for a specific card item [3].
	 *
	 * @param item the card item [3]
	 * @return the assigned behavior, or null if unregistered [3]
	 */
	@Nullable
	public static ClusterCardBehavior get(Item item) {
		return BEHAVIORS.get(item);
	}
}