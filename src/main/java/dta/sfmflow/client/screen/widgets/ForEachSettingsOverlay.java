package dta.sfmflow.client.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.api.client.widget.NodeSettingsOverlay;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.IFlowchartVariable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.ForEachComponent;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.util.Color;
import dta.sfmflow.util.MenuSlotRepositioner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ForEachSettingsOverlay extends NodeSettingsOverlay {
	private static final ResourceLocation FILTER_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/filter_slot.png");

	private final ForEachComponent component;
	private final CycleButton<ForEachComponent.IterationMode> modeBtn;
	private final EditBox nameEdit;

	public ForEachSettingsOverlay(ManagerScreen parentScreen, ForEachComponent component) {
		super(parentScreen, component);
		this.component = component;
		this.width = 240;
		this.height = 155;
		this.setX((parentScreen.width - this.width) / 2);
		this.setY(parentScreen.getOverlayTargetY(this.height));

		parentScreen.getMenu().setActiveComponent(component);
		PacketDistributor.sendToServer(new SetActiveFilterComponentPacket(
				parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getId()) != null
						? parentScreen.getMenu().getManagerBlockEntity().getBlockPos()
						: null,
				component.getId()));

		repositionGhostSlot();

		this.modeBtn = CycleButton.<ForEachComponent.IterationMode>builder(val -> Component.literal(val.name().replace("_", " ").toUpperCase()))
				.withValues(ForEachComponent.IterationMode.values())
				.withInitialValue(component.getIterationMode())
				.displayOnlyValue()
				.create(getX() + 90, getY() + 34, 120, 18, Component.literal("Mode"), (btn, value) -> {
					component.setIterationMode(value);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.nameEdit = new EditBox(parentScreen.getFont(), getX() + 60, getY() + 76, 150, 18, Component.literal("Element Name"));
		this.nameEdit.setValue(component.getElementName());
		this.nameEdit.setMaxLength(16);
		this.nameEdit.setResponder(text -> {
			component.setElementName(text);
			parentScreen.getMenu().getManagerBlockEntity().setChanged();
			sendSettingsUpdate();
		});

		this.children.add(new ApiWidgetAdapter<>(this.modeBtn));
		this.children.add(new ApiWidgetAdapter<>(this.nameEdit));

		this.children.add(new FlowWidgetText(parentScreen.getFont(), getX() + 15, getY() + 58, 200, 10,
				Component.literal("Element Variable Config:"), 0.75F, false, () -> 0xFF404040));

		// ColorPanel offset slightly to the right to leave space on the left for the icon preview
		this.children.add(new ColorPanelWidget(getX() + 35, getY() + 77));
	}

	private void repositionGhostSlot() {
		int slotX = getX() + 30;
		int slotY = getY() + 34;
		Slot slot = parentScreen.getMenu().slots.get(36);
		MenuSlotRepositioner.setSlotPosition(slot, slotX - parentScreen.getLeftPos(), slotY - parentScreen.getTopPos());
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		repositionGhostSlot();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		repositionGhostSlot();
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderComponent(guiGraphics, mouseX, mouseY, partialTick);

		int slotX = getX() + 30;
		int slotY = getY() + 34;

		ItemStack ghost = getGhostStack();
		boolean hasItem = !ghost.isEmpty();
		int vOffset = hasItem ? 18 : 0;

		guiGraphics.blit(FILTER_SLOT_TEXTURE, slotX, slotY, 0, vOffset, 18, 18, 18, 36);

		if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
			guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF8B8B8B);
		}

		if (hasItem) {
			guiGraphics.renderItem(ghost, slotX + 1, slotY + 1);
			guiGraphics.renderItemDecorations(parentScreen.getFont(), ghost, slotX + 1, slotY + 1);
		}

		// Render the custom element live preview [3]
		ResourceLocation elementTex = ResourceLocation.fromNamespaceAndPath("sfmflow", "textures/gui/flowcomponents/element_overlay.png");
		int hex = component.getElementColor().getHexColor();
		float r = ((hex >> 16) & 0xFF) / 255.0F;
		float g = ((hex >> 8) & 0xFF) / 255.0F;
		float b = (hex & 0xFF) / 255.0F;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(r, g, b, 1.0F);
		guiGraphics.blit(elementTex, getX() + 14, getY() + 77, 0, 0, 16, 16, 16, 16);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
		
		// Rounded boundary outline around the live preview [3]
		guiGraphics.renderOutline(getX() + 13, getY() + 76, 18, 18, 0xFF434343);
	}

	private ItemStack getGhostStack() {
		if (component.getBoundListVariableId() != null) {
			AbstractFlowComponent varComp = parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().get(component.getBoundListVariableId());
			if (varComp instanceof IFlowchartVariable flowchartVar) {
				return flowchartVar.toItemStack();
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void closeAndSave() {
		parentScreen.getMenu().setActiveComponent(null);
		PacketDistributor.sendToServer(
				new SetActiveFilterComponentPacket(parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), null));
		super.closeAndSave();
	}

	@OnlyIn(Dist.CLIENT)
	private class ColorPanelWidget extends AbstractFlowWidget {
		public ColorPanelWidget(int x, int y) {
			super(x, y, 16, 16, Component.literal("Color Panel"));
		}

		@Override
		protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			Color current = component.getElementColor();
			boolean hovered = mouseX >= getX() && mouseX < getX() + 16 && mouseY >= getY() && mouseY < getY() + 16;
			int border = hovered ? 0xFFD4AF37 : 0xFF8B8B8B;

			guiGraphics.fill(getX(), getY(), getX() + 16, getY() + 16, current.getHexColor() | 0xFF000000);
			guiGraphics.renderOutline(getX(), getY(), 16, 16, border);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (this.visible && this.active && mouseX >= getX() && mouseX < getX() + 16 && mouseY >= getY()
					&& mouseY < getY() + 16) {
				if (button == 0) {
					Color[] values = Color.values();
					int nextIdx = (component.getElementColor().ordinal() + 1) % values.length;
					component.setElementColor(values[nextIdx]);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
					Minecraft.getInstance().getSoundManager()
							.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return true;
				}
			}
			return false;
		}
	}
}