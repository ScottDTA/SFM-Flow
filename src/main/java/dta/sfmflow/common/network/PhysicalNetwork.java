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
 */
public class PhysicalNetwork
 {
  private final Set<BlockPos> scannedCables = new HashSet<>();
  private final List<ConnectionBlock> scannedInventories = new ArrayList<>();
  private long lastScanTime = 0L;
  private boolean isDirty = true;

  public PhysicalNetwork()
   {
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
      return; // Under cooldown restrictions [3]
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
   */
  private void performScan(Level level, BlockPos startPos)
   {
    long startTime = System.nanoTime();

    this.scannedCables.clear();
    this.scannedInventories.clear();

    Set<BlockPos> visited = new HashSet<>();
    Queue<ScanNode> queue = new ArrayDeque<>();

    // Seed BFS queue with starting neighbors
    visited.add(startPos);
    for (Direction dir : Direction.values())
     {
      BlockPos adjacent = startPos.relative(dir);
      BlockState state = level.getBlockState(adjacent);
      if (state.is(ModTags.CABLES))
       {
        visited.add(adjacent);
        queue.add(new ScanNode(adjacent, 1));

        // Evaluate starting cables if they are tagged as custom redstone/sensor targets [3]
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

      if (current.depth() >= maxDepth)
       {
        continue; // Depth limit reached [3]
       }

      for (Direction dir : Direction.values())
       {
        BlockPos neighbor = current.pos().relative(dir);
        if (visited.contains(neighbor))
         {
          continue;
         }

        BlockState state = level.getBlockState(neighbor);

        if (state.is(ModTags.CABLES))
         {
          visited.add(neighbor);
          queue.add(new ScanNode(neighbor, current.depth() + 1));

          // Evaluate cable-based redstone blocks as targets without stopping BFS search propagation [3]
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
   */
  private void evaluateAndAddInventory(Level level, BlockPos pos, BlockState state, int depth, Set<BlockPos> visited, boolean isCableComponent)
   {
    if (!isCableComponent)
     {
      visited.add(pos); // Mark target as visited to block traversal beyond standard boundaries [3]
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

    // Explicitly add redstone classification for tags representing emitters, receivers, or custom signal cables [3]
    if (state.is(ModTags.REDSTONE_CABLES))
     {
      discoveredTypes.add(ConnectionBlockType.REDSTONE);
     }

    if (!discoveredTypes.isEmpty())
     {
      ConnectionBlock connection = new ConnectionBlock(pos, depth);
      connection.setTypes(discoveredTypes);
      
      // Connection ID offset [3]
      int variablesOffset = VariableColor.values().length;
      connection.setId(variablesOffset + this.scannedInventories.size());

      this.scannedInventories.add(connection);
     }
   }
 }