package dta.sfmflow.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Handles multi-layered programmatic blending of filter cards inside screens
 * and inventories [3]. Uses model list rendering to prevent recursive stack
 * overflows in NeoForge 1.21.1 [3].
 */
@OnlyIn(Dist.CLIENT)
public class VariableCardRenderer extends BlockEntityWithoutLevelRenderer {
	private static VariableCardRenderer instance;

	// Bypasses empty builtin model by pointing directly to the baked flat card
	// standalone model [3]
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
		// Retrieve our pre-baked flat card model directly from the manager using
		// standalone coordinates [3]
		BakedModel baseModel = mc.getModelManager().getModel(FLAT_MODEL_RL);
		poseStack.pushPose();

		// renderModelLists bypasses the recursive BEWLR checks and renders the card
		// frame [3]
		mc.getItemRenderer().renderModelLists(baseModel, stack, packedLight, packedOverlay, poseStack,
				buffer.getBuffer(Sheets.translucentItemSheet()));
		poseStack.popPose();

		// Layer 2: 50% Nested Inner Ghost Stack Icon [3]
		ItemStack ghost = getLiveGhostStack(stack);
		if (ghost != null && !ghost.isEmpty()) {
			poseStack.pushPose();

			// Transform Matrix: Centers the item on X/Y (0.5f, 0.5f) [3]
			// Translation on Z (0.03f) provides the perfect buffer space to clear the
			// card's 3D extrusion depth cleanly [3]
			poseStack.translate(0.5f, 0.5f, 1.0f);
			poseStack.scale(0.5f, 0.5f, 0.5f);

			// Standard renderStatic is safe here as the nested item is not a BEWLR card [3]
			mc.getItemRenderer().renderStatic(ghost, displayContext, packedLight, packedOverlay, poseStack, buffer,
					mc.level, 0);
			poseStack.popPose();
		}
	}

	/**
	 * Pulls the live, synchronized filter stack from the client's workspace if
	 * available, falling back to the stack's saved component [3].
	 */
	private ItemStack getLiveGhostStack(ItemStack stack) {
		UUID varId = getVariableId(stack);
		if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
			var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
			if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
				return advancedVar.getFilterStack();
			}
		}
		var compVal = stack.get(ModDataComponents.FILTERED_ITEM.get());
		return compVal != null ? compVal.stack() : ItemStack.EMPTY;
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
}