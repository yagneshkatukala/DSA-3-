package com.unicodesearch.flow;

import java.util.ArrayList;
import java.util.List;

/**
 * Directed flow network stored as an adjacency list of paired edges.
 * Every addEdge(u, v, c) inserts a forward edge (index e, capacity c) and its
 * residual twin (index e^1, capacity 0), so the residual capacity of edge e
 * is  capacity - flow  and pushing f units along e does  flow[e] += f,
 * flow[e^1] -= f.
 *
 * Space: O(V + E).
 */
public class FlowNetwork {

    public static final class Edge {
        public final int from;
        public final int to;
        public final int capacity;
        public int flow;

        Edge(int from, int to, int capacity) {
            this.from = from;
            this.to = to;
            this.capacity = capacity;
        }

        public int residual() { return capacity - flow; }
    }

    private final int nodeCount;
    private final List<Edge> edges = new ArrayList<>();
    private final List<List<Integer>> adjacency = new ArrayList<>();

    public FlowNetwork(int nodeCount) {
        this.nodeCount = nodeCount;
        for (int i = 0; i < nodeCount; i++) adjacency.add(new ArrayList<>());
    }

    public int addEdge(int from, int to, int capacity) {
        int id = edges.size();
        edges.add(new Edge(from, to, capacity));
        edges.add(new Edge(to, from, 0));
        adjacency.get(from).add(id);
        adjacency.get(to).add(id + 1);
        return id;
    }

    public void push(int edgeId, int amount) {
        edges.get(edgeId).flow += amount;
        edges.get(edgeId ^ 1).flow -= amount;
    }

    public void resetFlow() {
        for (Edge e : edges) e.flow = 0;
    }

    public Edge edge(int id) { return edges.get(id); }
    public List<Integer> adjacent(int node) { return adjacency.get(node); }
    public int getNodeCount() { return nodeCount; }
    public int getEdgeCount() { return edges.size() / 2; } // forward edges only
    public int forwardEdgeIdAt(int k) { return 2 * k; }
}
