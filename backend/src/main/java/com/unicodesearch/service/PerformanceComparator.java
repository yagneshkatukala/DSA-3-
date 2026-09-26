package com.unicodesearch.service;

import com.unicodesearch.algorithm.AhoCorasick;
import com.unicodesearch.algorithm.KMP;
import com.unicodesearch.algorithm.NaiveSearch;
import com.unicodesearch.algorithm.RabinKarp;
import com.unicodesearch.algorithm.ZAlgorithm;
import com.unicodesearch.index.InvertedIndex;
import com.unicodesearch.model.Document;
import com.unicodesearch.model.MatchResult;
import com.unicodesearch.util.UnicodeUtil;

import java.util.*;

/**
 * Runs the SAME query through every string-matching algorithm (Naive, KMP,
 * Rabin-Karp, Z-Algorithm, Aho-Corasick) and records the actual measured
 * execution time of each -- powers the Algorithm Performance dashboard. No
 * number here is invented; every value comes from System.nanoTime().
 *
 * Fair-comparison rules:
 *  - Naive / KMP / Rabin-Karp / Z are single-pattern algorithms, so they are
 *    timed on the query's FIRST keyword over the documents the inverted index
 *    reports for that keyword (never the whole corpus).
 *  - Aho-Corasick is a multi-pattern algorithm; it is timed on ALL keywords at
 *    once over the AND-candidate set (documents that can contain every keyword).
 *  - "documentsScanned" is reported per row and is the size of that candidate
 *    set, NOT the corpus size (totalDocuments is reported separately).
 */
public class PerformanceComparator {

    private final InvertedIndex invertedIndex;

    public PerformanceComparator(InvertedIndex invertedIndex) {
        this.invertedIndex = invertedIndex;
    }

    public List<Map<String, Object>> compare(String rawQuery, String language) {
        List<String> tokens = new ArrayList<>(new LinkedHashSet<>(UnicodeUtil.tokenize(rawQuery)));
        if (tokens.isEmpty()) tokens = List.of(UnicodeUtil.toSearchable(rawQuery).trim());
        String keyword = tokens.get(0);

        List<Document> singleCandidates = filterByLanguage(invertedIndex.getDocumentsContaining(keyword), language);
        List<Document> multiCandidates = filterByLanguage(
                invertedIndex.getCandidateDocumentsForAllContaining(tokens, null), language);

        List<Map<String, Object>> table = new ArrayList<>();
        table.add(runAlgorithm("Naive", singleCandidates, keyword, NaiveSearch::search));
        table.add(runAlgorithm("KMP", singleCandidates, keyword, KMP::search));
        table.add(runAlgorithm("Rabin-Karp", singleCandidates, keyword, RabinKarp::search));
        table.add(runAlgorithm("Z-Algorithm", singleCandidates, keyword, ZAlgorithm::search));
        table.add(runAhoCorasick(multiCandidates, tokens));
        return table;
    }

    private List<Document> filterByLanguage(Set<String> ids, String language) {
        List<Document> docs = new ArrayList<>();
        for (String id : ids) {
            Document d = invertedIndex.getDocument(id);
            if (d == null) continue;
            if ("all".equalsIgnoreCase(language) || d.getLanguage().equalsIgnoreCase(language)) docs.add(d);
        }
        return docs;
    }

    private Map<String, Object> runAhoCorasick(List<Document> candidates, List<String> keywords) {
        long start = System.nanoTime();
        int totalMatches = 0;
        AhoCorasick automaton = new AhoCorasick(keywords);
        for (Document doc : candidates) {
            for (List<Integer> positions : automaton.searchAll(doc.getSearchableContent()).values()) {
                totalMatches += positions.size();
            }
        }
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        return row("Aho-Corasick", elapsedMs, totalMatches, candidates.size(), keywords.size(),
                "all " + keywords.size() + " keyword(s), one pass");
    }

    private interface Matcher {
        MatchResult run(String text, String pattern);
    }

    private Map<String, Object> runAlgorithm(String name, List<Document> candidates, String keyword, Matcher matcher) {
        long start = System.nanoTime();
        int totalMatches = 0;
        for (Document doc : candidates) {
            totalMatches += matcher.run(doc.getSearchableContent(), keyword).getMatchCount();
        }
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        return row(name, elapsedMs, totalMatches, candidates.size(), keyword.length(), "first keyword only");
    }

    private Map<String, Object> row(String name, double ms, int matches, int scanned, int patternSize, String scope) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("algorithm", name);
        row.put("executionTimeMs", Math.round(ms * 10000.0) / 10000.0);
        row.put("matches", matches);
        row.put("documentsScanned", scanned);
        row.put("totalDocuments", invertedIndex.getDocumentCount());
        row.put("patternLength", patternSize);
        row.put("scope", scope);
        return row;
    }
}
