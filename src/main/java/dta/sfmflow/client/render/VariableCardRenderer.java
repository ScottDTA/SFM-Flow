package dta.sfmflow.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.IVariableClientProperties;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IFlowchartVariable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.item.VariableCardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
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
 * and inventories. Uses model list rendering to prevent recursive stack
 * overflows in NeoForge 1.21.1, drawing fluid textures directly.
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

		BakedModel baseModel = mc.getModelManager().getModel(FLAT_MODEL_RL);
		poseStack.pushPose();

		boolean hasGlint = hasComponentFilter(stack);

		mc.getItemRenderer().renderModelLists(baseModel, stack, packedLight, packedOverlay, poseStack,
				ItemRenderer.getFoilBuffer(buffer, Sheets.translucentItemSheet(), true, hasGlint));
		poseStack.popPose();

		ResourceLocation typeKey = VariableCardItem.getVariableTypeKey(stack);
		if (typeKey != null) {
			var type = FlowComponentType.REGISTRY.get(typeKey);
			if (type != null) {
				var props = FlowClientRegistry.getProperties(type);
				if (props instanceof IVariableClientProperties varProps) {
					varProps.renderOverlay(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
				}
			}
		}

		// Flush the translucent glint and model rendering batch safely to prevent state leaking
		if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch();
		}
	}

	public static boolean hasComponentFilter(ItemStack stack) {
		UUID varId = VariableCardItem.getVariableId(stack);
		if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
			var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
			if (comp instanceof IFlowchartVariable flowchartVar) {
				return flowchartVar.hasGlint();
			}
		}
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			return customData.copyTag().getBoolean("UseComponentFilter");
		}
		return false;
	}
	
	/**
	 * Client-side helper delegate ensuring that all client properties classes and item 
	 * colors compiling against this class can resolve the variable UUID side-safely.
	 */
	public static @Nullable UUID getVariableId(ItemStack stack) {
		return VariableCardItem.getVariableId(stack);
	}

}