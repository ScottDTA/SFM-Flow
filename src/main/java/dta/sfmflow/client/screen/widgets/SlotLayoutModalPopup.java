package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.layout.SlotLayout;
import dta.sfmflow.api.client.layout.SlotEntry;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.api.client.layout.FlowLayoutRegistry;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.component.ISlotConfigurable; // Standardized slot configuration [3]
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.network.ClientInventoryCache;
import dta.sfmflow.client.screen.helper.SlotLayoutManager;
import dta.sfmflow.flowcomponents.FluidTransferComponent;
import dta.sfmflow.networking.packets.serverbound.RequestInventorySlotsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Symmetrical slot configuration modal popup that allows players to toggle
 * which slots in an inventory are active for a specific side [3].
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

	public SlotLayoutModalPopup(ManagerScreen parentScreen, ISideConfigurable sideModel, Direction side,
			BlockPos blockPos, Runnable onChanged) {
		super(parentScreen, 110, 100, Component.literal("Slot Layout"));
		this.sideModel = sideModel;
		this.side = side;
		this.blockPos = blockPos;
		this.onChanged = onChanged;
		this.isFluid = sideModel instanceof FluidTransferComponent;

		Level level = parentScreen.getMenu().getManagerBlockEntity().getLevel();
		IItemHandler sideHandler = null;
		IFluidHandler sideFluidHandler = null;
		BlockEntity be = null;

		if (level != null && blockPos != null) {
			be = level.getBlockEntity(blockPos);
			BlockState state = level.getBlockState(blockPos);
			ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			if (blockId != null) {
				this.layout = FlowLayoutRegistry.getLayout(blockId);
				if (this.layout == null) {
					this.layout = SlotLayoutManager.getLayout(blockId);
				}
			}

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

		if (this.layout == null) {
			try {
				FlowLogger.execution(
						"Inventory at %s does not have a registered slot layout; falling back to generic grid.",
						this.blockPos);
			} catch (Exception ignored) {
			}
		}

		if (isFluid) {
			this.totalSlots = this.nullFluidHandler != null ? this.nullFluidHandler.getTanks() : 0;
			if (sideFluidHandler != null) {
				int sideCount = sideFluidHandler.getTanks();
				for (int i = 0; i < sideCount; i++) {
					accessibleSlots.add(i);
				}
			}
		} else {
			this.totalSlots = this.nullHandler != null ? this.nullHandler.getSlots() : 0;
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

		if (this.layout != null) {
			this.unscaledWidth = this.layout.width();
			this.unscaledHeight = this.layout.height();
		} else {
			int cols = Math.min(9, this.totalSlots);
			if (cols <= 0)
				cols = 9;
			int rows = (this.totalSlots + cols - 1) / cols;
			if (rows <= 0)
				rows = 1;

			int gridW = cols * 20 - 2;
			int gridH = rows * 20 - 2;

			this.unscaledWidth = Math.max(110, 10 + gridW + 10);
			this.unscaledHeight = 20 + gridH + 30;
		}

		this.width = this.unscaledWidth / 2;
		this.height = this.unscaledHeight / 2;

		this.setX((parentScreen.width - this.width) / 2);
		this.setY((parentScreen.height - this.height) / 2);

		PacketDistributor.sendToServer(new RequestInventorySlotsPacket(this.blockPos));
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

		if (button == 0 && this.layout != null) {
			for (SlotEntry entry : this.layout.slots()) {
				int i = entry.index();
				int slotX = entry.x();
				int slotY = entry.y();

				if (localX >= slotX && localX < slotX + 18 && localY >= slotY && localY < slotY + 18) {
					if (accessibleSlots.contains(i)) {
						// Decoupled cast: support any sideModel that implements ISlotConfigurable [3]
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

		if (button == 0 && this.layout == null && this.totalSlots > 0) {
			int cols = Math.min(9, this.totalSlots);
			if (cols <= 0)
				cols = 9;
			int gridW = cols * 20 - 2;
			int startGridX = (this.unscaledWidth - gridW) / 2;

			for (int i = 0; i < this.totalSlots; i++) {
				int row = i / cols;
				int col = i % cols;

				int slotX = startGridX + col * 20;
				int slotY = GRID_START_Y + row * 20;

				if (localX >= slotX && localX < slotX + 18 && localY >= slotY && localY < slotY + 18) {
					if (accessibleSlots.contains(i)) {
						// Decoupled cast: support any sideModel that implements ISlotConfigurable [3]
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
			int c = 6;
			int m = 10;
			guiGraphics.blit(SUBMENU_BG, 0, 0, 0, 0, c, c, 22, 22);
			guiGraphics.blit(SUBMENU_BG, w - c, 0, 16, 0, c, c, 22, 22);
			guiGraphics.blit(SUBMENU_BG, 0, h - c, 0, 16, c, c, 22, 22);
			guiGraphics.blit(SUBMENU_BG, w - c, h - c, 16, 16, c, c, 22, 22);

			guiGraphics.blit(SUBMENU_BG, c, 0, w - 2 * c, c, (float) c, 0.0F, m, c, 22, 22);
			guiGraphics.blit(SUBMENU_BG, c, h - c, w - 2 * c, c, (float) c, 16.0F, m, c, 22, 22);
			guiGraphics.blit(SUBMENU_BG, 0, c, c, h - 2 * c, 0.0F, (float) c, c, m, 22, 22);
			guiGraphics.blit(SUBMENU_BG, w - c, c, c, h - 2 * c, 16.0F, (float) c, c, m, 22, 22);

			guiGraphics.blit(SUBMENU_BG, c, c, w - 2 * c, h - 2 * c, (float) c, (float) c, m, m, 22, 22);
		}

		String sideName = side != null ? side.name() : "GENERAL";
		String suffix = isFluid ? " TANKS" : " SLOTS";
		Component titleComponent = Component.literal(sideName + suffix);
		guiGraphics.drawCenteredString(parentScreen.getFont(), titleComponent, w / 2, 6, 0xFFD4AF37);

		ItemStack[] cachedItems = ClientInventoryCache.get(this.blockPos);

		if (this.layout != null) {
			for (SlotEntry entry : this.layout.slots()) {
				int i = entry.index();
				int slotX = entry.x();
				int slotY = entry.y();

				guiGraphics.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);

				boolean isAccessible = accessibleSlots.contains(i);

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
							guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
						}
					}

					boolean isEnabled = true;
					// Decoupled cast: support any sideModel that implements ISlotConfigurable [3]
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
		} else {
			int cols = Math.min(9, this.totalSlots);
			if (cols <= 0)
				cols = 9;
			int gridW = cols * 20 - 2;
			int startGridX = (this.unscaledWidth - gridW) / 2;

			for (int i = 0; i < this.totalSlots; i++) {
				int row = i / cols;
				int col = i % cols;

				int slotX = startGridX + col * 20;
				int slotY = GRID_START_Y + row * 20;

				guiGraphics.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);

				boolean isAccessible = accessibleSlots.contains(i);
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
					// Decoupled cast: support any sideModel that implements ISlotConfigurable [3]
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

		if (this.totalSlots > 0) {
			if (this.layout != null) {
				for (SlotEntry entry : this.layout.slots()) {
					int i = entry.index();
					int slotX = getX() + (int) (entry.x() * 0.5);
					int slotY = getY() + (int) (entry.y() * 0.5);

					if (mouseX >= slotX && mouseX < slotX + 9 && mouseY >= slotY && mouseY < slotY + 9) {
						boolean isAccessible = accessibleSlots.contains(i);
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
				int cols = Math.min(9, this.totalSlots);
				if (cols <= 0)
					cols = 9;
				int gridW = cols * 20 - 2;
				int startGridX = (this.unscaledWidth - gridW) / 2;
				int scaledStartX = getX() + (int) (startGridX * 0.5);
				int unscaledStartY = getY() + (int) (GRID_START_Y * 0.5);

				for (int i = 0; i < this.totalSlots; i++) {
					int row = i / cols;
					int col = i % cols;

					int slotX = scaledStartX + col * 10;
					int slotY = unscaledStartY + row * 10;

					if (mouseX >= slotX && mouseX < slotX + 9 && mouseY >= slotY && mouseY < slotY + 9) {
						boolean isAccessible = accessibleSlots.contains(i);
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