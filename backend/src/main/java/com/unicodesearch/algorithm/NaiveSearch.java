package com.unicodesearch.algorithm;

import com.unicodesearch.model.MatchResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Brute-force substring search, used ONLY as a baseline for the
 * performance-comparison dashboard. Not used in the actual search pipeline.
 *
 * Time complexity: O(n*m) worst case, where n = text length, m = pattern length.
 * Space complexity: O(1) auxiliary.
 */
public class NaiveSearch {

    public static MatchResult search(String text, String pattern) {
        long start = System.nanoTime();
        List<Integer> positions = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        if (m == 0 || m > n) {
            return new MatchResult(pattern, positions, elapsedMs(start));
        }

        for (int i = 0; i <= n - m; i++) {
            int j = 0;
            while (j < m && text.charAt(i + j) == pattern.charAt(j)) {
                j++;
            }
            if (j == m) {
                positions.add(i);
            }
        }
        return new MatchResult(pattern, positions, elapsedMs(start));
    }

    private static double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
