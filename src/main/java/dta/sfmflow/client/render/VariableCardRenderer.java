package dta.sfmflow.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Handles multi-layered programmatic blending of filter cards inside screens
 * and inventories [3]. Uses model list rendering to prevent recursive stack
 * overflows in NeoForge 1.21.1, drawing fluid textures directly [3].
 */
@OnlyIn(Dist.CLIENT)
public class VariableCardRenderer extends BlockEntityWithoutLevelRenderer {
	private static VariableCardRenderer instance;

	private static final ModelResourceLocation FLAT_MODEL_RL = ModelResourceLocation
			.standalone(ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "item/variable_card_flat"));

	private VariableCardRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	public static synchronized VariableCardRenderer getInstance() {
		if (instance == null) {
			instance = new VariableCardRenderer();
		}
		return instance;
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight, int packedOverlay) {
		Minecraft mc = Minecraft.getInstance();

		// Layer 1: Core Dyed Card Frame Asset [3]
		BakedModel baseModel = mc.getModelManager().getModel(FLAT_MODEL_RL);
		poseStack.pushPose();

		boolean hasGlint = hasComponentFilter(stack);

		mc.getItemRenderer().renderModelLists(baseModel, stack, packedLight, packedOverlay, poseStack,
				ItemRenderer.getFoilBuffer(buffer, Sheets.translucentItemSheet(), true, hasGlint));
		poseStack.popPose();

		// Layer 2: 50% Nested Inner Ghost Stack/Fluid Icon [3]
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
		} else {
			// Symmetrically fall back to standard item ghost icon render if not a fluid card [3]
			ItemStack ghost = getLiveGhostStack(stack);
			if (ghost != null && !ghost.isEmpty()) {
				poseStack.pushPose();

				poseStack.translate(0.5f, 0.5f, 1.0f);
				poseStack.scale(0.5f, 0.5f, 0.5f);

				ItemStack renderStack = ghost;
				if (hasGlint) {
					renderStack = ghost.copy();
					renderStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
				}

				mc.getItemRenderer().renderStatic(renderStack, displayContext, packedLight, packedOverlay, poseStack, buffer,
						mc.level, 0);
				poseStack.popPose();
			}
		}
	}

	/**
	 * Pulls the live, synchronized filter stack from the client's workspace if
	 * available, falling back to the stack's saved component [3].
	 * Cycles dynamically through all items inside the configured tag if UseTag is enabled [3].
	 */
	private ItemStack getLiveGhostStack(ItemStack stack) {
		UUID varId = getVariableId(stack);
		if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
			var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
			if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
				ItemStack ghost = advancedVar.getFilterStack();
				if (advancedVar.isUseTag() && !advancedVar.getSelectedTag().isEmpty()) {
					List<ItemStack> tagItems = getTagItems(advancedVar.getSelectedTag());
					if (!tagItems.isEmpty()) {
						long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
						int idx = (int) ((gameTime / 20) % tagItems.size()); // Cycle items once per second [3]
						return tagItems.get(idx);
					}
				}
				return ghost;
			}
		}

		var compVal = stack.get(ModDataComponents.FILTERED_ITEM.get());
		ItemStack ghost = compVal != null ? compVal.stack() : ItemStack.EMPTY;

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTag();
			if (tag.getBoolean("UseTag") && tag.contains("SelectedTag")) {
				List<ItemStack> tagItems = getTagItems(tag.getString("SelectedTag"));
				if (!tagItems.isEmpty()) {
					long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
					int idx = (int) ((gameTime / 20) % tagItems.size());
					return tagItems.get(idx);
				}
			}
		}

		return ghost;
	}

	private FluidStack getLiveGhostFluid(ItemStack stack) {
		UUID varId = getVariableId(stack);
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
				HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
				return FluidStack.parse(registries, tag.getCompound("FilterFluid")).orElse(FluidStack.EMPTY);
			}
		}
		return FluidStack.EMPTY;
	}

	private boolean hasComponentFilter(ItemStack stack) {
		UUID varId = getVariableId(stack);
		if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
			var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
			if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
				return advancedVar.isUseComponentFilter();
			}
		}
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			return customData.copyTag().getBoolean("UseComponentFilter");
		}
		return false;
	}

	private static List<ItemStack> getTagItems(String tagLocation) {
		List<ItemStack> items = new ArrayList<>();
		ResourceLocation tagLoc = ResourceLocation.tryParse(tagLocation);
		if (tagLoc != null) {
			var tagKey = TagKey.create(Registries.ITEM, tagLoc);
			BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(tag -> {
				tag.forEach(holder -> items.add(new ItemStack(holder.value())));
			});
		}
		return items;
	}

	@Nullable
	public static UUID getVariableId(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTag();
			if (tag.contains("VariableId")) {
				return tag.getUUID("VariableId");
			}
		}
		return null;
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

		float size = 0.5F; // Updated size to 0.5F to render at exactly 50% scale [3]
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
		var matrix = poseStack.last().pose();

		consumer.addVertex(matrix, -size, size, 0.01F)
				.setColor(r, g, b, a)
				.setUv(minU, minV)
				.setOverlay(packedOverlay)
				.setLight(packedLight)
				.setNormal(0.0F, 0.0F, 1.0F);

		consumer.addVertex(matrix, -size, -size, 0.01F)
				.setColor(r, g, b, a)
				.setUv(minU, maxV)
				.setOverlay(packedOverlay)
				.setLight(packedLight)
				.setNormal(0.0F, 0.0F, 1.0F);

		consumer.addVertex(matrix, size, -size, 0.01F)
				.setColor(r, g, b, a)
				.setUv(maxU, maxV)
				.setOverlay(packedOverlay)
				.setLight(packedLight)
				.setNormal(0.0F, 0.0F, 1.0F);

		consumer.addVertex(matrix, size, size, 0.01F)
				.setColor(r, g, b, a)
				.setUv(maxU, minV)
				.setOverlay(packedOverlay)
				.setLight(packedLight)
				.setNormal(0.0F, 0.0F, 1.0F);
	}
}