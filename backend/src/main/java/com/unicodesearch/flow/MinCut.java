package com.unicodesearch.flow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Minimum s-t cut from a completed maximum flow (CO4).
 *
 * After max flow, let S = set of nodes reachable from s in the RESIDUAL graph.
 * The edges going from S to V\S are all saturated and form a minimum cut; by
 * the max-flow min-cut theorem their total capacity equals the max-flow value.
 *
 * Time: O(V + E).  Space: O(V).
 */
public final class MinCut {

    public record Cut(boolean[] sourceSide, List<Integer> cutEdgeIds, int capacity) { }

    private MinCut() { }

    public static Cut compute(FlowNetwork g, int s) {
        boolean[] reach = new boolean[g.getNodeCount()];
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(s);
        reach[s] = true;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int id : g.adjacent(u)) {
                FlowNetwork.Edge e = g.edge(id);
                if (!reach[e.to] && e.residual() > 0) {
                    reach[e.to] = true;
                    queue.add(e.to);
                }
            }
        }
        List<Integer> cutEdges = new ArrayList<>();
        int capacity = 0;
        for (int k = 0; k < g.getEdgeCount(); k++) {
            int id = g.forwardEdgeIdAt(k);
            FlowNetwork.Edge e = g.edge(id);
            if (reach[e.from] && !reach[e.to]) {
                cutEdges.add(id);
                capacity += e.capacity;
            }
        }
        return new Cut(reach, cutEdges, capacity);
    }
}
