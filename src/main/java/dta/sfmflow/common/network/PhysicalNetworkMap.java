package dta.sfmflow.common.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;

/**
 * Backing graph map that converts coordinates to primitive ID sequences [3].
 * Guarantees garbage-free traversal bounds by decoupling BlockPos objects [3].
 */
public class PhysicalNetworkMap {
    private final Int2ObjectOpenHashMap<BlockPos> idToPos = new Int2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<BlockPos> posToId = new Object2IntOpenHashMap<>();
    private final Int2ObjectOpenHashMap<IntArrayList> adjacencyList = new Int2ObjectOpenHashMap<>();
    private int nextNodeId = 0;

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

    public void removeNode(int id) {
        BlockPos pos = idToPos.remove(id);
        if (pos != null) {
            posToId.removeInt(pos);
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
    }

    public void clear() {
        this.idToPos.clear();
        this.posToId.clear();
        this.adjacencyList.clear();
        this.nextNodeId = 0;
    }

    public int size() {
        return idToPos.size();
    }
}