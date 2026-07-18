package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractTargetSettingsOverlay;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ObserverTriggerComponent;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Settings overlay enabling visual configuration of Observer Triggers.
 */
@OnlyIn(Dist.CLIENT)
public class ObserverTriggerSettingsOverlay extends AbstractTargetSettingsOverlay {
	public ObserverTriggerSettingsOverlay(ManagerScreen parentScreen, ObserverTriggerComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"), 360);

		// Force-sync frontFacing to the actual block's in-world facing on initial load
		Level initialLevel = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		ConnectionBlock initialInv = getSelectedInventory();
		if (initialLevel != null && initialInv != null) {
			BlockState state = initialLevel.getBlockState(initialInv.getBlockPos());
			if (state.hasProperty(BlockStateProperties.FACING)) {
				component.setFrontFacing(state.getValue(BlockStateProperties.FACING));
			}
		}
	}

	@Override
	protected boolean onInventoryFilter(ConnectionBlock block) {
		// Sided Filter: Only show Observer Cables in our selection list
		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		if (level != null) {
			return level.getBlockState(block.getBlockPos()).is(ModBlocks.OBSERVER_CABLE_BLOCK.get());
		}
		return true;
	}

	@Override
	protected void onInventorySelected(ConnectionBlock newInv) {
		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		if (level != null) {
			BlockState state = level.getBlockState(newInv.getBlockPos());
			if (state.hasProperty(BlockStateProperties.FACING)) {
				((ObserverTriggerComponent) component).setFrontFacing(state.getValue(BlockStateProperties.FACING));
			}
		}
	}
}