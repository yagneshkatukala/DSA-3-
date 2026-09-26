package com.unicodesearch.algorithm;

import com.unicodesearch.model.MatchResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Rabin-Karp exact string matching using a rolling polynomial hash.
 *
 * Representation choice: Indic-script characters (Telugu U+0C00-U+0C7F,
 * Devanagari U+0900-U+097F, Tamil U+0B80-U+0BFF, Bengali U+0980-U+09FF)
 * fit inside a single Java `char` (UTF-16 code unit) for the vast
 * majority of code points used in this corpus, so we hash the text as a
 * sequence of `char` code units directly -- no lossy byte-level
 * conversion. We use a large prime modulus to keep hash collisions rare
 * while still fitting comfortably in a Java `long`.
 *
 * Rolling hash: instead of recomputing the hash of every m-length
 * window from scratch (O(m) per window), we update it in O(1) by
 * removing the outgoing character's contribution and adding the
 * incoming character's contribution.
 *
 * Because two different strings can (rarely) share the same hash, every
 * hash match is verified with a direct character comparison before being
 * accepted -- this guarantees correctness even in the presence of a
 * collision.
 *
 * Time complexity: O(n + m) average case; O(n*m) worst case (many hash
 * collisions requiring verification).
 * Space complexity: O(1) auxiliary.
 */
public class RabinKarp {

    private static final long PRIME = 1_000_000_007L;
    private static final long BASE = 256L;

    public static MatchResult search(String text, String pattern) {
        long start = System.nanoTime();
        List<Integer> positions = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        if (m == 0 || m > n) {
            return new MatchResult(pattern, positions, elapsedMs(start));
        }

        long patternHash = 0;
        long windowHash = 0;
        long highOrder = 1; // BASE^(m-1) % PRIME, used to remove the leading character when rolling

        for (int i = 0; i < m - 1; i++) {
            highOrder = (highOrder * BASE) % PRIME;
        }

        for (int i = 0; i < m; i++) {
            patternHash = (BASE * patternHash + pattern.charAt(i)) % PRIME;
            windowHash = (BASE * windowHash + text.charAt(i)) % PRIME;
        }

        int collisions = 0;

        for (int i = 0; i <= n - m; i++) {
            if (patternHash == windowHash) {
                // Verify to guard against hash collisions.
                if (text.regionMatches(i, pattern, 0, m)) {
                    positions.add(i);
                } else {
                    collisions++;
                }
            }
            if (i < n - m) {
                windowHash = (BASE * (windowHash - text.charAt(i) * highOrder) + text.charAt(i + m)) % PRIME;
                if (windowHash < 0) {
                    windowHash += PRIME;
                }
            }
        }

        return new MatchResult(pattern, positions, elapsedMs(start));
    }

    private static double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
