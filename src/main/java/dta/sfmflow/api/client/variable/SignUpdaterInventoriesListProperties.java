package dta.sfmflow.api.client.variable;

import com.mojang.blaze3d.vertex.PoseStack;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.client.IVariableClientProperties;
import dta.sfmflow.client.render.VariableCardRenderer;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.SignUpdaterInventoriesListVariableComponent;
import dta.sfmflow.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class SignUpdaterInventoriesListProperties implements INodeClientProperties, IVariableClientProperties {
	@Override public NodeCategory getCategory() { return NodeCategory.VARIABLE; }
	@Override public ResourceLocation getIconTexture() { return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/menu_buttons/sign_button.png"); }
	@Override public Component getDisplayName() { return Component.translatable("gui.sfmflow.sign_updater_inventories_list_variable"); }
	@Override public Supplier<Boolean> isEnabled() { return () -> true; }

	@Override
	public void renderOverlay(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		Minecraft mc = Minecraft.getInstance();
		List<ItemStack> listItems = getLiveInventoriesListStacks(stack);
		if (!listItems.isEmpty()) {
			long gameTime = mc.level != null ? mc.level.getGameTime() : 0;
			int idx = (int) ((gameTime / 20) % listItems.size());
			ItemStack cycleStack = listItems.get(idx);

			poseStack.pushPose();
			poseStack.translate(0.5f, 0.5f, 1.0f);
			poseStack.scale(0.5f, 0.5f, 0.5f);
			
			mc.getItemRenderer().renderStatic(cycleStack, displayContext, packedLight, packedOverlay, poseStack, buffer, mc.level, 0);
			poseStack.popPose();
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, List<Component> tooltipComponents) {
		Color tintColor = Color.WHITE;
		List<String> inventoriesNames = new ArrayList<>();

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTag();
			try { tintColor = Color.valueOf(tag.getString("FilterColor")); } catch (IllegalArgumentException ignored) {}
		}

		Level level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}

		UUID varId = VariableCardRenderer.getVariableId(stack);
		if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
			if (screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
				var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
				if (comp instanceof SignUpdaterInventoriesListVariableComponent listVar) {
					tintColor = listVar.getFilterColor();
					for (var entry : listVar.getEntries()) {
						for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
							if (block.getId() == entry.inventoryId()) {
								BlockState state = level.getBlockState(block.getBlockPos());
								if (state != null && !state.isAir()) {
									inventoriesNames.add(state.getBlock().getName().getString());
								}
								break;
							}
						}
					}
				}
			}
		} else if (customData != null) {
			CompoundTag tag = customData.copyTag();
			if (tag.contains("InventoriesListItems")) {
				ListTag list = tag.getList("InventoriesListItems", Tag.TAG_COMPOUND);
				for (int i = 0; i < list.size(); i++) {
					ItemStack.parse(level.registryAccess(), list.getCompound(i))
							.ifPresent(item -> inventoriesNames.add(item.getHoverName().getString()));
				}
			}
		}

		tooltipComponents.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Sign Updater Inventories List").withStyle(ChatFormatting.LIGHT_PURPLE)));
		tooltipComponents.add(Component.literal("Color: ").withStyle(ChatFormatting.GRAY).append(Component.literal(tintColor.getSerializedName().toUpperCase(Locale.ROOT)).withStyle(tintColor.getChatFormat())));

		if (inventoriesNames.isEmpty()) {
			tooltipComponents.add(Component.literal("Configured Blocks: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY)));
		} else {
			tooltipComponents.add(Component.literal("Configured Blocks:").withStyle(ChatFormatting.GRAY));
			for (String name : inventoriesNames) {
				tooltipComponents.add(Component.literal(" - " + name).withStyle(ChatFormatting.DARK_GREEN));
			}
		}
	}

	private List<ItemStack> getLiveInventoriesListStacks(ItemStack stack) {
		List<ItemStack> items = new ArrayList<>();
		UUID varId = VariableCardRenderer.getVariableId(stack);
		if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
			var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
			if (comp instanceof SignUpdaterInventoriesListVariableComponent listVar) {
				ClientLevel level = Minecraft.getInstance().level;
				if (level != null) {
					for (var entry : listVar.getEntries()) {
						for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
							if (block.getId() == entry.inventoryId()) {
								BlockState state = level.getBlockState(block.getBlockPos());
								if (!state.isAir()) {
									items.add(new ItemStack(state.getBlock().asItem()));
								}
								break;
							}
						}
					}
				}
			}
		}
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null && items.isEmpty()) {
			CompoundTag tag = customData.copyTag();
			if (tag.contains("InventoriesListItems")) {
				ListTag list = tag.getList("InventoriesListItems", Tag.TAG_COMPOUND);
				for (int i = 0; i < list.size(); i++) {
					ItemStack.parse(Minecraft.getInstance().level.registryAccess(), list.getCompound(i))
							.ifPresent(items::add);
				}
			}
		}
		return items;
	}
}