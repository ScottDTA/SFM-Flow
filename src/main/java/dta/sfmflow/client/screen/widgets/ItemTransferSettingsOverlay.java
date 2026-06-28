package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.render.HighlightManager;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom settings overlay mapped specifically to configure Item Input/Output
 * cards [3]. Houses horizontal side-scrolling block-model lists, text searches,
 * and a full-width 3D isometric 3x3x3 sunken block preview scene supporting
 * right-click dragging [3].
 */
@OnlyIn(Dist.CLIENT)
public class ItemTransferSettingsOverlay extends NodeSettingsOverlay {
	private final EditBox searchEdit;
	private final Button toggleWhitelistBtn;
	private final Checkbox highlightCheckbox;

	private float scrollX = 0.0F;

	// Orbit rotation variables defaulted to standard isometric angles [3]
	private float yawRotation = 45.0F;
	private float pitchRotation = 30.0F;

	/**
	 * Instantiates the overlay panels and aligns settings layout [3].
	 *
	 * @param parentScreen active manager screen panel [3]
	 * @param component    logical transfer component data model [3]
	 */
	public ItemTransferSettingsOverlay(ManagerScreen parentScreen, ItemTransferComponent component) {
		super(parentScreen, component);

		// Expanded bounds to accommodate the screen-wide 3D preview and 1x12 slot grid
		// [3]
		this.width = 300;
		this.height = 360;
		this.setX((parentScreen.width - 300) / 2);

		// Centered dynamically in the available space above the player inventory panel
		// [3]
		this.setY(25);

		component.setUseAll(false);
		// Force default slot configuration to Any (-1) [3]
		component.setTargetSlot(-1);

		// Centered Search Box setup [3]
		this.searchEdit = new EditBox(parentScreen.getFont(), getX() + 20, getY() + 40, 260, 14,
				Component.literal("Search"));
		this.searchEdit.setHint(Component.literal("Search inventories..."));
		this.searchEdit.setCanLoseFocus(true);
		this.children.add(new ApiWidgetAdapter<>(this.searchEdit));

		// Whitelist/Blacklist toggle button repositioned to the bottom config bar line
		// [3]
		this.toggleWhitelistBtn = Button
				.builder(Component.literal(component.isWhitelist() ? "Whitelist" : "Blacklist"), btn -> {
					component.setWhitelist(!component.isWhitelist());
					btn.setMessage(Component.literal(component.isWhitelist() ? "Whitelist" : "Blacklist"));
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
				}).pos(getX() + 160, getY() + 294).size(120, 14).build();
		this.children.add(new ApiWidgetAdapter<>(this.toggleWhitelistBtn));

		// Checkbox position inside the 250x210 scene box [3]
		int boxX = getX() + 25;
		int boxY = getY() + 78;

		ConnectionBlock initialInv = getSelectedInventory();
		boolean isInitiallyHighlighted = initialInv != null && HighlightManager.isHighlighted(initialInv.getBlockPos());

		// Uses Component.empty() so the checkbox doesn't draw default unscalable text
		// labels [3]
		this.highlightCheckbox = Checkbox.builder(Component.empty(), parentScreen.getFont()).pos(boxX + 4, boxY + 4)
				.selected(isInitiallyHighlighted).onValueChange((checkbox, selected) -> {
					ConnectionBlock currentInv = getSelectedInventory();
					if (currentInv != null) {
						if (selected) {
							HighlightManager.addHighlight(currentInv.getBlockPos());
						} else {
							HighlightManager.removeHighlight(currentInv.getBlockPos());
						}
					}
				}).build();
		this.children.add(new ApiWidgetAdapter<>(this.highlightCheckbox));
	}

	private ConnectionBlock getSelectedInventory() {
		int selectedId = ((ItemTransferComponent) component).getInventoryId();
		if (selectedId != -1) {
			for (ConnectionBlock block : parentScreen.getMenu().getManagerBlockEntity().getInventories()) {
				if (block.getId() == selectedId) {
					return block;
				}
			}
		}
		return null;
	}

	private void setCheckboxSelected(Checkbox checkbox, boolean selected) {
		try {
			java.lang.reflect.Field field = Checkbox.class.getDeclaredField("selected");
			field.setAccessible(true);
			field.setBoolean(checkbox, selected);
		} catch (Exception e) {
			try {
				for (java.lang.reflect.Field field : Checkbox.class.getDeclaredFields()) {
					if (field.getType() == boolean.class) {
						field.setAccessible(true);
						field.setBoolean(checkbox, selected);
						break;
					}
				}
			} catch (Exception ex) {
				dta.sfmflow.SFMFlow.LOGGER.error("Failed to set checkbox state", ex);
			}
		}
	}

	private List<ConnectionBlock> getFilteredInventories(Level level) {
		List<ConnectionBlock> list = parentScreen.getMenu().getManagerBlockEntity().getInventories();
		String query = searchEdit.getValue().toLowerCase(java.util.Locale.ROOT);
		if (query.isEmpty()) {
			return list;
		}
		List<ConnectionBlock> filtered = new ArrayList<>();
		for (ConnectionBlock inv : list) {
			String name = inv.getDisplayName(level).getString().toLowerCase(java.util.Locale.ROOT);
			if (name.contains(query)) {
				filtered.add(inv);
			}
		}
		return filtered;
	}

	/**
	 * Formulates dynamic coordinate projections matching the matrix rotations [3].
	 */
	private Vec2 getFaceScreenCoords(Direction face, float yaw, float pitch, float scale, int centerX, int centerY) {
		float x = face.getStepX() * 0.5F;
		float y = face.getStepY() * 0.5F;
		float z = face.getStepZ() * 0.5F;

		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);

		// Rotate Y
		double x1 = x * Math.cos(yawRad) + z * Math.sin(yawRad);
		double y1 = y;
		double z1 = -x * Math.sin(yawRad) + z * Math.cos(yawRad);

		// Rotate X
		double x2 = x1;
		double y2 = y1 * Math.cos(pitchRad) - z1 * Math.sin(pitchRad);
		double z2 = y1 * Math.sin(pitchRad) + z1 * Math.cos(pitchRad);

		float screenX = centerX + (float) (x2 * scale);
		float screenY = centerY - (float) (y2 * scale); // Inverted Y-axis coordinate

		return new Vec2(screenX, screenY, (float) z2);
	}

	private boolean sideSupportsItems(Level level, BlockPos pos, Direction side) {
		if (level == null || pos == null) {
			return false;
		}
		return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos,
				side) != null;
	}

	private List<Direction> getVisibleFaces() {
		int centerX = getX() + 25 + 125;
		int centerY = getY() + 78 + 105;

		List<FaceProj> faceList = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			Vec2 proj = getFaceScreenCoords(dir, yawRotation, pitchRotation, 40.0F, centerX, centerY);
			faceList.add(new FaceProj(dir, proj));
		}

		// Sort by Z descending to place the closest front faces first [3]
		faceList.sort((f1, f2) -> Float.compare(f2.proj.z, f1.proj.z));

		List<Direction> visibleFaces = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			visibleFaces.add(faceList.get(i).face);
		}
		return visibleFaces;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int listMinX = getX() + 20;
		int listMaxX = getX() + 280;
		int listMinY = getY() + 58;
		int listMaxY = getY() + 76;

		int boxX = getX() + 25;
		int boxY = getY() + 78;
		int boxW = 250;
		int boxH = 210;

		// 1. Intercept right click in the 3D viewport to focus and consume click [3]
		if (button == 1 && mouseX >= boxX && mouseX < boxX + boxW && mouseY >= boxY && mouseY < boxY + boxH) {
			return true;
		}

		if (button == 0) {
			// 2. Intercept left click inside the 3D viewport for side-toggling logic [3]
			ConnectionBlock selectedInv = getSelectedInventory();
			Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
			if (selectedInv != null && !selectedInv.isSleeping() && mouseX >= boxX && mouseX < boxX + boxW
					&& mouseY >= boxY && mouseY < boxY + boxH) {
				int centerX = boxX + boxW / 2;
				int centerY = boxY + boxH / 2;
				List<Direction> visibleFaces = getVisibleFaces();

				for (Direction face : visibleFaces) {
					Vec2 proj = getFaceScreenCoords(face, yawRotation, pitchRotation, 40.0F, centerX, centerY);
					double dx = mouseX - proj.x;
					double dy = mouseY - proj.y;
					if (dx * dx + dy * dy <= 36.0) { // 6px hit detection radius [3]
						if (sideSupportsItems(level, selectedInv.getBlockPos(), face)) {
							ItemTransferComponent transfer = (ItemTransferComponent) component;
							transfer.toggleSide(face);
							parentScreen.getMenu().getManagerBlockEntity().setChanged();
							sendSettingsUpdate(); // Transmit changes immediately [3]

							Minecraft.getInstance().getSoundManager()
									.play(net.minecraft.client.resources.sounds.SimpleSoundInstance
											.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
							return true;
						}
					}
				}
			}

			// 3. Intercept clicks on scrolling inventory icons (centered 260px width) [3]
			if (mouseX >= listMinX && mouseX < listMaxX && mouseY >= listMinY && mouseY < listMaxY) {
				List<ConnectionBlock> filtered = getFilteredInventories(level);
				for (int i = 0; i < filtered.size(); i++) {
					int cardX = getX() + 20 + i * 20 - (int) scrollX;
					int cardY = getY() + 58;
					if (mouseX >= cardX && mouseX < cardX + 18 && mouseY >= cardY && mouseY < cardY + 18) {
						ItemTransferComponent transfer = (ItemTransferComponent) component;
						transfer.setInventoryId(filtered.get(i).getId());
						parentScreen.getMenu().getManagerBlockEntity().setChanged();

						// Dynamically update highlight checkbox visual state upon card swaps [3]
						ConnectionBlock newInv = filtered.get(i);
						setCheckboxSelected(this.highlightCheckbox,
								HighlightManager.isHighlighted(newInv.getBlockPos()));

						return true;
					}
				}
			}

			// 4. Intercept clicks on 12-wide, 1-high Ghost Slot grid [3]
			int gridMinX = getX() + 30;
			int gridMaxX = getX() + 30 + 12 * 20;
			int gridMinY = getY() + 314;
			int gridMaxY = getY() + 314 + 20;

			if (mouseX >= gridMinX && mouseX < gridMaxX && mouseY >= gridMinY && mouseY < gridMaxY) {
				int col = (int) ((mouseX - gridMinX) / 20);
				if (col >= 0 && col < 12) {
					int slotIdx = col;
					ItemStack carried = parentScreen.getMenu().getCarried();
					if (carried != null && !carried.isEmpty()) {
						ItemStack copy = carried.copy();
						copy.setCount(1);
						((ItemTransferComponent) component).getFilterItems().set(slotIdx, copy);
					} else {
						((ItemTransferComponent) component).getFilterItems().set(slotIdx, ItemStack.EMPTY);
					}
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		int boxX = getX() + 25;
		int boxY = getY() + 78;
		int boxW = 250;
		int boxH = 210;

		// 5. Update the camera orbit angles when dragging right-click inside viewport
		// [3]
		if (button == 1 && mouseX >= boxX && mouseX < boxX + boxW && mouseY >= boxY && mouseY < boxY + boxH) {
			this.yawRotation += (float) dragX * 0.8F;
			this.pitchRotation = net.minecraft.util.Mth.clamp(this.pitchRotation + (float) dragY * 0.8F, -90.0F, 90.0F);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int listMinX = getX() + 20;
		int listMaxX = getX() + 280;
		int listMinY = getY() + 58;
		int listMaxY = getY() + 76;

		if (mouseX >= listMinX && mouseX < listMaxX && mouseY >= listMinY && mouseY < listMaxY) {
			var level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
			int maxScrollX = Math.max(0, getFilteredInventories(level).size() * 20 - 260);
			if (maxScrollX > 0) {
				this.scrollX = net.minecraft.util.Mth.clamp(this.scrollX - (float) scrollY * 10.0F, 0.0F,
						(float) maxScrollX);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderComponent(guiGraphics, mouseX, mouseY, partialTick);

		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Search Inventories:"), getX() + 20,
				getY() + 28, 0xFF404040, false);

		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();

		// Resolve selected inventory reference for 3D rendering [3]
		int selectedId = ((ItemTransferComponent) component).getInventoryId();
		ConnectionBlock selectedInv = null;
		if (selectedId != -1) {
			for (ConnectionBlock block : parentScreen.getMenu().getManagerBlockEntity().getInventories()) {
				if (block.getId() == selectedId) {
					selectedInv = block;
					break;
				}
			}
		}

		List<ConnectionBlock> filtered = getFilteredInventories(level);

		int listX = getX() + 20;
		int listY = getY() + 58;

		// Expanded scissoring clipping area (260px wide) [3]
		guiGraphics.enableScissor(listX, listY, listX + 260, listY + 18);

		for (int i = 0; i < filtered.size(); i++) {
			var inv = filtered.get(i);
			int cardX = listX + i * 20 - (int) scrollX;
			int cardY = listY;

			boolean isSelected = ((ItemTransferComponent) component).getInventoryId() == inv.getId()
					&& !((ItemTransferComponent) component).isUseAll();
			boolean hovered = mouseX >= cardX && mouseX < cardX + 18 && mouseY >= cardY && mouseY < cardY + 18;

			int border = isSelected ? 0xFF39FF14 : (hovered ? 0xFF8B8B8B : 0xFF434343);
			guiGraphics.renderOutline(cardX, cardY, 18, 18, border);

			var state = level.getBlockState(inv.getBlockPos());
			ItemStack blockStack = new ItemStack(state.getBlock().asItem());
			if (!blockStack.isEmpty()) {
				guiGraphics.renderItem(blockStack, cardX + 1, cardY + 1);
			}
		}

		guiGraphics.disableScissor();

		// Render Expanded Horizontal Scrollbar (260px width) [3]
		int maxScrollX = Math.max(0, filtered.size() * 20 - 260);
		if (maxScrollX > 0) {
			int scrollbarX = listX;
			int scrollbarY = listY + 20;

			guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 260, scrollbarY + 2, 0x40000000);

			int thumbWidth = (int) ((260.0F / (filtered.size() * 20.0F)) * 260.0F);
			thumbWidth = Math.max(15, Math.min(260, thumbWidth));
			int thumbX = scrollbarX + (int) ((scrollX / (float) maxScrollX) * (260 - thumbWidth));

			guiGraphics.fill(thumbX, scrollbarY, thumbX + thumbWidth, scrollbarY + 2, 0xFF8B8B8B);
		}

		// Center coordinates for the expanded 250x210 sunken viewport container [3]
		int boxX = getX() + 25;
		int boxY = getY() + 78;
		int boxW = 250;
		int boxH = 210;

		// Sunken backdrop fill
		guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF151515);

		// RECESS BEVEL: Top and Left are #555555, Bottom and Right are FFFFFF [3]
		guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFF555555); // Top Outer
		guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxH, 0xFF555555); // Left Outer (Fixed Variable Name) [3]
		guiGraphics.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0xFFFFFFFF); // Bottom Outer
		guiGraphics.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, 0xFFFFFFFF); // Right Outer

		if (selectedInv != null && !selectedInv.isSleeping()) {
			render3DScene(guiGraphics, level, selectedInv.getBlockPos());
		} else {
			guiGraphics.drawCenteredString(parentScreen.getFont(), "NO", boxX + 125, boxY + 95, 0xFF8B8B8B);
			guiGraphics.drawCenteredString(parentScreen.getFont(), "PREVIEW", boxX + 125, boxY + 107, 0xFF8B8B8B);
		}

		// Draw checkbox text label at 75% scale to fit the box corner nicely [3]
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(boxX + 22, boxY + 9, 0);
		guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("In-World Highlight"), 0, 0, 0xFFAAAAAA,
				false);
		guiGraphics.pose().popPose();

		// Draw lower labeling config bar
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Item Filter:"), getX() + 20, getY() + 297,
				0xFF404040, false);

		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}

		// Centered 1x12 Ghost Slot Grid Rendering [3]
		ItemTransferComponent transfer = (ItemTransferComponent) component;
		int gridStartX = getX() + 30;
		int gridStartY = getY() + 314;

		for (int c = 0; c < 12; c++) {
			int slotX = gridStartX + c * 20;
			int slotY = gridStartY;
			boolean hovered = mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18;

			guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, hovered ? 0xFF353535 : 0xFF151515);
			guiGraphics.renderOutline(slotX, slotY, 18, 18, hovered ? 0xFF8B8B8B : 0xFF434343);

			int slotIdx = c;
			ItemStack stack = transfer.getFilterItems().get(slotIdx);
			if (stack != null && !stack.isEmpty()) {
				guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
			}
		}

		// Tooltip rendering pass for scrolling inventories list
		if (mouseX >= listX && mouseX < listX + 260 && mouseY >= listY && mouseY < listY + 18) {
			for (int i = 0; i < filtered.size(); i++) {
				var inv = filtered.get(i);
				int cardX = listX + i * 20 - (int) scrollX;
				if (mouseX >= cardX && mouseX < cardX + 18) {
					guiGraphics.renderTooltip(parentScreen.getFont(), inv.getDisplayName(level), mouseX, mouseY);
				}
			}
		}

		// Tooltip rendering pass for 1x12 ghost slot grid items [3]
		if (mouseX >= gridStartX && mouseX < gridStartX + 12 * 20 && mouseY >= gridStartY && mouseY < gridStartY + 20) {
			int col = (int) ((mouseX - gridStartX) / 20);
			if (col >= 0 && col < 12) {
				ItemStack stack = transfer.getFilterItems().get(col);
				if (stack != null && !stack.isEmpty()) {
					guiGraphics.renderTooltip(parentScreen.getFont(), stack, mouseX, mouseY);
				}
			}
		}
	}

	/**
	 * Encodes component modifications onto NBT and sends a save packet to the
	 * server instantly [3].
	 */
	private void sendSettingsUpdate() {
		net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
		component.saveData(nbt);
		net.neoforged.neoforge.network.PacketDistributor
				.sendToServer(new dta.sfmflow.networking.packets.serverbound.SaveComponentSettings(
						parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
	}

	/**
	 * Draws an untextured 3D quad using the active RenderType's vertex format [3].
	 *
	 * @param builder the active vertex compiler buffer [3]
	 * @param matrix  the transformation model matrix [3]
	 * @param r       the red color channel (0-255) [3]
	 * @param g       the green color channel (0-255) [3]
	 * @param b       the blue color channel (0-255) [3]
	 */
	private void draw3DQuad(VertexConsumer builder, org.joml.Matrix4f matrix, float x1, float y1, float z1, float x2,
			float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int b) {
		// Feed exactly 4 vertices per quad since RenderType.debugQuads() expects
		// quad-based topology [3]
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 255);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 255);
		builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, 255);
		builder.addVertex(matrix, x4, y4, z4).setColor(r, g, b, 255);
	}

	private void draw3DLine(VertexConsumer builder, org.joml.Matrix4f matrix, Direction face, float x1, float y1,
			float z1, float x2, float y2, float z2, int r, int g, int b) {
		float nx = face.getStepX();
		float ny = face.getStepY();
		float nz = face.getStepZ();
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, 255).setNormal(nx, ny, nz);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 255).setNormal(nx, ny, nz);
	}

	/**
	 * Renders solid 3D color quads or diagonal crosses representing face toggles
	 * directly mapped on block faces [3].
	 */
	private void draw3DMarker(PoseStack poseStack, MultiBufferSource bufferSource, Direction face, boolean active,
			boolean supported) {
		org.joml.Matrix4f matrix = poseStack.last().pose();
		int r, g, b;
		if (supported) {
			if (active) {
				r = 57;
				g = 255;
				b = 20; // Neon Green (#39FF14) [3]
			} else {
				r = 255;
				g = 0;
				b = 0; // Red [3]
			}
		} else {
			r = 255;
			g = 0;
			b = 0; // Red X [3]
		}

		if (supported) {
			// Centered 8x8 squares mapped precisely to corrected face positions (-0.5 to
			// 0.5) and offset [3]
			VertexConsumer builder = bufferSource.getBuffer(RenderType.debugQuads());
			switch (face) {
			case UP -> draw3DQuad(builder, matrix, -0.25F, 0.505F, -0.25F, -0.25F, 0.505F, 0.25F, 0.25F, 0.505F, 0.25F,
					0.25F, 0.505F, -0.25F, r, g, b);
			case DOWN -> draw3DQuad(builder, matrix, -0.25F, -0.505F, -0.25F, 0.25F, -0.505F, -0.25F, 0.25F, -0.505F,
					0.25F, -0.25F, -0.505F, 0.25F, r, g, b);
			case NORTH -> draw3DQuad(builder, matrix, -0.25F, -0.25F, -0.505F, 0.25F, -0.25F, -0.505F, 0.25F, 0.25F,
					-0.505F, -0.25F, 0.25F, -0.505F, r, g, b);
			case SOUTH -> draw3DQuad(builder, matrix, -0.25F, -0.25F, 0.505F, -0.25F, 0.25F, 0.505F, 0.25F, 0.25F,
					0.505F, 0.25F, -0.25F, 0.505F, r, g, b);
			case WEST -> draw3DQuad(builder, matrix, -0.505F, -0.25F, -0.25F, -0.505F, -0.25F, 0.25F, -0.505F, 0.25F,
					0.25F, -0.505F, 0.25F, -0.25F, r, g, b);
			case EAST -> draw3DQuad(builder, matrix, 0.505F, -0.25F, -0.25F, 0.505F, 0.25F, -0.25F, 0.505F, 0.25F,
					0.25F, 0.505F, -0.25F, 0.25F, r, g, b);
			}
		} else {
			// Diagonal crossed lines mapped precisely to corrected face positions (-0.5 to
			// 0.5) [3]
			VertexConsumer builder = bufferSource.getBuffer(RenderType.lines());
			float nx = face.getStepX();
			float ny = face.getStepY();
			float nz = face.getStepZ();
			switch (face) {
			case UP -> {
				draw3DLine(builder, matrix, face, -0.25F, 0.505F, -0.25F, 0.25F, 0.505F, 0.25F, r, g, b);
				draw3DLine(builder, matrix, face, -0.25F, 0.505F, 0.25F, 0.25F, 0.505F, -0.25F, r, g, b);
			}
			case DOWN -> {
				draw3DLine(builder, matrix, face, -0.25F, -0.505F, -0.25F, 0.25F, -0.505F, 0.25F, r, g, b);
				draw3DLine(builder, matrix, face, -0.25F, -0.505F, 0.25F, 0.25F, -0.505F, -0.25F, r, g, b);
			}
			case NORTH -> {
				draw3DLine(builder, matrix, face, -0.25F, -0.25F, -0.505F, 0.25F, 0.25F, -0.505F, r, g, b);
				draw3DLine(builder, matrix, face, -0.25F, 0.25F, -0.505F, 0.25F, -0.25F, -0.505F, r, g, b);
			}
			case SOUTH -> {
				draw3DLine(builder, matrix, face, -0.25F, -0.25F, 0.505F, 0.25F, 0.25F, 0.505F, r, g, b);
				draw3DLine(builder, matrix, face, -0.25F, 0.25F, 0.505F, 0.25F, -0.25F, 0.505F, r, g, b);
			}
			case WEST -> {
				draw3DLine(builder, matrix, face, -0.505F, -0.25F, -0.25F, -0.505F, 0.25F, 0.25F, r, g, b);
				draw3DLine(builder, matrix, face, -0.505F, -0.25F, 0.25F, -0.505F, 0.25F, -0.25F, r, g, b);
			}
			case EAST -> {
				draw3DLine(builder, matrix, face, 0.505F, -0.25F, -0.25F, 0.505F, 0.25F, 0.25F, r, g, b);
				draw3DLine(builder, matrix, face, 0.505F, -0.25F, 0.25F, 0.505F, 0.25F, -0.25F, r, g, b);
			}
			}
		}
	}

	/**
	 * Renders a 3D isometric mini-scene of the selected inventory centered in a
	 * 3x3x3 grid [3].
	 *
	 * @param guiGraphics the drawing graphics interface [3]
	 * @param level       the client world [3]
	 * @param centerPos   the coordinates of the centered selected block [3]
	 */
	private void render3DScene(GuiGraphics guiGraphics, Level level, BlockPos centerPos) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();

		// Center isometric projection offsets in the middle of our 250x210 box [3]
		int centerX = getX() + 25 + 125;
		int centerY = getY() + 78 + 105;
		poseStack.translate(centerX, centerY, 150.0F);

		// Render block states scaled up to fill the 250x210 space - synced to 40.0F [3]
		float scale = 40.0F;
		poseStack.scale(scale, -scale, scale);

		// Orbit transformations with right-click manual drag values [3]
		poseStack.mulPose(Axis.XP.rotationDegrees(pitchRotation));
		poseStack.mulPose(Axis.YP.rotationDegrees(yawRotation));

		// Center block models individually around center position offsets [3]
		poseStack.translate(-0.5F, -0.5F, -0.5F);

		BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

		// Sided block rendering requires standard 3D item illumination [3]
		com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
		com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

		// Pass 1: Render only the opaque center block first with depth writing active
		// [3]
		BlockState centerState = level.getBlockState(centerPos);
		if (!centerState.isAir()) {
			poseStack.pushPose();
			blockRenderer.renderSingleBlock(centerState, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
			poseStack.popPose(); // Pop immediately to prevent internal model renderer matrix shifts from leaking
									// [3]

			poseStack.pushPose(); // Push a clean, isolated matrix for drawing markers precisely on the block
			poseStack.translate(0.5F, 0.5F, 0.5F); // Revert the block-centering translation to align markers with the centered model [3]
			// faces [3]
			// Draw the 3D face state markers right on the center block model [3]
			List<Direction> visibleFaces = getVisibleFaces();
			ItemTransferComponent transfer = (ItemTransferComponent) component;
			for (Direction face : visibleFaces) {
				boolean active = transfer.isSideActive(face);
				boolean supported = sideSupportsItems(level, centerPos, face);
				draw3DMarker(poseStack, bufferSource, face, active, supported);
			}
			poseStack.popPose();
		}
		// Flush the center block so its depth values are registered in the buffer [3]
		bufferSource.endBatch();

		
		
		// Pass 2: Disable depth writing and render the translucent neighboring ghost
		// blocks [3]
		com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
		GhostBufferSource ghostSource = new GhostBufferSource(bufferSource, 0.3F);

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue; // Skip the center block since it was drawn in Pass 1 [3]
					}

					BlockPos currentPos = centerPos.offset(dx, dy, dz);
					BlockState state = level.getBlockState(currentPos);
					if (state.isAir()) {
						continue;
					}

					poseStack.pushPose();
					poseStack.translate(dx, dy, dz);
					blockRenderer.renderSingleBlock(state, poseStack, ghostSource, 15728880, OverlayTexture.NO_OVERLAY);
					poseStack.popPose();
				}
			}
		}

		// Flush the translucent neighboring ghost blocks [3]
		bufferSource.endBatch();

        

		// Restore default depth writing state and flat item illumination [3]
		com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
		com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
		com.mojang.blaze3d.platform.Lighting.setupForFlatItems();

		poseStack.popPose();
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

	private static class Vec2 {
		public final float x;
		public final float y;
		public final float z;

		public Vec2(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static class FaceProj {
		public final Direction face;
		public final Vec2 proj;

		public FaceProj(Direction face, Vec2 proj) {
			this.face = face;
			this.proj = proj;
		}
	}

	/**
	 * Wrapper buffer source that routes standard block render types to translucent
	 * ones.
	 */
	@OnlyIn(Dist.CLIENT)
	private static class GhostBufferSource implements net.minecraft.client.renderer.MultiBufferSource {
		private final net.minecraft.client.renderer.MultiBufferSource delegate;
		private final float alpha;

		public GhostBufferSource(net.minecraft.client.renderer.MultiBufferSource delegate, float alpha) {
			this.delegate = delegate;
			this.alpha = alpha;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(net.minecraft.client.renderer.RenderType renderType) {
			// Force standard block translucent render type to enable alpha transparency
			// blending [3]
			return new GhostVertexConsumer(delegate.getBuffer(net.minecraft.client.renderer.RenderType.translucent()),
					alpha);
		}
	}

	/**
	 * Intercepts vertex and quad compilation calls and injects a custom alpha
	 * value.
	 */
	@OnlyIn(Dist.CLIENT)
	private static class GhostVertexConsumer implements com.mojang.blaze3d.vertex.VertexConsumer {
		private final com.mojang.blaze3d.vertex.VertexConsumer delegate;
		private final float ghostAlpha;

		public GhostVertexConsumer(com.mojang.blaze3d.vertex.VertexConsumer delegate, float ghostAlpha) {
			this.delegate = delegate;
			this.ghostAlpha = ghostAlpha;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) {
			delegate.setColor(r, g, b, (int) (a * ghostAlpha));
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(int color) {
			// Intercept and rewrite the packed color's alpha component [3]
			int a = (color >> 24) & 0xFF;
			int r = (color >> 16) & 0xFF;
			int g = (color >> 8) & 0xFF;
			int b = color & 0xFF;
			int newA = (int) (a * ghostAlpha);
			int newColor = (newA << 24) | (r << 16) | (g << 8) | b;
			delegate.setColor(newColor);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(float r, float g, float b, float a) {
			delegate.setColor(r, g, b, a * ghostAlpha);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setLight(int light) {
			delegate.setLight(light);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setOverlay(int overlay) {
			delegate.setOverlay(overlay);
			return this;
		}
	}
}