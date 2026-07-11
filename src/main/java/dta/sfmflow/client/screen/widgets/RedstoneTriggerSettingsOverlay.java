package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.BlockPreview3DWidget;
import dta.sfmflow.api.client.widget.InventorySelectorWidget;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.RedstoneTriggerComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Multi-interval and sided threshold configuration screen for Redstone Triggers [3].
 */
@OnlyIn(Dist.CLIENT)
public class RedstoneTriggerSettingsOverlay extends NodeSettingsOverlay {
	private final InventorySelectorWidget selectorWidget;
	private final BlockPreview3DWidget previewWidget;

	private final CycleButton<Boolean> requiresAllBtn;
	private ApiWidgetAdapter<CycleButton<Boolean>> requiresAllAdapter; 

	// While High Columns
	private final CycleButton<IntervalTriggerComponent.TimeUnit> highUnitBtn;
	private HighIntervalSlider highIntervalSlider;

	// While Low Columns
	private final CycleButton<IntervalTriggerComponent.TimeUnit> lowUnitBtn;
	private LowIntervalSlider lowIntervalSlider;

	public RedstoneTriggerSettingsOverlay(ManagerScreen parentScreen, RedstoneTriggerComponent component) {
		super(parentScreen, component);
		this.width = 300;
		this.height = 360; // Expanded to 360 to prevent viewport clip desyncs [3]
		this.setX((parentScreen.width - 300) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		parentScreen.getMenu().setActiveComponent(component);
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId()));

		// Height increased to 190 to allow correct 3D block projections and prevent ghosting leaks [3]
		this.previewWidget = new BlockPreview3DWidget(getX() + 25, getY() + 78, 250, 190,
				() -> getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, component,
				face -> sideSupportsRedstone(parentScreen.getMenu().getManagerBlockEntity().getLevel(),
						getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, face),
				parentScreen, () -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component,
				ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"),
				parentScreen, 
				// Sided Filter: Only show Redstone Receivers in list [3]
				block -> {
					Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
					if (level != null) {
						return level.getBlockState(block.getBlockPos()).is(dta.sfmflow.block.ModBlocks.REDSTONE_RECEIVER_BLOCK.get());
					}
					return true;
				},
				newInv -> {
					component.setActiveSidesMask(0); // Reset side selection mask [3]
					if (this.previewWidget != null) {
						this.previewWidget.updateHighlightState();
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		// 1. "Requires All" Mode Cycle Button in top right of viewport with custom description tooltips [3]
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

		// 2. "While High" Interval Column [3]
		this.highUnitBtn = CycleButton.<IntervalTriggerComponent.TimeUnit>builder(IntervalTriggerComponent.TimeUnit::getDisplayName)
				.withValues(IntervalTriggerComponent.TimeUnit.values())
				.withInitialValue(component.getHighTimeUnit())
				.displayOnlyValue()
				.create(getX() + 15, getY() + 286, 120, 18, Component.literal("High Unit"), (btn, value) -> {
					component.setHighTimeUnit(value);
					this.highIntervalSlider.refresh();
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.highIntervalSlider = new HighIntervalSlider(getX() + 15, getY() + 308, 120, 18, component, this);

		// 3. "While Low" Interval Column [3]
		this.lowUnitBtn = CycleButton.<IntervalTriggerComponent.TimeUnit>builder(IntervalTriggerComponent.TimeUnit::getDisplayName)
				.withValues(IntervalTriggerComponent.TimeUnit.values())
				.withInitialValue(component.getLowTimeUnit())
				.displayOnlyValue()
				.create(getX() + 160, getY() + 286, 120, 18, Component.literal("Low Unit"), (btn, value) -> {
					component.setLowTimeUnit(value);
					this.lowIntervalSlider.refresh();
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.lowIntervalSlider = new LowIntervalSlider(getX() + 160, getY() + 308, 120, 18, component, this);

		this.children.add(this.previewWidget);
		this.children.add(this.selectorWidget);
		this.children.add(this.requiresAllAdapter);
		this.children.add(new ApiWidgetAdapter<>(this.highUnitBtn));
		this.children.add(new ApiWidgetAdapter<>(this.highIntervalSlider));
		this.children.add(new ApiWidgetAdapter<>(this.lowUnitBtn));
		this.children.add(new ApiWidgetAdapter<>(this.lowIntervalSlider));

		// Symmetrical headers [3]
		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 15, getY() + 274, 120, 10,
				Component.literal("While High Interval"), 0.75F, false, () -> 0xFF404040));
		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 160, getY() + 274, 120, 10,
				Component.literal("While Low Interval"), 0.75F, false, () -> 0xFF404040));
	}

	private ConnectionBlock getSelectedInventory() {
		int selectedId = ((RedstoneTriggerComponent) component).getInventoryId();
		if (selectedId != -1) {
			for (ConnectionBlock block : parentScreen.getMenu().getManagerBlockEntity().getInventories()) {
				if (block.getId() == selectedId) {
					return block;
				}
			}
		}
		return null;
	}

	private boolean sideSupportsRedstone(Level level, BlockPos pos, Direction side) {
		if (level == null || pos == null) {
			return false;
		}
		var flowCap = FlowCapabilityRegistry.get(ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"));
		if (flowCap != null) {
			return flowCap.isPresent(level, pos, level.getBlockState(pos), level.getBlockEntity(pos), side);
		}
		return false;
	}

	public void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId(), nbt));
	}

	@Override
	public void closeAndSave() {
		parentScreen.getMenu().setActiveComponent(null);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}

	@OnlyIn(Dist.CLIENT)
	private static class HighIntervalSlider extends AbstractSliderButton {
		private final RedstoneTriggerComponent component;
		private final RedstoneTriggerSettingsOverlay overlay;

		public HighIntervalSlider(int x, int y, int width, int height, RedstoneTriggerComponent component, RedstoneTriggerSettingsOverlay overlay) {
			super(x, y, width, height, Component.literal("Interval"), getInitialValueProgress(component));
			this.component = component;
			this.overlay = overlay;
			this.updateMessage();
		}

		private static double getInitialValueProgress(RedstoneTriggerComponent comp) {
			int val = comp.getHighIntervalValue();
			int min = getMinLimit(comp);
			int max = getMaxLimit(comp);
			return (double) (val - min) / (double) (max - min);
		}

		private static int getMinLimit(RedstoneTriggerComponent comp) {
			return (comp.getHighTimeUnit() == IntervalTriggerComponent.TimeUnit.TICKS) ? 5 : 1;
		}

		private static int getMaxLimit(RedstoneTriggerComponent comp) {
			return (comp.getHighTimeUnit() == IntervalTriggerComponent.TimeUnit.TICKS) ? 100 : 60;
		}

		public void refresh() {
			this.value = getInitialValueProgress(this.component);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int min = getMinLimit(this.component);
			int max = getMaxLimit(this.component);
			int val = min + (int) Math.round(this.value * (max - min));
			setMessage(Component.literal(val + " " + this.component.getHighTimeUnit().getDisplayName().getString()));
		}

		@Override
		protected void applyValue() {
			int min = getMinLimit(this.component);
			int max = getMaxLimit(this.component);
			int val = min + (int) Math.round(this.value * (max - min));
			this.component.setHighIntervalValue(val);
			this.overlay.sendSettingsUpdate();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int min = getMinLimit(this.component);
				int max = getMaxLimit(this.component);
				int val = this.component.getHighIntervalValue();
				int newVal = Mth.clamp(val + (scrollY > 0 ? 1 : -1), min, max);

				if (newVal != val) {
					this.component.setHighIntervalValue(newVal);
					this.value = (double) (newVal - min) / (double) (max - min);
					this.updateMessage();
					this.overlay.sendSettingsUpdate();
				}
				return true;
			}
			return false;
		}

		@Override
		public void playDownSound(SoundManager soundManager) {
			// Silent
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class LowIntervalSlider extends AbstractSliderButton {
		private final RedstoneTriggerComponent component;
		private final RedstoneTriggerSettingsOverlay overlay;

		public LowIntervalSlider(int x, int y, int width, int height, RedstoneTriggerComponent component, RedstoneTriggerSettingsOverlay overlay) {
			super(x, y, width, height, Component.literal("Interval"), getInitialValueProgress(component));
			this.component = component;
			this.overlay = overlay;
			this.updateMessage();
		}

		private static double getInitialValueProgress(RedstoneTriggerComponent comp) {
			int val = comp.getLowIntervalValue();
			int min = getMinLimit(comp);
			int max = getMaxLimit(comp);
			return (double) (val - min) / (double) (max - min);
		}

		private static int getMinLimit(RedstoneTriggerComponent comp) {
			return (comp.getLowTimeUnit() == IntervalTriggerComponent.TimeUnit.TICKS) ? 5 : 1;
		}

		private static int getMaxLimit(RedstoneTriggerComponent comp) {
			return (comp.getLowTimeUnit() == IntervalTriggerComponent.TimeUnit.TICKS) ? 100 : 60;
		}

		public void refresh() {
			this.value = getInitialValueProgress(this.component);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int min = getMinLimit(this.component);
			int max = getMaxLimit(this.component);
			int val = min + (int) Math.round(this.value * (max - min));
			setMessage(Component.literal(val + " " + this.component.getLowTimeUnit().getDisplayName().getString()));
		}

		@Override
		protected void applyValue() {
			int min = getMinLimit(this.component);
			int max = getMaxLimit(this.component);
			int val = min + (int) Math.round(this.value * (max - min));
			this.component.setLowIntervalValue(val);
			this.overlay.sendSettingsUpdate();
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
				int min = getMinLimit(this.component);
				int max = getMaxLimit(this.component);
				int val = this.component.getLowIntervalValue();
				int newVal = Mth.clamp(val + (scrollY > 0 ? 1 : -1), min, max);

				if (newVal != val) {
					this.component.setLowIntervalValue(newVal);
					this.value = (double) (newVal - min) / (double) (max - min);
					this.updateMessage();
					this.overlay.sendSettingsUpdate();
				}
				return true;
			}
			return false;
		}

		@Override
		public void playDownSound(SoundManager soundManager) {
			// Silent
		}
	}
}