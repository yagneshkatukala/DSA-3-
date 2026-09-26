package com.unicodesearch.flow;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * Dinic's max flow (CO4).
 *
 * Phase loop:
 *   1. BFS from s builds a LEVEL graph (distance in edges in the residual graph).
 *   2. DFS sends a BLOCKING flow along level-increasing edges only, using a
 *      per-node "current edge" pointer so dead edges are never re-examined.
 * Stops when t is unreachable. The number of phases is at most V.
 *
 * Time: O(V^2 * E) in general; O(E * sqrt(V)) on unit-capacity bipartite
 *       graphs -- exactly the keyword/document matching network built here.
 * Space: O(V).
 * The "augmentations" value reported is the number of BFS phases.
 */
public final class Dinic {

    private Dinic() { }

    public static FlowResult run(FlowNetwork g, int s, int t) {
        long start = System.nanoTime();
        g.resetFlow();
        int flow = 0, phases = 0;
        int n = g.getNodeCount();
        int[] level = new int[n];
        int[] next = new int[n];

        while (buildLevels(g, s, t, level)) {
            Arrays.fill(next, 0);
            int pushed;
            while ((pushed = blockingDfs(g, s, t, Integer.MAX_VALUE, level, next)) > 0) {
                flow += pushed;
            }
            phases++;
        }
        return new FlowResult("Dinic", flow, phases, (System.nanoTime() - start) / 1_000_000.0);
    }

    private static boolean buildLevels(FlowNetwork g, int s, int t, int[] level) {
        Arrays.fill(level, -1);
        level[s] = 0;
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(s);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int id : g.adjacent(u)) {
                FlowNetwork.Edge e = g.edge(id);
                if (level[e.to] < 0 && e.residual() > 0) {
                    level[e.to] = level[u] + 1;
                    queue.add(e.to);
                }
            }
        }
        return level[t] >= 0;
    }

    private static int blockingDfs(FlowNetwork g, int u, int t, int limit, int[] level, int[] next) {
        if (u == t) return limit;
        for (; next[u] < g.adjacent(u).size(); next[u]++) {
            int id = g.adjacent(u).get(next[u]);
            FlowNetwork.Edge e = g.edge(id);
            if (e.residual() > 0 && level[e.to] == level[u] + 1) {
                int pushed = blockingDfs(g, e.to, t, Math.min(limit, e.residual()), level, next);
                if (pushed > 0) {
                    g.push(id, pushed);
                    return pushed;
                }
            }
        }
        return 0;
    }
}
