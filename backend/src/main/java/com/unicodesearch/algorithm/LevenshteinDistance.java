package com.unicodesearch.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Levenshtein edit distance -- Dynamic Programming (CO3).
 *
 * Problem: the minimum number of single-character INSERTIONS, DELETIONS and
 * SUBSTITUTIONS needed to turn string a (length m) into string b (length n).
 *
 * Sub-problem: D[i][j] = edit distance between the first i characters of a
 *                        and the first j characters of b.
 *
 * Base cases:  D[i][0] = i      (delete all i characters)
 *              D[0][j] = j      (insert all j characters)
 *
 * Recurrence:  D[i][j] = D[i-1][j-1]                         if a[i] == b[j]
 *              D[i][j] = 1 + min( D[i-1][j]      (deletion),
 *                                 D[i][j-1]      (insertion),
 *                                 D[i-1][j-1]    (substitution) )   otherwise
 *
 * Answer: D[m][n].
 *
 * Strings are compared by Unicode CODE POINT (not UTF-16 unit), so Indic
 * and any supplementary-plane text is handled correctly.
 *
 * distance()        : O(m*n) time, O(min(m,n)) space (two rolling rows)
 * distanceBounded() : O(m*n) worst case time but exits early as soon as a
 *                     whole row exceeds the bound, O(min(m,n)) space
 * explain()         : O(m*n) time and space -- keeps the full table so the
 *                     optimal edit script can be recovered by backtracking.
 */
public final class LevenshteinDistance {

    private LevenshteinDistance() { }

    /** Classic two-row DP. */
    public static int distance(String a, String b) {
        int[] x = a.codePoints().toArray();
        int[] y = b.codePoints().toArray();
        if (x.length < y.length) { int[] t = x; x = y; y = t; } // y is the shorter -> smaller rows
        int[] prev = new int[y.length + 1];
        int[] curr = new int[y.length + 1];
        for (int j = 0; j <= y.length; j++) prev[j] = j;

        for (int i = 1; i <= x.length; i++) {
            curr[0] = i;
            for (int j = 1; j <= y.length; j++) {
                int cost = (x[i - 1] == y[j - 1]) ? 0 : 1;
                curr[j] = Math.min(Math.min(prev[j] + 1,        // deletion
                                            curr[j - 1] + 1),   // insertion
                                   prev[j - 1] + cost);         // substitution / match
            }
            int[] t = prev; prev = curr; curr = t;
        }
        return prev[y.length];
    }

    /**
     * Distance if it is <= maxDistance, otherwise maxDistance + 1.
     * Rows only ever increase along a path, so once every entry of a row is
     * above the bound the final answer must be too -- we stop immediately.
     */
    public static int distanceBounded(String a, String b, int maxDistance) {
        int[] x = a.codePoints().toArray();
        int[] y = b.codePoints().toArray();
        if (Math.abs(x.length - y.length) > maxDistance) return maxDistance + 1; // length gap is a lower bound
        if (x.length < y.length) { int[] t = x; x = y; y = t; }
        int[] prev = new int[y.length + 1];
        int[] curr = new int[y.length + 1];
        for (int j = 0; j <= y.length; j++) prev[j] = j;

        for (int i = 1; i <= x.length; i++) {
            curr[0] = i;
            int rowMin = curr[0];
            for (int j = 1; j <= y.length; j++) {
                int cost = (x[i - 1] == y[j - 1]) ? 0 : 1;
                curr[j] = Math.min(Math.min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost);
                rowMin = Math.min(rowMin, curr[j]);
            }
            if (rowMin > maxDistance) return maxDistance + 1;
            int[] t = prev; prev = curr; curr = t;
        }
        return Math.min(prev[y.length], maxDistance + 1);
    }

    /** Full DP table D[0..m][0..n] (used for explanation / teaching / tests). */
    public static int[][] table(String a, String b) {
        int[] x = a.codePoints().toArray();
        int[] y = b.codePoints().toArray();
        int[][] d = new int[x.length + 1][y.length + 1];
        for (int i = 0; i <= x.length; i++) d[i][0] = i;
        for (int j = 0; j <= y.length; j++) d[0][j] = j;
        for (int i = 1; i <= x.length; i++) {
            for (int j = 1; j <= y.length; j++) {
                int cost = (x[i - 1] == y[j - 1]) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d;
    }

    /**
     * Recovers one optimal edit script by walking the DP table backwards from
     * D[m][n] to D[0][0]. Each step is one of MATCH / SUBSTITUTE / DELETE / INSERT.
     */
    public static List<String> explain(String a, String b) {
        int[] x = a.codePoints().toArray();
        int[] y = b.codePoints().toArray();
        int[][] d = table(a, b);
        List<String> ops = new ArrayList<>();
        int i = x.length, j = y.length;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && x[i - 1] == y[j - 1] && d[i][j] == d[i - 1][j - 1]) {
                ops.add("MATCH " + new String(Character.toChars(x[i - 1])));
                i--; j--;
            } else if (i > 0 && j > 0 && d[i][j] == d[i - 1][j - 1] + 1) {
                ops.add("SUBSTITUTE " + new String(Character.toChars(x[i - 1]))
                        + " -> " + new String(Character.toChars(y[j - 1])));
                i--; j--;
            } else if (i > 0 && d[i][j] == d[i - 1][j] + 1) {
                ops.add("DELETE " + new String(Character.toChars(x[i - 1])));
                i--;
            } else {
                ops.add("INSERT " + new String(Character.toChars(y[j - 1])));
                j--;
            }
        }
        Collections.reverse(ops);
        return ops;
    }
}
