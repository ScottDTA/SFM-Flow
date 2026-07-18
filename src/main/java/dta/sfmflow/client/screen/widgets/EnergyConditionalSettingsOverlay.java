package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.BlockPreview3DWidget;
import dta.sfmflow.api.client.widget.InventorySelectorWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.EnergyConditionalComponent;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class EnergyConditionalSettingsOverlay extends NodeSettingsOverlay {
	private final InventorySelectorWidget selectorWidget;
	private final BlockPreview3DWidget previewWidget;
	private final CycleButton<EnergyConditionalComponent.ConditionOperator> opBtn;
	private final EditBox thresholdEdit;

	public EnergyConditionalSettingsOverlay(ManagerScreen parentScreen, EnergyConditionalComponent component) {
		super(parentScreen, component);
		this.width = 300;
		this.height = 360;
		this.setX((parentScreen.width - 300) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		parentScreen.getMenu().setActiveComponent(component);
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(),
				component.getId()));

		this.previewWidget = new BlockPreview3DWidget(getX() + 25, getY() + 78, 250, 190,
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, component,
						face -> sideSupportsCapability(
								parentScreen.getMenu().getManagerBlockEntity().getLevel(),
								getSelectedInventory(),
								face,
								ResourceLocation.fromNamespaceAndPath("sfmflow", "energy")
						), 
				parentScreen, () -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component,
				ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"),
				parentScreen, newInv -> {
					component.setActiveSidesMask(0);
					if (this.previewWidget != null) {
						this.previewWidget.updateHighlightState();
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.opBtn = CycleButton.<EnergyConditionalComponent.ConditionOperator>builder(val -> {
					return Component.literal("Operator: " + val.getSymbol());
				})
				.withValues(EnergyConditionalComponent.ConditionOperator.values())
				.withInitialValue(component.getOperator())
				.displayOnlyValue()
				// Stretched to occupy a centered 260px width
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

		this.children.add(this.previewWidget);
		this.children.add(this.selectorWidget);
		this.children.add(new ApiWidgetAdapter<>(this.opBtn));
		this.children.add(new ApiWidgetAdapter<>(this.thresholdEdit));

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 155, getY() + 311, 120, 10,
				Component.literal("FE Threshold Value"), 0.75F, false, () -> 0xFF404040));
	}

	@Override
	public void closeAndSave() {
		parentScreen.getMenu().setActiveComponent(null);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}
}