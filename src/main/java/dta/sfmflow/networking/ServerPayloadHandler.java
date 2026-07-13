package dta.sfmflow.networking;

import java.lang.reflect.Field;
import java.util.Locale;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.block.entity.CableClusterBlockEntity;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket;
import dta.sfmflow.networking.packets.clientbound.SyncInventorySlotsPacket;
import dta.sfmflow.networking.packets.clientbound.SyncSideConfigPropertiesPacket;
import dta.sfmflow.networking.packets.serverbound.BindVariablePacket;
import dta.sfmflow.networking.packets.serverbound.CanvasActionPacket;
import dta.sfmflow.networking.packets.serverbound.CreateNodePacket;
import dta.sfmflow.networking.packets.serverbound.RemoveConnectionPacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import dta.sfmflow.networking.packets.serverbound.CreateConnectionPacket;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SetActiveFilterComponentPacket;
import dta.sfmflow.networking.packets.serverbound.SyncCarriedItemPacket;
import dta.sfmflow.networking.packets.serverbound.SyncClusterSlotDirectionPacket;
import dta.sfmflow.networking.packets.serverbound.RequestInventorySlotsPacket;
import dta.sfmflow.networking.packets.serverbound.RequestSideConfigPropertiesPacket;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles serverbound network payloads, registering coordinates updates, click
 * tasks, and workspace state updates safely on the server thread.
 */
public class ServerPayloadHandler {
	private ServerPayloadHandler() {
	}

	public static void handleCanvasAction(final CanvasActionPacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					manager.executeCanvasAction(data.action(), data.componentId());
					manager.setChanged();
				}
			}
		});
	}

	public static void handleCreateNode(final CreateNodePacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					ResourceLocation typeLoc = data.componentTypeLoc();
					FlowComponentType type = FlowComponentType.REGISTRY.get(typeLoc);

					if (type != null && manager.getFlowComponents().size() < ServerConfig.MAX_COMPONENT_AMOUNT.get()) {
						manager.addFlowComponent(type, context.player());
						manager.setChanged();
					} else {
						SFMFlow.LOGGER.warn("Attempted to spawn invalid or blocked node: {}", typeLoc);
					}
				}
			}
		});
	}

	public static void handleComponentMoved(final ComponentMoved data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					manager.componentMoved(data, context);
					manager.setChanged();
				}
			}
		});
	}

	public static void handleSaveComponentSettings(final SaveComponentSettings data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				AbstractFlowComponent component = manager.getFlowComponents().get(data.componentId());
				if (component != null) {
					component.loadData(data.settings());
					manager.setDataDirty(true);
					manager.setChanged();

					ServerPlayer player = (ServerPlayer) context.player();
					ItemStack carried = player.containerMenu.getCarried();
					if (carried != null && !carried.isEmpty()) {
						player.getInventory().placeItemBackInInventory(carried);
						player.containerMenu.setCarried(ItemStack.EMPTY);
						player.containerMenu.broadcastChanges();
					}

					manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), data.componentId(),
							SyncComponentDeltaPacket.DeltaType.SETTINGS, data.settings()));
				}
			}
		});
	}

	public static void handleSyncClusterSlotDirection(final SyncClusterSlotDirectionPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof CableClusterBlockEntity be) {
				be.setSlotDirection(data.slotIndex(), data.directionOrdinal());
			}
		});
	}

	public static void handleCreateConnection(final CreateConnectionPacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				var connections = manager.getFlowConnections();

				connections.removeIf(conn -> (conn.getSourceComponentId().equals(data.sourceId())
						&& conn.getOutputNodeIndex() == data.outputIdx())

						|| (conn.getTargetComponentId().equals(data.targetId())
								&& conn.getInputNodeIndex() == data.inputIdx()));

				connections.add(new FlowComponentConnections(data.sourceId(), data.outputIdx(), data.targetId(),
						data.inputIdx()));
				manager.setDataDirty(true);
				manager.setChanged();

				CompoundTag dataTag = new CompoundTag();
				ListTag listTag = new ListTag();
				for (var conn : connections) {
					CompoundTag connTag = new CompoundTag();
					conn.save(connTag);
					listTag.add(connTag);
				}
				dataTag.put("connections", listTag);

				manager.broadcastConnectionsUpdate(new SyncConnectionsPacket(manager.getBlockPos(), dataTag));
			}
		});
	}

	public static void handleRemoveConnection(final RemoveConnectionPacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				var connections = manager.getFlowConnections();

				connections.removeIf(conn -> conn.getSourceComponentId().equals(data.sourceId())
						&& conn.getOutputNodeIndex() == data.outputIdx()
						&& conn.getTargetComponentId().equals(data.targetId())
						&& conn.getInputNodeIndex() == data.inputIdx());
				manager.setDataDirty(true);
				manager.setChanged();

				CompoundTag dataTag = new CompoundTag();
				ListTag listTag = new ListTag();
				for (var conn : connections) {
					CompoundTag connTag = new CompoundTag();
					conn.save(connTag);
					listTag.add(connTag);
				}
				dataTag.put("connections", listTag);

				manager.broadcastConnectionsUpdate(new SyncConnectionsPacket(manager.getBlockPos(), dataTag));
			}
		});
	}

	public static void handleBindVariable(final BindVariablePacket data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getBlockEntity(data.pos()) instanceof ManagerBlockEntity manager) {
				var component = manager.getFlowComponents().get(data.componentId());
				if (component instanceof ItemTransferComponent transfer) {
					if (data.isGroupVariable()) {
						transfer.setBoundGroupVariableId(data.variableId());
					} else {
						transfer.setBoundFilterVariableId(data.variableId());
					}
					manager.setDataDirty(true);
					manager.setChanged();

					CompoundTag settingsTag = new CompoundTag();
					transfer.saveData(settingsTag);
					manager.broadcastDeltaUpdate(new SyncComponentDeltaPacket(manager.getBlockPos(), transfer.getId(),
							SyncComponentDeltaPacket.DeltaType.SETTINGS, settingsTag));
				}
			}
		});
	}

	public static void handleRequestInventorySlots(final RequestInventorySlotsPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer player = (ServerPlayer) context.player();
			Level level = player.level();
			if (level.hasChunkAt(data.pos())) {
				CompoundTag dataTag = new CompoundTag();
				ListTag list = new ListTag();
				ListTag accessibleList = new ListTag();
				int totalSlotsVal = 0;

				ResourceLocation capId = data.capabilityId();

				// Symmetrically segment queries based on the requested capability ID
				if (capId.getPath().equals("item")) {
					IItemHandler nullHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, data.pos(), null);
					IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, data.pos(),
							data.side());

					if (nullHandler != null) {
						totalSlotsVal = nullHandler.getSlots();

						for (int i = 0; i < totalSlotsVal; i++) {
							ItemStack stack = nullHandler.getStackInSlot(i);
							if (!stack.isEmpty()) {
								CompoundTag slotTag = new CompoundTag();
								slotTag.putInt("slot", i);
								slotTag.put("item", stack.save(level.registryAccess()));
								list.add(slotTag);
							}
						}

						if (itemHandler != null) {
							int sideCount = itemHandler.getSlots();
							if (sideCount == totalSlotsVal) {
								for (int i = 0; i < totalSlotsVal; i++) {
									accessibleList.add(IntTag.valueOf(i));
								}
							} else {
								for (int i = 0; i < sideCount; i++) {
									accessibleList.add(IntTag.valueOf(i));
								}
							}
						}

						if (level.getBlockEntity(data.pos()) instanceof WorldlyContainer worldly
								&& data.side() != null) {
							accessibleList.clear();
							int[] slots = worldly.getSlotsForFace(data.side());
							if (slots != null) {
								for (int s : slots) {
									accessibleList.add(IntTag.valueOf(s));
								}
							}
						}
					}
				} else if (capId.getPath().equals("fluid")) {
					IFluidHandler nullFluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, data.pos(),
							null);
					if (nullFluidHandler == null) {
						nullFluidHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.FluidHandler.BLOCK,
								level, data.pos(), level.getBlockState(data.pos()), null);
					}
					IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, data.pos(),
							data.side());
					if (fluidHandler == null) {
						fluidHandler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.FluidHandler.BLOCK,
								level, data.pos(), level.getBlockState(data.pos()), data.side());
					}

					if (nullFluidHandler != null) {
						totalSlotsVal = nullFluidHandler.getTanks();

						for (int i = 0; i < totalSlotsVal; i++) {
							FluidStack fluid = nullFluidHandler.getFluidInTank(i);
							if (!fluid.isEmpty()) {
								Item bucket = fluid.getFluid().getBucket();
								if (bucket != null && bucket != Items.AIR) {
									ItemStack bucketStack = new ItemStack(bucket);
									CompoundTag slotTag = new CompoundTag();
									slotTag.putInt("slot", i);
									slotTag.put("item", bucketStack.save(level.registryAccess()));
									list.add(slotTag);
								}
							}
						}

						if (fluidHandler != null) {
							int sideCount = fluidHandler.getTanks();
							if (sideCount == totalSlotsVal) {
								for (int i = 0; i < totalSlotsVal; i++) {
									accessibleList.add(IntTag.valueOf(i));
								}
							} else {
								for (int i = 0; i < sideCount; i++) {
									accessibleList.add(IntTag.valueOf(i));
								}
							}
						}
					}
				}

				dataTag.put("items", list);
				dataTag.putInt("totalSlots", totalSlotsVal);
				dataTag.put("accessibleSlots", accessibleList);

				PacketDistributor.sendToPlayer(player,
						new SyncInventorySlotsPacket(data.pos(), data.side(), capId, dataTag));
			}
		});
	}

	/**
	 * Activates the targeted component safely on both the clientbound and
	 * serverbound container menus.
	 */
	public static void handleSetActiveFilterComponent(final SetActiveFilterComponentPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof ManagerMenu menu) {
				ManagerBlockEntity manager = menu.getManagerBlockEntity();
				if (!manager.isRemoved() && manager.getBlockPos().equals(data.pos())) {
					if (data.componentId() == null) {
						menu.setActiveComponent(null); // Deactivate ghost slots
					} else {
						var comp = manager.getFlowComponents().get(data.componentId());
						if (comp != null) {
							// Generalization fix: set the generic component on the server-side menu
							menu.setActiveComponent(comp);
						}
					}
				}
			}
		});
	}

	/**
	 * Synchronizes the visual card stack safely on the server menu container.
	 */
	public static void handleSyncCarriedItem(final SyncCarriedItemPacket payload, final IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer player = (ServerPlayer) context.player();
			ItemStack stack = payload.carried();

			// EXPLOIT FIREWALL: Only allow setting the carried item if it is a
			// VARIABLE_CARD or empty
			if (stack.isEmpty() || stack.is(ModItems.VARIABLE_CARD.get())) {
				player.containerMenu.setCarried(stack);
				player.containerMenu.broadcastChanges();
			}
		});
	}

	public static void handleRequestSideConfigProperties(final RequestSideConfigPropertiesPacket data,
			final IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer player = (ServerPlayer) context.player();
			Level level = player.level();
			if (level.hasChunkAt(data.pos())) {
				CompoundTag properties = new CompoundTag();

				if (data.capabilityId().getPath().equals("energy")) {
					IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, data.pos(),
							data.side());
					if (energy == null) {
						// Fallback to non-sided query if sided check resolves to null
						energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, data.pos(), null);
					}
					if (energy != null) {
						int capacity = energy.getMaxEnergyStored();

						// 1. Inspect rate fields via reflection fallback
						int extractLimit = reflectField(energy, "maxExtract", capacity);
						int receiveLimit = reflectField(energy, "maxReceive", capacity);

						// 2. Query actual max per-tick transfer limits via simulated extraction/receive
						// if stored
						if (extractLimit == capacity) {
							int simExtract = energy.extractEnergy(Integer.MAX_VALUE, true);
							if (simExtract > 0) {
								extractLimit = simExtract;
							}
						}
						if (receiveLimit == capacity) {
							int simReceive = energy.receiveEnergy(Integer.MAX_VALUE, true);
							if (simReceive > 0) {
								receiveLimit = simReceive;
							}
						}

						properties.putInt("MaxEnergy", capacity);
						properties.putInt("MaxExtract", extractLimit);
						properties.putInt("MaxReceive", receiveLimit);
						properties.putInt("CurrentEnergy", energy.getEnergyStored());
					}
				}

				// Symmetrically send properties update back to the client
				PacketDistributor.sendToPlayer(player,
						new SyncSideConfigPropertiesPacket(data.pos(), data.side(), data.capabilityId(), properties));
			}
		});
	}

	private static int reflectField(Object obj, String fieldName, int defaultValue) {
		return reflectFieldRecursive(obj, fieldName, defaultValue, 0);
	}

	private static int reflectFieldRecursive(Object obj, String fieldName, int defaultValue, int depth) {
		if (obj == null || depth > 2) {
			return defaultValue;
		}
		Class<?> clazz = obj.getClass();
		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);
				Object val = field.get(obj);
				if (val instanceof Number num) {
					return num.intValue();
				}
			} catch (Exception ignored) {
				// Alternative capitalization mapping support
				try {
					String altName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
					Field field = clazz.getDeclaredField(altName);
					field.setAccessible(true);
					Object val = field.get(obj);
					if (val instanceof Number num) {
						return num.intValue();
					}
				} catch (Exception ignored2) {
				}
			}

			// Recurse through member fields to resolve nested delegate adapters
			for (Field f : clazz.getDeclaredFields()) {
				try {
					f.setAccessible(true);
					Object member = f.get(obj);
					if (member != null && member != obj) {
						String lowerName = f.getName().toLowerCase(Locale.ROOT);
						if (lowerName.contains("handler") || lowerName.contains("delegate")
								|| lowerName.contains("container") || lowerName.contains("storage")
								|| lowerName.contains("wrapped")) {
							int result = reflectFieldRecursive(member, fieldName, -1, depth + 1);
							if (result != -1) {
								return result;
							}
						}
					}
				} catch (Exception ignored) {
				}
			}
			clazz = clazz.getSuperclass();
		}
		return defaultValue;
	}
}