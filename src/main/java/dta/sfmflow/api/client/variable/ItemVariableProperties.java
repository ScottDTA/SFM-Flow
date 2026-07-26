package dta.sfmflow.api.client.variable;

import com.mojang.blaze3d.vertex.PoseStack;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.client.IVariableClientProperties;
import dta.sfmflow.client.render.VariableCardRenderer;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.registry.ModDataComponents;
import dta.sfmflow.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ItemVariableProperties implements INodeClientProperties, IVariableClientProperties {
	@Override public NodeCategory getCategory() { return NodeCategory.VARIABLE; }
	@Override public ResourceLocation getIconTexture() { return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/menu_buttons/variable_button.png"); }
	@Override public Component getDisplayName() { return Component.translatable("gui.sfmflow.advanced_item_filter_variable"); }
	@Override public Supplier<Boolean> isEnabled() { return () -> true; }

	@Override
	public void renderOverlay(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		Minecraft mc = Minecraft.getInstance();
		ItemStack ghost = getLiveGhostStack(stack);
		if (!ghost.isEmpty()) {
			poseStack.pushPose();
			poseStack.translate(0.5f, 0.5f, 1.0f);
			poseStack.scale(0.5f, 0.5f, 0.5f);

			ItemStack renderStack = ghost;
			if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("UseComponentFilter")) {
				renderStack = ghost.copy();
				renderStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			}

			mc.getItemRenderer().renderStatic(renderStack, displayContext, packedLight, packedOverlay, poseStack, buffer, mc.level, 0);
			poseStack.popPose();
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, List<Component> tooltipComponents) {
		ItemStack ghost = getLiveGhostStack(stack);
		boolean useQty = false;
		int qty = 1;
		Color tintColor = Color.WHITE;
		boolean useModId = false;
		boolean useTag = false;
		String selectedTag = "";

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTag();
			useQty = tag.getBoolean("UseQuantity");
			qty = tag.getInt("Quantity");
			try { tintColor = Color.valueOf(tag.getString("FilterColor")); } catch (IllegalArgumentException ignored) {}
			useModId = tag.getBoolean("UseModId");
			useTag = tag.getBoolean("UseTag");
			selectedTag = tag.getString("SelectedTag");
		}

		if (ghost.isEmpty()) {
			tooltipComponents.add(Component.literal("Item: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Any").withStyle(ChatFormatting.DARK_GRAY)));
		} else {
			tooltipComponents.add(Component.literal("Item: ").withStyle(ChatFormatting.GRAY).append(ghost.getHoverName().copy().withStyle(ChatFormatting.YELLOW)));
		}

		tooltipComponents.add(Component.literal("Quantity: ").withStyle(ChatFormatting.GRAY).append(Component.literal(useQty ? String.valueOf(qty) : "Any").withStyle(useQty ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)));
		tooltipComponents.add(Component.literal("Color: ").withStyle(ChatFormatting.GRAY).append(Component.literal(tintColor.getSerializedName().toUpperCase(Locale.ROOT)).withStyle(tintColor.getChatFormat())));

		if (useModId) {
			String modIdStr = ghost.isEmpty() ? "Any" : BuiltInRegistries.ITEM.getKey(ghost.getItem()).getNamespace();
			tooltipComponents.add(Component.literal("ModID Filter: ").withStyle(ChatFormatting.GRAY).append(Component.literal(modIdStr).withStyle(ChatFormatting.AQUA)));
		}
		if (useTag && !selectedTag.isEmpty()) {
			tooltipComponents.add(Component.literal("Tag Filter: ").withStyle(ChatFormatting.GRAY).append(Component.literal("#" + selectedTag).withStyle(ChatFormatting.DARK_GREEN)));
		}
	}

	private ItemStack getLiveGhostStack(ItemStack stack) {
		UUID varId = VariableCardRenderer.getVariableId(stack);
		if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
			var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
			if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
				ItemStack ghost = advancedVar.getFilterStack();
				if (advancedVar.isUseTag() && !advancedVar.getSelectedTag().isEmpty()) {
					List<ItemStack> tagItems = getTagItems(advancedVar.getSelectedTag());
					if (!tagItems.isEmpty()) {
						long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
						int idx = (int) ((gameTime / 20) % tagItems.size());
						return tagItems.get(idx);
					}
				}
				return ghost;
			}
		}
		ModDataComponents.FilteredItemComponent compVal = stack.get(ModDataComponents.FILTERED_ITEM.get());
		if (compVal != null) {
			return compVal.stack();
		}
		return ItemStack.EMPTY;
	}

	private List<ItemStack> getTagItems(String tagLocation) {
		List<ItemStack> items = new ArrayList<>();
		ResourceLocation tagLoc = ResourceLocation.tryParse(tagLocation);
		if (tagLoc != null) {
			var tagKey = TagKey.create(Registries.ITEM, tagLoc);
			BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(tag -> tag.forEach(holder -> items.add(new ItemStack(holder.value()))));
		}
		return items;
	}
}