package dta.sfmflow.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.flowchart.Flowchart;
import dta.sfmflow.common.network.PhysicalNetwork;
import dta.sfmflow.common.network.PhysicalNetworkMap;
import dta.sfmflow.registry.ModTags;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import dta.sfmflow.screen.ManagerMenu;
import dta.sfmflow.util.ConnectionBlock;
import dta.sfmflow.util.ConnectionBlockType;
import dta.sfmflow.kernel.ExecutionRingBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Backing BlockEntity class for the Manager block [3]. Stores layout settings,
 * flowchart canvas data, and delegates scans to standalone network models [3].
 */
public class ManagerBlockEntity extends BlockEntity implements MenuProvider {
	private Flowchart flowchart = new Flowchart(new java.util.HashMap<>(), new ArrayList<>());
	protected final ContainerData data;
	private int commandCount = 0;
	private boolean needsRefresh = false;
	private boolean isFirstTick = true;

	private final PhysicalNetwork physicalNetwork = new PhysicalNetwork();

	private final ExecutionRingBuffer executionBuffer = new ExecutionRingBuffer(1024);

	private final List<dta.sfmflow.api.variable.InventoryGroupVariable> groupVariables = new java.util.ArrayList<>();
	private final List<dta.sfmflow.api.variable.ItemFilterVariable> filterVariables = new java.util.ArrayList<>();

	public List<dta.sfmflow.api.variable.InventoryGroupVariable> getGroupVariables() {
		return this.groupVariables;
	}

	public List<dta.sfmflow.api.variable.ItemFilterVariable> getFilterVariables() {
		return this.filterVariables;
	}

	/**
	 * Instantiates a new Manager block entity and binds synchronized container
	 * variables [3].
	 *
	 * @param pos        block position coordinates [3]
	 * @param blockState block state properties [3]
	 */
	public ManagerBlockEntity(BlockPos pos, BlockState blockState) {
		super(ModBlockEntities.MANAGER_BE.get(), pos, blockState);

		data = new ContainerData() {
			@Override
			public int get(int index) {
				return ManagerBlockEntity.this.commandCount;
			}

			@Override
			public void set(int index, int value) {
				ManagerBlockEntity.this.commandCount = value;
			}

			@Override
			public int getCount() {
				return 1;
			}
		};
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new ManagerMenu(containerId, playerInventory, this, this.data);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.sfmflow.manager_block");
	}

	public ExecutionRingBuffer getExecutionBuffer() {
		return this.executionBuffer;
	}

	/**
	 * Ticking loop driving pathfinder scanning sweeps on the physical server level
	 * [3]. Upgraded to evaluate timer triggers on every tick, and trigger
	 * block-entity updates immediately upon scan completions [3].
	 *
	 * @param pLevel      world level instance [3]
	 * @param pBlockPos   manager coordinates [3]
	 * @param pBlockState manager block state [3]
	 */
	public void tick(Level pLevel, BlockPos pBlockPos, BlockState pBlockState) {
		if (pLevel != null && !pLevel.isClientSide()) {
			if (this.isFirstTick) {
				this.isFirstTick = false;
				PhysicalNetworkMap map = this.physicalNetwork.getNetworkMap();
				boolean graphSane = true;

				for (BlockPos storedPos : map.getAllPositions()) {
					if (!pLevel.getBlockState(storedPos).is(ModTags.CABLES)) {
						graphSane = false;
						break;
					}
				}

				if (!graphSane) {
					map.clear();
					this.physicalNetwork.markDirty();
				}
			}

			// Synchronously poll and execute published tasks from the lock-free ring buffer
			// [3]
			this.executionBuffer.pollAndExecute(task -> {
				// Query capabilities with side-specific directions (furnaces, machines, etc.)
				// [3]
				var source = pLevel.getCapability(Capabilities.ItemHandler.BLOCK, task.getSourcePos(),
						task.getSourceSide());
				var target = pLevel.getCapability(Capabilities.ItemHandler.BLOCK, task.getTargetPos(),
						task.getTargetSide());

				if (source == null) {
					source = pLevel.getCapability(Capabilities.ItemHandler.BLOCK, task.getSourcePos(), null);
				}
				if (target == null) {
					target = pLevel.getCapability(Capabilities.ItemHandler.BLOCK, task.getTargetPos(), null);
				}

				if (source != null && target != null) {
					ItemStack simExtracted = source.extractItem(task.getSourceSlot(), task.getCount(), true);
					if (ItemStack.isSameItemSameComponents(simExtracted, task.getItem())) {
						ItemStack targetRemaining;
						if (task.getTargetSlot() != -1) {
							targetRemaining = target.insertItem(task.getTargetSlot(), simExtracted, true);
						} else {
							targetRemaining = ItemHandlerHelper.insertItemStacked(target, simExtracted, true);
						}

						int realTransferCount = simExtracted.getCount() - targetRemaining.getCount();

						if (realTransferCount > 0) {
							ItemStack realExtracted = source.extractItem(task.getSourceSlot(), realTransferCount,
									false);
							if (task.getTargetSlot() != -1) {
								target.insertItem(task.getTargetSlot(), realExtracted, false);
							} else {
								ItemHandlerHelper.insertItemStacked(target, realExtracted, false);
							}
						}
					}
				}
			});

			// Symmetrical timer evaluation: gather all elapsed interval triggers [3]
			List<UUID> activeTriggers = new ArrayList<>();
			long currentTime = pLevel.getGameTime();
			for (var comp : this.getFlowComponents().values()) {
				if (comp instanceof dta.sfmflow.flowcomponents.IntervalTriggerComponent trigger) {
					long elapsed = currentTime - trigger.getLastExecutionTick();

					if (elapsed < 0) {
						trigger.setLastExecutionTick(currentTime);
					} else if (elapsed >= trigger.getTotalTicks()) {
						trigger.setLastExecutionTick(currentTime);
						activeTriggers.add(trigger.getId());
					}
				}
			}

			// Only schedule planning runs if active triggers have actually elapsed [3]
			if (!activeTriggers.isEmpty()) {
				var snapshot = dta.sfmflow.api.execution.ThreadSafeInventorySnapshot.create(this);
				dta.sfmflow.kernel.FlowExecutionKernel.submitTask(this, snapshot, activeTriggers);
			}

			// Sync to client dynamically whenever physical cable topology changes [3]
			boolean scanned = this.physicalNetwork.tickCheckAndScan(pLevel, pBlockPos);
			if (scanned) {
				this.setChanged(); // Force immediate client-side synchronizations! [3]
			}
		}
	}

	public void updateInventories() {
		this.physicalNetwork.markDirty();
	}

	public PhysicalNetwork getPhysicalNetwork() {
		return this.physicalNetwork;
	}

	public List<ConnectionBlock> getInventories() {
		List<ConnectionBlock> scanned = this.physicalNetwork.getScannedInventories();
		if (this.level != null) {
			PhysicalNetworkMap map = this.physicalNetwork.getNetworkMap();
			for (ConnectionBlock block : scanned) {
				int nodeId = map.getNodeId(block.getBlockPos());
				if (nodeId != -1 && map.isNodeSleeping(nodeId)) {
					block.setSleeping(true);
				} else if (!this.level.hasChunkAt(block.getBlockPos())) {
					block.setSleeping(true);
				} else {
					block.setSleeping(false);
				}
			}
		}
		return scanned;
	}

	@Override
	public void onLoad() {
		if (this.level != null && !this.level.isClientSide()) {
			updateInventories();
		}
	}

	@Override
	protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
		Flowchart.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this.flowchart)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode flowchart: {}", err))
				.ifPresent(nbt -> pTag.put("flowchart", nbt));

		PhysicalNetworkMap map = this.physicalNetwork.getNetworkMap();
		java.util.Collection<BlockPos> positions = map.getAllPositions();
		long[] flatPosArray = new long[positions.size()];
		int idx = 0;
		for (BlockPos mappedPos : positions) {
			flatPosArray[idx++] = mappedPos.asLong();
		}
		pTag.putLongArray("ScannedCablePositions", flatPosArray);

		dta.sfmflow.api.variable.InventoryGroupVariable.CODEC.codec().listOf()
				.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this.groupVariables)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode group variables: {}", err))
				.ifPresent(nbt -> pTag.put("GroupVariables", nbt));

		dta.sfmflow.api.variable.ItemFilterVariable.CODEC.codec().listOf()
				.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this.filterVariables)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode filter variables: {}", err))
				.ifPresent(nbt -> pTag.put("FilterVariables", nbt));

		// Serialize scanned inventories list to synchronize targets dynamically to the
		// client [3]
		ListTag invList = new ListTag();
		for (ConnectionBlock inv : this.physicalNetwork.getScannedInventories()) {
			CompoundTag invTag = new CompoundTag();
			invTag.putLong("pos", inv.getBlockPos().asLong());
			invTag.putInt("id", inv.getId());
			invTag.putInt("distance", inv.getCableDistance());

			ListTag typeList = new ListTag();
			for (ConnectionBlockType type : inv.getTypes()) {
				typeList.add(net.minecraft.nbt.StringTag.valueOf(type.name()));
			}
			invTag.put("types", typeList);
			invList.add(invTag);
		}
		pTag.put("ScannedInventories", invList);

		super.saveAdditional(pTag, pRegistries);
	}

	@Override
	protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
		super.loadAdditional(pTag, pRegistries);

		if (pTag.contains("flowchart")) {
			try {
				this.flowchart = Flowchart.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, pTag.get("flowchart"))
						.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode flowchart map: {}", err))
						.orElseGet(() -> new Flowchart(new java.util.HashMap<>(), new ArrayList<>()));
			} catch (Exception e) {
				SFMFlow.LOGGER.error(
						"CRITICAL: Caught unhandled internal Mojang DFU structural exception while decoding flowchart data!",
						e);
				this.flowchart = new Flowchart(new java.util.HashMap<>(), new ArrayList<>());
			}
		} else {
			this.flowchart = new Flowchart(new java.util.HashMap<>(), new ArrayList<>());
		}

		if (pTag.contains("ScannedCablePositions")) {
			long[] flatPosArray = pTag.getLongArray("ScannedCablePositions");
			PhysicalNetworkMap map = this.physicalNetwork.getNetworkMap();
			map.clear();
			for (long longVal : dirOrdinals(flatPosOrdinals(flatPosArray))) {
				map.getOrAddNode(BlockPos.of(longVal));
			}
		}

		if (pTag.contains("GroupVariables")) {
			dta.sfmflow.api.variable.InventoryGroupVariable.CODEC.codec().listOf()
					.parse(net.minecraft.nbt.NbtOps.INSTANCE, pTag.get("GroupVariables"))
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode group variables: {}", err))
					.ifPresent(list -> {
						this.groupVariables.clear();
						this.groupVariables.addAll(list);
					});
		}
		if (pTag.contains("FilterVariables")) {
			dta.sfmflow.api.variable.ItemFilterVariable.CODEC.codec().listOf()
					.parse(net.minecraft.nbt.NbtOps.INSTANCE, pTag.get("FilterVariables"))
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode filter variables: {}", err))
					.ifPresent(list -> {
						this.filterVariables.clear();
						this.filterVariables.addAll(list);
					});
		}

		// Parse and cache the synchronized scanned inventories list on client [3]
		if (pTag.contains("ScannedInventories")) {
			ListTag invList = pTag.getList("ScannedInventories", Tag.TAG_COMPOUND);
			List<ConnectionBlock> scanned = this.physicalNetwork.getScannedInventories();
			scanned.clear();
			for (int i = 0; i < invList.size(); i++) {
				CompoundTag invTag = invList.getCompound(i);
				BlockPos pos = BlockPos.of(invTag.getLong("pos"));
				int id = invTag.getInt("id");
				int distance = invTag.getInt("distance");

				ConnectionBlock inv = new ConnectionBlock(pos, distance);
				inv.setId(id);

				EnumSet<ConnectionBlockType> types = EnumSet.noneOf(ConnectionBlockType.class);
				ListTag typeList = invTag.getList("types", Tag.TAG_STRING);
				for (int k = 0; k < typeList.size(); k++) {
					try {
						types.add(ConnectionBlockType.valueOf(typeList.getString(k)));
					} catch (IllegalArgumentException e) {
						// Ignore unrecognized values
					}
				}
				inv.setTypes(types);
				scanned.add(inv);
			}
		}

		commandCount = flowchart.components().size();
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void addFlowComponent(FlowComponentType type, Player player) {
		if (flowchart.components().size() < ServerConfig.MAX_COMPONENT_AMOUNT.get()) {
			UUID newUUID = UUID.randomUUID();
			AbstractFlowComponent newComponent = type.createComponent(newUUID);
			newComponent.setZ(flowchart.components().size() + 1);
			flowchart.components().put(newUUID, newComponent);
			this.setChanged();
			commandCount = flowchart.components().size();

			CompoundTag tag = new CompoundTag();
			newComponent.saveData(tag);
			broadcastDeltaUpdate(new SyncComponentDeltaPacket(this.worldPosition, newUUID,
					SyncComponentDeltaPacket.DeltaType.ADD, tag));
		}
	}

	public Map<UUID, AbstractFlowComponent> getFlowComponents() {
		return flowchart.components();
	}

	public List<FlowComponentConnections> getFlowConnections() {
		return flowchart.connections();
	}

	public void executeCanvasAction(CanvasAction action, UUID componentId) {
		switch (action) {
		case DELETE -> handleDelete(componentId);
		case COPY -> handleCopy(componentId);
		case TOGGLE_OPEN -> handleToggleOpen(componentId);
		}
	}

	private void handleDelete(UUID componentId) {
		this.flowchart.components().remove(componentId);
		this.flowchart.connections().removeIf(wire -> wire.getSourceComponentId().equals(componentId)
				|| wire.getTargetComponentId().equals(componentId));
		this.setChanged();
		this.commandCount = this.flowchart.components().size();

		broadcastDeltaUpdate(new SyncComponentDeltaPacket(this.worldPosition, componentId,
				SyncComponentDeltaPacket.DeltaType.REMOVE, new CompoundTag()));
	}

	private void handleCopy(UUID componentId) {
		AbstractFlowComponent original = this.flowchart.components().get(componentId);
		if (original != null && this.flowchart.components().size() < ServerConfig.MAX_COMPONENT_AMOUNT.get()) {
			UUID newId = UUID.randomUUID();
			AbstractFlowComponent copy = original.getType().createComponent(newId);
			CompoundTag settings = new CompoundTag();
			original.saveData(settings);
			copy.loadData(settings);

			copy.setBaseProperties(new AbstractFlowComponent.BaseProperties(newId, copy.getX(), copy.getY(),
					copy.getZ(), copy.getCustomName(), copy.getColorMask()));

			int h = copy.getVisualHeight();
			int nextX = original.getX() + 10;
			int nextY = original.getY() + 10;

			if (nextX + copy.getVisualWidth() > AbstractFlowComponent.CANVAS_MAX_X) {
				nextX = original.getX() - 10;
			}
			nextX = Math.max(22, Math.min(nextX, AbstractFlowComponent.CANVAS_MAX_X - copy.getVisualWidth()));

			if (nextY + h > AbstractFlowComponent.CANVAS_MAX_Y) {
				nextY = original.getY() - 10;
			}

			int minY = copy.hasInputNodes() ? 10 : 4;
			nextY = Math.max(minY, Math.min(nextY, AbstractFlowComponent.CANVAS_MAX_Y - h));

			copy.setX(nextX);
			copy.setY(nextY);
			copy.setZ(original.getZ() + 1);
			this.flowchart.components().put(newId, copy);
			this.setChanged();
			this.commandCount = this.flowchart.components().size();

			CompoundTag tag = new CompoundTag();
			copy.saveData(tag);
			broadcastDeltaUpdate(new SyncComponentDeltaPacket(this.worldPosition, newId,
					SyncComponentDeltaPacket.DeltaType.ADD, tag));
		}
	}

	private void handleToggleOpen(UUID componentId) {
	}

	public void componentMoved(ComponentMoved pData, IPayloadContext context) {
		for (ComponentMoved.Entry entry : pData.entries()) {
			AbstractFlowComponent component = flowchart.components().get(entry.id());
			if (component != null) {
				component.setX(entry.x());
				component.setY(entry.y());
				component.setZ(entry.z());

				CompoundTag dataTag = new CompoundTag();
				dataTag.putInt("x", entry.x());
				dataTag.putInt("y", entry.y());
				dataTag.putInt("z", entry.z());
				broadcastDeltaUpdate(new SyncComponentDeltaPacket(this.worldPosition, entry.id(),
						SyncComponentDeltaPacket.DeltaType.MOVE, dataTag));
			}
		}
	}

	@Override
	public void setChanged() {
		super.setChanged();

		if (this.level != null && !this.level.isClientSide()) {
			BlockState state = this.getBlockState();
			this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		this.saveAdditional(tag, registries);
		return tag;
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
			HolderLookup.Provider registries) {
		super.onDataPacket(connection, packet, registries);
		this.needsRefresh = true;
	}

	public boolean pollNeedsRefresh() {
		boolean r = this.needsRefresh;
		this.needsRefresh = false;
		return r;
	}

	public void markNeedsRefresh() {
		this.needsRefresh = true;
	}

	public void broadcastDeltaUpdate(SyncComponentDeltaPacket packet) {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		this.level.players().stream()
				.filter(player -> player.containerMenu instanceof ManagerMenu menu
						&& menu.getManagerBlockEntity().getBlockPos().equals(this.worldPosition))
				.forEach(player -> net.neoforged.neoforge.network.PacketDistributor
						.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, packet));
	}

	public void broadcastConnectionsUpdate(dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket packet) {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		this.level.players().stream()
				.filter(player -> player.containerMenu instanceof ManagerMenu menu
						&& menu.getManagerBlockEntity().getBlockPos().equals(this.worldPosition))
				.forEach(player -> net.neoforged.neoforge.network.PacketDistributor
						.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, packet));
	}

	private long[] flatPosOrdinals(long[] arr) {
		return arr != null ? arr : new long[0];
	}

	private long[] dirOrdinals(long[] arr) {
		return arr;
	}
}