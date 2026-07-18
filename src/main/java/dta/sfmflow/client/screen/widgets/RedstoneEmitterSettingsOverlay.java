package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractTargetSettingsOverlay;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.RedstoneEmitterComponent;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Side configuration layout overlay for Redstone Emitters.
 */
@OnlyIn(Dist.CLIENT)
public class RedstoneEmitterSettingsOverlay extends AbstractTargetSettingsOverlay {
	public RedstoneEmitterSettingsOverlay(ManagerScreen parentScreen, RedstoneEmitterComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"), 360);
	}

	@Override
	protected boolean onInventoryFilter(ConnectionBlock block) {
		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		if (level != null) {
			return level.getBlockState(block.getBlockPos()).is(ModBlocks.REDSTONE_EMITTER_BLOCK.get());
		}
		return true;
	}
}