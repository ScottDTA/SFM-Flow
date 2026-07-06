package dta.sfmflow.common.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Backing graph map that converts coordinates to primitive ID sequences [3].
 * Guarantees garbage-free traversal bounds by decoupling BlockPos objects [3].
 * Upgraded to track spatial chunk groups, sleeping nodes, and capability lists
 * [3].
 */
public class PhysicalNetworkMap {
	private final Int2ObjectOpenHashMap<BlockPos> idToPos = new Int2ObjectOpenHashMap<>();
	private final Object2IntOpenHashMap<BlockPos> posToId = new Object2IntOpenHashMap<>();
	private final Int2ObjectOpenHashMap<IntArrayList> adjacencyList = new Int2ObjectOpenHashMap<>();
	private int nextNodeId = 0;

	// Spatial Chunk-Boundary Groupings [3]
	private final Long2ObjectOpenHashMap<IntArrayList> chunkToNodeGroup = new Long2ObjectOpenHashMap<>();
	private final BitSet sleepingNodes = new BitSet();

	// Multi-Capability Sorting Indices [3]
	private final Map<ResourceLocation, IntArrayList> capabilityIndices = new HashMap<>();

	/**
	 * Initializes the PhysicalNetworkMap [3].
	 */
	public PhysicalNetworkMap() {
		this.posToId.defaultReturnValue(-1); // Safety bounds: represent unassigned positions
	}

	public int getOrAddNode(BlockPos pos) {
		int id = posToId.getInt(pos);
		if (id == -1) {
			id = nextNodeId++;
			posToId.put(pos, id);
			idToPos.put(id, pos);
			adjacencyList.put(id, new IntArrayList());

			// Track spatial chunk coordinate grouping [3]
			long chunkKey = ChunkPos.asLong(pos);
			this.chunkToNodeGroup.computeIfAbsent(chunkKey, k -> new IntArrayList()).add(id);
		}
		return id;
	}

	public int getNodeId(BlockPos pos) {
		return posToId.getInt(pos);
	}

	public BlockPos getPos(int id) {
		return idToPos.get(id);
	}

	public IntArrayList getNeighbors(int id) {
		return adjacencyList.get(id);
	}

	public void addEdge(int nodeA, int nodeB) {
		IntArrayList listA = adjacencyList.get(nodeA);
		if (listA != null && !listA.contains(nodeB)) {
			listA.add(nodeB);
		}
		IntArrayList listB = adjacencyList.get(nodeB);
		if (listB != null && !listB.contains(nodeA)) {
			listB.add(nodeA);
		}
	}

	/**
	 * Registers a node ID under its scanned capability classification list [3].
	 *
	 * @param type the capability classification ResourceLocation [3]
	 * @param id   the registered node ID [3]
	 */
	public void indexCapability(ResourceLocation type, int id) {
		capabilityIndices.computeIfAbsent(type, k -> new IntArrayList()).add(id);
	}

	/**
	 * Retrieves all registered node IDs matching a target capability classification
	 * [3].
	 *
	 * @param type capability classification ResourceLocation [3]
	 * @return primitive list of node IDs [3]
	 */
	public IntArrayList getNodesWithCapability(ResourceLocation type) {
		return capabilityIndices.getOrDefault(type, new IntArrayList());
	}

	/**
	 * Configures the sleeping state for a specific node ID [3].
	 *
	 * @param id       node ID target [3]
	 * @param sleeping true to suspend the node during chunk unloads [3]
	 */
	public void setNodeSleeping(int id, boolean sleeping) {
		if (sleeping) {
			sleepingNodes.set(id);
		} else {
			sleepingNodes.clear(id);
		}
	}

	/**
	 * Checks if a specific node ID is currently flagged as sleeping [3].
	 *
	 * @param id node ID query [3]
	 * @return true if sleeping [3]
	 */
	public boolean isNodeSleeping(int id) {
		return sleepingNodes.get(id);
	}

	/**
	 * Retrieves all registered node IDs residing inside a specific packed chunk
	 * coordinate [3].
	 *
	 * @param chunkPacked packed long representation [3]
	 * @return primitive list of node IDs [3]
	 */
	public IntArrayList getNodesInChunk(long chunkPacked) {
		return chunkToNodeGroup.get(chunkPacked);
	}

	public void removeNode(int id) {
		BlockPos pos = idToPos.remove(id);
		if (pos != null) {
			posToId.removeInt(pos);
			long chunkKey = ChunkPos.asLong(pos);
			IntArrayList chunkGroup = chunkToNodeGroup.get(chunkKey);
			if (chunkGroup != null) {
				chunkGroup.rem(id);
			}
		}
		IntArrayList neighbors = adjacencyList.remove(id);
		if (neighbors != null) {
			for (int i = 0; i < neighbors.size(); i++) {
				int neighborId = neighbors.getInt(i);
				IntArrayList neighborList = adjacencyList.get(neighborId);
				if (neighborList != null) {
					neighborList.rem(id); // fastutil primitive list value removal
				}
			}
		}
		for (IntArrayList list : capabilityIndices.values()) {
			list.rem(id);
		}
		sleepingNodes.clear(id);
	}

	public Collection<BlockPos> getAllPositions() {
		return idToPos.values();
	}

	public void clear() {
		this.idToPos.clear();
		this.posToId.clear();
		this.adjacencyList.clear();
		this.chunkToNodeGroup.clear();
		this.sleepingNodes.clear();
		for (IntArrayList list : this.capabilityIndices.values()) {
			list.clear();
		}
		this.nextNodeId = 0;
	}

	public int size() {
		return idToPos.size();
	}
}