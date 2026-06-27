package dta.sfmflow.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import dta.sfmflow.util.Variable;
import dta.sfmflow.util.VariableColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Backing BlockEntity class for the Manager block [3]. Stores layout settings,
 * flowchart canvas data, and delegates scans to standalone network models [3].
 * Updated to reflect the simplified, compact component layout and purged NBT
 * properties [3]. Upgraded to serialize flat long arrays, perform first-tick
 * validation sweeps, and run chunk-safety filters [3].
 */
public class ManagerBlockEntity extends BlockEntity implements MenuProvider {
	private final Variable[] variables;
	private Flowchart flowchart = new Flowchart(new java.util.HashMap<>(), new ArrayList<>());
	protected final ContainerData data;
	private int commandCount = 0;
	private boolean needsRefresh = false;
	private boolean isFirstTick = true; // Symmetrical first-tick validation sweep flag [3]

	private final PhysicalNetwork physicalNetwork = new PhysicalNetwork();

	/**
	 * Instantiates a new Manager block entity and binds synchronized container
	 * variables [3].
	 *
	 * @param pos        block position coordinates [3]
	 * @param blockState block state properties [3]
	 */
	public ManagerBlockEntity(BlockPos pos, BlockState blockState) {
		super(ModBlockEntities.MANAGER_BE.get(), pos, blockState);

		variables = new Variable[VariableColor.values().length];
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

	/**
	 * Ticking loop driving pathfinder scanning sweeps on the physical server level
	 * [3]. Upgraded with an offline first-tick verification sweep to inspect graph
	 * sanity [3].
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

				// Symmetrical offline verification sweep: inspect loaded nodes [3]
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
			this.physicalNetwork.tickCheckAndScan(pLevel, pBlockPos);
		}
	}

	/**
	 * Triggers a cable-network rebuild during the next scanning cycle [3].
	 */
	public void updateInventories() {
		this.physicalNetwork.markDirty();
	}

	/**
	 * Retrieves the physical network topology model [3].
	 *
	 * @return PhysicalNetwork coordinator [3]
	 */
	public PhysicalNetwork getPhysicalNetwork() {
		return this.physicalNetwork;
	}

	/**
	 * Exposes active connection blocks cached on our standalone network model [3].
	 * Upgraded with a double-pass check querying graph sleep states and chunk
	 * loading bounds [3].
	 *
	 * @return a list of scanned targets [3]
	 */
	public List<ConnectionBlock> getInventories() {
		List<ConnectionBlock> scanned = this.physicalNetwork.getScannedInventories();
		if (this.level != null) {
			PhysicalNetworkMap map = this.physicalNetwork.getNetworkMap();
			for (ConnectionBlock block : scanned) {
				int nodeId = map.getNodeId(block.getBlockPos());
				// Double-Pass safety: evaluate the graph sleep bitset AND physical chunk
				// boundaries [3]
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

		// Save flat long array representing cable nodes without nesting overhead [3]
		PhysicalNetworkMap map = this.physicalNetwork.getNetworkMap();
		java.util.Collection<BlockPos> positions = map.getAllPositions();
		long[] flatPosArray = new long[positions.size()];
		int idx = 0;
		for (BlockPos mappedPos : positions) {
			flatPosArray[idx++] = mappedPos.asLong();
		}
		pTag.putLongArray("ScannedCablePositions", flatPosArray);

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
			for (long longVal : dirOrdinals(flatPosOrdinals(flatPosArray))) { // Decoding longs back into map nodes [3]
				map.getOrAddNode(BlockPos.of(longVal));
			}
		}

		commandCount = flowchart.components().size();
	}

	// Defensive typecast safety helper
	private long[] getLongArrayHelper(CompoundTag tag) {
		return tag.getLongArray("ScannedCablePositions");
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/**
	 * Instantiates and registers a new visual flow component to the manager,
	 * respecting the maximum performance thresholds configured server-side [3].
	 *
	 * @param type   the component type registry definition [3]
	 * @param player the player initiating the layout add action [3]
	 */
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

	/**
	 * Routes the serverbound canvas request dynamically according to the target
	 * CanvasAction [3].
	 *
	 * @param action      the canvas action execution target [3]
	 * @param componentId the UUID of the targeted component [3]
	 */
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
		// No-op: Components are locked in a compact canvas footprint.
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

	/**
	 * Broadcasts connection update packets to all observing clients [3].
	 *
	 * @param packet the sync connection wire packet [3]
	 */
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

	// Defensive deserialization mapper
	private long[] flatPosOrdinals(long[] arr) {
		return arr != null ? arr : new long[0];
	}

	private long[] dirOrdinals(long[] arr) {
		return arr;
	}
}