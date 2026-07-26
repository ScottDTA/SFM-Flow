package dta.sfmflow.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.client.IVariableClientProperties;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.EnergyInventoriesListVariableComponent;
import dta.sfmflow.flowcomponents.ItemInventoriesListVariableComponent;
import dta.sfmflow.flowcomponents.RedstoneInventoriesListVariableComponent;
import dta.sfmflow.flowcomponents.SignUpdaterInventoriesListVariableComponent;
import dta.sfmflow.flowcomponents.FluidInventoriesListVariableComponent;
import dta.sfmflow.registry.ModDataComponents;
import dta.sfmflow.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public final class VariableClientProperties {

	private VariableClientProperties() {}

	public static class ItemVariableProperties implements INodeClientProperties, IVariableClientProperties {
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

	public static class FluidVariableProperties implements INodeClientProperties, IVariableClientProperties {
		@Override public NodeCategory getCategory() { return NodeCategory.VARIABLE; }
		@Override public ResourceLocation getIconTexture() { return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/menu_buttons/fluid_variable_button.png"); }
		@Override public Component getDisplayName() { return Component.translatable("gui.sfmflow.advanced_fluid_filter_variable"); }
		@Override public Supplier<Boolean> isEnabled() { return () -> true; }

		@Override
		public void renderOverlay(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
			Minecraft mc = Minecraft.getInstance();
			FluidStack fluid = getLiveGhostFluid(stack);
			if (!fluid.isEmpty()) {
				poseStack.pushPose();
				poseStack.translate(0.5f, 0.5f, 1.0f);
				poseStack.scale(0.5f, 0.5f, 0.5f);

				IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluid.getFluid());
				ResourceLocation stillTexture = clientFluid.getStillTexture(fluid);
				if (stillTexture != null) {
					int tintColor = clientFluid.getTintColor(fluid);
					TextureAtlasSprite fluidSprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
					drawFluidQuad(poseStack, buffer, fluidSprite, tintColor, packedLight, packedOverlay);
				}
				poseStack.popPose();
			}
		}

		@Override
		public void appendTooltip(ItemStack stack, List<Component> tooltipComponents) {
			FluidStack fluid = getLiveGhostFluid(stack);
			boolean useQty = false;
			int qty = 1000;
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

			if (fluid.isEmpty()) {
				tooltipComponents.add(Component.literal("Fluid: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Any").withStyle(ChatFormatting.DARK_GRAY)));
			} else {
				tooltipComponents.add(Component.literal("Fluid: ").withStyle(ChatFormatting.GRAY).append(Component.literal(fluid.getHoverName().getString()).withStyle(ChatFormatting.BLUE)));
			}

			tooltipComponents.add(Component.literal("Volume: ").withStyle(ChatFormatting.GRAY).append(Component.literal(useQty ? qty + " mB" : "Any").withStyle(useQty ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)));
			tooltipComponents.add(Component.literal("Color: ").withStyle(ChatFormatting.GRAY).append(Component.literal(tintColor.getSerializedName().toUpperCase(Locale.ROOT)).withStyle(tintColor.getChatFormat())));

			if (useModId) {
				String modIdStr = fluid.isEmpty() ? "Any" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getNamespace();
				tooltipComponents.add(Component.literal("ModID Filter: ").withStyle(ChatFormatting.GRAY).append(Component.literal(modIdStr).withStyle(ChatFormatting.AQUA)));
			}
			if (useTag && !selectedTag.isEmpty()) {
				tooltipComponents.add(Component.literal("Tag Filter: ").withStyle(ChatFormatting.GRAY).append(Component.literal("#" + selectedTag).withStyle(ChatFormatting.DARK_GREEN)));
			}
		}

		private FluidStack getLiveGhostFluid(ItemStack stack) {
			UUID varId = VariableCardRenderer.getVariableId(stack);
			if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
				var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
				if (comp instanceof AdvancedFluidFilterVariableComponent advancedVar) {
					return advancedVar.getFilterFluid();
				}
			}
			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData != null) {
				CompoundTag tag = customData.copyTag();
				if (tag.contains("FilterFluid")) {
					return FluidStack.parse(Minecraft.getInstance().level.registryAccess(), tag.getCompound("FilterFluid")).orElse(FluidStack.EMPTY);
				}
			}
			return FluidStack.EMPTY;
		}

		private void drawFluidQuad(PoseStack poseStack, MultiBufferSource buffer, TextureAtlasSprite sprite, int tintColor, int packedLight, int packedOverlay) {
			float minU = sprite.getU0();
			float maxU = sprite.getU1();
			float minV = sprite.getV0();
			float maxV = sprite.getV1();

			float r = ((tintColor >> 16) & 0xFF) / 255.0F;
			float g = ((tintColor >> 8) & 0xFF) / 255.0F;
			float b = (tintColor & 0xFF) / 255.0F;
			float a = ((tintColor >> 24) & 0xFF) / 255.0F;
			if (a <= 0.0F) a = 1.0F;

			float size = 0.5F;
			VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
			var matrix = poseStack.last().pose();

			consumer.addVertex(matrix, -size, size, 0.01F).setColor(r, g, b, a).setUv(minU, minV).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0F, 0.0F, 1.0F);
			consumer.addVertex(matrix, -size, -size, 0.01F).setColor(r, g, b, a).setUv(minU, maxV).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0F, 0.0F, 1.0F);
			consumer.addVertex(matrix, size, -size, 0.01F).setColor(r, g, b, a).setUv(maxU, maxV).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0F, 0.0F, 1.0F);
			consumer.addVertex(matrix, size, size, 0.01F).setColor(r, g, b, a).setUv(maxU, minV).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0F, 0.0F, 1.0F);
		}
	}

	public static class ItemInventoriesListProperties implements INodeClientProperties, IVariableClientProperties {
		@Override public NodeCategory getCategory() { return NodeCategory.VARIABLE; }
		@Override public ResourceLocation getIconTexture() { return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/menu_buttons/variable_button.png"); }
		@Override public Component getDisplayName() { return Component.translatable("gui.sfmflow.item_inventories_list_variable"); }
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

			ClientLevel level = Minecraft.getInstance().level;
			if (level == null) {
				return;
			}

			UUID varId = VariableCardRenderer.getVariableId(stack);
			if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
				if (screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof ItemInventoriesListVariableComponent listVar) {
						tintColor = listVar.getFilterColor();
						for (var entry : listVar.getEntries()) {
							var inventories = screen.getMenu().getManagerBlockEntity().getInventories();
							if (inventories != null) {
								for (var block : inventories) {
									if (block != null && block.getId() == entry.inventoryId()) {
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

			tooltipComponents.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Inventories List").withStyle(ChatFormatting.LIGHT_PURPLE)));
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
				if (comp instanceof ItemInventoriesListVariableComponent listVar) {
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

	public static class FluidInventoriesListProperties implements INodeClientProperties, IVariableClientProperties {
		@Override public NodeCategory getCategory() { return NodeCategory.VARIABLE; }
		@Override public ResourceLocation getIconTexture() { return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/menu_buttons/fluid_variable_button.png"); }
		@Override public Component getDisplayName() { return Component.translatable("gui.sfmflow.fluid_inventories_list_variable"); }
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

			ClientLevel level = Minecraft.getInstance().level;
			if (level == null) {
				return;
			}

			UUID varId = VariableCardRenderer.getVariableId(stack);
			if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
				if (screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof FluidInventoriesListVariableComponent listVar) {
						tintColor = listVar.getFilterColor();
						for (var entry : listVar.getEntries()) {
							var inventories = screen.getMenu().getManagerBlockEntity().getInventories();
							if (inventories != null) {
								for (var block : inventories) {
									if (block != null && block.getId() == entry.inventoryId()) {
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

			tooltipComponents.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Fluid Inventories List").withStyle(ChatFormatting.LIGHT_PURPLE)));
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
				if (comp instanceof FluidInventoriesListVariableComponent listVar) {
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
	
	public static class EnergyInventoriesListProperties implements INodeClientProperties, IVariableClientProperties {
		@Override public NodeCategory getCategory() { return NodeCategory.VARIABLE; }
		@Override public ResourceLocation getIconTexture() { return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/menu_buttons/energy_input_button.png"); }
		@Override public Component getDisplayName() { return Component.translatable("gui.sfmflow.energy_inventories_list_variable"); }
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

			ClientLevel level = Minecraft.getInstance().level;
			if (level == null) {
				return;
			}

			UUID varId = VariableCardRenderer.getVariableId(stack);
			if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
				if (screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof EnergyInventoriesListVariableComponent listVar) {
						tintColor = listVar.getFilterColor();
						for (var entry : listVar.getEntries()) {
							var inventories = screen.getMenu().getManagerBlockEntity().getInventories();
							if (inventories != null) {
								for (var block : inventories) {
									if (block != null && block.getId() == entry.inventoryId()) {
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

			tooltipComponents.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Energy Inventories List").withStyle(ChatFormatting.LIGHT_PURPLE)));
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
				if (comp instanceof EnergyInventoriesListVariableComponent listVar) {
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
	
	public static class RedstoneInventoriesListProperties implements INodeClientProperties, IVariableClientProperties {
		@Override public NodeCategory getCategory() { return NodeCategory.VARIABLE; }
		@Override public ResourceLocation getIconTexture() { return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "textures/gui/menu_buttons/redstone_trigger_button.png"); }
		@Override public Component getDisplayName() { return Component.translatable("gui.sfmflow.redstone_inventories_list_variable"); }
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

			ClientLevel level = Minecraft.getInstance().level;
			if (level == null) {
				return;
			}

			UUID varId = VariableCardRenderer.getVariableId(stack);
			if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
				if (screen.getMenu() != null && screen.getMenu().getManagerBlockEntity() != null) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof RedstoneInventoriesListVariableComponent listVar) {
						tintColor = listVar.getFilterColor();
						for (var entry : listVar.getEntries()) {
							var inventories = screen.getMenu().getManagerBlockEntity().getInventories();
							if (inventories != null) {
								for (var block : inventories) {
									if (block != null && block.getId() == entry.inventoryId()) {
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

			tooltipComponents.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Redstone Inventories List").withStyle(ChatFormatting.LIGHT_PURPLE)));
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
				if (comp instanceof RedstoneInventoriesListVariableComponent listVar) {
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
	
	public static class SignUpdaterInventoriesListProperties implements INodeClientProperties, IVariableClientProperties {
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

			ClientLevel level = Minecraft.getInstance().level;
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
							var inventories = screen.getMenu().getManagerBlockEntity().getInventories();
							if (inventories != null) {
								for (var block : inventories) {
									if (block != null && block.getId() == entry.inventoryId()) {
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

}