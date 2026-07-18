package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.api.client.widget.AbstractTargetSettingsOverlay;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.RedstoneConditionalComponent;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Settings overlay enabling visual configuration of Redstone Conditionals.
 */
@OnlyIn(Dist.CLIENT)
public class RedstoneConditionalSettingsOverlay extends AbstractTargetSettingsOverlay {
	private final CycleButton<Boolean> requiresAllBtn;
	private ApiWidgetAdapter<CycleButton<Boolean>> requiresAllAdapter;

	public RedstoneConditionalSettingsOverlay(ManagerScreen parentScreen, RedstoneConditionalComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"), 360);

		this.requiresAllBtn = CycleButton.<Boolean>builder(val -> val ? Component.literal("ALL") : Component.literal("ANY"))
				.withValues(true, false)
				.withInitialValue(component.isRequiresAll())
				.displayOnlyValue()
				.create(getX() + 155, getY() + 82, 110, 16, Component.literal("Requires All"), (btn, value) -> {
					component.setRequiresAll(value);
					this.requiresAllAdapter.setCustomTooltip(Tooltip.create(value 
							? Component.literal("ALL (AND): Every active side must simultaneously satisfy the comparison.") 
							: Component.literal("ANY (OR): Only one active side needs to satisfy the comparison.")));
					sendSettingsUpdate();
				});

		this.requiresAllAdapter = new ApiWidgetAdapter<>(this.requiresAllBtn);
		this.requiresAllAdapter.setCustomTooltip(Tooltip.create(component.isRequiresAll() 
				? Component.literal("ALL (AND): Every active side must simultaneously satisfy the comparison.") 
				: Component.literal("ANY (OR): Only one active side needs to satisfy the comparison.")));

		this.children.add(this.requiresAllAdapter);
	}

	@Override
	protected boolean onInventoryFilter(ConnectionBlock block) {
		// Sided Filter: Accept both Redstone Receivers and Emitter blocks in selector list
		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		if (level != null) {
			BlockState state = level.getBlockState(block.getBlockPos());
			return state.is(ModBlocks.REDSTONE_RECEIVER_BLOCK.get()) ||
				   state.is(ModBlocks.REDSTONE_EMITTER_BLOCK.get());
		}
		return true;
	}
}