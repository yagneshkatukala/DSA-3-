package com.unicodesearch.algorithm;

import com.unicodesearch.model.MatchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Z-Algorithm exact string matching (CO2).
 *
 * The Z-array of a string S stores, for every index i, the length of the
 * longest substring starting at i that is also a prefix of S. To find a
 * pattern P in a text T we compute the Z-array of  P + SEP + T  (SEP is a
 * character that occurs in neither string). Any index i in the T part with
 * Z[i] >= |P| is an occurrence of P in T.
 *
 * The Z-array is built in linear time by maintaining a window [l, r) -- the
 * rightmost segment that matches a prefix -- and reusing previously computed
 * values inside it, so no character is compared more than a constant number
 * of times.
 *
 * Time complexity : O(n + m)   (n = text length, m = pattern length)
 * Space complexity: O(n + m)   for the Z-array of the concatenation
 */
public class ZAlgorithm {

    /** Separator that cannot appear inside a token or query keyword. */
    private static final char SEPARATOR = '\uFFFF';

    /** Builds the Z-array of s (z[0] is defined as s.length()). */
    public static int[] buildZ(String s) {
        int n = s.length();
        int[] z = new int[n];
        if (n == 0) return z;
        z[0] = n;
        int l = 0, r = 0; // [l, r) = rightmost prefix-matching window seen so far
        for (int i = 1; i < n; i++) {
            if (i < r) {
                z[i] = Math.min(r - i, z[i - l]); // reuse: mirror position inside the window
            }
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
                z[i]++; // extend only past the window's right edge
            }
            if (i + z[i] > r) {
                l = i;
                r = i + z[i];
            }
        }
        return z;
    }

    public static MatchResult search(String text, String pattern) {
        long start = System.nanoTime();
        List<Integer> positions = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) {
            return new MatchResult(pattern, positions, elapsedMs(start));
        }

        int[] z = buildZ(pattern + SEPARATOR + text);
        for (int i = m + 1; i < z.length; i++) {
            if (z[i] >= m) {
                positions.add(i - m - 1); // index in the original text
            }
        }
        return new MatchResult(pattern, positions, elapsedMs(start));
    }

    private static double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
