package com.unicodesearch.service;

import com.unicodesearch.algorithm.LevenshteinDistance;
import com.unicodesearch.index.InvertedIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Typo-tolerant "did you mean" lookup over the ACTUAL dictionary of the
 * inverted index (CO3 -- Dynamic Programming).
 *
 * For a misspelled keyword every dictionary term whose Levenshtein distance
 * is within an adaptive threshold is a suggestion. Nothing is hard-coded:
 * suggestions exist only if the corpus dictionary contains them.
 *
 * Cost control (the corpus is never scanned, only the dictionary):
 *   1. length-difference pruning  |len(a)-len(b)| > maxDistance -> skip
 *   2. bounded DP with early exit once a whole DP row exceeds maxDistance
 *
 * Time: O(V * m * n) worst case (V dictionary terms, m,n term lengths),
 *       far less in practice thanks to the two prunings.
 * Space: O(min(m, n)) per comparison plus O(suggestions).
 */
public class FuzzySearch {

    public record Suggestion(String term, int distance, int documentFrequency) { }

    private final InvertedIndex index;

    public FuzzySearch(InvertedIndex index) {
        this.index = index;
    }

    /** Longer words tolerate more typos; very short words tolerate at most one. */
    public static int maxDistanceFor(String keyword) {
        int len = keyword.codePointCount(0, keyword.length());
        if (len <= 2) return 0;
        if (len <= 4) return 1;
        if (len <= 8) return 2;
        return 3;
    }

    /** Closest dictionary terms, ordered by (distance asc, document frequency desc, term). */
    public List<Suggestion> suggest(String keyword, int limit) {
        int maxDist = maxDistanceFor(keyword);
        List<Suggestion> found = new ArrayList<>();
        int kwLen = keyword.codePointCount(0, keyword.length());
        for (String term : index.getDictionary()) {
            int tLen = term.codePointCount(0, term.length());
            if (Math.abs(tLen - kwLen) > maxDist) continue; // pruning 1
            int d = LevenshteinDistance.distanceBounded(keyword, term, maxDist); // pruning 2
            if (d <= maxDist) found.add(new Suggestion(term, d, index.getDocumentFrequency(term)));
        }
        found.sort(Comparator.comparingInt(Suggestion::distance)
                .thenComparing(Comparator.comparingInt(Suggestion::documentFrequency).reversed())
                .thenComparing(Suggestion::term));
        return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }
}
