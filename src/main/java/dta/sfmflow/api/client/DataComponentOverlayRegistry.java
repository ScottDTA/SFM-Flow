package dta.sfmflow.api.client;

import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.AbstractModalPopup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

/**
 * Public client-only API registry allowing developers to register custom configuration 
 * modals for specific DataComponentType properties [3].
 */
@OnlyIn(Dist.CLIENT)
public final class DataComponentOverlayRegistry {
	private static final Map<DataComponentType<?>, BiFunction<ManagerScreen, ItemStack, AbstractModalPopup>> REGISTRY = new HashMap<>();

	private DataComponentOverlayRegistry() {}

	/**
	 * Registers a custom settings modal factory for a specific DataComponentType [3].
	 *
	 * @param type    the targeted data component type [3]
	 * @param factory the modal instantiation factory [3]
	 */
	public static <T> void register(DataComponentType<T> type, BiFunction<ManagerScreen, ItemStack, AbstractModalPopup> factory) {
		if (type != null && factory != null) {
			REGISTRY.put(type, factory);
		}
	}

	/**
	 * Dynamic overlay resolver that instantiates a registered custom modal, 
	 * or returns null if none is registered [3].
	 */
	public static @Nullable AbstractModalPopup createOverlay(DataComponentType<?> type, ManagerScreen screen, ItemStack stack) {
		BiFunction<ManagerScreen, ItemStack, AbstractModalPopup> factory = REGISTRY.get(type);
		if (factory != null) {
			return factory.apply(screen, stack);
		}
		return null;
	}
}