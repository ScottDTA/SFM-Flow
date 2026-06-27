package dta.sfmflow.common.network;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.BitSet;

/**
 * Handles fast graph mutation validations without triggering slow world rescans [3].
 */
public class NetworkMutationEngine {

    private NetworkMutationEngine() {}

    /**
     * Performs an in-memory primitive micro-BFS to check if a redundant connection path remains [3].
     * Avoids reading physical world blocks, keeping the check completely CPU/RAM-bound [3].
     *
     * @param map the graph map reference [3]
     * @param startNode starting primitive node ID [3]
     * @param targetNode destination target node ID [3]
     * @return true if a redundant path remains active [3]
     */
    public static boolean checkStillConnected(PhysicalNetworkMap map, int startNode, int targetNode) {
        if (startNode == targetNode) return true;

        BitSet visited = new BitSet(map.size());
        IntArrayList queue = new IntArrayList();

        queue.add(startNode);
        visited.set(startNode);

        int head = 0;
        while (head < queue.size()) {
            int current = queue.getInt(head++);
            if (current == targetNode) {
                return true;
            }

            IntArrayList neighbors = map.getNeighbors(current);
            if (neighbors != null) {
                for (int i = 0; i < neighbors.size(); i++) {
                    int neighbor = neighbors.getInt(i);
                    if (!visited.get(neighbor)) {
                        visited.set(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return false;
    }
}