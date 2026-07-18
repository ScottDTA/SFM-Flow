package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.helper.FlowLayoutHelper;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;


/**
 * Custom settings modal dedicated to configuring enchantment matching profiles [3].
 */
@OnlyIn(Dist.CLIENT)
public class EnchantmentsComponentSettingsModal extends AbstractModalPopup {
	private final AdvancedItemFilterVariableComponent component;
	private final ItemStack filterStack;
	private final CycleButton<String> modeBtn;
	private final EditBox minEdit;
	private final EditBox maxEdit;
	
	private final ApiWidgetAdapter<EditBox> minEditAdapter;
	private final ApiWidgetAdapter<EditBox> maxEditAdapter;
	
	private String highlightedEnchId = "";

	public EnchantmentsComponentSettingsModal(ManagerScreen parentScreen, ItemStack filterStack) {
		super(parentScreen, 240, 180, Component.literal("Enchantment Settings"));
		this.filterStack = filterStack;

		AbstractFlowComponent comp = parentScreen.getMenu().getActiveComponent();
		if (comp instanceof AdvancedItemFilterVariableComponent varComp) {
			this.component = varComp;
		} else {
			throw new IllegalStateException("Active component is not an Advanced Filter Card!");
		}

		CompoundTag tag = getEnchSettings();
		String mode = tag.getString("mode");

		this.modeBtn = CycleButton.<String>builder(val -> Component.literal(val.replace("_", " ")))
				.withValues("HAS_ANY", "HAS_NONE", "CONTAINS", "EXACT")
				.withInitialValue(mode)
				.displayOnlyValue()
				.create(getX() + 15, getY() + 16, 210, 18, Component.literal("Ench Mode"), (btn, val) -> {
					CompoundTag settings = getEnchSettings();
					settings.putString("mode", val);
					parentScreen.getMenu().getManagerBlockEntity().setChanged();
					sendSettingsUpdate();
				});

		this.minEdit = new EditBox(parentScreen.getFont(), getX() + 125, getY() + 48, 100, 18, Component.literal("Min"));
		this.minEdit.setFilter(text -> text.matches("\\d*"));
		this.minEdit.setResponder(text -> {
			if (highlightedEnchId.isEmpty()) return;
			try {
				int val = Integer.parseInt(text);
				CompoundTag settings = getEnchSettings();
				ListTag list = settings.getList("enchantments", Tag.TAG_COMPOUND);
				CompoundTag entry = getEnchEntry(list, highlightedEnchId);
				entry.putInt("minL", Mth.clamp(val, 1, 10));
				sendSettingsUpdate();
			} catch (NumberFormatException ignored) {
			}
		});

		this.maxEdit = new EditBox(parentScreen.getFont(), getX() + 125, getY() + 84, 100, 18, Component.literal("Max"));
		this.maxEdit.setFilter(text -> text.matches("\\d*"));
		this.maxEdit.setResponder(text -> {
			if (highlightedEnchId.isEmpty()) return;
			try {
				int val = Integer.parseInt(text);
				CompoundTag settings = getEnchSettings();
				ListTag list = settings.getList("enchantments", Tag.TAG_COMPOUND);
				CompoundTag entry = getEnchEntry(list, highlightedEnchId);
				entry.putInt("maxL", Mth.clamp(val, 1, 10));
				sendSettingsUpdate();
			} catch (NumberFormatException ignored) {
			}
		});

		this.minEditAdapter = new ApiWidgetAdapter<>(this.minEdit);
		this.maxEditAdapter = new ApiWidgetAdapter<>(this.maxEdit);

		this.children.add(new ApiWidgetAdapter<>(this.modeBtn));
		this.children.add(this.minEditAdapter);
		this.children.add(this.maxEditAdapter);

		this.children.add(new EnchScrollListWidget(getX() + 15, getY() + 38, 100, 110));
	}

	private CompoundTag getEnchSettings() {
		CompoundTag tag = component.getCustomComponentSettings().getCompound("minecraft:enchantments");
		if (tag.isEmpty()) {
			tag.putString("mode", "HAS_ANY");
			tag.put("enchantments", new ListTag());
			component.getCustomComponentSettings().put("minecraft:enchantments", tag);
		}
		return tag;
	}

	private CompoundTag getEnchEntry(ListTag list, String id) {
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			if (entry.getString("id").equals(id)) {
				return entry;
			}
		}
		CompoundTag newEntry = new CompoundTag();
		newEntry.putString("id", id);
		newEntry.putInt("minL", 1);
		newEntry.putInt("maxL", 10);
		list.add(newEntry);
		return newEntry;
	}

	private void sendSettingsUpdate() {
		CompoundTag nbt = new CompoundTag();
		component.saveData(nbt);
		PacketDistributor.sendToServer(new SaveComponentSettings(
				parentScreen.getMenu().getManagerBlockEntity().getBlockPos(), component.getId(), nbt));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		// Delegate focus setting to base class [3]
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;

		if (button == 0 && mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14) {
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			close();
			return true;
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		render9SliceBackground(guiGraphics);

		drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "ENCHANTMENTS", getX() + width / 2, getY() + 6, 0xFFD4AF37);

		CompoundTag settings = getEnchSettings();
		String mode = settings.getString("mode");

		boolean showRange = "CONTAINS".equals(mode) || "EXACT".equals(mode);
		
		this.minEditAdapter.visible = showRange && !highlightedEnchId.isEmpty();
		this.maxEditAdapter.visible = showRange && !highlightedEnchId.isEmpty();

		if (showRange) {
			if (!highlightedEnchId.isEmpty()) {
				drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "Min Level", getX() + 175, getY() + 38, 0xFF404040);
				drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "Max Level", getX() + 175, getY() + 74, 0xFF404040);
			} else {
				drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "Highlight active", getX() + 175, getY() + 54, 0xFF8B8B8B);
				drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "ench to edit bounds", getX() + 175, getY() + 66, 0xFF8B8B8B);
			}
		} else {
			drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "Mode does not check", getX() + 175, getY() + 54, 0xFF8B8B8B);
			drawStringWithoutShadow(guiGraphics, parentScreen.getFont(), "individual levels", getX() + 175, getY() + 66, 0xFF8B8B8B);
		}

		for (var child : children) {
			if (child instanceof AbstractFlowWidget widget) {
				if (widget.visible) {
					widget.active = this.active;
					widget.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}
		}

		int btnX = getX() + (width - 80) / 2;
		int btnY = getY() + height - 22;
		boolean hovered = mouseX >= btnX && mouseX < btnX + 80 && mouseY >= btnY && mouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, hovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 40, btnY + 3, 0xFFFFFFFF);
	}

	private void drawStringWithoutShadow(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, String text, int x, int y, int color) {
		int w = font.width(text);
		guiGraphics.drawString(font, text, x - w / 2, y, color, false);
	}

	/**
	 * Scrollable checklist displaying all enchantments currently held by the template ghost stack [3].
	 */
	@OnlyIn(Dist.CLIENT)
	private class EnchScrollListWidget extends AbstractFlowWidget {
		private float scrollY = 0.0F;

		public EnchScrollListWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Component.literal("Ench List"));
		}

		private List<Holder<Enchantment>> getEnchList() {
			List<Holder<Enchantment>> list = new ArrayList<>();
			Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
			if (level != null && !filterStack.isEmpty()) {
				var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
				for (var holder : registry.holders().toList()) {
					if (holder.value().canEnchant(filterStack)) {
						list.add(holder);
					}
				}
			}
			return list;
		}

		@Override
		protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			CompoundTag settings = getEnchSettings();
			String mode = settings.getString("mode");
			boolean isChecking = "CONTAINS".equals(mode) || "EXACT".equals(mode);

			if (!isChecking) {
				return;
			}

			guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF111111);
			guiGraphics.renderOutline(getX(), getY(), width, height, 0xFF434343);

			List<Holder<Enchantment>> enchs = getEnchList();
			if (enchs.isEmpty()) {
				guiGraphics.drawString(parentScreen.getFont(), "No enchants", getX() + 4, getY() + 4, 0xFF8B8B8B, false);
				return;
			}

			guiGraphics.enableScissor(getX(), getY() + 1, getX() + width, getY() + height - 1);

			ListTag list = settings.getList("enchantments", Tag.TAG_COMPOUND);
			int startY = getY() + 4 - (int) scrollY;
			for (int i = 0; i < enchs.size(); i++) {
				Holder<Enchantment> holder = enchs.get(i);
				ResourceLocation loc = holder.unwrapKey().map(ResourceKey::location).orElse(null);
				if (loc == null) continue;

				String typeStr = loc.toString();
				int itemY = startY + i * 12;

				boolean isEnabled = false;
				for (int k = 0; k < list.size(); k++) {
					if (list.getCompound(k).getString("id").equals(typeStr)) {
						isEnabled = true;
						break;
					}
				}

				boolean hoveredBox = mouseX >= getX() + 4 && mouseX < getX() + 12 && mouseY >= itemY && mouseY < itemY + 8;
				boolean hoveredText = mouseX >= getX() + 14 && mouseX < getX() + width && mouseY >= itemY && mouseY < itemY + 11;
				boolean isHighlighted = highlightedEnchId.equals(typeStr);

				int checkboxBorder = hoveredBox ? 0xFFD4AF37 : 0xFF8B8B8B;
				guiGraphics.fill(getX() + 4, itemY + 2, getX() + 10, itemY + 8, isEnabled ? 0xFF39FF14 : 0xFF222222);
				guiGraphics.renderOutline(getX() + 4, itemY + 2, 6, 6, checkboxBorder);

				int textColor = isHighlighted ? 0xFFD4AF37 : (hoveredText ? 0xFFFFFFFF : 0xFF8B8B8B);
				guiGraphics.drawString(parentScreen.getFont(), loc.getPath(), getX() + 14, itemY, textColor, false);
			}

			guiGraphics.disableScissor();

			int maxScroll = Math.max(0, enchs.size() * 12 - (height - 8));
			if (maxScroll > 0) {
				int sbX = getX() + width - 4;
				guiGraphics.fill(sbX, getY() + 2, sbX + 2, getY() + height - 2, 0x40000000);

				int thumbHeight = (int) (((double) height / (enchs.size() * 12)) * height);
				thumbHeight = Math.max(8, Math.min(height, thumbHeight));
				int thumbY = getY() + 2 + (int) ((scrollY / maxScroll) * (height - 4 - thumbHeight));

				guiGraphics.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, 0xFF8B8B8B);
			}

			// Render hover tooltip of the full enchantment registry name string
			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				if (FlowLayoutHelper.isWidgetActiveAndOnTop(this, parentScreen)) {
					int row = (int) ((mouseY - getY() + scrollY - 4) / 12);
					if (row >= 0 && row < enchs.size()) {
						ResourceLocation loc = enchs.get(row).unwrapKey()
								.map(ResourceKey::location)
								.orElse(null);
						if (loc != null) {
							guiGraphics.renderTooltip(parentScreen.getFont(), Component.literal(loc.toString()), mouseX, mouseY);
						}
					}
				}
			}
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			CompoundTag settings = getEnchSettings();
			String mode = settings.getString("mode");
			boolean isChecking = "CONTAINS".equals(mode) || "EXACT".equals(mode);

			if (!this.visible || !this.active || !isChecking) {
				return false;
			}

			if (mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				List<Holder<Enchantment>> enchs = getEnchList();
				int startY = getY() + 4 - (int) scrollY;

				for (int i = 0; i < enchs.size(); i++) {
					Holder<Enchantment> holder = enchs.get(i);
					ResourceLocation loc = holder.unwrapKey().map(ResourceKey::location).orElse(null);
					if (loc == null) continue;

					int itemY = startY + i * 12;
					String typeStr = loc.toString();

					// Checkbox Toggle Bounds Click
					if (mouseX >= getX() + 4 && mouseX < getX() + 12 && mouseY >= itemY && mouseY < itemY + 8) {
						ListTag list = settings.getList("enchantments", Tag.TAG_COMPOUND);
						boolean found = false;
						for (int k = 0; k < list.size(); k++) {
							if (list.getCompound(k).getString("id").equals(typeStr)) {
								list.remove(k);
								found = true;
								break;
							}
						}
						if (!found) {
							getEnchEntry(list, typeStr);
						}
						parentScreen.getMenu().getManagerBlockEntity().setChanged();
						sendSettingsUpdate();
						Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						return true;
					}

					// Label Click [Highlights item to configure Min/Max levels in the right column]
					if (mouseX >= getX() + 14 && mouseX < getX() + width && mouseY >= itemY && mouseY < itemY + 11) {
						highlightedEnchId = typeStr;
						Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						
						ListTag list = settings.getList("enchantments", Tag.TAG_COMPOUND);
						CompoundTag entry = getEnchEntry(list, typeStr);
						minEdit.setValue(String.valueOf(entry.contains("minL") ? entry.getInt("minL") : 1));
						maxEdit.setValue(String.valueOf(entry.contains("maxL") ? entry.getInt("maxL") : 10));
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			CompoundTag settings = getEnchSettings();
			String mode = settings.getString("mode");
			boolean isChecking = "CONTAINS".equals(mode) || "EXACT".equals(mode);

			if (this.visible && this.active && isChecking && mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height) {
				List<Holder<Enchantment>> enchs = getEnchList();
				int maxScroll = Math.max(0, enchs.size() * 12 - (height - 8));
				if (maxScroll > 0) {
					this.scrollY = Mth.clamp(this.scrollY - (float) scrollY * 6.0F, 0.0F, (float) maxScroll);
					return true;
				}
			}
			return false;
		}
	}
}