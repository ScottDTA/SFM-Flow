package dta.sfmflow.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.List;

/**
 * Public client-only interface enabling addon developers to register custom 
 * central overlay rendering and tooltip lines for custom Variable cards.
 */
@OnlyIn(Dist.CLIENT)
public interface IVariableClientProperties {
	/**
	 * Renders the custom central ghost icon or fluid/chemical texture overlay on the card.
	 */
	void renderOverlay(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay);

	/**
	 * Appends custom localized tooltip detail lines for the card.
	 */
	void appendTooltip(ItemStack stack, List<Component> tooltipComponents);
}