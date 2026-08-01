package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.IFlowchartVariable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.networking.packets.serverbound.SyncCarriedItemPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class InventoryDrawerWidget extends AbstractFlowWidget {

	private static final ResourceLocation SLOT_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/variable_drawer_slot.png");

	private static final ResourceLocation BACKGROUND_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/variable_drawer_bg.png");

	private static final ResourceLocation BACKGROUND_END_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/variable_drawer_end.png");

	private static final ResourceLocation HANDLE_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/variable_drawer_handle.png");

	private static final int BG_TEX_WIDTH = 16;
	private static final int BG_TEX_HEIGHT = 82;
	private static final int HANDLE_TEX_WIDTH = 12;
	private static final int HANDLE_TEX_HEIGHT = 84;

	private static boolean isDrawerOpen = true;

	private final ManagerScreen parentScreen;
	private final EditBox searchEdit;
	private int scrollY = 0;

	private boolean open = isDrawerOpen;
	private transient float slideProgress = isDrawerOpen ? 1.0F : 0.0F;
	private transient long lastFrameTime = 0L;

	public InventoryDrawerWidget(ManagerScreen parentScreen, int x, int y, int width, int height) {
		super(isDrawerOpen ? x - width : x, y, width, height, Component.literal("Inventories Drawer"));
		this.parentScreen = parentScreen;

		this.searchEdit = new EditBox(parentScreen.getFont(), getX() + 4, getY() + 6, width - 11, 12,
				Component.literal("Search"));
		this.searchEdit.setHint(Component.literal("Search..."));
		this.searchEdit.setCanLoseFocus(true);
		this.children.add(new ApiWidgetAdapter<>(this.searchEdit));
	}

	private List<IFlowchartVariable> getFilteredVariables() {
		List<IFlowchartVariable> filtered = new ArrayList<>();
		String query = searchEdit.getValue().toLowerCase(Locale.ROOT).trim();

		var components = parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().values();
		for (var comp : components) {
			if (comp instanceof IFlowchartVariable listVar && listVar.isInventoryList()) {
				String componentName = comp.getName().getString().toLowerCase(Locale.ROOT);
				String colorName = listVar.getFilterColor().getSerializedName().toLowerCase(Locale.ROOT);

				if (query.isEmpty() || componentName.contains(query) || colorName.contains(query)) {
					filtered.add(listVar);
				}
			}
		}
		return filtered;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active || !isMouseOver(mouseX, mouseY)) {
			return false;
		}

		ItemStack carried = parentScreen.getMenu().getCarried();
		if (button == 0 && !carried.isEmpty() && carried.is(ModItems.VARIABLE_CARD.get())) {
			if (Util.getMillis() - parentScreen.getLastVariablePickupTime() < 250L) {
				return false;
			}
			parentScreen.getMenu().setCarried(ItemStack.EMPTY);
			PacketDistributor.sendToServer(new SyncCarriedItemPacket(ItemStack.EMPTY));
			parentScreen.getMinecraft().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		}

		if (button == 0 && mouseX >= getX() - HANDLE_TEX_WIDTH && mouseX < getX()
				&& mouseY >= getY() - 1 && mouseY < getY() + HANDLE_TEX_HEIGHT - 1) {
			this.open = !this.open;
			isDrawerOpen = this.open;
			this.parentScreen.getMinecraft().getSoundManager()
					.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		}

		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}

		int gridX = getX() + 8;
		int gridY = getY() + 20;

		if (button == 0 && mouseX >= gridX && mouseX < gridX + 54 && mouseY >= gridY && mouseY < gridY + 54) {
			List<IFlowchartVariable> vars = getFilteredVariables();

			for (int i = 0; i < vars.size(); i++) {
				int col = i % 3;
				int row = i / 3;

				int cellX = getCellX(col);
				int cellY = getCellY(row);

				if (mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18) {
					IFlowchartVariable clickedVar = vars.get(i);
					ItemStack stack = clickedVar.toItemStack();

					parentScreen.getMenu().setCarried(stack);
					PacketDistributor.sendToServer(new SyncCarriedItemPacket(stack));
					parentScreen.setLastVariablePickupTime(Util.getMillis()); // Record pickup timestamp [3]

					Minecraft.getInstance().getSoundManager().play(
							SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return true;
				}
			}
		}

		return false;
	}

	private int getCellX(int col) {
		return getX() + 8 + col * 18;
	}

	private int getCellY(int row) {
		return getY() + 20 + row * 18 - scrollY;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int gridX = getX() + 8;
		int gridY = getY() + 20;

		if (mouseX >= gridX && mouseX < gridX + 54 && mouseY >= gridY && mouseY < gridY + 54) {
			List<IFlowchartVariable> vars = getFilteredVariables();
			int rows = (vars.size() + 2) / 3;
			int maxScrollY = Math.max(0, rows * 18 - 54);

			if (maxScrollY > 0) {
				this.scrollY = Mth.clamp(this.scrollY - (int) scrollY * 10, 0, maxScrollY);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		if (lastFrameTime == 0L) {
			lastFrameTime = now;
		}
		float deltaTime = (now - lastFrameTime) / 1000.0F;
		lastFrameTime = now;
		deltaTime = Math.min(deltaTime, 0.1F);

		float speed = 8.0F;
		int openX = parentScreen.getLeftPos() + 168 - width;
		int closedX = parentScreen.getLeftPos() + 168;

		if (open) {
			if (slideProgress < 1.0F) {
				slideProgress = Math.min(1.0F, slideProgress + deltaTime * speed);
				this.setX((int) Math.round(closedX + (openX - closedX) * slideProgress));
			}
		} else {
			if (slideProgress > 0.0F) {
				slideProgress = Math.max(0.0F, slideProgress - deltaTime * speed);
				this.setX((int) Math.round(closedX + (openX - closedX) * slideProgress));
			}
		}

		guiGraphics.enableScissor(0, 0, parentScreen.getLeftPos() + 168, parentScreen.height);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		guiGraphics.blit(BACKGROUND_END_TX, getX(), getY(), 4, height, (float) 4, 0.0F, -4, BG_TEX_HEIGHT, 4, BG_TEX_HEIGHT);
		guiGraphics.blit(BACKGROUND_TX, getX() + 4, getY(), width - 4, height, (float) BG_TEX_WIDTH, 0.0F, -BG_TEX_WIDTH, BG_TEX_HEIGHT, BG_TEX_WIDTH, BG_TEX_HEIGHT);

		guiGraphics.blit(HANDLE_TX, getX() - HANDLE_TEX_WIDTH, getY() - 1, HANDLE_TEX_WIDTH, HANDLE_TEX_HEIGHT, (float) HANDLE_TEX_WIDTH, 0.0F, -HANDLE_TEX_WIDTH, HANDLE_TEX_HEIGHT, HANDLE_TEX_WIDTH, HANDLE_TEX_HEIGHT);

		guiGraphics.pose().pushPose();
		int textWidth = parentScreen.getFont().width("Inventories");
		int fontHeight = parentScreen.getFont().lineHeight;

		float textX = (getX() - HANDLE_TEX_WIDTH) + (HANDLE_TEX_WIDTH - fontHeight) / 2.0F;
		float textY = (getY() - 1) + (HANDLE_TEX_HEIGHT + textWidth) / 2.0F;

		guiGraphics.pose().translate(textX, textY, 0.0F);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(270.0F));
		guiGraphics.drawString(parentScreen.getFont(), "Inventories", 0, 1, 0xFF404040, false);
		guiGraphics.pose().popPose();

		this.searchEdit.setX(getX() + 4);
		this.searchEdit.setY(getY() + 6);
		this.searchEdit.render(guiGraphics, mouseX, mouseY, partialTick);

		int gridX = getX() + 8;
		int gridY = getY() + 20;

		guiGraphics.enableScissor(gridX, gridY, gridX + 54, gridY + 54);

		List<IFlowchartVariable> vars = getFilteredVariables();

		for (int i = 0; i < vars.size(); i++) {
			int col = i % 3;
			int row = i / 3;

			int cellX = getCellX(col);
			int cellY = getCellY(row);

			if (cellY + 18 < gridY || cellY > gridY + 54) {
				continue;
			}

			boolean isHovered = mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18;
			guiGraphics.blit(SLOT_TX, cellX, cellY, 0.0F, isHovered ? 18.0F : 0.0F, 18, 18, 18, 36);

			ItemStack cardStack = vars.get(i).toItemStack();
			if (!cardStack.isEmpty()) {
				guiGraphics.renderItem(cardStack, cellX + 1, cellY + 1);
				guiGraphics.renderItemDecorations(parentScreen.getFont(), cardStack, cellX + 1, cellY + 1);
			}
		}

		guiGraphics.disableScissor();
		guiGraphics.disableScissor();
	}

	/**
	 * Defer tooltip rendering to the final, clean screen draw pass.
	 * This prevents items, backgrounds, or custom meshes from culling the text.
	 */
	public void renderTooltipDeferred(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if (!this.visible) {
			return;
		}

		int gridX = getX() + 8;
		int gridY = getY() + 20;

		boolean hoveringHandle = mouseX >= getX() - HANDLE_TEX_WIDTH && mouseX < getX()
				&& mouseY >= getY() - 1 && mouseY < getY() + HANDLE_TEX_HEIGHT - 1;

		if (hoveringHandle) {
			Component tooltipText = open ? Component.literal("Close Drawer") : Component.literal("Open Drawer");
			guiGraphics.renderTooltip(parentScreen.getFont(), tooltipText, mouseX, mouseY);
		} else if (mouseX >= gridX && mouseX < gridX + 54 && mouseY >= gridY && mouseY < gridY + 54) {
			List<IFlowchartVariable> vars = getFilteredVariables();
			for (int i = 0; i < vars.size(); i++) {
				int col = i % 3;
				int row = i / 3;
				int cellX = getCellX(col);
				int cellY = getCellY(row);

				if (mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18) {
					ItemStack hoverStack = vars.get(i).toItemStack();
					if (!hoverStack.isEmpty()) {
						guiGraphics.renderTooltip(parentScreen.getFont(), hoverStack, mouseX, mouseY);
					}
				}
			}
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		double leftBound = getX() - HANDLE_TEX_WIDTH;
		double rightBound = parentScreen.getLeftPos() + 168;
		double topBound = getY() - 1;
		double bottomBound = getY() + height + 1;
		return this.visible && mouseX >= leftBound && mouseX < rightBound && mouseY >= topBound && mouseY < bottomBound;
	}

	@Override
	public void setX(int x) {
		int dif = this.getX() - x;
		super.setX(x);
		updateChildrenXPositions(dif);
	}

	@Override
	public void setY(int y) {
		int dif = this.getY() - y;
		super.setY(y);
		updateChildrenYPositions(dif);
	}
}