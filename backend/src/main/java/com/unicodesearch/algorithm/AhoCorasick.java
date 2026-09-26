package com.unicodesearch.algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Aho-Corasick multi-pattern matcher.
 *
 * Builds directly on top of Trie: every keyword is first inserted into the
 * trie exactly as normal. Then a BFS pass computes a "failure link" for
 * every node -- the node reachable by the longest proper suffix of the
 * current path that is also a prefix of some pattern. When a character
 * mismatch occurs during scanning, instead of restarting the match from
 * the root (like scanning with N separate KMP calls would effectively
 * require), we jump straight to the failure link, which already
 * represents "how much of some pattern do we still match". This is what
 * lets Aho-Corasick find ALL keyword occurrences in ONE linear pass over
 * the text, regardless of how many keywords are being searched for.
 *
 * "Output" propagation: a node's failure link may itself terminate a
 * pattern (e.g. keyword "భాష" is a suffix of some longer inserted
 * keyword's path) -- so each node's output list is the union of its own
 * word (if any) plus its failure link's output list. This lets a single
 * position in the text report multiple overlapping keyword matches.
 *
 * Time complexity:
 *   - Build:  O(sum of pattern lengths)      -- trie construction + BFS failure links
 *   - Search: O(text length + number of matches reported)
 * Space complexity: O(sum of pattern lengths) for the automaton.
 */
public class AhoCorasick extends Trie {

    private boolean built = false;

    public AhoCorasick(List<String> keywords) {
        for (String kw : keywords) {
            if (kw != null && !kw.isEmpty()) insert(kw);
        }
        build();
    }

    /** BFS construction of failure links and output propagation, run once after all inserts. */
    private void build() {
        Queue<TrieNode> queue = new ArrayDeque<>();
        for (TrieNode child : root.children.values()) {
            child.failureLink = root;
            // BUGFIX: a depth-1 node that ends a keyword (single-character
            // keyword) must report that keyword; previously its output list
            // stayed empty and such keywords were never detected.
            if (child.isEndOfWord) child.outputs.add(child.word);
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();

            for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();

                TrieNode fallback = current.failureLink;
                while (fallback != null && fallback.getChild(c) == null) {
                    fallback = fallback.failureLink;
                }
                child.failureLink = (fallback == null) ? root : fallback.getChild(c);
                if (child.failureLink == child) child.failureLink = root;

                // Output propagation: inherit failure link's outputs too.
                child.outputs = new ArrayList<>(child.failureLink.outputs);
                if (child.isEndOfWord) child.outputs.add(0, child.word);

                queue.add(child);
            }
        }
        built = true;
    }

    /**
     * Scans the text once and returns every keyword occurrence found,
     * keyed by keyword, with all match positions (start index of the match).
     */
    public Map<String, List<Integer>> searchAll(String text) {
        Map<String, List<Integer>> matches = new HashMap<>();
        TrieNode current = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            while (current != root && current.getChild(c) == null) {
                current = current.failureLink;
            }
            TrieNode next = current.getChild(c);
            current = (next != null) ? next : root;

            for (String matchedWord : current.outputs) {
                int startPos = i - matchedWord.length() + 1;
                matches.computeIfAbsent(matchedWord, k -> new ArrayList<>()).add(startPos);
            }
        }
        return matches;
    }

    /**
     * The AND filter used by multi-keyword search: scans the text ONCE and
     * returns the number of occurrences of every query keyword (0 if absent).
     * The document qualifies only if NO keyword has a zero count.
     */
    public Map<String, Integer> countAll(String text, List<String> keywords) {
        Map<String, List<Integer>> found = searchAll(text);
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String kw : keywords) {
            counts.put(kw, found.getOrDefault(kw, java.util.Collections.emptyList()).size());
        }
        return counts;
    }

    /** True iff every keyword has at least one match in the counts map (AND semantics). */
    public static boolean containsAllKeywords(Map<String, Integer> counts) {
        for (int c : counts.values()) {
            if (c == 0) return false;
        }
        return !counts.isEmpty();
    }

    public boolean isBuilt() {
        return built;
    }
}
