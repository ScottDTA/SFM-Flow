package dta.sfmflow.api.client;

import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.component.ISlotConfigurable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.AbstractModalPopup;
import dta.sfmflow.client.screen.widgets.SlotLayoutModalPopup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public client-only API registry allowing developers to register custom configuration 
 * popups when shift-clicking block faces inside the 3D block preview widget [3].
 */
@OnlyIn(Dist.CLIENT)
public final class SideConfigPopupRegistry {

	@FunctionalInterface
	public interface ISideConfigPopupFactory {
		/**
		 * Instantiates a custom side configuration popup modal [3].
		 */
		@Nullable AbstractModalPopup create(
				ManagerScreen screen,
				ISideConfigurable sideModel,
				Direction face,
				BlockPos pos,
				Runnable onChanged
		);
	}

	// Use LinkedHashMap to preserve registration order (subclasses registered first take precedence) [3]
	private static final Map<Class<? extends ISideConfigurable>, ISideConfigPopupFactory> REGISTRY = new LinkedHashMap<>();

	private SideConfigPopupRegistry() {}

	/**
	 * Registers a custom settings modal factory for a specific ISideConfigurable class or subclass [3].
	 *
	 * @param clazz   the targeted class type [3]
	 * @param factory the popup instantiation factory [3]
	 */
	public static <T extends ISideConfigurable> void register(Class<T> clazz, ISideConfigPopupFactory factory) {
		if (clazz != null && factory != null) {
			REGISTRY.put(clazz, factory);
		}
	}

	/**
	 * Resolves a custom popup based on registered classes, or defaults to the standard slot configuration [3].
	 */
	public static @Nullable AbstractModalPopup createPopup(
			ManagerScreen screen,
			ISideConfigurable sideModel,
			Direction face,
			BlockPos pos,
			Runnable onChanged
	) {
		// Search custom class-based registrations [3]
		for (Map.Entry<Class<? extends ISideConfigurable>, ISideConfigPopupFactory> entry : REGISTRY.entrySet()) {
			if (entry.getKey().isInstance(sideModel)) {
				AbstractModalPopup popup = entry.getValue().create(screen, sideModel, face, pos, onChanged);
				if (popup != null) {
					return popup;
				}
			}
		}

		// Fallback: Default to standard slot-based configuration if slot routing is supported [3]
		if (sideModel instanceof ISlotConfigurable) {
			return new SlotLayoutModalPopup(screen, sideModel, face, pos, onChanged);
		}

		return null;
	}
}