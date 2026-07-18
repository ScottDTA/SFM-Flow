package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.layout.SlotLayout;
import dta.sfmflow.api.client.widget.AbstractModalPopup;
import dta.sfmflow.api.client.layout.SlotEntry;
import dta.sfmflow.api.client.layout.LayoutKey;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.api.client.NineSliceUtil;
import dta.sfmflow.api.client.layout.FlowLayoutRegistry;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.component.ISlotConfigurable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.network.ClientInventoryCache;
import dta.sfmflow.client.screen.helper.SlotLayoutManager;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
import dta.sfmflow.flowcomponents.FluidConditionalComponent;
import dta.sfmflow.networking.packets.serverbound.RequestInventorySlotsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Visual configuration modal displaying individual slot mappings for selected
 * block directions and capabilities.
 */
@OnlyIn(Dist.CLIENT)
public class SlotLayoutModalPopup extends AbstractModalPopup {
	private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID,
			"textures/gui/flowcomponents/generic_slot.png");

	private static final int GRID_START_Y = 20;

	private final ISideConfigurable sideModel;
	private final Direction side;
	private final BlockPos blockPos;
	private final Runnable onChanged;

	private final int totalSlots;
	private final Set<Integer> accessibleSlots = new HashSet<>();
	private @Nullable IItemHandler nullHandler = null;
	private @Nullable IFluidHandler nullFluidHandler = null;
	private @Nullable SlotLayout layout = null;
	private final boolean isFluid;

	private final int unscaledWidth;
	private final int unscaledHeight;

	// Dynamic capability tracking
	private final ResourceLocation capabilityId;

	public SlotLayoutModalPopup(ManagerScreen parentScreen, ISideConfigurable sideModel, Direction side,
			BlockPos blockPos, Runnable onChanged) {
		this(parentScreen, sideModel, side, blockPos, resolveCapabilityId(sideModel), onChanged);
	}

	public SlotLayoutModalPopup(ManagerScreen parentScreen, ISideConfigurable sideModel, Direction side,
			BlockPos blockPos, ResourceLocation capabilityId, Runnable onChanged) {
		this(parentScreen, sideModel, side, blockPos, capabilityId, onChanged,
				parentScreen.getMenu().getManagerBlockEntity().getLevel(),
				capabilityId.getPath().equals("fluid"));
	}

	private SlotLayoutModalPopup(ManagerScreen parentScreen, ISideConfigurable sideModel, Direction side,
			BlockPos blockPos, ResourceLocation capabilityId, Runnable onChanged, Level level, boolean isFluid) {
		this(parentScreen, sideModel, side, blockPos, capabilityId, onChanged, level, isFluid,
				resolveLayout(level, blockPos, capabilityId),
				resolveTotalSlots(level, blockPos, isFluid));
	}

	private SlotLayoutModalPopup(ManagerScreen parentScreen, ISideConfigurable sideModel, Direction side,
			BlockPos blockPos, ResourceLocation capabilityId, Runnable onChanged, Level level, boolean isFluid,
			@Nullable SlotLayout layout, int totalSlots) {
		super(parentScreen, getUnscaledWidth(layout, totalSlots) / 2, getUnscaledHeight(layout, totalSlots) / 2,
				Component.literal("Slot Layout"));

		this.sideModel = sideModel;
		this.side = side;
		this.blockPos = blockPos;
		this.onChanged = onChanged;
		this.isFluid = isFluid;
		this.capabilityId = capabilityId;
		this.layout = layout;
		this.totalSlots = totalSlots;
		this.unscaledWidth = getUnscaledWidth(layout, totalSlots);
		this.unscaledHeight = getUnscaledHeight(layout, totalSlots);

		IItemHandler sideHandler = null;
		IFluidHandler sideFluidHandler = null;
		BlockEntity be = null;

		if (level != null && blockPos != null) {
			be = level.getBlockEntity(blockPos);
			BlockState state = level.getBlockState(blockPos);

			if (isFluid) {
				this.nullFluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, blockPos, null);
				if (this.nullFluidHandler == null) {
					this.nullFluidHandler = SpecialBlockCapabilityRegistry
							.getCapability(Capabilities.FluidHandler.BLOCK, level, blockPos, state, null);
				}
				sideFluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, blockPos, side);
				if (sideFluidHandler == null) {
					sideFluidHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.FluidHandler.BLOCK,
							level, blockPos, state, side);
				}
			} else {
				this.nullHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos, null);
				if (this.nullHandler == null) {
					this.nullHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.ItemHandler.BLOCK,
							level, blockPos, state, null);
				}
				sideHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos, side);
				if (sideHandler == null) {
					sideHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.ItemHandler.BLOCK, level,
							blockPos, state, side);
				}
			}
		}

		if (isFluid) {
			if (sideFluidHandler != null) {
				int sideCount = sideFluidHandler.getTanks();
				for (int i = 0; i < sideCount; i++) {
					accessibleSlots.add(i);
				}
			}
		} else {
			if (be instanceof WorldlyContainer worldly && side != null) {
				int[] slots = worldly.getSlotsForFace(side);
				if (slots != null) {
					for (int s : slots) {
						accessibleSlots.add(s);
					}
				}
			} else if (sideHandler != null) {
				int sideCount = sideHandler.getSlots();
				if (sideCount == this.totalSlots) {
					for (int i = 0; i < this.totalSlots; i++) {
						accessibleSlots.add(i);
					}
				} else {
					for (int i = 0; i < sideCount; i++) {
						accessibleSlots.add(i);
					}
				}
			}
		}

		// Dispatch the exact, verified capability ID over the packet handshake
		PacketDistributor.sendToServer(new RequestInventorySlotsPacket(this.blockPos, this.side, this.capabilityId));
	}

	private static ResourceLocation resolveCapabilityId(ISideConfigurable sideModel) {
		if (sideModel instanceof FluidTransferComponent || sideModel instanceof FluidConditionalComponent) {
			return ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid");
		}
		return ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
	}

	private static @Nullable SlotLayout resolveLayout(Level level, BlockPos blockPos, ResourceLocation capabilityId) {
		if (level == null || blockPos == null) {
			return null;
		}
		BlockState state = level.getBlockState(blockPos);
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (blockId != null) {
			LayoutKey key = new LayoutKey(blockId, capabilityId);
			SlotLayout layout = FlowLayoutRegistry.getLayout(key);
			if (layout == null) {
				layout = SlotLayoutManager.getLayout(key);
			}
			return layout;
		}
		return null;
	}

	private static int resolveTotalSlots(Level level, BlockPos blockPos, boolean isFluid) {
		if (level == null || blockPos == null) {
			return 0;
		}
		BlockState state = level.getBlockState(blockPos);
		if (isFluid) {
			IFluidHandler nullFluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, blockPos, null);
			if (nullFluidHandler == null) {
				nullFluidHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.FluidHandler.BLOCK, level,
						blockPos, state, null);
			}
			return nullFluidHandler != null ? nullFluidHandler.getTanks() : 0;
		} else {
			IItemHandler nullHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos, null);
			if (nullHandler == null) {
				nullHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.ItemHandler.BLOCK, level,
						blockPos, state, null);
			}
			return nullHandler != null ? nullHandler.getSlots() : 0;
		}
	}

	private static int getUnscaledWidth(@Nullable SlotLayout layout, int totalSlots) {
		if (layout != null) {
			return layout.width();
		}
		int cols = Math.min(9, totalSlots);
		if (cols <= 0)
			cols = 9;
		int gridW = cols * 20 - 2;
		return Math.max(110, 10 + gridW + 10);
	}

	private static int getUnscaledHeight(@Nullable SlotLayout layout, int totalSlots) {
		if (layout != null) {
			return layout.height();
		}
		int cols = Math.min(9, totalSlots);
		if (cols <= 0)
			cols = 9;
		int rows = (totalSlots + cols - 1) / cols;
		if (rows <= 0)
			rows = 1;
		int gridH = rows * 20 - 2;
		return 20 + gridH + 30;
	}

	private Set<Integer> getLiveAccessibleSlots() {
		CompoundTag cachedData = ClientInventoryCache.get(this.blockPos, this.side, this.capabilityId);
		if (cachedData.contains("accessibleSlots")) {
			Set<Integer> live = new HashSet<>();
			ListTag list = cachedData.getList("accessibleSlots", Tag.TAG_INT);
			for (int i = 0; i < list.size(); i++) {
				live.add(list.getInt(i));
			}
			return live;
		}
		return this.accessibleSlots;
	}

	private int getLiveTotalSlots() {
		CompoundTag cachedData = ClientInventoryCache.get(this.blockPos, this.side, this.capabilityId);
		if (cachedData.contains("totalSlots")) {
			return cachedData.getInt("totalSlots");
		}
		return this.totalSlots;
	}

	private ItemStack[] getLiveCachedItems() {
		CompoundTag cachedData = ClientInventoryCache.get(this.blockPos, this.side, this.capabilityId);
		int total = getLiveTotalSlots();
		ItemStack[] items = new ItemStack[total];
		for (int i = 0; i < total; i++) {
			items[i] = ItemStack.EMPTY;
		}
		if (cachedData.contains("items")) {
			ListTag list = cachedData.getList("items", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag slotTag = list.getCompound(i);
				int slot = slotTag.getInt("slot");
				if (slot >= 0 && slot < total) {
					items[slot] = ItemStack
							.parse(Minecraft.getInstance().level.registryAccess(), slotTag.getCompound("item"))
							.orElse(ItemStack.EMPTY);
				}
			}
		}
		return items;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.visible || !this.active) {
			return false;
		}

		double localX = (mouseX - getX()) * 2.0;
		double localY = (mouseY - getY()) * 2.0;

		int btnX = (this.unscaledWidth - 80) / 2;
		int btnY = this.unscaledHeight - 22;

		if (button == 0 && localX >= btnX && localX < btnX + 80 && localY >= btnY && localY < btnY + 14) {
			Minecraft.getInstance().getSoundManager()
					.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			close();
			return true;
		}

		Set<Integer> liveAccessible = getLiveAccessibleSlots();
		int liveTotal = getLiveTotalSlots();

		if (button == 0 && this.layout != null) {
			for (SlotEntry entry : this.layout.slots()) {
				int i = entry.index();
				int slotX = entry.x();
				int slotY = entry.y();
				int sWidth = entry.width();
				int sHeight = entry.height();

				if (localX >= slotX && localX < slotX + sWidth && localY >= slotY && localY < slotY + sHeight) {
					if (liveAccessible.contains(i)) {
						if (sideModel instanceof ISlotConfigurable transfer) {
							transfer.toggleSlot(side, i);
							this.onChanged.run();
							Minecraft.getInstance().getSoundManager()
									.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
							return true;
						}
					}
				}
			}
		}

		if (button == 0 && this.layout == null && liveTotal > 0) {
			int cols = Math.min(9, liveTotal);
			if (cols <= 0)
				cols = 9;
			int gridW = cols * 20 - 2;
			int startGridX = (this.unscaledWidth - gridW) / 2;

			for (int i = 0; i < liveTotal; i++) {
				int row = i / cols;
				int col = i % cols;

				int slotX = startGridX + col * 20;
				int slotY = GRID_START_Y + row * 20;

				if (localX >= slotX && localX < slotX + 18 && localY >= slotY && localY < slotY + 18) {
					if (liveAccessible.contains(i)) {
						if (sideModel instanceof ISlotConfigurable transfer) {
							transfer.toggleSlot(side, i);
							this.onChanged.run();
							Minecraft.getInstance().getSoundManager()
									.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	@Override
	protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		double localMouseX = (mouseX - getX()) * 2.0;
		double localMouseY = (mouseY - getY()) * 2.0;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(getX(), getY(), 0);
		guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);

		int w = this.unscaledWidth;
		int h = this.unscaledHeight;

		if (this.layout != null) {
			guiGraphics.blit(this.layout.background(), 0, 0, 0, 0, w, h, w, h);
		} else {
			NineSliceUtil.drawDefault(guiGraphics, 0, 0, w, h);
		}

		String sideName = side != null ? side.name() : "GENERAL";
		String suffix = isFluid ? " TANKS" : " SLOTS";
		Component titleComponent = Component.literal(sideName + suffix);
		guiGraphics.drawCenteredString(parentScreen.getFont(), titleComponent, w / 2, 6, 0xFFD4AF37);

		ItemStack[] cachedItems = getLiveCachedItems();
		Set<Integer> liveAccessible = getLiveAccessibleSlots();
		int liveTotal = getLiveTotalSlots();

		if (this.layout != null) {
			for (SlotEntry entry : this.layout.slots()) {
				int i = entry.index();
				int slotX = entry.x();
				int slotY = entry.y();
				int sWidth = entry.width();
				int sHeight = entry.height();

				if (entry.useGenericTexture()) {
					guiGraphics.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, sWidth, sHeight, 18, 18);
				} else if (entry.customTexture().isPresent()) {
					guiGraphics.blit(entry.customTexture().get(), slotX, slotY, 0, 0, sWidth, sHeight, sWidth, sHeight);
				}

				boolean isAccessible = liveAccessible.contains(i);

				if (!isAccessible) {
					guiGraphics.fill(slotX + 1, slotY + 1, slotX + sWidth - 1, slotY + sHeight - 1, 0x80151515);

					for (int k = 0; k < Math.min(sWidth, sHeight) - 4; k++) {
						guiGraphics.fill(slotX + 2 + k, slotY + 2 + k, slotX + 3 + k, slotY + 3 + k, 0xFFFF0000);
						guiGraphics.fill(slotX + 2 + k, slotY + sHeight - 3 - k, slotX + 3 + k, slotY + sHeight - 2 - k,
								0xFFFF0000);
					}
				} else {
					if (cachedItems != null && i >= 0 && i < cachedItems.length) {
						ItemStack stack = cachedItems[i];
						if (stack != null && !stack.isEmpty()) {
							boolean drewFluid = false;
							if (isFluid) {
								var fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
								if (fluidHandler != null && fluidHandler.getTanks() > 0) {
									FluidStack fluidStack = fluidHandler.getFluidInTank(0);
									if (!fluidStack.isEmpty()) {
										IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions
												.of(fluidStack.getFluid());
										ResourceLocation stillTexture = clientFluid.getStillTexture(fluidStack);
										if (stillTexture != null) {
											int tintColor = clientFluid.getTintColor(fluidStack);
											TextureAtlasSprite fluidSprite = Minecraft.getInstance()
													.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);

											float r = ((tintColor >> 16) & 0xFF) / 255.0F;
											float g = ((tintColor >> 8) & 0xFF) / 255.0F;
											float b = (tintColor & 0xFF) / 255.0F;
											float a = ((tintColor >> 24) & 0xFF) / 255.0F;
											if (a <= 0.0F)
												a = 1.0F;

											RenderSystem.setShaderColor(r, g, b, a);
											guiGraphics.blit(slotX + 1, slotY + 1, 0, sWidth - 2, sHeight - 2,
													fluidSprite);
											RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
											drewFluid = true;
										}
									}
								}
							}
							if (!drewFluid) {
								int centeredItemX = slotX + (sWidth - 16) / 2;
								int centeredItemY = slotY + (sHeight - 16) / 2;
								guiGraphics.renderItem(stack, centeredItemX, centeredItemY);
							}
						}
					}

					boolean isEnabled = true;
					if (sideModel instanceof ISlotConfigurable transfer) {
						isEnabled = transfer.isSlotEnabled(side, i);
					}
					if (isEnabled) {
						guiGraphics.fill(slotX + 1, slotY + 1, slotX + sWidth - 1, slotY + sHeight - 1, 0x5039FF14);
						guiGraphics.renderOutline(slotX, slotY, sWidth, sHeight, 0xFF39FF14);
					} else {
						guiGraphics.fill(slotX + 1, slotY + 1, slotX + sWidth - 1, slotY + sHeight - 1, 0x50FF0000);
						guiGraphics.renderOutline(slotX, slotY, sWidth, sHeight, 0xFFFF0000);
					}
				}
			}
		} else {
			int cols = Math.min(9, liveTotal);
			if (cols <= 0)
				cols = 9;
			int gridW = cols * 20 - 2;
			int startGridX = (this.unscaledWidth - gridW) / 2;

			for (int i = 0; i < liveTotal; i++) {
				int row = i / cols;
				int col = i % cols;

				int slotX = startGridX + col * 20;
				int slotY = GRID_START_Y + row * 20;

				guiGraphics.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);

				boolean isAccessible = liveAccessible.contains(i);
				if (!isAccessible) {
					guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x80151515);
					for (int k = 0; k < 12; k++) {
						guiGraphics.fill(slotX + 3 + k, slotY + 3 + k, slotX + 5 + k, slotY + 4 + k, 0xFFFF0000);
						guiGraphics.fill(slotX + 3 + k, slotY + 14 - k, slotX + 5 + k, slotY + 15 - k, 0xFFFF0000);
					}
				} else {
					if (cachedItems != null && i >= 0 && i < cachedItems.length) {
						ItemStack stack = cachedItems[i];
						if (stack != null && !stack.isEmpty()) {
							boolean drewFluid = false;
							if (isFluid) {
								var fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
								if (fluidHandler != null && fluidHandler.getTanks() > 0) {
									FluidStack fluidStack = fluidHandler.getFluidInTank(0);
									if (!fluidStack.isEmpty()) {
										IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions
												.of(fluidStack.getFluid());
										ResourceLocation stillTexture = clientFluid.getStillTexture(fluidStack);
										if (stillTexture != null) {
											int tintColor = clientFluid.getTintColor(fluidStack);
											TextureAtlasSprite fluidSprite = Minecraft.getInstance()
													.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);

											float r = ((tintColor >> 16) & 0xFF) / 255.0F;
											float g = ((tintColor >> 8) & 0xFF) / 255.0F;
											float b = (tintColor & 0xFF) / 255.0F;
											float a = ((tintColor >> 24) & 0xFF) / 255.0F;
											if (a <= 0.0F)
												a = 1.0F;

											RenderSystem.setShaderColor(r, g, b, a);
											guiGraphics.blit(slotX + 1, slotY + 1, 0, 16, 16, fluidSprite);
											RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
											drewFluid = true;
										}
									}
								}
							}
							if (!drewFluid) {
								guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
							}
						}
					}

					boolean isEnabled = true;
					if (sideModel instanceof ISlotConfigurable transfer) {
						isEnabled = transfer.isSlotEnabled(side, i);
					}
					if (isEnabled) {
						guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x5039FF14);
						guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFF39FF14);
					} else {
						guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x50FF0000);
						guiGraphics.renderOutline(slotX, slotY, 18, 18, 0xFFFF0000);
					}
				}
			}
		}

		int btnX = (w - 80) / 2;
		int btnY = h - 22;
		boolean btnHovered = localMouseX >= btnX && localMouseX < btnX + 80 && localMouseY >= btnY
				&& localMouseY < btnY + 14;

		guiGraphics.fill(btnX, btnY, btnX + 80, btnY + 14, btnHovered ? 0xFF555555 : 0xFF222222);
		guiGraphics.renderOutline(btnX, btnY, 80, 14, 0xFFD4AF37);
		guiGraphics.drawCenteredString(parentScreen.getFont(), "Close", btnX + 40, btnY + 3, 0xFFFFFFFF);

		guiGraphics.pose().popPose();

		if (liveTotal > 0) {
			if (this.layout != null) {
				for (SlotEntry entry : this.layout.slots()) {
					int i = entry.index();
					int slotX = getX() + (int) (entry.x() * 0.5);
					int slotY = getY() + (int) (entry.y() * 0.5);
					int scaledW = (int) (entry.width() * 0.5);
					int scaledH = (int) (entry.height() * 0.5);

					if (mouseX >= slotX && mouseX < slotX + scaledW && mouseY >= slotY && mouseY < slotY + scaledH) {
						boolean isAccessible = liveAccessible.contains(i);
						if (!isAccessible) {
							guiGraphics.renderTooltip(parentScreen.getFont(),
									Component.translatable("gui.sfmflow.error.slot_not_accessible"), mouseX, mouseY);
							break;
						} else if (cachedItems != null && i >= 0 && i < cachedItems.length) {
							ItemStack stack = cachedItems[i];
							if (stack != null && !stack.isEmpty()) {
								guiGraphics.renderTooltip(parentScreen.getFont(), stack, mouseX, mouseY);
								break;
							}
						}
					}
				}
			} else {
				int cols = Math.min(9, liveTotal);
				if (cols <= 0)
					cols = 9;
				int gridW = cols * 20 - 2;
				int startGridX = (this.unscaledWidth - gridW) / 2;
				int scaledStartX = getX() + (int) (startGridX * 0.5);
				int scaledStartY = getY() + (int) (GRID_START_Y * 0.5);

				for (int i = 0; i < liveTotal; i++) {
					int row = i / cols;
					int col = i % cols;

					int slotX = scaledStartX + col * 10;
					int slotY = scaledStartY + row * 10;

					if (mouseX >= slotX && mouseX < slotX + 9 && mouseY >= slotY && mouseY < slotY + 9) {
						boolean isAccessible = liveAccessible.contains(i);
						if (!isAccessible) {
							guiGraphics.renderTooltip(parentScreen.getFont(),
									Component.translatable("gui.sfmflow.error.slot_not_accessible"), mouseX, mouseY);
							break;
						} else if (cachedItems != null && i >= 0 && i < cachedItems.length) {
							ItemStack stack = cachedItems[i];
							if (stack != null && !stack.isEmpty()) {
								guiGraphics.renderTooltip(parentScreen.getFont(), stack, mouseX, mouseY);
								break;
							}
						}
					}
				}
			}
		}
	}
}