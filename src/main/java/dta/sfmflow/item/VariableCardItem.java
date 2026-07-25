package dta.sfmflow.item;

import dta.sfmflow.client.render.VariableCardRenderer;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.IVariableClientProperties;
import dta.sfmflow.api.component.FlowComponentType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Custom item representing the advanced filter card capability. Safely
 * hooks into client-only rendering structures without causing server-side
 * crashes.
 */
public class VariableCardItem extends Item {

	private static Function<ItemStack, Object[]> tooltipDataResolver = stack -> null;

	public VariableCardItem(Properties properties) {
		super(properties);
	}

	public static void setTooltipDataResolver(Function<ItemStack, Object[]> resolver) {
		tooltipDataResolver = resolver;
	}

	/**
	 * Unpacks and resolves the Component Type ResourceLocation of this card safely.
	 */
	public static @Nullable ResourceLocation getVariableTypeKey(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTag();
			if (tag.contains("VariableType")) {
				return ResourceLocation.tryParse(tag.getString("VariableType"));
			}
			// Symmetrical backward-compatibility fallback
			if (tag.contains("FilterFluid")) {
				return ResourceLocation.fromNamespaceAndPath("sfmflow", "advanced_fluid_filter_variable");
			}
			if (tag.contains("entries")) {
				return ResourceLocation.fromNamespaceAndPath("sfmflow", "item_inventories_list_variable");
			}
		}
		return ResourceLocation.fromNamespaceAndPath("sfmflow", "advanced_item_filter_variable");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
			TooltipFlag tooltipFlag) {
		ResourceLocation typeKey = getVariableTypeKey(stack);
		if (typeKey != null) {
			var type = FlowComponentType.REGISTRY.get(typeKey);
			if (type != null) {
				var props = FlowClientRegistry.getProperties(type);
				if (props instanceof IVariableClientProperties varProps) {
					varProps.appendTooltip(stack, tooltipComponents);
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
					return;
				}
			}
		}
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return VariableCardRenderer.getInstance();
			}
		});
	}
}