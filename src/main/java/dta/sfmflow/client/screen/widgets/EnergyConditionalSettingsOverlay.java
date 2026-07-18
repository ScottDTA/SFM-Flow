package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractTargetSettingsOverlay;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.EnergyConditionalComponent;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Settings overlay enabling visual configuration of Energy Conditionals.
 */
@OnlyIn(Dist.CLIENT)
public class EnergyConditionalSettingsOverlay extends AbstractTargetSettingsOverlay {
	private final CycleButton<EnergyConditionalComponent.ConditionOperator> opBtn;
	private final EditBox thresholdEdit;

	public EnergyConditionalSettingsOverlay(ManagerScreen parentScreen, EnergyConditionalComponent component) {
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"), 360);

		this.opBtn = CycleButton.<EnergyConditionalComponent.ConditionOperator>builder(val -> {
					return Component.literal("Operator: " + val.getSymbol());
				})
				.withValues(EnergyConditionalComponent.ConditionOperator.values())
				.withInitialValue(component.getOperator())
				.displayOnlyValue()
				.create(getX() + 20, getY() + 272, 260, 18, Component.literal("Operator"), (btn, value) -> {
					component.setOperator(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.thresholdEdit = new EditBox(parentScreen.getFont(), getX() + 20, getY() + 306, 120, 18, Component.literal("FE Threshold"));
		this.thresholdEdit.setValue(String.valueOf(component.getThreshold()));
		this.thresholdEdit.setFilter(text -> text.matches("\\d*"));
		this.thresholdEdit.setResponder(text -> {
			try {
				int val = Integer.parseInt(text);
				if (val >= 0) {
					component.setThreshold(val);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				}
			} catch (NumberFormatException ignored) {}
		});

		this.children.add(new ApiWidgetAdapter<>(this.opBtn));
		this.children.add(new ApiWidgetAdapter<>(this.thresholdEdit));

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 155, getY() + 311, 120, 10,
				Component.literal("FE Threshold Value"), 0.75F, false, () -> 0xFF404040));
	}
}