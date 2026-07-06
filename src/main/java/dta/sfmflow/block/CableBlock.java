package dta.sfmflow.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.common.network.PhysicalNetwork;
import dta.sfmflow.common.network.PhysicalNetworkMap;
import dta.sfmflow.common.network.NetworkMutationEngine;
import dta.sfmflow.common.network.CableNetworkRegistry;
import dta.sfmflow.registry.ModTags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Physical network medium that routes inventory scans to adjacent capability
 * providers [3]. Upgraded to support O(1) extension additions and in-memory
 * split-testing micro-BFS unlinking [3]. Enforces the Single Controller
 * Constraint to prevent bridging independent networks [3].
 */
public class CableBlock extends Block {
	public CableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (!level.isClientSide()) {
			ManagerBlockEntity manager = findConnectedManager(level, pos);
			if (manager != null) {
				PhysicalNetwork network = manager.getPhysicalNetwork();
				if (!network.isDirty()) {
					boolean touchesCapability = false;
					// Check if any newly connected neighbor exposes registry capabilities [3]
					for (Direction dir : Direction.values()) {
						BlockPos neighbor = pos.relative(dir);
						BlockState nState = level.getBlockState(neighbor);
						if (!nState.is(ModTags.CABLES) && !nState.is(ModBlocks.MANAGER_BLOCK.get())) {
							BlockEntity be = level.getBlockEntity(neighbor);
							for (var cap : dta.sfmflow.api.capability.FlowCapabilityRegistry.getRegisteredCapabilities().values()) {
								if (cap.isPresentAnywhere(level, neighbor, nState, be)) {
									touchesCapability = true;
									break;
								}
							}
						}
						if (touchesCapability)
							break;
					}

					if (!touchesCapability) {
						// O(1) extension optimization path [3]
						PhysicalNetworkMap map = network.getNetworkMap();
						int newId = map.getOrAddNode(pos);
						for (Direction dir : Direction.values()) {
							BlockPos neighbor = pos.relative(dir);
							int neighborId = map.getNodeId(neighbor);
							if (neighborId != -1) {
								map.addEdge(newId, neighborId);
							}
						}
						CableNetworkRegistry.registerCable(level, pos, manager.getBlockPos());
						super.onPlace(state, level, pos, oldState, isMoving);
						return;
					}
				}
			}
			markNearbyNetworksDirty(level, pos);
		}
		super.onPlace(state, level, pos, oldState, isMoving);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			super.onRemove(state, level, pos, newState, isMoving);

			if (!level.isClientSide()) {
				ManagerBlockEntity manager = findConnectedManager(level, pos);
				if (manager != null) {
					PhysicalNetwork network = manager.getPhysicalNetwork();
					if (!network.isDirty()) {
						PhysicalNetworkMap map = network.getNetworkMap();
						int deletedId = map.getNodeId(pos);
						if (deletedId != -1) {
							it.unimi.dsi.fastutil.ints.IntArrayList neighbors = map.getNeighbors(deletedId);
							if (neighbors != null && neighbors.size() >= 2) {
								boolean stillConnected = true;
								int firstNeighbor = neighbors.getInt(0);
								for (int i = 1; i < neighbors.size(); i++) {
									int otherNeighbor = neighbors.getInt(i);
									if (!NetworkMutationEngine.checkStillConnected(map, firstNeighbor, otherNeighbor)) {
										stillConnected = false;
										break;
									}
								}

								if (stillConnected) {
									map.removeNode(deletedId);
									CableNetworkRegistry.unregisterCable(level, pos);
									return;
								}
							} else if (neighbors == null || neighbors.size() < 2) {
								map.removeNode(deletedId);
								CableNetworkRegistry.unregisterCable(level, pos);
								return;
							}
						}
					}
				}
				CableNetworkRegistry.unregisterCable(level, pos);
				markNearbyNetworksDirty(level, pos);
			}
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			BlockPos neighborPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (!level.isClientSide()) {
			ManagerBlockEntity manager = findConnectedManager(level, pos);
			if (manager != null) {
				PhysicalNetwork network = manager.getPhysicalNetwork();
				if (!network.isDirty()) {
					PhysicalNetworkMap map = network.getNetworkMap();
					int currentId = map.getNodeId(pos);
					if (currentId != -1) {
						BlockState neighborState = level.getBlockState(neighborPos);
						if (neighborState.is(ModTags.CABLES)) {
							int neighborId = map.getOrAddNode(neighborPos);
							map.addEdge(currentId, neighborId);
							CableNetworkRegistry.registerCable(level, neighborPos, manager.getBlockPos());
							return;
						}
					}
				}
			}
			markNearbyNetworksDirty(level, pos);
		}
	}

	public static void markNearbyNetworksDirty(LevelAccessor level, BlockPos startPos) {
		if (level.isClientSide()) {
			return;
		}

		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		queue.add(startPos);
		visited.add(startPos);

		int maxSearch = 64;

		while (!queue.isEmpty() && visited.size() < maxSearch) {
			BlockPos current = queue.poll();

			for (Direction dir : Direction.values()) {
				BlockPos neighbor = current.relative(dir);
				if (visited.contains(neighbor)) {
					continue;
				}

				BlockState state = level.getBlockState(neighbor);

				if (state.is(ModBlocks.MANAGER_BLOCK.get())) {
					BlockEntity be = level.getBlockEntity(neighbor);
					if (be instanceof ManagerBlockEntity manager) {
						manager.getPhysicalNetwork().markDirty();
					}
				} else if (state.is(ModTags.CABLES)) {
					visited.add(neighbor);
					queue.add(neighbor);
				}
			}
		}
	}

	@Nullable
	public static ManagerBlockEntity findConnectedManager(LevelAccessor level, BlockPos startPos) {
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		queue.add(startPos);
		visited.add(startPos);

		int maxSearch = 64;

		while (!queue.isEmpty() && visited.size() < maxSearch) {
			BlockPos current = queue.poll();

			for (Direction dir : Direction.values()) {
				BlockPos neighbor = current.relative(dir);
				if (visited.contains(neighbor)) {
					continue;
				}

				BlockState state = level.getBlockState(neighbor);

				if (state.is(ModBlocks.MANAGER_BLOCK.get())) {
					BlockEntity be = level.getBlockEntity(neighbor);
					if (be instanceof ManagerBlockEntity manager) {
						return manager;
					}
				} else if (state.is(ModTags.CABLES)) {
					visited.add(neighbor);
					queue.add(neighbor);
				}
			}
		}
		return null;
	}
}