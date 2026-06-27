package dta.sfmflow.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.registry.ModTags;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Physical network medium that routes inventory scans to adjacent capability providers [3].
 */
public class CableBlock extends Block
 {
  public CableBlock(Properties properties)
   {
    super(properties);
   }

  @Override
  protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving)
   {
    super.onPlace(state, level, pos, oldState, isMoving);
    markNearbyNetworksDirty(level, pos);
   }

  @Override
  protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
   {
    if (!state.is(newState.getBlock()))
     {
      super.onRemove(state, level, pos, newState, isMoving);
      markNearbyNetworksDirty(level, pos);
     }
   }

  @Override
  protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston)
   {
    super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    markNearbyNetworksDirty(level, pos);
   }

  /**
   * Fast, safe BFS search tracing adjacent cable pathways to flag any connected Managers as dirty [3].
   * Bounded to a conservative search depth to eliminate TPS stalls during neighbor cascades [3].
   * Exposed as static to allow advanced network blocks to cleanly trigger topology invalidations [3].
   *
   * @param level level accessor instance [3]
   * @param startPos block position to start tracing from [3]
   */
  public static void markNearbyNetworksDirty(LevelAccessor level, BlockPos startPos)
   {
    if (level.isClientSide())
     {
      return;
     }

    Set<BlockPos> visited = new HashSet<>();
    Queue<BlockPos> queue = new ArrayDeque<>();
    queue.add(startPos);
    visited.add(startPos);

    int maxSearch = 64; // Bounded search limit to preserve server performance [3]

    while (!queue.isEmpty() && visited.size() < maxSearch)
     {
      BlockPos current = queue.poll();

      for (Direction dir : Direction.values())
       {
        BlockPos neighbor = current.relative(dir);
        if (visited.contains(neighbor))
         {
          continue;
         }

        BlockState state = level.getBlockState(neighbor);

        if (state.is(ModBlocks.MANAGER_BLOCK.get()))
         {
          BlockEntity be = level.getBlockEntity(neighbor);
          if (be instanceof ManagerBlockEntity manager)
           {
            manager.getPhysicalNetwork().markDirty();
           }
         }
        else if (state.is(ModTags.CABLES))
         {
          visited.add(neighbor);
          queue.add(neighbor);
         }
       }
     }
   }
 }