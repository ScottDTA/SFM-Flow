package dta.sfmflow.common.network;

import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.registry.ModTags;
import dta.sfmflow.util.ConnectionBlock;
import dta.sfmflow.util.ConnectionBlockType;
import dta.sfmflow.util.VariableColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * Manages topological scanning and target inventory indices [3].
 * Extracted to cleanly separate physical networks from logical operations [3].
 * Upgraded to use primitive-dense graph map structures and BitSet visited masks [3].
 */
public class PhysicalNetwork
 {
  private final Set<BlockPos> scannedCables = new HashSet<>();
  private final List<ConnectionBlock> scannedInventories = new ArrayList<>();
  private final PhysicalNetworkMap networkMap = new PhysicalNetworkMap();
  private long lastScanTime = 0L;
  private boolean isDirty = true;

  public PhysicalNetwork()
   {
   }

  /**
   * Retrieves the backing graph map converting coordinates to primitive ID sequences [3].
   *
   * @return graph map reference [3]
   */
  public PhysicalNetworkMap getNetworkMap() {
      return this.networkMap;
  }

  /**
   * Checks if the active topological cache is currently flagged as dirty [3].
   *
   * @return true if marked dirty [3]
   */
  public boolean isDirty() {
      return this.isDirty;
  }

  /**
   * Throttled ticking loop protecting server performance [3].
   * Skips evaluations on active scan cooldowns [3].
   *
   * @param level physical server level [3]
   * @param startPos starting block position [3]
   */
  public void tickCheckAndScan(Level level, BlockPos startPos)
   {
    if (level.isClientSide())
     {
      return;
     }

    if (level.getGameTime() - lastScanTime < ServerConfig.NETWORK_SCAN_COOLDOWN.get())
     {
      return;
     }

    if (isDirty)
     {
      performScan(level, startPos);
     }
   }

  /**
   * Marks the topological cache as dirty [3].
   */
  public void markDirty()
   {
    this.isDirty = true;
   }

  public List<ConnectionBlock> getScannedInventories()
   {
    return scannedInventories;
   }

  /**
   * Core non-blocking BFS pathfinder [3].
   * Maps valid capability-providing targets without storing memory-leaking handler references [3].
   * Leverages fastutil primitive mapping layers and BitSet visited masks [3].
   */
  private void performScan(Level level, BlockPos startPos)
   {
    long startTime = System.nanoTime();

    this.scannedCables.clear();
    this.scannedInventories.clear();
    this.networkMap.clear(); // Clear graph map first [3]

    java.util.BitSet visited = new java.util.BitSet();
    Queue<ScanNode> queue = new ArrayDeque<>();

    int startId = this.networkMap.getOrAddNode(startPos);
    visited.set(startId);

    // Seed BFS queue with starting neighbors
    for (Direction dir : Direction.values())
     {
      BlockPos adjacent = startPos.relative(dir);
      BlockState state = level.getBlockState(adjacent);
      if (state.is(ModTags.CABLES))
       {
        int adjacentId = this.networkMap.getOrAddNode(adjacent);
        visited.set(adjacentId);
        this.networkMap.addEdge(startId, adjacentId);

        queue.add(new ScanNode(adjacent, 1));

        if (state.is(ModTags.REDSTONE_CABLES))
         {
          evaluateAndAddInventory(level, adjacent, state, 1, visited, true);
         }
       }
      else if (!state.is(ModBlocks.MANAGER_BLOCK.get()))
       {
        evaluateAndAddInventory(level, adjacent, state, 1, visited, false);
       }
     }

    int maxDepth = ServerConfig.MAX_CABLE_LENGTH.get();
    int maxInventories = ServerConfig.MAX_CONNECTED_INVENTORIES.get();

    while (!queue.isEmpty())
     {
      ScanNode current = queue.poll();
      this.scannedCables.add(current.pos());
      int currentId = this.networkMap.getNodeId(current.pos());

      if (current.depth() >= maxDepth)
       {
        continue;
       }

      for (Direction dir : Direction.values())
       {
        BlockPos neighbor = current.pos().relative(dir);
        int neighborId = this.networkMap.getNodeId(neighbor);

        if (neighborId != -1 && visited.get(neighborId))
         {
          this.networkMap.addEdge(currentId, neighborId);
          continue;
         }

        BlockState state = level.getBlockState(neighbor);

        if (state.is(ModTags.CABLES))
         {
          int newNeighborId = this.networkMap.getOrAddNode(neighbor);
          visited.set(newNeighborId);
          this.networkMap.addEdge(currentId, newNeighborId);

          queue.add(new ScanNode(neighbor, current.depth() + 1));

          if (state.is(ModTags.REDSTONE_CABLES))
           {
            evaluateAndAddInventory(level, neighbor, state, current.depth() + 1, visited, true);
           }
         }
        else if (this.scannedInventories.size() < maxInventories && !state.is(ModBlocks.MANAGER_BLOCK.get()))
         {
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

  /**
   * Evaluates if a target pos is a capability provider or specialized redstone block and creates a ConnectionBlock [3].
   * Bypasses further pathfinding traversal beyond the target boundary if it is a standard terminal node [3].
   *
   * @param level level context [3]
   * @param pos coordinate positions [3]
   * @param state block behavior parameters [3]
   * @param depth search depth [3]
   * @param visited node ID visited index [3]
   * @param isCableComponent if true, registers as part of the cable graph [3]
   */
  private void evaluateAndAddInventory(Level level, BlockPos pos, BlockState state, int depth, java.util.BitSet visited, boolean isCableComponent)
   {
    if (!isCableComponent)
     {
      int posId = this.networkMap.getOrAddNode(pos);
      visited.set(posId); // Mark target as visited to block traversal beyond standard boundaries [3]
     }

    BlockEntity be = level.getBlockEntity(pos);
    EnumSet<ConnectionBlockType> discoveredTypes = EnumSet.noneOf(ConnectionBlockType.class);

    for (ConnectionBlockType type : ConnectionBlockType.values())
     {
      if (type.isPresentAnywhere(level, pos, state, be))
       {
        discoveredTypes.add(type);
       }
     }

    if (state.is(ModTags.REDSTONE_CABLES))
     {
      discoveredTypes.add(ConnectionBlockType.REDSTONE);
     }

    if (!discoveredTypes.isEmpty())
     {
      ConnectionBlock connection = new ConnectionBlock(pos, depth);
      connection.setTypes(discoveredTypes);
      
      int variablesOffset = VariableColor.values().length;
      connection.setId(variablesOffset + this.scannedInventories.size());

      this.scannedInventories.add(connection);
     }
   }
 }