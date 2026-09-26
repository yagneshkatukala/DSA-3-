package com.unicodesearch.flow;

/**
 * Ford-Fulkerson max flow (CO4) with DFS path search.
 *
 * Repeat: find ANY augmenting path s -> t in the residual graph with a
 * depth-first search, push the bottleneck capacity along it, until no path
 * remains. Correct for integer capacities.
 *
 * Time: O(E * F) where F is the max-flow value (each augmentation adds >= 1).
 * Space: O(V) for the visited array and recursion stack.
 */
public final class FordFulkerson {

    private FordFulkerson() { }

    public static FlowResult run(FlowNetwork g, int s, int t) {
        long start = System.nanoTime();
        g.resetFlow();
        int flow = 0, augmentations = 0;
        while (true) {
            boolean[] visited = new boolean[g.getNodeCount()];
            int pushed = dfs(g, s, t, Integer.MAX_VALUE, visited);
            if (pushed == 0) break;
            flow += pushed;
            augmentations++;
        }
        return new FlowResult("Ford-Fulkerson", flow, augmentations, (System.nanoTime() - start) / 1_000_000.0);
    }

    private static int dfs(FlowNetwork g, int u, int t, int bottleneck, boolean[] visited) {
        if (u == t) return bottleneck;
        visited[u] = true;
        for (int id : g.adjacent(u)) {
            FlowNetwork.Edge e = g.edge(id);
            if (!visited[e.to] && e.residual() > 0) {
                int pushed = dfs(g, e.to, t, Math.min(bottleneck, e.residual()), visited);
                if (pushed > 0) {
                    g.push(id, pushed);
                    return pushed;
                }
            }
        }
        return 0;
    }
}
