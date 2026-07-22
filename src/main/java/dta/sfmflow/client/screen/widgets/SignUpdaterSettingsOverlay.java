package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractTargetSettingsOverlay;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.SignUpdaterComponent;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class SignUpdaterSettingsOverlay extends AbstractTargetSettingsOverlay {

	public SignUpdaterSettingsOverlay(ManagerScreen parentScreen, AbstractFlowComponent component) {
		// Adjusted height slightly to 365 to fit the new buttons row perfectly [3]
		super(parentScreen, component, ResourceLocation.fromNamespaceAndPath("sfmflow", "sign_updater"), 365);

		SignUpdaterComponent signUpdater = (SignUpdaterComponent) component;

		this.previewWidget.setHeight(130);

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 15, getY() + 215, 120, 10,
				Component.literal("Front Sign Text"), 0.75F, false, () -> 0xFF404040));
		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 150, getY() + 215, 120, 10,
				Component.literal("Back Sign Text"), 0.75F, false, () -> 0xFF404040));

		// =========================================================================
		// FRONT SIDE CYCLE BUTTONS (Color, Glow, Wax)
		// =========================================================================
		CycleButton<DyeColor> frontColorBtn = CycleButton.<DyeColor>builder(val -> Component.literal(val.name().substring(0, 3).toUpperCase(Locale.ROOT)))
				.withValues(DyeColor.values())
				.withInitialValue(signUpdater.getFrontColor())
				.displayOnlyValue()
				.create(getX() + 15, getY() + 230, 38, 16, Component.empty(), (btn, value) -> {
					signUpdater.setFrontColor(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});
		this.children.add(new ApiWidgetAdapter<>(frontColorBtn));

		CycleButton<Boolean> frontGlowBtn = CycleButton.<Boolean>builder(val -> val ? Component.literal("GLW") : Component.literal("DRK"))
				.withValues(true, false)
				.withInitialValue(signUpdater.isFrontGlow())
				.displayOnlyValue()
				.create(getX() + 57, getY() + 230, 38, 16, Component.empty(), (btn, value) -> {
					signUpdater.setFrontGlow(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});
		this.children.add(new ApiWidgetAdapter<>(frontGlowBtn));

		CycleButton<Boolean> frontWaxBtn = CycleButton.<Boolean>builder(val -> val ? Component.literal("WAX") : Component.literal("RAW"))
				.withValues(true, false)
				.withInitialValue(signUpdater.isWaxed())
				.displayOnlyValue()
				.create(getX() + 99, getY() + 230, 38, 16, Component.empty(), (btn, value) -> {
					signUpdater.setWaxed(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
					// Automatically refresh the layout to synchronize the Back Column's Wax button visually [3]
					parentScreen.refreshWidgetLayout();
				});
		this.children.add(new ApiWidgetAdapter<>(frontWaxBtn));

		// =========================================================================
		// BACK SIDE CYCLE BUTTONS (Color, Glow, Wax)
		// =========================================================================
		CycleButton<DyeColor> backColorBtn = CycleButton.<DyeColor>builder(val -> Component.literal(val.name().substring(0, 3).toUpperCase(Locale.ROOT)))
				.withValues(DyeColor.values())
				.withInitialValue(signUpdater.getBackColor())
				.displayOnlyValue()
				.create(getX() + 150, getY() + 230, 38, 16, Component.empty(), (btn, value) -> {
					signUpdater.setBackColor(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});
		this.children.add(new ApiWidgetAdapter<>(backColorBtn));

		CycleButton<Boolean> backGlowBtn = CycleButton.<Boolean>builder(val -> val ? Component.literal("GLW") : Component.literal("DRK"))
				.withValues(true, false)
				.withInitialValue(signUpdater.isBackGlow())
				.displayOnlyValue()
				.create(getX() + 192, getY() + 230, 38, 16, Component.empty(), (btn, value) -> {
					signUpdater.setBackGlow(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});
		this.children.add(new ApiWidgetAdapter<>(backGlowBtn));

		CycleButton<Boolean> backWaxBtn = CycleButton.<Boolean>builder(val -> val ? Component.literal("WAX") : Component.literal("RAW"))
				.withValues(true, false)
				.withInitialValue(signUpdater.isWaxed())
				.displayOnlyValue()
				.create(getX() + 234, getY() + 230, 38, 16, Component.empty(), (btn, value) -> {
					signUpdater.setWaxed(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
					// Automatically refresh the layout to synchronize the Front Column's Wax button visually [3]
					parentScreen.refreshWidgetLayout();
				});
		this.children.add(new ApiWidgetAdapter<>(backWaxBtn));

		// =========================================================================
		// TEXT INPUT ROWS (Shifted down to start at 252)
		// =========================================================================
		for (int i = 0; i < 4; i++) {
			final int index = i;

			EditBox frontEdit = new EditBox(parentScreen.getFont(), getX() + 35, getY() + 252 + i * 22, 100, 16, Component.empty());
			frontEdit.setValue(signUpdater.getFrontLines().get(index));
			frontEdit.setResponder(text -> {
				signUpdater.getFrontLines().set(index, text);
				parentScreen.getMenu().getManagerBlockEntity().setChanged();
				sendSettingsUpdate();
			});
			frontEdit.setEditable(signUpdater.getUpdateFront().get(index));
			this.children.add(new ApiWidgetAdapter<>(frontEdit));

			Checkbox frontCheck = Checkbox.builder(Component.empty(), parentScreen.getFont())
					.pos(getX() + 15, getY() + 252 + i * 22)
					.selected(signUpdater.getUpdateFront().get(index))
					.onValueChange((checkbox, selected) -> {
						signUpdater.getUpdateFront().set(index, selected);
						frontEdit.setEditable(selected);
						parentScreen.getMenu().getManagerBlockEntity().setChanged();
						sendSettingsUpdate();
					}).build();
			this.children.add(new ApiWidgetAdapter<>(frontCheck));

			EditBox backEdit = new EditBox(parentScreen.getFont(), getX() + 170, getY() + 252 + i * 22, 100, 16, Component.empty());
			backEdit.setValue(signUpdater.getBackLines().get(index));
			backEdit.setResponder(text -> {
				signUpdater.getBackLines().set(index, text);
				parentScreen.getMenu().getManagerBlockEntity().setChanged();
				sendSettingsUpdate();
			});
			backEdit.setEditable(signUpdater.getUpdateBack().get(index));
			this.children.add(new ApiWidgetAdapter<>(backEdit));

			Checkbox backCheck = Checkbox.builder(Component.empty(), parentScreen.getFont())
					.pos(getX() + 150, getY() + 252 + i * 22)
					.selected(signUpdater.getUpdateBack().get(index))
					.onValueChange((checkbox, selected) -> {
						signUpdater.getUpdateBack().set(index, selected);
						backEdit.setEditable(selected);
						parentScreen.getMenu().getManagerBlockEntity().setChanged();
						sendSettingsUpdate();
					}).build();
			this.children.add(new ApiWidgetAdapter<>(backCheck));
		}
	}

	@Override
	protected boolean onInventoryFilter(ConnectionBlock block) {
		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		if (level != null) {
			return level.getBlockState(block.getBlockPos()).is(ModBlocks.SIGN_UPDATER_CABLE_BLOCK.get());
		}
		return true;
	}

	@Override
	protected void onInventorySelected(ConnectionBlock newInv) {
		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		if (level != null) {
			BlockState state = level.getBlockState(newInv.getBlockPos());
			if (state.hasProperty(BlockStateProperties.FACING)) {
				((SignUpdaterComponent) component).setFrontFacing(state.getValue(BlockStateProperties.FACING));
			}
		}
	}
}