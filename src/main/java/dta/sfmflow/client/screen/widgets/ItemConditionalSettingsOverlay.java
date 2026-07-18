package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.BlockPreview3DWidget;
import dta.sfmflow.api.client.widget.InventorySelectorWidget;
import dta.sfmflow.api.client.widget.ItemFilterWidget;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ItemConditionalComponent;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ItemConditionalSettingsOverlay extends NodeSettingsOverlay {
	private final InventorySelectorWidget selectorWidget;
	private final BlockPreview3DWidget previewWidget;
	private final ItemFilterWidget filterWidget;
	private final CycleButton<ItemConditionalComponent.MatchMode> matchModeBtn;
	private final CycleButton<ItemConditionalComponent.ConditionOperator> opBtn;

	public ItemConditionalSettingsOverlay(ManagerScreen parentScreen, ItemConditionalComponent component) {
		super(parentScreen, component);
		this.width = 300;
		this.height = 360;
		this.setX((parentScreen.width - 300) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		parentScreen.getMenu().setActiveComponent(component);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos() != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null, component.getId()));

		this.previewWidget = new BlockPreview3DWidget(getX() + 25, getY() + 78, 250, 190,
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, component,
						face -> sideSupportsCapability(
								parentScreen.getMenu().getManagerBlockEntity().getLevel(),
								getSelectedInventory(),
								face,
								ResourceLocation.fromNamespaceAndPath("sfmflow", "item")
						), 
				parentScreen, () -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component,
				ResourceLocation.fromNamespaceAndPath("sfmflow", "item"), parentScreen, newInv -> {
					component.setActiveSidesMask(0);
					for (Direction dir : Direction.values()) {
						component.setEnabledSlotsMask(dir, -1L);
					}
					if (this.previewWidget != null) {
						this.previewWidget.updateHighlightState();
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		// Dynamic match-mode toggle
		this.matchModeBtn = CycleButton.<ItemConditionalComponent.MatchMode>builder(val -> {
			return Component.literal(
					val == ItemConditionalComponent.MatchMode.MATCH_ALL ? "MATCH ALL (AND)" : "MATCH ANY (OR)");
		}).withValues(ItemConditionalComponent.MatchMode.values()).withInitialValue(component.getMatchMode())
				.displayOnlyValue()
				.create(getX() + 20, getY() + 272, 120, 18, Component.literal("Match Mode"), (btn, value) -> {
					component.setMatchMode(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		// Dynamic operator selector
		this.opBtn = CycleButton.<ItemConditionalComponent.ConditionOperator>builder(val -> {
			return Component.literal("Operator: " + val.getSymbol());
		}).withValues(ItemConditionalComponent.ConditionOperator.values()).withInitialValue(component.getOperator())
				.displayOnlyValue()
				.create(getX() + 160, getY() + 272, 120, 18, Component.literal("Operator"), (btn, value) -> {
					component.setOperator(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.children.add(this.previewWidget);
		this.children.add(this.selectorWidget);
		this.children.add(new ApiWidgetAdapter<>(this.matchModeBtn));
		this.children.add(new ApiWidgetAdapter<>(this.opBtn));

		this.filterWidget = new ItemFilterWidget(getX() + 30, getY() + 294, component, parentScreen, false, () -> {
			parentScreen.getMenu().getManagerBlockEntity().setChanged();
			sendSettingsUpdate();
		});
		this.children.add(this.filterWidget);
	}

	@Override
	public void closeAndSave() {
		// Deactivate the ghost slots polymorphically
		parentScreen.getMenu().setActiveComponent(null);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}
}