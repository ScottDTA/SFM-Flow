package dta.sfmflow.registry;

import dta.sfmflow.SFMFlow;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * Registry manager for custom NeoForge 1.21.1 Data Component Types [3].
 * Uses the specialized registerComponentType helper of DeferredRegister.DataComponents [3].
 */
public class ModDataComponents {
	// Specialized registrar designed to cleanly capture custom DataComponentType parameters [3]
	public static final DeferredRegister.DataComponents COMPONENTS = 
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SFMFlow.MODID);

	// Wraps the mutable ItemStack inside an immutable record to satisfy NeoForge validation [3]
	public static final Supplier<DataComponentType<FilteredItemComponent>> FILTERED_ITEM = 
			COMPONENTS.registerComponentType("filtered_item", builder -> builder
					.persistent(FilteredItemComponent.CODEC));

	public static final Supplier<DataComponentType<Boolean>> IS_TAG_FILTER = 
			COMPONENTS.registerComponentType("is_tag_filter", builder -> builder
					.persistent(Codec.BOOL));

	public static final Supplier<DataComponentType<Boolean>> IS_COMPONENT_FILTER = 
			COMPONENTS.registerComponentType("is_component_filter", builder -> builder
					.persistent(Codec.BOOL));

	public static void register(IEventBus eventBus) {
		COMPONENTS.register(eventBus);
	}

	/**
	 * Immutable data wrapper since raw net.minecraft.world.item.ItemStack does not 
	 * implement safe value-based equals/hashCode and is mutable [3].
	 */
	public record FilteredItemComponent(ItemStack stack) {
		public static final Codec<FilteredItemComponent> CODEC = 
				ItemStack.CODEC.xmap(FilteredItemComponent::new, FilteredItemComponent::stack);

		public FilteredItemComponent {
			stack = stack.copy(); // Ensure a deep copy of the stack to enforce immutability [3]
		}
	}
}