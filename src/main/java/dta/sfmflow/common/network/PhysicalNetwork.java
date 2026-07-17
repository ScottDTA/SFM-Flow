package dta.sfmflow.common.network;

import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.CableClusterBlockEntity;
import dta.sfmflow.registry.ModTags;
import dta.sfmflow.util.ConnectionBlock;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import java.util.*;

/**
 * Manages topological scanning and target inventory indices. Extracted to
 * cleanly separate physical networks from logical operations. Upgraded to index
 * capability nodes and cache BlockCapabilityCache handlers safely.
 */
public class PhysicalNetwork {
	private final Set<BlockPos> scannedCables = new HashSet<>();
	private final List<ConnectionBlock> scannedInventories = new java.util.concurrent.CopyOnWriteArrayList<>();
	private final PhysicalNetworkMap networkMap = new PhysicalNetworkMap();
	private long lastScanTime = 0L;
	private boolean isDirty = true;

	public PhysicalNetwork() {
	}

	public PhysicalNetworkMap getNetworkMap() {
		return this.networkMap;
	}

	public boolean isDirty() {
		return this.isDirty;
	}

	/**
	 * Fast spatial boundary unloader that flags all node IDs inside the unloaded
	 * chunk as sleeping.
	 *
	 * @param chunkPacked packed long representation
	 */
	public void handleChunkUnload(long chunkPacked) {
		IntArrayList nodesInChunk = this.networkMap.getNodesInChunk(chunkPacked);
		if (nodesInChunk != null) {
			for (int i = 0; i < nodesInChunk.size(); i++) {
				int nodeId = nodesInChunk.getInt(i);
				this.networkMap.setNodeSleeping(nodeId, true);
			}
		}
	}

	/**
	 * Throttled ticking loop protecting server performance. Skips evaluations on
	 * active scan cooldowns.
	 *
	 * @param level    physical server level
	 * @param startPos starting block position
	 * @return true if a full network rescan was executed
	 */
	public boolean tickCheckAndScan(Level level, BlockPos startPos) {
		if (level.isClientSide()) {
			return false;
		}

		if (level.getGameTime() - lastScanTime < ServerConfig.NETWORK_SCAN_COOLDOWN.get()) {
			return false;
		}

		if (isDirty) {
			performScan(level, startPos);
			return true;
		}
		return false;
	}

	public void markDirty() {
		this.isDirty = true;
	}

	public List<ConnectionBlock> getScannedInventories() {
		return scannedInventories;
	}

	private void performScan(Level level, BlockPos startPos) {
		long startTime = System.nanoTime();

		// Cleanly unregister previously scanned cables to prevent stale dangling pointer leaks [3]
		for (BlockPos oldCable : this.scannedCables) {
			CableNetworkRegistry.unregisterCable(level, oldCable);
		}
		this.scannedCables.clear();
		this.scannedInventories.clear();
		this.networkMap.clear();

		BitSet visited = new BitSet();
		Queue<ScanNode> queue = new ArrayDeque<>();

		int startId = this.networkMap.getOrAddNode(startPos);
		visited.set(startId);

		// Seed BFS queue with starting neighbors
		for (Direction dir : Direction.values()) {
			BlockPos adjacent = startPos.relative(dir);
			BlockState state = level.getBlockState(adjacent);
			
			// Only allow conductive cables/clusters to extend the pathfinding search [3]
			if (canConductNetwork(level, adjacent, state)) {
				int adjacentId = this.networkMap.getOrAddNode(adjacent);
				visited.set(adjacentId);
				this.networkMap.addEdge(startId, adjacentId);
				CableNetworkRegistry.registerCable(level, adjacent, startPos);

				queue.add(new ScanNode(adjacent, 1));
			} else if (!state.is(ModBlocks.MANAGER_BLOCK.get())) {
				evaluateAndAddInventory(level, adjacent, state, 1, visited, false);
			}
		}

		int maxDepth = ServerConfig.MAX_CABLE_LENGTH.get();
		int maxInventories = ServerConfig.MAX_CONNECTED_INVENTORIES.get();

		while (!queue.isEmpty()) {
			ScanNode current = queue.poll();
			this.scannedCables.add(current.pos());
			int currentId = this.networkMap.getNodeId(current.pos());
			CableNetworkRegistry.registerCable(level, current.pos(), startPos);

			if (current.depth() >= maxDepth) {
				continue;
			}

			for (Direction dir : Direction.values()) {
				BlockPos neighbor = current.pos().relative(dir);
				int neighborId = this.networkMap.getNodeId(neighbor);

				if (neighborId != -1 && visited.get(neighborId)) {
					this.networkMap.addEdge(currentId, neighborId);
					continue;
				}

				BlockState state = level.getBlockState(neighbor);

				// Only allow conductive cables/clusters to extend the pathfinding search [3]
				if (canConductNetwork(level, neighbor, state)) {
					int newNeighborId = this.networkMap.getOrAddNode(neighbor);
					visited.set(newNeighborId);
					this.networkMap.addEdge(currentId, newNeighborId);

					queue.add(new ScanNode(neighbor, current.depth() + 1));
				} else if (this.scannedInventories.size() < maxInventories
						&& !state.is(ModBlocks.MANAGER_BLOCK.get())) {
					evaluateAndAddInventory(level, neighbor, state, current.depth() + 1, visited, false);
				}
			}
		}

		this.isDirty = false;
		this.lastScanTime = level.getGameTime();

		long durationNs = System.nanoTime() - startTime;
		double durationMs = durationNs / 1_000_000.0;

		FlowLogger.pathfinder("Scan completed in %.3f ms. Cables: %d, Targets: %d", durationMs,
				this.scannedCables.size(), this.scannedInventories.size());
	}

	/**
	 * Helper determining if a block is a conductive medium that can extend the network pathfinding search.
	 */
	private boolean canConductNetwork(Level level, BlockPos pos, BlockState state) {
		if (state.is(ModTags.CONDUCTIVE_CABLES)) {
			return true;
		}
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof CableClusterBlockEntity cluster) {
			return cluster.hasCableCard();
		}
		return false;
	}

	private boolean isClusterAndMissingCable(Level level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof CableClusterBlockEntity cluster) {
			return !cluster.hasCableCard();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void evaluateAndAddInventory(Level level, BlockPos pos, BlockState state, int depth, BitSet visited,
			boolean isCableComponent) {
		int posId = this.networkMap.getOrAddNode(pos);

		if (!isCableComponent) {
			visited.set(posId);
		}

		BlockEntity be = level.getBlockEntity(pos);

		// If the block is a Cable Cluster, index and add each active card independently
		if (be instanceof CableClusterBlockEntity cluster) {
			int numSlots = cluster.getNumSlots();
			for (int slotIndex = 0; slotIndex < numSlots; slotIndex++) {
				ItemStack card = cluster.getInventory().getStackInSlot(slotIndex);
				if (card.isEmpty()) {
					continue;
				}

				Set<ResourceLocation> cardCaps = new HashSet<>();
				if (cluster.isItemCard(slotIndex)) {
					cardCaps.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"));
				} else if (cluster.isFluidCard(slotIndex)) {
					cardCaps.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"));
				}

				// Map redstone emitter/receiver cards
				if (card.is(ModBlocks.REDSTONE_EMITTER_BLOCK.get().asItem()) ||
						card.is(ModBlocks.REDSTONE_RECEIVER_BLOCK.get().asItem())) {
					cardCaps.add(ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone"));
				}

				if (!cardCaps.isEmpty()) {
					Direction cardDir = cluster.getSlotDirection(slotIndex);
					ConnectionBlock connection = new ConnectionBlock(level, pos, depth, slotIndex, card.copy(), cardDir);
					connection.setTypes(cardCaps);
					connection.setId(Objects.hash(pos, slotIndex));

					if (level instanceof ServerLevel serverLevel) {
						for (ResourceLocation capId : cardCaps) {
							var flowCap = FlowCapabilityRegistry.get(capId);
							if (flowCap != null && flowCap.getCapability() != null) {
								var nullCache = BlockCapabilityCache.create(
										(BlockCapability<Object, Direction>) flowCap.getCapability(), serverLevel, pos, null);
								connection.registerCache(capId, null, nullCache);

								for (Direction dir : Direction.values()) {
									var dirCache = BlockCapabilityCache.create(
											(BlockCapability<Object, Direction>) flowCap.getCapability(), serverLevel, pos, dir);
									connection.registerCache(capId, dir, dirCache);
								}
							}
						}
					}
					this.scannedInventories.add(connection);
				}
			}
			return; // Skip adding the Cable Cluster block itself
		}

		Set<ResourceLocation> discoveredTypes = new HashSet<>();

		// Query dynamically registered capabilities in our FlowCapabilityRegistry
		for (var entry : FlowCapabilityRegistry.getRegisteredCapabilities().entrySet()) {
			ResourceLocation capId = entry.getKey();
			var cap = entry.getValue();
			if (cap.isPresentAnywhere(level, pos, state, be)) {
				discoveredTypes.add(capId);
				this.networkMap.indexCapability(capId, posId);
			}
		}

		// Special redstone-cables tag fallback to register redstone pseudo-capability
		if (state.is(ModTags.REDSTONE_CABLES)) {
			ResourceLocation redstoneCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone");
			discoveredTypes.add(redstoneCapId);
			this.networkMap.indexCapability(redstoneCapId, posId);
		}
		
		// Explicitly check for Sculk Trigger Cable blocks [3]
				if (state.is(ModBlocks.SCULK_TRIGGER_CABLE_BLOCK.get())) {
					ResourceLocation sculkCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "sculk");
					discoveredTypes.add(sculkCapId);
					this.networkMap.indexCapability(sculkCapId, posId);
				}

		if (!discoveredTypes.isEmpty()) {
			Direction blockDir = null;
			if (state.hasProperty(BlockStateProperties.FACING)) {
				blockDir = state.getValue(BlockStateProperties.FACING);
			} else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
				blockDir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			}

			ConnectionBlock connection = new ConnectionBlock(level, pos, depth, -1, ItemStack.EMPTY, blockDir);
			connection.setTypes(discoveredTypes);

			if (level instanceof ServerLevel serverLevel) {
				// Dynamically allocate and register BlockCapabilityCache for each discovered
				// capability across all 6 directions plus the non-directional side context
				for (ResourceLocation capId : discoveredTypes) {
					var flowCap = FlowCapabilityRegistry.get(capId);
					if (flowCap != null && flowCap.getCapability() != null) {
						// Register non-directional cache [3]
						var nullCache = BlockCapabilityCache.create(
								(BlockCapability<Object, Direction>) flowCap.getCapability(), serverLevel, pos, null);
						connection.registerCache(capId, null, nullCache);

						// Register direction-specific caches
						for (Direction dir : Direction.values()) {
							var dirCache = BlockCapabilityCache.create(
									(BlockCapability<Object, Direction>) flowCap.getCapability(), serverLevel, pos,
									dir);
							connection.registerCache(capId, dir, dirCache);
						}
					}
				}
			}

			// Symmetrical Persistent ID Assignment: use the coordinate hashcode to prevent binding drift
			connection.setId(pos.hashCode());

			this.scannedInventories.add(connection);
		}
	}
}