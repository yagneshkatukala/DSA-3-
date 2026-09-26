package com.unicodesearch.algorithm;

import java.util.HashMap;
import java.util.Map;

/**
 * A single node in the Trie / Aho-Corasick automaton.
 *
 * Uses a HashMap<Character, TrieNode> for children rather than a fixed-size
 * array, because Indic-script alphabets (Telugu/Devanagari/Tamil/Bengali)
 * span a huge Unicode range -- a fixed 256/26-slot array (typical for
 * ASCII tries) would be wasteful and would not even cover these scripts.
 * A HashMap gives us average O(1) child lookup for any Unicode
 * character, at the cost of you needing to explain that "O(1)" is
 * average-case, not worst-case (see complexity docs).
 */
public class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord = false;
    String word = null;      // the complete keyword this node terminates, if any
    TrieNode failureLink = null; // Aho-Corasick failure link (longest proper suffix that is also a prefix of some pattern)
    java.util.List<String> outputs = new java.util.ArrayList<>(); // all patterns ending at/via this node

    public TrieNode getChild(char c) {
        return children.get(c);
    }
}
