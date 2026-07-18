package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFilterableTargetSettingsOverlay;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ItemConditionalComponent;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Settings overlay enabling visual configuration of Item Conditionals.
 */
@OnlyIn(Dist.CLIENT)
public class ItemConditionalSettingsOverlay extends AbstractFilterableTargetSettingsOverlay {
	private final CycleButton<ItemConditionalComponent.MatchMode> matchModeBtn;
	private final CycleButton<ItemConditionalComponent.ConditionOperator> opBtn;

	public ItemConditionalSettingsOverlay(ManagerScreen parentScreen, ItemConditionalComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "item"), 360);

		this.matchModeBtn = CycleButton.<ItemConditionalComponent.MatchMode>builder(val -> {
					return Component.literal(val == ItemConditionalComponent.MatchMode.MATCH_ALL ? "MATCH ALL (AND)" : "MATCH ANY (OR)");
				})
				.withValues(ItemConditionalComponent.MatchMode.values())
				.withInitialValue(component.getMatchMode())
				.displayOnlyValue()
				.create(getX() + 20, getY() + 272, 120, 18, Component.literal("Match Mode"), (btn, value) -> {
					component.setMatchMode(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.opBtn = CycleButton.<ItemConditionalComponent.ConditionOperator>builder(val -> {
					return Component.literal("Operator: " + val.getSymbol());
				})
				.withValues(ItemConditionalComponent.ConditionOperator.values())
				.withInitialValue(component.getOperator())
				.displayOnlyValue()
				.create(getX() + 160, getY() + 272, 120, 18, Component.literal("Operator"), (btn, value) -> {
					component.setOperator(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.children.add(new ApiWidgetAdapter<>(this.matchModeBtn));
		this.children.add(new ApiWidgetAdapter<>(this.opBtn));
	}
}