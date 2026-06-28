package dta.sfmflow.common.network;

import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.registry.ModTags;
import dta.sfmflow.util.ConnectionBlock;
import dta.sfmflow.util.ConnectionBlockType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.*;

/**
 * Manages topological scanning and target inventory indices [3].
 * Extracted to cleanly separate physical networks from logical operations [3].
 * Upgraded to index capability nodes and cache BlockCapabilityCache handlers safely [3].
 */
public class PhysicalNetwork {
  private final Set<BlockPos> scannedCables = new HashSet<>();
  private final List<ConnectionBlock> scannedInventories = new ArrayList<>();
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
   * Fast spatial boundary unloader that flags all node IDs inside the unloaded chunk as sleeping [3].
   *
   * @param chunkPacked packed long representation [3]
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
   * Throttled ticking loop protecting server performance [3].
   * Skips evaluations on active scan cooldowns [3].
   *
   * @param level physical server level [3]
   * @param startPos starting block position [3]
   * @return true if a full network rescan was executed [3]
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
      return true; // Return true to notify manager block of completed scans [3]
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
        dta.sfmflow.common.network.CableNetworkRegistry.unregisterCable(level, oldCable);
    }
    this.scannedCables.clear();
    this.scannedInventories.clear();
    this.networkMap.clear();

    java.util.BitSet visited = new java.util.BitSet();
    Queue<ScanNode> queue = new ArrayDeque<>();

    int startId = this.networkMap.getOrAddNode(startPos);
    visited.set(startId);

    // Seed BFS queue with starting neighbors
    for (Direction dir : Direction.values()) {
      BlockPos adjacent = startPos.relative(dir);
      BlockState state = level.getBlockState(adjacent);
      if (state.is(ModTags.CABLES)) {
        int adjacentId = this.networkMap.getOrAddNode(adjacent);
        visited.set(adjacentId);
        this.networkMap.addEdge(startId, adjacentId);
        dta.sfmflow.common.network.CableNetworkRegistry.registerCable(level, adjacent, startPos);

        queue.add(new ScanNode(adjacent, 1));

        if (state.is(ModTags.REDSTONE_CABLES)) {
          evaluateAndAddInventory(level, adjacent, state, 1, visited, true);
         }
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
      dta.sfmflow.common.network.CableNetworkRegistry.registerCable(level, current.pos(), startPos);

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

        if (state.is(ModTags.CABLES)) {
          int newNeighborId = this.networkMap.getOrAddNode(neighbor);
          visited.set(newNeighborId);
          this.networkMap.addEdge(currentId, newNeighborId);

          queue.add(new ScanNode(neighbor, current.depth() + 1));

          if (state.is(ModTags.REDSTONE_CABLES)) {
            evaluateAndAddInventory(level, neighbor, state, current.depth() + 1, visited, true);
           }
         } else if (this.scannedInventories.size() < maxInventories && !state.is(ModBlocks.MANAGER_BLOCK.get())) {
          evaluateAndAddInventory(level, neighbor, state, current.depth() + 1, visited, false);
         }
       }
     }

    this.isDirty = false;
    this.lastScanTime = level.getGameTime();

    long durationNs = System.nanoTime() - startTime;
    double durationMs = durationNs / 1_000_000.0;

    FlowLogger.pathfinder("Scan completed in %.3f ms. Cables: %d, Targets: %d", 
                          durationMs, this.scannedCables.size(), this.scannedInventories.size());
   }

  private void evaluateAndAddInventory(Level level, BlockPos pos, BlockState state, int depth, java.util.BitSet visited, boolean isCableComponent) {
    int posId = this.networkMap.getOrAddNode(pos);

    if (!isCableComponent) {
      visited.set(posId);
     }

    BlockEntity be = level.getBlockEntity(pos);
    EnumSet<ConnectionBlockType> discoveredTypes = EnumSet.noneOf(ConnectionBlockType.class);

    for (ConnectionBlockType type : ConnectionBlockType.values()) {
      if (type.isPresentAnywhere(level, pos, state, be)) {
        discoveredTypes.add(type);
        this.networkMap.indexCapability(type, posId);
       }
     }

    if (state.is(ModTags.REDSTONE_CABLES)) {
      discoveredTypes.add(ConnectionBlockType.REDSTONE);
      this.networkMap.indexCapability(ConnectionBlockType.REDSTONE, posId);
     }

    if (!discoveredTypes.isEmpty()) {
      ConnectionBlock connection = new ConnectionBlock(pos, depth);
      connection.setTypes(discoveredTypes);
      
      if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
          connection.setItemCache(BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, serverLevel, pos, Direction.UP));
          connection.setFluidCache(BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, serverLevel, pos, Direction.UP));
      }

      // Symmetrical Persistent ID Assignment: use the coordinate hashcode to prevent binding drift [3]
      connection.setId(pos.hashCode());

      this.scannedInventories.add(connection);
     }
   }
}