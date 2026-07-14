package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.BlockPreview3DWidget;
import dta.sfmflow.api.client.widget.InventorySelectorWidget;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.RedstoneConditionalComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class RedstoneConditionalSettingsOverlay extends NodeSettingsOverlay {
	private final InventorySelectorWidget selectorWidget;
	private final BlockPreview3DWidget previewWidget;
	private final CycleButton<Boolean> requiresAllBtn;
	private ApiWidgetAdapter<CycleButton<Boolean>> requiresAllAdapter;

	public RedstoneConditionalSettingsOverlay(ManagerScreen parentScreen, RedstoneConditionalComponent component) {
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
				face -> sideSupportsRedstone(parentScreen.getMenu().getManagerBlockEntity().getLevel(),
						getSelectedInventory() != null ? getSelectedInventory().getBlockPos() : null, face),
				parentScreen, () -> {
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.selectorWidget = new InventorySelectorWidget(getX() + 20, getY() + 28, component,
				ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"), parentScreen,
				// Custom filter to accept both receivers and emitters
				block -> {
					Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
					if (level != null) {
						BlockState state = level.getBlockState(block.getBlockPos());
						return state.is(ModBlocks.REDSTONE_RECEIVER_BLOCK.get()) ||
						       state.is(ModBlocks.REDSTONE_EMITTER_BLOCK.get());
					}
					return true;
				},
				newInv -> {
					component.setActiveSidesMask(0);
					if (this.previewWidget != null) {
						this.previewWidget.updateHighlightState();
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		// 1. "Requires All" Mode Cycle Button in the top right of the block view scene
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

		this.children.add(this.previewWidget);
		this.children.add(this.selectorWidget);
		this.children.add(this.requiresAllAdapter);
	}

	private ConnectionBlock getSelectedInventory() {
		int selectedId = ((RedstoneConditionalComponent) component).getInventoryId();
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

	private void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
	}

	@Override
	public void closeAndSave() {
		parentScreen.getMenu().setActiveComponent(null);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}
}