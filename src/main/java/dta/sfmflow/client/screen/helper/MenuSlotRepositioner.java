package dta.sfmflow.client.screen.helper;

import dta.sfmflow.SFMFlow;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility helper that dynamically repositions slots using Java Reflection [3].
 * Caches field lookups to optimize performance [3].
 */
@OnlyIn(Dist.CLIENT)
public final class MenuSlotRepositioner {
	private static Field xField;
	private static Field yField;
	private static boolean initialized = false;

	private MenuSlotRepositioner() {}

	private static synchronized void init() {
		if (initialized) return;
		try {
			xField = Slot.class.getDeclaredField("x");
			yField = Slot.class.getDeclaredField("y");
			xField.setAccessible(true);
			yField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			try {
				// Fallback search by type mapping if mappings differ [3]
				List<Field> intFields = new ArrayList<>();
				for (Field field : Slot.class.getDeclaredFields()) {
					if (field.getType() == int.class) {
						intFields.add(field);
					}
				}
				if (intFields.size() >= 3) {
					xField = intFields.get(1);
					yField = intFields.get(2);
					xField.setAccessible(true);
					yField.setAccessible(true);
				}
			} catch (Exception ex) {
				SFMFlow.LOGGER.error("Failed to resolve dynamic Slot fields via fallback", ex);
			}
		} catch (Exception e) {
			SFMFlow.LOGGER.error("Failed to initialize dynamic Slot repositioner fields", e);
		}
		initialized = true;
	}

	/**
	 * Modifies a slot's coordinate mapping at runtime using cached reflection fields [3].
	 *
	 * @param slot the container slot instance to update [3]
	 * @param x    new target X coordinate [3]
	 * @param y    new target Y coordinate [3]
	 */
	public static void setSlotPosition(Slot slot, int x, int y) {
		init();
		if (xField != null && yField != null) {
			try {
				xField.setInt(slot, x);
				yField.setInt(slot, y);
			} catch (Exception e) {
				SFMFlow.LOGGER.error("Failed to dynamically reposition container slot", e);
			}
		}
	}
}