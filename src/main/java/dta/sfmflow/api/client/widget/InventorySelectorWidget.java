package dta.sfmflow.api.client.widget;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.block.entity.FilterGhostSlot;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.helper.FlowLayoutHelper;
import dta.sfmflow.networking.packets.serverbound.SyncCarriedItemPacket;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reusable side-scrolling UI selector list allowing users to find, search, and
 * select target blocks matching a specific capability registry key. Upgraded to
 * support real-time filtering via custom inventory list variable cards.
 */
@OnlyIn(Dist.CLIENT)
public class InventorySelectorWidget extends AbstractFlowWidget {
	private static final ResourceLocation FILTER_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/filter_slot.png");

	private final IInventoryTarget model;
	private ResourceLocation capabilityType;
	private final ManagerScreen parentScreen;
	private final EditBox searchEdit;
	private final Consumer<ConnectionBlock> onSelected;
	private final Predicate<ConnectionBlock> filter;

	private float scrollX = 0.0F;

	// Real-time filtering variables
	private ItemStack boundListCard = ItemStack.EMPTY;
	private final List<Integer> allowedInventoryIds = new ArrayList<>();

	public InventorySelectorWidget(int x, int y, IInventoryTarget model, ResourceLocation capabilityType,
			ManagerScreen parentScreen, Consumer<ConnectionBlock> onSelected) {
		this(x, y, model, capabilityType, parentScreen, block -> true, onSelected);
	}

	public InventorySelectorWidget(int x, int y, IInventoryTarget model, ResourceLocation capabilityType,
			ManagerScreen parentScreen, Predicate<ConnectionBlock> filter, Consumer<ConnectionBlock> onSelected) {
		super(x, y, 260, 52, Component.literal("Inventory Selector"));
		this.model = model;
		this.capabilityType = capabilityType;
		this.parentScreen = parentScreen;
		this.onSelected = onSelected;
		this.filter = filter;

		// Shortened local search box to leave space on the right for the filter slot
		this.searchEdit = new EditBox(parentScreen.getFont(), getX(), getY() + 12, 238, 14,
				Component.literal("Search"));
		this.searchEdit.setHint(Component.literal("Search..."));
		this.searchEdit.setCanLoseFocus(true);
		this.children.add(new ApiWidgetAdapter<>(this.searchEdit));
	}

	private List<ConnectionBlock> getFilteredInventories(Level level) {
		List<ConnectionBlock> list = parentScreen.getMenu().getManagerBlockEntity().getInventories();
		String query = searchEdit.getValue().toLowerCase(Locale.ROOT);

		List<ConnectionBlock> filtered = new ArrayList<>();
		for (ConnectionBlock inv : list) {
			// Pre-filter by target capability type, dynamic block filters, and active list
			// variable card selections
			if (inv.getTypes().contains(capabilityType) && (this.filter == null || this.filter.test(inv))) {

				// Apply active list variable filter if a card is bound
				if (!this.boundListCard.isEmpty() && !this.allowedInventoryIds.contains(inv.getId())) {
					continue;
				}

				String name = inv.getDisplayName(level).getString().toLowerCase(Locale.ROOT);
				if (query.isEmpty() || name.contains(query)) {
					filtered.add(inv);
				}
			}
		}
		return filtered;
	}

	private void bindListCard(ItemStack stack) {
		this.boundListCard = stack.copy();
		this.allowedInventoryIds.clear();
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTag();
			if (tag.contains("entries")) {
				ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
				for (int i = 0; i < list.size(); i++) {
					CompoundTag entryTag = list.getCompound(i);
					this.allowedInventoryIds.add(entryTag.getInt("inventoryId"));
				}
			}
		}
		this.resetScroll();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		// Check variable list filter ghost slot clicks first
		int slotX = getX() + 242;
		int slotY = getY() + 10;
		if (button == 0 && mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
			ItemStack carried = parentScreen.getMenu().getCarried();

			if (!carried.isEmpty() && FilterGhostSlot.isInventoryListCard(carried)) {
				// Bind the inventories list variable card to filter the selector list
				bindListCard(carried);

				// Clear the carried cursor stack securely [3]
				parentScreen.getMenu().setCarried(ItemStack.EMPTY);
				PacketDistributor.sendToServer(new SyncCarriedItemPacket(ItemStack.EMPTY));

				Minecraft.getInstance().getSoundManager()
						.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			} else if (carried.isEmpty() && !this.boundListCard.isEmpty()) {
				// Just clear the slot and un-filter the list
				this.boundListCard = ItemStack.EMPTY;
				this.allowedInventoryIds.clear();
				this.resetScroll();

				Minecraft.getInstance().getSoundManager()
						.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
		}

		for (GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused(child); // Set focused child on click so key events propagate
				return true;
			}
		}

		// Check selection click on container block icons
		int listX = getX();
		int listY = getY() + 30;

		if (button == 0 && mouseX >= listX && mouseX < listX + 260 && mouseY >= listY && mouseY < listY + 18) {
			Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
			List<ConnectionBlock> filtered = getFilteredInventories(level);

			for (int i = 0; i < filtered.size(); i++) {
				int cardX = listX + i * 20 - (int) scrollX;
				if (mouseX >= cardX && mouseX < cardX + 18) {
					ConnectionBlock clickedBlock = filtered.get(i);
					model.setInventoryId(clickedBlock.getId());
					this.onSelected.accept(clickedBlock);
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int listX = getX();
		int listY = getY() + 30;

		if (mouseX >= listX && mouseX < listX + 260 && mouseY >= listY && mouseY < listY + 18) {
			Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
			int maxScrollX = Math.max(0, getFilteredInventories(level).size() * 20 - 260);
			if (maxScrollX > 0) {
				this.scrollX = Mth.clamp(this.scrollX - (float) scrollY * 10.0F, 0.0F, (float) maxScrollX);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Render section header
		guiGraphics.drawString(parentScreen.getFont(), Component.literal("Search Inventories:"), getX(), getY(),
				0xFF404040, false);

		// Render the search box child
		for (GuiEventListener child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				widget.visible = this.visible;
				widget.active = this.active;
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}

		// Render the custom variable list filter slot on the right
		int slotX = getX() + 242;
		int slotY = getY() + 10;
		boolean slotHovered = mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18;

		int vOffset = this.boundListCard.isEmpty() ? 0 : 18;
		guiGraphics.blit(FILTER_SLOT_TEXTURE, slotX, slotY, 0, vOffset, 18, 18, 18, 36);

		if (slotHovered) {
			guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF8B8B8B);
		}

		if (!this.boundListCard.isEmpty()) {
			guiGraphics.renderItem(this.boundListCard, slotX + 1, slotY + 1);
			guiGraphics.renderItemDecorations(parentScreen.getFont(), this.boundListCard, slotX + 1, slotY + 1);
		}

		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		List<ConnectionBlock> filtered = getFilteredInventories(level);

		int listX = getX();
		int listY = getY() + 30;

		// Apply hardware scissors mask around selection row
		guiGraphics.enableScissor(listX, listY, listX + 260, listY + 18);

		// Flush deferred item renders inside the scissor mask bounds
		for (int i = 0; i < filtered.size(); i++) {
			ConnectionBlock inv = filtered.get(i);
			int cardX = listX + i * 20 - (int) scrollX;

			boolean isSelected = model.getInventoryId() == inv.getId();
			boolean hovered = mouseX >= cardX && mouseX < cardX + 18 && mouseY >= listY && mouseY < listY + 18;

			// Query the model dynamically to check if this inventory is bound or contained
			boolean inList = model.isInventoryBound(inv.getId());

			// Render a semi-translucent green background to indicate inclusion in the list
			if (inList) {
				guiGraphics.fill(cardX + 1, listY + 1, cardX + 17, listY + 17, 0x4039FF14);
			}

			// Soft green border for list members, vibrant green for the active target, dark
			// charcoal otherwise
			int border = isSelected ? 0xFF39FF14 : (hovered ? 0xFF8B8B8B : (inList ? 0xAA39FF14 : 0xFF434343));
			guiGraphics.renderOutline(cardX, listY, 18, 18, border);

			// Render the Card's icon instead of the Cluster block
			ItemStack blockStack;
			if (inv.getSlotIndex() >= 0 && !inv.getCardStack().isEmpty()) {
				blockStack = inv.getCardStack();
			} else {
				BlockState state = level.getBlockState(inv.getBlockPos());
				blockStack = new ItemStack(state.getBlock().asItem());
			}

			if (!blockStack.isEmpty()) {
				guiGraphics.renderItem(blockStack, cardX + 1, listY + 1);
			}
		}

		// Flush deferred item renders inside the scissor mask bounds
		guiGraphics.flush();

		guiGraphics.disableScissor();

		// Render horizontal scrollbar if elements exceed viewport boundaries
		int maxScrollX = Math.max(0, filtered.size() * 20 - 260);
		if (maxScrollX > 0) {
			int scrollbarY = listY + 20;
			guiGraphics.fill(listX, scrollbarY, listX + 260, scrollbarY + 2, 0x40000000);

			int thumbWidth = (int) ((260.0F / (filtered.size() * 20.0F)) * 260.0F);
			thumbWidth = Math.max(15, Math.min(260, thumbWidth));
			int thumbX = listX + (int) ((scrollX / (float) maxScrollX) * (260 - thumbWidth));

			guiGraphics.fill(thumbX, scrollbarY, thumbX + thumbWidth, scrollbarY + 2, 0xFF8B8B8B);
		}

		// Tooltip rendering pass for block icons
		if (mouseX >= listX && mouseX < listX + 260 && mouseY >= listY && mouseY < listY + 18) {
			for (int i = 0; i < filtered.size(); i++) {
				ConnectionBlock inv = filtered.get(i);
				int cardX = listX + i * 20 - (int) scrollX;
				if (mouseX >= cardX && mouseX < cardX + 18) {
					// Draw multi-line tooltips using GuiGraphics component lists
					guiGraphics.renderComponentTooltip(parentScreen.getFont(), inv.getMultiLineTooltip(level), mouseX,
							mouseY);
				}
			}
		}

		// Render filter slot tooltip [3]
		if (slotHovered) {
			if (FlowLayoutHelper.isWidgetActiveAndOnTop(this, parentScreen)) {
				if (this.boundListCard.isEmpty()) {
					guiGraphics.renderTooltip(parentScreen.getFont(),
							Component.literal("Filter by Inventories List card (Place card here)"), mouseX, mouseY);
				} else {
					List<Component> lines = new ArrayList<>();
					lines.add(this.boundListCard.getHoverName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
					lines.add(Component.literal("Click to clear filter").withStyle(ChatFormatting.GRAY));
					guiGraphics.renderComponentTooltip(parentScreen.getFont(), lines, mouseX, mouseY);
				}
			}
		}
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

	/**
	 * Dynamically updates the filtered capability type for this selector.
	 */
	public void setCapabilityType(ResourceLocation capabilityType) {
		this.capabilityType = capabilityType;
	}

	/**
	 * Resets the scroll bar back to the start to prevent viewport clipping.
	 */
	public void resetScroll() {
		this.scrollX = 0.0F;
	}
}