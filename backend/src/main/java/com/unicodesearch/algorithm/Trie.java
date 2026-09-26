package com.unicodesearch.algorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard Trie (prefix tree) for storing search keywords.
 *
 * Data structure: each edge represents one character; each root-to-node
 * path spells a prefix of some inserted word. Shared prefixes are shared
 * in the tree, which is what makes multi-keyword matching efficient --
 * this structure is reused directly as the automaton skeleton for
 * Aho-Corasick.
 *
 * Time complexity: insert/search O(k) where k = length of the word.
 * Space complexity: O(total characters across all inserted words) worst case.
 */
public class Trie {
    protected final TrieNode root = new TrieNode();

    public void insert(String word) {
        if (word == null || word.isEmpty()) return;
        TrieNode current = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            TrieNode next = current.getChild(c);
            if (next == null) {
                next = new TrieNode();
                current.children.put(c, next);
            }
            current = next;
        }
        current.isEndOfWord = true;
        current.word = word;
    }

    /** Exact word lookup. */
    public boolean search(String word) {
        TrieNode node = traverse(word);
        return node != null && node.isEndOfWord;
    }

    /** Returns true if any inserted word starts with the given prefix. */
    public boolean startsWith(String prefix) {
        return traverse(prefix) != null;
    }

    private TrieNode traverse(String s) {
        TrieNode current = root;
        for (int i = 0; i < s.length(); i++) {
            current = current.getChild(s.charAt(i));
            if (current == null) return null;
        }
        return current;
    }

    /** Returns all complete words stored under a given prefix (simple DFS collection). */
    public List<String> collectWordsWithPrefix(String prefix) {
        List<String> results = new ArrayList<>();
        TrieNode node = traverse(prefix);
        if (node != null) collect(node, results);
        return results;
    }

    private void collect(TrieNode node, List<String> results) {
        if (node.isEndOfWord) results.add(node.word);
        for (TrieNode child : node.children.values()) {
            collect(child, results);
        }
    }

    protected TrieNode getRoot() {
        return root;
    }
}
