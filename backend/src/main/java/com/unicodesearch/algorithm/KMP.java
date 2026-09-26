package com.unicodesearch.algorithm;

import com.unicodesearch.model.MatchResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Knuth-Morris-Pratt exact string matching.
 *
 * Concept: when a mismatch occurs after matching some prefix of the
 * pattern, we already know something about the text we just scanned
 * (because it matched the pattern prefix). KMP precomputes an LPS
 * (Longest Proper Prefix which is also a Suffix) array for the pattern
 * so that, on a mismatch, it can shift the pattern using this knowledge
 * instead of re-comparing characters it has already seen. This avoids
 * ever moving the text pointer backwards.
 *
 * Works directly on Java char sequences, which is safe for Telugu,
 * Devanagari, Tamil and Bengali text once the input has been NFC
 * normalized upstream (see UnicodeUtil) — matching is done UTF-16
 * code-unit by code-unit on already-normalized strings, so combining
 * marks compare consistently.
 *
 * Time complexity: O(n + m)  -- n = text length, m = pattern length
 * Space complexity: O(m)     -- for the LPS array
 */
public class KMP {

    /** Builds the Longest Prefix Suffix (failure function) array for the pattern. */
    public static int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0; // length of the previous longest prefix-suffix
        int i = 1;
        lps[0] = 0;
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len != 0) {
                len = lps[len - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    /**
     * Allocation-free "does text contain pattern" using a PRECOMPUTED LPS array
     * (build it once per pattern, reuse across thousands of texts). No timing,
     * no result objects -- used when scanning the dictionary. O(n) per text.
     */
    public static boolean contains(String text, String pattern, int[] lps) {
        int n = text.length(), m = pattern.length();
        if (m == 0 || m > n) return false;
        int i = 0, j = 0;
        while (i < n) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++; j++;
                if (j == m) return true;
            } else if (j != 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return false;
    }

    public static MatchResult search(String text, String pattern) {
        long start = System.nanoTime();
        List<Integer> positions = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        if (m == 0 || m > n) {
            return new MatchResult(pattern, positions, elapsedMs(start));
        }

        int[] lps = buildLPS(pattern);
        int i = 0; // index into text
        int j = 0; // index into pattern

        while (i < n) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    positions.add(i - j);
                    j = lps[j - 1]; // continue searching for further occurrences
                }
            } else if (j != 0) {
                j = lps[j - 1]; // use failure function instead of restarting
            } else {
                i++;
            }
        }

        return new MatchResult(pattern, positions, elapsedMs(start));
    }

    private static double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
