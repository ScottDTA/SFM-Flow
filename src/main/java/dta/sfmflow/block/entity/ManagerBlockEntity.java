package dta.sfmflow.block.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.action.CanvasAction;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.event.PreFlowchartPlanningEvent;
import dta.sfmflow.api.event.RebuildManagerListenersEvent;
import dta.sfmflow.api.event.TaskExecutionEvent;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.api.flowchart.Flowchart;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.api.variable.InventoryGroupVariable;
import dta.sfmflow.api.variable.ItemFilterVariable;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.common.network.PhysicalNetwork;
import dta.sfmflow.common.network.PhysicalNetworkMap;
import dta.sfmflow.common.network.SculkEventListener;
import dta.sfmflow.registry.ModTags;
import dta.sfmflow.flowcomponents.FlowComponentConnections;
import dta.sfmflow.flowcomponents.SculkTriggerComponent;
import dta.sfmflow.kernel.ExecutionRingBuffer;
import dta.sfmflow.kernel.FlowExecutionKernel;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import dta.sfmflow.screen.ManagerMenu;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Backing BlockEntity class for the Manager block [3]. Tracks active logical
 * elements and delegates external layout saves/loads to DataStateManager.
 */
public class ManagerBlockEntity extends BlockEntity implements MenuProvider {
	private Flowchart flowchart = new Flowchart(new HashMap<>(), new ArrayList<>());
	protected final ContainerData data;
	private int commandCount = 0;
	private boolean needsRefresh = false;
	private boolean isFirstTick = true;

	private UUID managerId;
	private boolean loadedExternal = false;
	private boolean isDataDirty = false;

	private transient HolderLookup.Provider savedRegistries = null;
	private final List<AbstractTriggerComponent> cachedTriggers = new ArrayList<>();
	private boolean isTriggerCacheDirty = true;

	private final PhysicalNetwork physicalNetwork = new PhysicalNetwork();
	private final ExecutionRingBuffer executionBuffer = new ExecutionRingBuffer(1024);
	private final AtomicBoolean planningActive = new AtomicBoolean(false);

	private final List<InventoryGroupVariable> groupVariables = new ArrayList<>();
	private final List<ItemFilterVariable> filterVariables = new ArrayList<>();

	private static final List<ManagerBlockEntity> ACTIVE_MANAGERS = new CopyOnWriteArrayList<>();

	private transient long rollingExecutionTimeNs = 0;
	private transient int rollingExecutedTasks = 0;
	private transient int rollingTicks = 0;

	private final AtomicInteger planningBreakerTrips = new AtomicInteger(0);

	// Multi-thread cached performance structures
	private transient CompoundTag cachedFlowchartNbt = null;
	private transient ThreadSafeInventorySnapshot.SnapshotProfile snapshotProfile = null;
	private transient boolean isProfileDirty = true;

	public List<InventoryGroupVariable> getGroupVariables() {
		return this.groupVariables;
	}

	public List<ItemFilterVariable> getFilterVariables() {
		return this.filterVariables;
	}

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
		return this.planningBreakerTrips.get();
	}

	public void incrementBreakerTrips() {
		this.planningBreakerTrips.incrementAndGet();
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

	public boolean isDataDirty() {
		return this.isDataDirty;
	}

	/**
	 * Assembles a selective SnapshotProfile filtering targeted inventories and
	 * capabilities.
	 */
	public ThreadSafeInventorySnapshot.SnapshotProfile getSnapshotProfile() {
		if (this.snapshotProfile == null || this.isProfileDirty) {
			Set<BlockPos> positions = new HashSet<>();
			Set<ResourceLocation> capabilities = new HashSet<>();

			for (AbstractFlowComponent comp : this.flowchart.components().values()) {
				if (comp instanceof IInventoryTarget target) {
					int targetId = target.getInventoryId();
					if (targetId != -1) {
						for (ConnectionBlock block : getInventories()) {
							if (block.getId() == targetId) {
								positions.add(block.getBlockPos());
								break;
							}
						}
					}
				}

				// Special manual query mappings for sculk triggers
				if (comp instanceof SculkTriggerComponent sculk) {
					int targetId = sculk.getInventoryId();
					if (targetId != -1) {
						for (ConnectionBlock block : getInventories()) {
							if (block.getId() == targetId) {
								positions.add(block.getBlockPos());
								break;
							}
						}
					}
				}

				// Dynamically extract corresponding capability filters
				ResourceLocation typeKey = FlowComponentType.REGISTRY.getKey(comp.getType());
				if (typeKey != null) {
					String path = typeKey.getPath();
					if (path.contains("item")) {
						capabilities.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"));
					} else if (path.contains("fluid")) {
						capabilities.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"));
					} else if (path.contains("energy")) {
						capabilities.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"));
					} else if (path.contains("redstone")) {
						capabilities.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"));
					} else if (path.contains("sculk")) {
						capabilities.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "sculk"));
					}
				}
			}
			this.snapshotProfile = new ThreadSafeInventorySnapshot.SnapshotProfile(positions, capabilities);
			this.isProfileDirty = false;
		}
		return this.snapshotProfile;
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
				var preEvent = new TaskExecutionEvent.Pre(this, task);
				NeoForge.EVENT_BUS.post(preEvent);

				if (preEvent.isCanceled()) {
					return;
				}
				var executor = FlowCapabilityRegistry.getTransfer(task.getCapabilityId());
				if (executor != null) {
					boolean executed = executor.execute(pLevel, task.getSourcePos(), task.getSourceSide(),
							task.getTargetPos(), task.getTargetSide(), task.getTaskParams());
					tasksRan[0]++;
					if (executed) {
						NeoForge.EVENT_BUS.post(new TaskExecutionEvent.Post(this, task));
					}

				}
			}, maxTimeNs);

			long elapsed = System.nanoTime() - startTime;
			updateProfiling(elapsed, tasksRan[0]);

			// Dynamically rebuild the trigger array list only when changes occur
			if (this.isTriggerCacheDirty) {
				rebuildTriggerCache();
			}

			List<UUID> activeTriggers = new ArrayList<>();
			long currentTime = pLevel.getGameTime();

			for (int i = 0; i < this.cachedTriggers.size(); i++) {
				var comp = this.cachedTriggers.get(i);
				if (comp.evaluateTrigger(pLevel, pBlockPos, currentTime)) {
					activeTriggers.add(comp.getId());
				}
			}

			if (!activeTriggers.isEmpty()) {
				// Atomically attempt to acquire the lock before submitting a background task
				if (this.planningActive.compareAndSet(false, true)) {
					var profile = getSnapshotProfile();
					var snapshot = ThreadSafeInventorySnapshot.create(this, profile);

					// 1. Resolve or lazily compute the cached flowchart NBT [3]
					var registries = pLevel.registryAccess();
					if (this.cachedFlowchartNbt == null || this.isDataDirty) {
						var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
						this.cachedFlowchartNbt = (CompoundTag) Flowchart.CODEC.encodeStart(ops, this.flowchart)
								.resultOrPartial(err -> SFMFlow.LOGGER
										.error("Failed to clone flowchart state for planning: {}", err))
								.orElse(new CompoundTag());
						this.isDataDirty = false;
					}
					CompoundTag flowchartNbt = this.cachedFlowchartNbt.copy(); // Deep copy so worker can safely
																				// parse/modify

					NeoForge.EVENT_BUS
							.post(new PreFlowchartPlanningEvent(this, snapshot, flowchartNbt, activeTriggers));

					// 2. Submit task using decentralized thread-safe parameters and callbacks
					FlowExecutionKernel.submitTask(this.executionBuffer, this::incrementBreakerTrips, snapshot,
							flowchartNbt, registries, activeTriggers, () -> this.planningActive.set(false));
				} else {
					// Task coalescing: Skip scheduling this sweep to prevent memory leaks
					FlowLogger.execution(
							"Skipping planning sweep for manager at %s as a background task is already active.",
							this.worldPosition);
				}
			}

			boolean scanned = this.physicalNetwork.tickCheckAndScan(pLevel, pBlockPos);
			if (scanned) {
				this.setChanged();
				this.rebuildListeners();
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
			ACTIVE_MANAGERS.add(this);
			if (!this.loadedExternal) {
				loadExternalData();
			}
			this.isTriggerCacheDirty = true; // Mark dirty on block load
			updateInventories();

			this.rebuildListeners();
		}
	}

	private void loadExternalData() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null || this.level == null || this.level.isClientSide() || this.managerId == null) {
			return;
		}
		try {
			HolderLookup.Provider registries = this.savedRegistries != null ? this.savedRegistries
					: this.level.registryAccess();
			var loaded = DataStateManager.loadSync(server, this.managerId, registries);
			this.flowchart = loaded.flowchart();
			this.groupVariables.clear();
			this.groupVariables.addAll(loaded.groupVariables());
			this.filterVariables.clear();
			this.filterVariables.addAll(loaded.filterVariables());
			this.isTriggerCacheDirty = true;
			this.isProfileDirty = true;
		} catch (Exception e) {
			SFMFlow.LOGGER.error("Failed to load external flowchart data for manager: {}", this.managerId, e);
			this.flowchart = new Flowchart(new HashMap<>(), new ArrayList<>());
		}
		this.commandCount = this.flowchart.components().size();
		this.loadedExternal = true;
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		ACTIVE_MANAGERS.remove(this);

		this.rebuildListeners();
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		ACTIVE_MANAGERS.remove(this);

		this.rebuildListeners();
	}

	@Override
	protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
		if (this.managerId == null) {
			this.managerId = UUID.randomUUID();
		}
		pTag.putUUID("ManagerId", this.managerId);

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			if (this.isDataDirty) {
				DataStateManager.saveSync(server, this.managerId, this.flowchart, this.groupVariables,
						this.filterVariables, pRegistries);
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
			invTag.putInt("slotIndex", inv.getSlotIndex());
			invTag.putInt("direction", inv.getDirection() != null ? inv.getDirection().ordinal() : -1);

			if (!inv.getCardStack().isEmpty()) {
				invTag.put("cardStack", inv.getCardStack().save(pRegistries));
			}

			ListTag typeList = new ListTag();
			for (ResourceLocation type : inv.getTypes()) {
				typeList.add(StringTag.valueOf(type.toString()));
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
			this.isDataDirty = true;
			this.isProfileDirty = true;
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
		CanvasActionHandler.execute(this, action, componentId);
	}

	public void componentMoved(ComponentMoved pData, IPayloadContext context) {
		CanvasActionHandler.move(this, pData, context);
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

		// Call .codec().listOf() to safely serialize lists of variables [3]
		InventoryGroupVariable.CODEC.listOf().encodeStart(ops, this.groupVariables)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode group variables: {}", err))
				.ifPresent(nbt -> tag.put("GroupVariables", nbt));

		ItemFilterVariable.CODEC.listOf().encodeStart(ops, this.filterVariables)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode filter variables: {}", err))
				.ifPresent(nbt -> tag.put("FilterVariables", nbt));

		return tag;
	}

	@Override
	protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
		super.loadAdditional(pTag, pRegistries);
		this.savedRegistries = pRegistries; // Save the composite provider context for onLoad

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
			for (long longVal : flatPosArray) {
				map.getOrAddNode(BlockPos.of(longVal));
			}
		}

		if (pTag.contains("GroupVariables")) {
			InventoryGroupVariable.CODEC.listOf().parse(ops, pTag.get("GroupVariables"))
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode group variables: {}", err))
					.ifPresent(list -> {
						this.groupVariables.clear();
						this.groupVariables.addAll(list);
						this.loadedExternal = true;
					});
		}
		if (pTag.contains("FilterVariables")) {
			ItemFilterVariable.CODEC.listOf().parse(ops, pTag.get("FilterVariables"))
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
				int slotIndex = invTag.contains("slotIndex") ? invTag.getInt("slotIndex") : -1;
				int dirOrd = invTag.getInt("direction");
				Direction direction = (dirOrd == -1) ? null : Direction.values()[dirOrd]; // Load direction [3]

				ItemStack cardStack = invTag.contains("cardStack")
						? ItemStack.parse(pRegistries, invTag.getCompound("cardStack")).orElse(ItemStack.EMPTY)
						: ItemStack.EMPTY;

				ConnectionBlock inv = new ConnectionBlock(level, pos, distance, slotIndex, cardStack, direction);
				inv.setId(id);

				Set<ResourceLocation> types = new HashSet<>();
				ListTag typeList = invTag.getList("types", Tag.TAG_STRING);
				for (int k = 0; k < typeList.size(); k++) {
					String strVal = typeList.getString(k);
					if ("ITEM".equals(strVal)) {
						types.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"));
					} else if ("FLUID".equals(strVal)) {
						types.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"));
					} else if ("ENERGY".equals(strVal)) {
						types.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "energy"));
					} else if ("CHEMICAL".equals(strVal)) {
						types.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "chemical"));
					} else if ("REDSTONE".equals(strVal)) {
						types.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"));
					} else {
						ResourceLocation res = ResourceLocation.tryParse(strVal);
						if (res != null) {
							types.add(res);
						}
					}
				}
				inv.setTypes(types);
				scanned.add(inv);
			}
		}

		commandCount = flowchart != null ? flowchart.components().size() : 0;
		this.isTriggerCacheDirty = true;
		this.isProfileDirty = true;
		this.cachedFlowchartNbt = null; // Clear cached NBT on reload [3]
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

	public void deleteExternalData() {
		if (this.level == null || this.level.isClientSide() || this.managerId == null
				|| this.level.getServer() == null) {
			return;
		}
		DataStateManager.deleteSync(this.level.getServer(), this.managerId);
	}

	private void rebuildTriggerCache() {
		this.cachedTriggers.clear();
		if (this.flowchart != null && this.flowchart.components() != null) {
			for (var comp : this.flowchart.components().values()) {
				if (comp instanceof AbstractTriggerComponent trigger) {
					this.cachedTriggers.add(trigger);
				}
			}
		}
		this.isTriggerCacheDirty = false;
	}

	public void setDataDirty(boolean dirty) {
		this.isDataDirty = dirty;
		if (dirty) {
			this.isTriggerCacheDirty = true; // Mark dirty on edits
			this.isProfileDirty = true; // Re-compile snapshot profile
		}
	}

	public void rebuildListeners() {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		SculkEventListener.rebuildManagerListeners(this);

		NeoForge.EVENT_BUS.post(new RebuildManagerListenersEvent(this));
	}
}