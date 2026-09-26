package com.unicodesearch.snippet;

import com.unicodesearch.algorithm.KMP;
import com.unicodesearch.util.UnicodeUtil;

import java.util.*;

/**
 * Builds readable, MULTI-LINE snippets with highlighted keywords.
 *
 * Positions come from the pattern matchers and refer to the SEARCHABLE text
 * (NFC + case folding). Because that text has exactly the same length as the
 * NORMALIZED text, the same offsets are valid in the normalized text, so the
 * snippet is cut from the normalized text -- original casing ("India") and
 * original line breaks are preserved. Matched words are wrapped in [[ ]] which
 * the frontend renders as highlights.
 *
 * Strategy: split the article into lines, pick the (up to) 3 lines that
 * contain the most DISTINCT query keywords, shorten over-long lines around
 * their first match, highlight every match (overlapping matches are merged),
 * and join the lines with '\n' so the article structure is not flattened.
 *
 * Time: O(n + M log M) for an article of n chars and M match positions.
 */
public class SnippetExtractor {

    private static final int MAX_LINES = 3;
    private static final int MAX_LINE_CHARS = 240;
    private static final int LEAD_CHARS = 80;

    /**
     * @param normalizedText  NFC text with original case and line breaks
     * @param positionsByKeyword keyword (searchable form) -> match start offsets
     */
    public static String build(String normalizedText, Map<String, List<Integer>> positionsByKeyword) {
        if (normalizedText == null || normalizedText.isEmpty()) return "";

        // intervals: {start, endExclusive, keywordIndex}
        List<int[]> intervals = new ArrayList<>();
        int kwIdx = 0;
        for (Map.Entry<String, List<Integer>> e : positionsByKeyword.entrySet()) {
            int len = e.getKey().length();
            for (int pos : e.getValue()) {
                if (pos >= 0 && pos + len <= normalizedText.length()) intervals.add(new int[]{pos, pos + len, kwIdx});
            }
            kwIdx++;
        }
        if (intervals.isEmpty()) return head(normalizedText);
        intervals.sort(Comparator.comparingInt((int[] a) -> a[0]).thenComparingInt(a -> a[1]));

        // line boundaries (exclusive of \r and \n)
        List<int[]> lines = new ArrayList<>();
        int s = 0;
        for (int i = 0; i <= normalizedText.length(); i++) {
            if (i == normalizedText.length() || normalizedText.charAt(i) == '\n') {
                int end = i;
                if (end > s && normalizedText.charAt(end - 1) == '\r') end--;
                lines.add(new int[]{s, end});
                s = i + 1;
            }
        }

        // score each line by distinct keywords it contains
        List<int[]> scored = new ArrayList<>(); // {lineIdx, distinct, firstStart}
        for (int li = 0; li < lines.size(); li++) {
            int[] ln = lines.get(li);
            Set<Integer> distinct = new HashSet<>();
            int first = -1;
            for (int[] iv : intervals) {
                if (iv[0] >= ln[0] && iv[0] < ln[1]) {
                    distinct.add(iv[2]);
                    if (first < 0) first = iv[0];
                }
            }
            if (!distinct.isEmpty()) scored.add(new int[]{li, distinct.size(), first});
        }
        scored.sort((a, b) -> a[1] != b[1] ? Integer.compare(b[1], a[1]) : Integer.compare(a[2], b[2]));
        List<int[]> chosen = new ArrayList<>(scored.subList(0, Math.min(MAX_LINES, scored.size())));
        chosen.sort(Comparator.comparingInt(a -> a[2])); // back to document order

        StringBuilder sb = new StringBuilder();
        for (int[] c : chosen) {
            int[] ln = lines.get(c[0]);
            int ws = ln[0], we = ln[1];
            if (we - ws > MAX_LINE_CHARS) {
                ws = Math.max(ln[0], c[2] - LEAD_CHARS);
                we = Math.min(ln[1], ws + MAX_LINE_CHARS);
            }
            if (sb.length() > 0) sb.append('\n');
            if (ws > ln[0]) sb.append("... ");
            appendHighlighted(sb, normalizedText, ws, we, intervals);
            if (we < ln[1]) sb.append(" ...");
        }
        return sb.toString();
    }

    private static void appendHighlighted(StringBuilder sb, String text, int ws, int we, List<int[]> intervals) {
        // merge overlapping intervals that fall in [ws, we)
        List<int[]> merged = new ArrayList<>();
        for (int[] iv : intervals) {
            if (iv[0] < ws || iv[0] >= we) continue;
            int end = Math.min(iv[1], we);
            if (!merged.isEmpty() && iv[0] < merged.get(merged.size() - 1)[1]) {
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], end);
            } else {
                merged.add(new int[]{iv[0], end});
            }
        }
        int cursor = ws;
        for (int[] m : merged) {
            sb.append(text, cursor, m[0]).append("[[").append(text, m[0], m[1]).append("]]");
            cursor = m[1];
        }
        sb.append(text, cursor, we);
    }

    private static String head(String text) {
        String t = text.strip();
        return t.length() > 200 ? t.substring(0, 200) + "..." : t;
    }

    /** Snippet around the first occurrence of one keyword (case-insensitive for Latin text). */
    public static String extract(String rawText, String keyword) {
        return extractForKeywords(rawText, keyword == null ? List.of() : List.of(keyword));
    }

    /** Snippet highlighting every occurrence of the given keywords. */
    public static String extractForKeywords(String rawText, List<String> keywords) {
        if (rawText == null || rawText.isEmpty()) return "";
        String normalized = UnicodeUtil.normalize(rawText);
        String searchable = UnicodeUtil.foldCase(normalized);
        Map<String, List<Integer>> positions = new LinkedHashMap<>();
        for (String kw : keywords) {
            if (kw == null || kw.isEmpty()) continue;
            String key = UnicodeUtil.toSearchable(kw);
            positions.put(key, KMP.search(searchable, key).getPositions());
        }
        return build(normalized, positions);
    }
}
