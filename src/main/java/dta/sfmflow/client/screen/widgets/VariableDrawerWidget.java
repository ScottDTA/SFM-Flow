package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.networking.packets.serverbound.SyncCarriedItemPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.Util;
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

/**
 * Side-sliding drawer widget displaying a searchable, vertically scrollable 3x3
 * grid of active variables [3].
 */
@OnlyIn(Dist.CLIENT)
public class VariableDrawerWidget extends AbstractFlowWidget {

	// Path to your custom dual-state slot texture (18x36 px) [3]
	private static final ResourceLocation SLOT_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/variable_drawer_slot.png");

	// Path to your custom horizontally-tiling background texture [3]
	private static final ResourceLocation BACKGROUND_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/variable_drawer_bg.png");

	private static final ResourceLocation BACKGROUND_END_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/variable_drawer_end.png");

	// Path to your custom vertical drawer handle texture (12x84 px) [3]
	private static final ResourceLocation HANDLE_TX = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/variable_drawer_handle.png");

	private static final int BG_TEX_WIDTH = 16;
	private static final int BG_TEX_HEIGHT = 82;
	private static final int HANDLE_TEX_WIDTH = 12;
	private static final int HANDLE_TEX_HEIGHT = 84;

	// Persistent state matching user layout preference on GUI re-entry [3]
	private static boolean isDrawerOpen = true;

	private final ManagerScreen parentScreen;
	private final EditBox searchEdit;
	private int scrollY = 0;

	private boolean open = isDrawerOpen;
	private transient float slideProgress = isDrawerOpen ? 1.0F : 0.0F;
	private transient long lastFrameTime = 0L;

	public VariableDrawerWidget(ManagerScreen parentScreen, int x, int y, int width, int height) {
		super(x, y, width, height, Component.literal("Variables Drawer"));
		this.parentScreen = parentScreen;

		this.searchEdit = new EditBox(parentScreen.getFont(), getX() + 4, getY() + 6, width - 11, 12,
				Component.literal("Search"));
		this.searchEdit.setHint(Component.literal("Search..."));
		this.searchEdit.setCanLoseFocus(true);
		this.children.add(new ApiWidgetAdapter<>(this.searchEdit));
	}

	private List<AdvancedItemFilterVariableComponent> getFilteredVariables() {
		List<AdvancedItemFilterVariableComponent> filtered = new ArrayList<>();
		String query = searchEdit.getValue().toLowerCase(Locale.ROOT).trim();

		var components = parentScreen.getMenu().getManagerBlockEntity().getFlowComponents().values();
		for (var comp : components) {
			if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
				// Symmetrical Check: Skip empty variables in the drawer selection [3]
				if (advancedVar.getFilterStack().isEmpty()) {
					continue;
				}

				// Symmetrical query mapping: check nickname, item name, and color name [3]
				String componentName = advancedVar.getName().getString().toLowerCase(Locale.ROOT);
				String itemName = advancedVar.getFilterStack().getHoverName().getString().toLowerCase(Locale.ROOT);
				String colorName = advancedVar.getFilterColor().getSerializedName().toLowerCase(Locale.ROOT);

				if (query.isEmpty() || componentName.contains(query) || itemName.contains(query)
						|| colorName.contains(query)) {
					filtered.add(advancedVar);
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

		// Toggle state if left-clicking on the handle bounds [3]
		if (button == 0 && mouseX >= getX() + width && mouseX < getX() + width + HANDLE_TEX_WIDTH
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

		// Grid bounds check (54x54 viewport) [3]
		int gridX = getX() + 8;
		int gridY = getY() + 20;

		if (button == 0 && mouseX >= gridX && mouseX < gridX + 54 && mouseY >= gridY && mouseY < gridY + 54) {
			List<AdvancedItemFilterVariableComponent> vars = getFilteredVariables();

			for (int i = 0; i < vars.size(); i++) {
				int col = i % 3;
				int row = i / 3;

				// Local variables renamed to prevent JVM compilation shadow conflicts [3]
				int cellX = getCellX(col);
				int cellY = getCellY(row);

				if (mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18) {
					AdvancedItemFilterVariableComponent clickedVar = vars.get(i);
					ItemStack stack = clickedVar.toItemStack();

					// Activate cursor tracking by setting menu's carried stack natively [3]
					parentScreen.getMenu().setCarried(stack);

					// Dispatch synchronization packet immediately to the server [3]
					PacketDistributor.sendToServer(new SyncCarriedItemPacket(stack));
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
			List<AdvancedItemFilterVariableComponent> vars = getFilteredVariables();
			int rows = (vars.size() + 2) / 3;
			int maxScrollY = Math.max(0, rows * 18 - 54); // Correctly bound to 54px [3]

			if (maxScrollY > 0) {
				this.scrollY = Mth.clamp(this.scrollY - (int) scrollY * 10, 0, maxScrollY);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Update slide animation progress frame-rate independently [3]
		long now = Util.getMillis();
		if (lastFrameTime == 0L) {
			lastFrameTime = now;
		}
		float deltaTime = (now - lastFrameTime) / 1000.0F;
		lastFrameTime = now;
		deltaTime = Math.min(deltaTime, 0.1F); // Shield against frame rate lag spikes [3]

		float speed = 8.0F;
		int openX = parentScreen.getLeftPos() + 344;
		int closedX = parentScreen.getLeftPos() + 344 - width;

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

		// Enable scissor mask to clip everything left of the player inventory boundary
		// [3]
		guiGraphics.enableScissor(parentScreen.getLeftPos() + 344, 0, parentScreen.width, parentScreen.height);

		// Render custom background texture stretched horizontally to match width [3]
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		guiGraphics.blit(BACKGROUND_TX, getX(), getY(), width - 3, height, 0.0F, 0.0F, BG_TEX_WIDTH, BG_TEX_HEIGHT,
				BG_TEX_WIDTH, BG_TEX_HEIGHT);
		guiGraphics.blit(BACKGROUND_END_TX, getX() + width - 3, getY(), 4, height, 0.0F, 0.0F, 4, BG_TEX_HEIGHT, 4,
				BG_TEX_HEIGHT);

		// Draw custom handle on the far right edge, shifted up 1px to center vertically
		// [3]
		guiGraphics.blit(HANDLE_TX, getX() + width, getY() - 1, 0, 0, HANDLE_TEX_WIDTH, HANDLE_TEX_HEIGHT,
				HANDLE_TEX_WIDTH, HANDLE_TEX_HEIGHT);

		// Render rotated vertical "Item Vars" label inside the handle bounds [3]
		guiGraphics.pose().pushPose();
		int textWidth = parentScreen.getFont().width("Item Vars");
		int fontHeight = parentScreen.getFont().lineHeight;

		float textX = (getX() + width) + (HANDLE_TEX_WIDTH + fontHeight) / 2.0F;
		float textY = (getY() - 1) + (HANDLE_TEX_HEIGHT - textWidth) / 2.0F;

		guiGraphics.pose().translate(textX, textY, 0.0F);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(90.0F));
		guiGraphics.drawString(parentScreen.getFont(), "Item Vars", 0, 0, 0xFF404040, false);
		guiGraphics.pose().popPose();

		// Render search input edit box [3]
		this.searchEdit.setX(getX() + 4);
		this.searchEdit.setY(getY() + 6);
		this.searchEdit.render(guiGraphics, mouseX, mouseY, partialTick);

		// Sub-scissor region to bound slot items strictly within the 54x54 viewport
		// cell window [3]
		int gridX = getX() + 8;
		int gridY = getY() + 20;
		guiGraphics.enableScissor(gridX, gridY, gridX + 54, gridY + 54);

		List<AdvancedItemFilterVariableComponent> vars = getFilteredVariables();

		for (int i = 0; i < vars.size(); i++) {
			int col = i % 3;
			int row = i / 3;

			int cellX = getCellX(col);
			int cellY = getCellY(row);

			// Optimize out cells floating completely above or below the active mask window
			// [3]
			if (cellY + 18 < gridY || cellY > gridY + 54) {
				continue;
			}

			boolean isHovered = mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18;

			// Correctly blit UV from custom slot texture sheet (V: 18 when hovered, 0
			// otherwise) [3]
			guiGraphics.blit(SLOT_TX, cellX, cellY, 0.0F, isHovered ? 18.0F : 0.0F, 18, 18, 18, 36);

			ItemStack filterCardStack = vars.get(i).toItemStack();

			if (!filterCardStack.isEmpty()) {
				// Reverted & Simplified: Passes rendering execution straight to your BEWLR
				// pipeline [3]
				guiGraphics.renderItem(filterCardStack, cellX + 1, cellY + 1);
				guiGraphics.renderItemDecorations(parentScreen.getFont(), filterCardStack, cellX + 1, cellY + 1);
			}
		}

		// Close out tracking regions in reverse order to prevent stencil buffer
		// corruption [3]
		guiGraphics.disableScissor(); // Close out inner 54x54 grid area [3]
		guiGraphics.disableScissor(); // Close out main screen overlay area [3]

		// Render item tooltips outside of scissor regions to prevent text truncation
		// glitches [3]
		boolean hoveringHandle = mouseX >= getX() + width && mouseX < getX() + width + HANDLE_TEX_WIDTH
				&& mouseY >= getY() - 1 && mouseY < getY() + HANDLE_TEX_HEIGHT - 1;
		if (hoveringHandle) {
			Component tooltipText = open ? Component.literal("Close Drawer") : Component.literal("Open Drawer");
			guiGraphics.renderTooltip(parentScreen.getFont(), tooltipText, mouseX, mouseY);
		} else if (mouseX >= gridX && mouseX < gridX + 54 && mouseY >= gridY && mouseY < gridY + 54) {
			for (int i = 0; i < vars.size(); i++) {
				int col = i % 3;
				int row = i / 3;
				int cellX = getCellX(col);
				int cellY = getCellY(row);

				if (mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18) {
					ItemStack hoverStack = vars.get(i).toItemStack();
					if (!hoverStack.isEmpty()) {
						// Utilizes the standard 1.20+ ItemStack-based tooltip render mapping to avoid
						// screen coordinate overrides [3]
						guiGraphics.renderTooltip(parentScreen.getFont(), hoverStack, mouseX, mouseY);
					}
				}
			}
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		// Restrict mouse interaction boundary so only the visible portion of the
		// drawer/handle intercepts input [3]
		double leftBound = parentScreen.getLeftPos() + 344;
		double rightBound = getX() + width + HANDLE_TEX_WIDTH;
		double topBound = getY() - 1;
		double bottomBound = getY() + height + 1;
		return this.visible && mouseX >= leftBound && mouseX < rightBound && mouseY >= topBound && mouseY < bottomBound;
	}

	@Override
	public void setX(int x) {
		int dif = x - this.getX();
		super.setX(x);
		updateChildrenXPositions(dif);
	}

	@Override
	public void setY(int y) {
		int dif = y - this.getY();
		super.setY(y);
		updateChildrenYPositions(dif);
	}
}