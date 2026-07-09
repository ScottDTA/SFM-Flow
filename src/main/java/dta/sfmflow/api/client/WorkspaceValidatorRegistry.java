package dta.sfmflow.api.client;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-only registry managing visual validation rules and error tooltips for flowchart nodes [3].
 * Allows third-party addon developers to register custom validation checks side-safely [3].
 */
@OnlyIn(Dist.CLIENT)
public final class WorkspaceValidatorRegistry {

	@OnlyIn(Dist.CLIENT)
	public interface INodeValidator<T extends AbstractFlowComponent> {
		/**
		 * Evaluates if the component exhibits active errors [3].
		 */
		boolean hasError(ManagerScreen screen, T component);

		/**
		 * Resolves the active error tooltip displayed when hovering over the card [3].
		 */
		@Nullable Component getErrorTooltip(ManagerScreen screen, T component);

		/**
		 * Evaluates if the component exhibits active warnings [3].
		 */
		default boolean hasWarning(ManagerScreen screen, T component) { return false; }

		/**
		 * Resolves the active warning tooltip [3].
		 */
		default @Nullable Component getWarningTooltip(ManagerScreen screen, T component) { return null; }
	}

	// Use LinkedHashMap to support subclass lookup resolutions sequentially [3]
	private static final Map<Class<? extends AbstractFlowComponent>, INodeValidator<?>> REGISTRY = new LinkedHashMap<>();

	private WorkspaceValidatorRegistry() {}

	/**
	 * Registers a custom node validator for a specific flowchart component class [3].
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AbstractFlowComponent> void register(Class<T> clazz, INodeValidator<T> validator) {
		if (clazz != null && validator != null) {
			REGISTRY.put(clazz, (INodeValidator<?>) validator);
		}
	}

	/**
	 * Resolves a registered validator, checking superclass assignments as a fallback [3].
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable <T extends AbstractFlowComponent> INodeValidator<T> getValidator(Class<? extends AbstractFlowComponent> clazz) {
		for (Map.Entry<Class<? extends AbstractFlowComponent>, INodeValidator<?>> entry : REGISTRY.entrySet()) {
			if (entry.getKey().isAssignableFrom(clazz)) {
				return (INodeValidator<T>) entry.getValue();
			}
		}
		return null;
	}
}