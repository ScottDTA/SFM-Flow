package dta.sfmflow.item;

import dta.sfmflow.client.render.VariableCardRenderer;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.registry.ModDataComponents;
import dta.sfmflow.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Custom item representing the advanced filter card capability [3]. Safely
 * hooks into client-only rendering structures without causing server-side
 * crashes [3].
 */
public class VariableCardItem extends Item {

	// A side-safe functional resolver callback injected during client setup [3]
	private static Function<ItemStack, Object[]> tooltipDataResolver = stack -> null;

	public VariableCardItem(Properties properties) {
		super(properties);
	}

	public static void setTooltipDataResolver(Function<ItemStack, Object[]> resolver) {
		tooltipDataResolver = resolver;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
			TooltipFlag tooltipFlag) {
		ItemStack ghost = ItemStack.EMPTY;
		boolean useQty = false;
		int qty = 1;
		Color tintColor = Color.WHITE;
		boolean useModId = false;
		boolean useTag = false;
		String selectedTag = "";

		// 1. Attempt live client lookup using the injected side-safe resolver [3]
		Object[] liveData = tooltipDataResolver.apply(stack);
		if (liveData != null) {
			ghost = (ItemStack) liveData[0];
			useQty = (Boolean) liveData[1];
			qty = (Integer) liveData[2];
			tintColor = (Color) liveData[3];
			// Live tracking for modid & tags [3]
			if (Minecraft.getInstance().screen instanceof ManagerScreen screen) {
				UUID varId = VariableCardRenderer.getVariableId(stack);
				if (varId != null) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
						useModId = advancedVar.isUseModId();
						useTag = advancedVar.isUseTag();
						selectedTag = advancedVar.getSelectedTag();
					}
				}
			}
		} else {
			// 2. Fallback to reading static components/NBT outside of active screen [3]
			ModDataComponents.FilteredItemComponent compVal = stack.get(ModDataComponents.FILTERED_ITEM.get());
			if (compVal != null) {
				ghost = compVal.stack();
			}

			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData != null) {
				CompoundTag tag = customData.copyTag();
				if (tag.contains("UseQuantity")) {
					useQty = tag.getBoolean("UseQuantity");
					qty = tag.getInt("Quantity");
				}
				if (tag.contains("FilterColor")) {
					try {
						tintColor = Color.valueOf(tag.getString("FilterColor"));
					} catch (IllegalArgumentException ignored) {
					}
				}
				if (tag.contains("UseModId")) {
					useModId = tag.getBoolean("UseModId");
				}
				if (tag.contains("UseTag")) {
					useTag = tag.getBoolean("UseTag");
					selectedTag = tag.getString("SelectedTag");
				}
			}
		}

		// Line 1: Filtered Item [3]
		if (ghost.isEmpty()) {
			tooltipComponents.add(Component.literal("Item: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal("Any").withStyle(ChatFormatting.DARK_GRAY)));
		} else {
			tooltipComponents.add(Component.literal("Item: ").withStyle(ChatFormatting.GRAY)
					.append(ghost.getHoverName().copy().withStyle(ChatFormatting.YELLOW)));
		}

		// Line 2: Quantity setting [3]
		if (useQty) {
			tooltipComponents.add(Component.literal("Quantity: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(String.valueOf(qty)).withStyle(ChatFormatting.GOLD)));
		} else {
			tooltipComponents.add(Component.literal("Quantity: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal("Any").withStyle(ChatFormatting.DARK_GRAY)));
		}

		// Line 3: Card Tint Color [3]
		tooltipComponents.add(Component.literal("Color: ").withStyle(ChatFormatting.GRAY).append(Component
				.literal(tintColor.getSerializedName().toUpperCase(Locale.ROOT)).withStyle(tintColor.getChatFormat())));

		// Line 4: ModID Filter [3]
		if (useModId) {
			String modIdStr = ghost.isEmpty() ? "Any" : BuiltInRegistries.ITEM.getKey(ghost.getItem()).getNamespace();
			tooltipComponents.add(Component.literal("ModID Filter: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(modIdStr).withStyle(ChatFormatting.AQUA)));
		}

		// Line 5: Tag Filter [3]
		if (useTag && !selectedTag.isEmpty()) {
			tooltipComponents.add(Component.literal("Tag Filter: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal("#" + selectedTag).withStyle(ChatFormatting.DARK_GREEN)));
		}

		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
	}


	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				// Returns our singleton layered block entity item rendering controller [3]
				return VariableCardRenderer.getInstance();
			}
		});
	}
}