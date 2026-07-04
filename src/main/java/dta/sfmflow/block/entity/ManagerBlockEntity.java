package dta.sfmflow.block.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import org.jetbrains.annotations.Nullable;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.api.flowchart.Flowchart;
import dta.sfmflow.api.variable.InventoryGroupVariable;
import dta.sfmflow.api.variable.ItemFilterVariable;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.common.network.PhysicalNetwork;
import dta.sfmflow.common.network.PhysicalNetworkMap;
import dta.sfmflow.registry.ModTags;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import dta.sfmflow.screen.ManagerMenu;
import dta.sfmflow.util.ConnectionBlock;
import dta.sfmflow.util.ConnectionBlockType;
import dta.sfmflow.kernel.ExecutionRingBuffer;
import dta.sfmflow.kernel.FlowExecutionKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Backing BlockEntity class for the Manager block [3]. Tracks active logical elements 
 * and delegatess external layout saves/loads to DataStateManager [3].
 */
public class ManagerBlockEntity extends BlockEntity implements MenuProvider {
	private Flowchart flowchart = new Flowchart(new HashMap<>(), new ArrayList<>());
	protected final ContainerData data;
	private int commandCount = 0;
	private boolean needsRefresh = false;
	private boolean isFirstTick = true;

	private UUID managerId;
	private boolean loadedExternal = false;
	private boolean isDataDirty = false; // Persistent dirty state tracking [3]

	private final PhysicalNetwork physicalNetwork = new PhysicalNetwork();
	private final ExecutionRingBuffer executionBuffer = new ExecutionRingBuffer(1024);

	private final List<InventoryGroupVariable> groupVariables = new ArrayList<>();
	private final List<ItemFilterVariable> filterVariables = new ArrayList<>();

	// Thread-safe collection tracking active server-side block entity instances [3]
	private static final List<ManagerBlockEntity> ACTIVE_MANAGERS = new CopyOnWriteArrayList<>();

	private transient long rollingExecutionTimeNs = 0;
	private transient int rollingExecutedTasks = 0;
	private transient int rollingTicks = 0;
	private transient int planningBreakerTrips = 0;

	public List<InventoryGroupVariable> getGroupVariables() {
		return this.groupVariables;
	}

	public List<ItemFilterVariable> getFilterVariables() {
		return this.filterVariables;
	}

	/**
	 * Retrieves an unmodifiable view of all active loaded server manager block
	 * entities [3].
	 */
	public static List<ManagerBlockEntity> getActiveManagers() {
		return ACTIVE_MANAGERS;
	}

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

	public double getAverageExecutionTimeUs() {
		if (rollingTicks == 0)
			return 0.0;
		return (rollingExecutionTimeNs / (double) rollingTicks) / 1000.0;
	}

	public double getAverageTasksPerTick() {
		if (rollingTicks == 0)
			return 0.0;
		return rollingExecutedTasks / (double) rollingTicks;
	}

	public int getBufferBacklog() {
		return (int) (this.executionBuffer.getWriteSequence() - this.executionBuffer.getReadSequence());
	}

	public int getBreakerTrips() {
		return this.planningBreakerTrips;
	}

	public void incrementBreakerTrips() {
		this.planningBreakerTrips++;
	}

	private void updateProfiling(long elapsedNs, int tasksExecuted) {
		this.rollingExecutionTimeNs += elapsedNs;
		this.rollingExecutedTasks += tasksExecuted;
		this.rollingTicks++;

		if (this.rollingTicks >= 100) {
			this.rollingExecutionTimeNs /= 2;
			this.rollingExecutedTasks /= 2;
			this.rollingTicks /= 2;
		}
	}

	public void setDataDirty(boolean dirty) {
		this.isDataDirty = dirty;
	}

	public boolean isDataDirty() {
		return this.isDataDirty;
	}

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

			long maxTimeNs = ServerConfig.MAX_EXECUTION_BUDGET_US.get() * 1000L;
			int[] tasksRan = new int[1];
			long startTime = System.nanoTime();

			this.executionBuffer.pollAndExecuteThrottled(task -> {
				var executor = FlowCapabilityRegistry.getTransfer(task.getCapabilityId());
				if (executor != null) {
					executor.execute(pLevel, task.getSourcePos(), task.getSourceSide(), task.getTargetPos(),
							task.getTargetSide(), task.getTaskParams());
					tasksRan[0]++;
				}
			}, maxTimeNs);

			long elapsed = System.nanoTime() - startTime;
			updateProfiling(elapsed, tasksRan[0]);

			List<UUID> activeTriggers = new ArrayList<>();
			long currentTime = pLevel.getGameTime();
			for (var comp : this.getFlowComponents().values()) {
				if (comp instanceof IntervalTriggerComponent trigger) {
					long elapsedTrigger = currentTime - trigger.getLastExecutionTick();

					if (elapsedTrigger < 0) {
						trigger.setLastExecutionTick(currentTime);
					} else if (elapsedTrigger >= trigger.getTotalTicks()) {
						// Diagnostic trace is now gated behind ServerConfig debug logging option [3]
						if (ServerConfig.ENABLE_DEBUG_LOGGING.get()) {
							SFMFlow.LOGGER.info(
									"[SFM-Flow] Trigger Fired: ID={}, Hash={}, GameTime={}, LastExecuted={}, Elapsed={}, Total={}",
									trigger.getId(), System.identityHashCode(trigger), currentTime,
									trigger.getLastExecutionTick(), elapsedTrigger, trigger.getTotalTicks());
						}
						trigger.setLastExecutionTick(currentTime);
						activeTriggers.add(trigger.getId());
					}
				}
			}

			if (!activeTriggers.isEmpty()) {
				var snapshot = ThreadSafeInventorySnapshot.create(this);
				FlowExecutionKernel.submitTask(this, snapshot, activeTriggers);
			}

			boolean scanned = this.physicalNetwork.tickCheckAndScan(pLevel, pBlockPos);
			if (scanned) {
				this.setChanged();
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
		super.onLoad();
		if (this.level != null && !this.level.isClientSide()) {
			ACTIVE_MANAGERS.add(this); // Statically register loaded block entity [3]
			if (!this.loadedExternal) {
				loadExternalData();
			}
			updateInventories();
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		ACTIVE_MANAGERS.remove(this); // Unregister when chunk unloads [3]
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		ACTIVE_MANAGERS.remove(this); // Unregister when block is broken/removed [3]
	}

	@Override
	protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
		if (this.managerId == null) {
			this.managerId = UUID.randomUUID();
		}
		pTag.putUUID("ManagerId", this.managerId);

		// Synchronously serialize and asynchronously save to disk only if data is dirty [3]
		if (this.level != null && !this.level.isClientSide() && this.level.getServer() != null) {
			if (this.isDataDirty) {
				DataStateManager.saveAsync(this.level.getServer(), this.managerId, this.flowchart,
						this.groupVariables, this.filterVariables, pRegistries);
				this.isDataDirty = false;
			}
		}

		PhysicalNetworkMap map = this.physicalNetwork.getNetworkMap();
		Collection<BlockPos> positions = map.getAllPositions();
		long[] flatPosArray = new long[positions.size()];
		int idx = 0;
		for (BlockPos mappedPos : positions) {
			flatPosArray[idx++] = mappedPos.asLong();
		}
		pTag.putLongArray("ScannedCablePositions", flatPosArray);

		ListTag invList = new ListTag();
		for (ConnectionBlock inv : this.physicalNetwork.getScannedInventories()) {
			CompoundTag invTag = new CompoundTag();
			invTag.putLong("pos", inv.getBlockPos().asLong());
			invTag.putInt("id", inv.getId());
			invTag.putInt("distance", inv.getCableDistance());

			ListTag typeList = new ListTag();
			for (ConnectionBlockType type : inv.getTypes()) {
				typeList.add(StringTag.valueOf(type.name()));
			}
			invTag.put("types", typeList);
			invList.add(invTag);
		}
		pTag.put("ScannedInventories", invList);

		super.saveAdditional(pTag, pRegistries);
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
			this.isDataDirty = true; // Mark dirty! [3]
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
		this.isDataDirty = true; // Mark dirty! [3]
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
			this.isDataDirty = true; // Mark dirty! [3]
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
				this.isDataDirty = true; // Mark dirty! [3]

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

		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		Flowchart.CODEC.encodeStart(ops, this.flowchart)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode flowchart: {}", err))
				.ifPresent(nbt -> tag.put("flowchart", nbt));

		InventoryGroupVariable.CODEC.codec().listOf().encodeStart(ops, this.groupVariables)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode group variables: {}", err))
				.ifPresent(nbt -> tag.put("GroupVariables", nbt));

		ItemFilterVariable.CODEC.codec().listOf().encodeStart(ops, this.filterVariables)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode filter variables: {}", err))
				.ifPresent(nbt -> tag.put("FilterVariables", nbt));

		return tag;
	}

	@Override
	protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
		super.loadAdditional(pTag, pRegistries);

		if (pTag.contains("ManagerId")) {
			this.managerId = pTag.getUUID("ManagerId");
		} else {
			this.managerId = UUID.randomUUID();
		}

		var ops = RegistryOps.create(NbtOps.INSTANCE, pRegistries);

		if (pTag.contains("flowchart")) {
			try {
				this.flowchart = Flowchart.CODEC.parse(ops, pTag.get("flowchart"))
						.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode flowchart map: {}", err))
						.orElseGet(() -> new Flowchart(new HashMap<>(), new ArrayList<>()));
				this.loadedExternal = true;
			} catch (Exception e) {
				SFMFlow.LOGGER.error(
						"CRITICAL: Caught unhandled internal Mojang DFU structural exception while decoding flowchart data!",
						e);
				this.flowchart = new Flowchart(new HashMap<>(), new ArrayList<>());
			}
		} else if (this.level == null || this.level.isClientSide()) {
			if (this.flowchart == null) {
				this.flowchart = new Flowchart(new HashMap<>(), new ArrayList<>());
			}
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
			InventoryGroupVariable.CODEC.codec().listOf().parse(ops, pTag.get("GroupVariables"))
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode group variables: {}", err))
					.ifPresent(list -> {
						this.groupVariables.clear();
						this.groupVariables.addAll(list);
						this.loadedExternal = true;
					});
		}
		if (pTag.contains("FilterVariables")) {
			ItemFilterVariable.CODEC.codec().listOf().parse(ops, pTag.get("FilterVariables"))
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode filter variables: {}", err))
					.ifPresent(list -> {
						this.filterVariables.clear();
						this.filterVariables.addAll(list);
						this.loadedExternal = true;
					});
		}

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

		commandCount = flowchart != null ? flowchart.components().size() : 0;
	}

	private void loadExternalData() {
		if (this.level == null || this.level.isClientSide() || this.managerId == null
				|| this.level.getServer() == null) {
			return;
		}
		try {
			var loaded = DataStateManager.loadSync(this.level.getServer(), this.managerId,
					RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
			this.flowchart = loaded.flowchart();
			this.groupVariables.clear();
			this.groupVariables.addAll(loaded.groupVariables());
			this.filterVariables.clear();
			this.filterVariables.addAll(loaded.filterVariables());
		} catch (Exception e) {
			SFMFlow.LOGGER.error("Failed to load external flowchart data for manager: {}", this.managerId, e);
			this.flowchart = new Flowchart(new HashMap<>(), new ArrayList<>());
		}
		this.commandCount = this.flowchart.components().size();
		this.loadedExternal = true;
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
				.forEach(player -> PacketDistributor.sendToPlayer((ServerPlayer) player, packet));
	}

	public void broadcastConnectionsUpdate(SyncConnectionsPacket packet) {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		this.level.players().stream()
				.filter(player -> player.containerMenu instanceof ManagerMenu menu
						&& menu.getManagerBlockEntity().getBlockPos().equals(this.worldPosition))
				.forEach(player -> PacketDistributor.sendToPlayer((ServerPlayer) player, packet));
	}

	private long[] flatPosOrdinals(long[] arr) {
		return arr != null ? arr : new long[0];
	}

	private long[] dirOrdinals(long[] arr) {
		return arr;
	}

	public void deleteExternalData() {
		if (this.level == null || this.level.isClientSide() || this.managerId == null
				|| this.level.getServer() == null) {
			return;
		}
		DataStateManager.deleteSync(this.level.getServer(), this.managerId);
	}
}