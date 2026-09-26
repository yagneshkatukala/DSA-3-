package com.unicodesearch.flow;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * Edmonds-Karp max flow (CO4): Ford-Fulkerson where the augmenting path is
 * always the SHORTEST one (fewest edges), found with breadth-first search.
 * Choosing shortest paths bounds the number of augmentations by O(V*E)
 * independent of the capacities.
 *
 * Time: O(V * E^2).   Space: O(V).
 */
public final class EdmondsKarp {

    private EdmondsKarp() { }

    public static FlowResult run(FlowNetwork g, int s, int t) {
        long start = System.nanoTime();
        g.resetFlow();
        int flow = 0, augmentations = 0;
        int[] parentEdge = new int[g.getNodeCount()];

        while (true) {
            Arrays.fill(parentEdge, -1);
            Queue<Integer> queue = new ArrayDeque<>();
            queue.add(s);
            boolean[] seen = new boolean[g.getNodeCount()];
            seen[s] = true;
            while (!queue.isEmpty() && !seen[t]) {
                int u = queue.poll();
                for (int id : g.adjacent(u)) {
                    FlowNetwork.Edge e = g.edge(id);
                    if (!seen[e.to] && e.residual() > 0) {
                        seen[e.to] = true;
                        parentEdge[e.to] = id;
                        queue.add(e.to);
                    }
                }
            }
            if (!seen[t]) break;

            int bottleneck = Integer.MAX_VALUE;
            for (int v = t; v != s; v = g.edge(parentEdge[v]).from) {
                bottleneck = Math.min(bottleneck, g.edge(parentEdge[v]).residual());
            }
            for (int v = t; v != s; v = g.edge(parentEdge[v]).from) {
                g.push(parentEdge[v], bottleneck);
            }
            flow += bottleneck;
            augmentations++;
        }
        return new FlowResult("Edmonds-Karp", flow, augmentations, (System.nanoTime() - start) / 1_000_000.0);
    }
}
