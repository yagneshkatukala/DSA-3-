package com.unicodesearch.service;

import com.unicodesearch.flow.Dinic;
import com.unicodesearch.flow.EdmondsKarp;
import com.unicodesearch.flow.FlowNetwork;
import com.unicodesearch.flow.FlowResult;
import com.unicodesearch.flow.FordFulkerson;
import com.unicodesearch.flow.MinCut;
import com.unicodesearch.index.InvertedIndex;
import com.unicodesearch.model.Document;
import com.unicodesearch.util.UnicodeUtil;

import java.util.*;

/**
 * CO4 -- Network Flow analysis of the keyword <-> document relationship.
 *
 * This is a SEPARATE analysis feature; it never participates in normal
 * search ranking or filtering.
 *
 * Model (a bipartite flow network):
 *
 *      SOURCE --(demand)--> keyword_i --(1)--> document_j --(1)--> SINK
 *
 *   - one node per distinct query keyword
 *   - one node per document that contains at least one of the keywords
 *     (restricted to the chosen language)
 *   - an edge keyword_i -> document_j (capacity 1) iff document_j contains
 *     keyword_i (edges come from the inverted index, never hard-coded)
 *   - SOURCE -> keyword_i has capacity = demand (how many distinct documents
 *     each keyword should be "assigned")
 *   - document_j -> SINK has capacity 1 (a document is used as the witness
 *     for at most one keyword)
 *
 * Meaning: max flow = size of a maximum keyword-document MATCHING. If it
 * equals demand * |keywords| every keyword can be given its own distinct
 * witness document(s). Otherwise the MIN CUT identifies the bottleneck: the
 * keywords / documents whose supply is exhausted (Hall's-theorem violators).
 *
 * All three implemented flow algorithms (Ford-Fulkerson, Edmonds-Karp, Dinic)
 * are run on the same network so their results and costs can be compared.
 *
 * Network size: V = 2 + K + D, E = K + D + (keyword-document incidences).
 */
public class KeywordDocumentFlowAnalyzer {

    private final InvertedIndex index;

    public KeywordDocumentFlowAnalyzer(InvertedIndex index) {
        this.index = index;
    }

    public Map<String, Object> analyze(String rawQuery, String language, String algorithm, int demand) {
        long overallStart = System.nanoTime();
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> keywords = new ArrayList<>(new LinkedHashSet<>(UnicodeUtil.tokenize(rawQuery)));
        if (demand < 1) demand = 1;

        out.put("query", rawQuery);
        out.put("language", language);
        out.put("demand", demand);

        if (keywords.isEmpty()) {
            out.put("error", "Enter at least one keyword for the network-flow analysis.");
            return out;
        }
        if (keywords.size() > SearchEngine.MAX_KEYWORDS) {
            out.put("error", "Too many keywords: " + keywords.size() + " provided, maximum allowed is "
                    + SearchEngine.MAX_KEYWORDS + ".");
            return out;
        }

        // ---- 1. keyword -> documents edges, straight from the inverted index ----
        Map<String, List<String>> docsOfKeyword = new LinkedHashMap<>();
        Map<String, Integer> docNodeIndex = new LinkedHashMap<>();
        for (String kw : keywords) {
            List<String> docs = new ArrayList<>();
            for (String id : index.getDocumentsContaining(kw)) {
                Document d = index.getDocument(id);
                if (d == null) continue;
                if (!"all".equalsIgnoreCase(language) && !d.getLanguage().equalsIgnoreCase(language)) continue;
                docs.add(id);
                docNodeIndex.putIfAbsent(id, docNodeIndex.size());
            }
            docsOfKeyword.put(kw, docs);
        }

        // ---- 2. build the flow network ----
        int K = keywords.size();
        int D = docNodeIndex.size();
        final int SOURCE = 0, SINK = 1;
        int keywordBase = 2, docBase = 2 + K;
        FlowNetwork g = new FlowNetwork(2 + K + D);

        Map<Integer, String> nodeName = new HashMap<>();
        nodeName.put(SOURCE, "SOURCE");
        nodeName.put(SINK, "SINK");
        for (int i = 0; i < K; i++) {
            nodeName.put(keywordBase + i, keywords.get(i));
            g.addEdge(SOURCE, keywordBase + i, demand);
        }
        for (Map.Entry<String, Integer> e : docNodeIndex.entrySet()) {
            nodeName.put(docBase + e.getValue(), e.getKey());
            g.addEdge(docBase + e.getValue(), SINK, 1);
        }
        int keywordDocumentEdges = 0;
        List<int[]> kdEdgeIds = new ArrayList<>(); // {edgeId, keywordIdx, docNode}
        for (int i = 0; i < K; i++) {
            for (String docId : docsOfKeyword.get(keywords.get(i))) {
                int docNode = docBase + docNodeIndex.get(docId);
                int id = g.addEdge(keywordBase + i, docNode, 1);
                kdEdgeIds.add(new int[]{id, i, docNode});
                keywordDocumentEdges++;
            }
        }

        // ---- 3. run every implemented algorithm; the selected one drives the report ----
        String selected = normalizeAlgorithm(algorithm);
        List<FlowResult> runs = new ArrayList<>();
        runs.add(FordFulkerson.run(g, SOURCE, SINK));
        runs.add(EdmondsKarp.run(g, SOURCE, SINK));
        runs.add(Dinic.run(g, SOURCE, SINK));

        FlowResult chosen = runs.get(2);
        for (FlowResult r : runs) {
            if (normalizeAlgorithm(r.algorithm()).equals(selected)) chosen = r;
        }
        // Re-run the chosen algorithm last so the network holds ITS flow (for matching/min-cut extraction).
        FlowResult finalRun = switch (normalizeAlgorithm(chosen.algorithm())) {
            case "fordfulkerson" -> FordFulkerson.run(g, SOURCE, SINK);
            case "edmondskarp" -> EdmondsKarp.run(g, SOURCE, SINK);
            default -> Dinic.run(g, SOURCE, SINK);
        };

        // ---- 4. matching = keyword->document edges carrying flow ----
        List<Map<String, Object>> matching = new ArrayList<>();
        Map<String, Integer> assigned = new LinkedHashMap<>();
        for (String kw : keywords) assigned.put(kw, 0);
        for (int[] rec : kdEdgeIds) {
            if (g.edge(rec[0]).flow > 0) {
                String kw = keywords.get(rec[1]);
                String docId = nodeName.get(rec[2]);
                Document d = index.getDocument(docId);
                assigned.merge(kw, 1, Integer::sum);
                Map<String, Object> pair = new LinkedHashMap<>();
                pair.put("keyword", kw);
                pair.put("documentId", docId);
                pair.put("title", d == null ? docId : d.getTitle());
                matching.add(pair);
            }
        }
        List<Map<String, Object>> unmatched = new ArrayList<>();
        for (String kw : keywords) {
            if (assigned.get(kw) < demand) {
                Map<String, Object> u = new LinkedHashMap<>();
                u.put("keyword", kw);
                u.put("assigned", assigned.get(kw));
                u.put("demand", demand);
                u.put("documentsContainingKeyword", docsOfKeyword.get(kw).size());
                unmatched.add(u);
            }
        }

        // ---- 5. min cut from the residual graph ----
        MinCut.Cut cut = MinCut.compute(g, SOURCE);
        List<Map<String, Object>> cutEdges = new ArrayList<>();
        for (int id : cut.cutEdgeIds()) {
            FlowNetwork.Edge e = g.edge(id);
            Map<String, Object> ce = new LinkedHashMap<>();
            ce.put("from", nodeName.get(e.from));
            ce.put("to", nodeName.get(e.to));
            ce.put("capacity", e.capacity);
            cutEdges.add(ce);
        }
        List<String> sourceSideKeywords = new ArrayList<>();
        for (int i = 0; i < K; i++) if (cut.sourceSide()[keywordBase + i]) sourceSideKeywords.add(keywords.get(i));
        int sourceSideDocs = 0;
        for (int j = 0; j < D; j++) if (cut.sourceSide()[docBase + j]) sourceSideDocs++;

        Map<String, Object> cutJson = new LinkedHashMap<>();
        cutJson.put("capacity", cut.capacity());
        cutJson.put("equalsMaxFlow", cut.capacity() == finalRun.maxFlow());
        cutJson.put("sourceSideKeywords", sourceSideKeywords);
        cutJson.put("sourceSideDocuments", sourceSideDocs);
        cutJson.put("cutEdgeCount", cutEdges.size());
        cutJson.put("cutEdges", cutEdges.size() > 60 ? cutEdges.subList(0, 60) : cutEdges);

        // ---- 6. report ----
        List<Map<String, Object>> runJson = new ArrayList<>();
        boolean consistent = true;
        for (FlowResult r : runs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("algorithm", r.algorithm());
            row.put("maxFlow", r.maxFlow());
            row.put("augmentationsOrPhases", r.augmentations());
            row.put("executionTimeMs", Math.round(r.executionTimeMs() * 10000.0) / 10000.0);
            runJson.add(row);
            if (r.maxFlow() != finalRun.maxFlow()) consistent = false;
        }

        Map<String, Integer> degrees = new LinkedHashMap<>();
        for (String kw : keywords) degrees.put(kw, docsOfKeyword.get(kw).size());

        out.put("algorithm", finalRun.algorithm());
        out.put("keywordNodes", K);
        out.put("documentNodes", D);
        out.put("totalNodes", 2 + K + D);
        out.put("keywordDocumentEdges", keywordDocumentEdges);
        out.put("totalEdges", g.getEdgeCount());
        out.put("keywordDegrees", degrees);
        out.put("requestedFlow", K * demand);
        out.put("maxFlow", finalRun.maxFlow());
        out.put("fullyMatched", finalRun.maxFlow() == K * demand);
        out.put("matchingSize", matching.size());
        out.put("matching", matching.size() > 200 ? matching.subList(0, 200) : matching);
        out.put("unmatchedKeywords", unmatched);
        out.put("minCut", cutJson);
        out.put("algorithmRuns", runJson);
        out.put("algorithmsAgree", consistent);
        out.put("executionTimeMs", Math.round(finalRun.executionTimeMs() * 10000.0) / 10000.0);
        out.put("totalTimeMs", Math.round((System.nanoTime() - overallStart) / 1000.0) / 1000.0);
        return out;
    }

    private static String normalizeAlgorithm(String a) {
        if (a == null) return "dinic";
        String s = a.toLowerCase().replace("-", "").replace("_", "").replace(" ", "");
        if (s.startsWith("ford")) return "fordfulkerson";
        if (s.startsWith("edmonds") || s.equals("ek")) return "edmondskarp";
        return "dinic";
    }
}
