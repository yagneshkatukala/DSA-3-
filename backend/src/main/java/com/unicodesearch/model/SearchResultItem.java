package com.unicodesearch.model;

import java.util.List;
import java.util.Map;

/** A single ranked search result returned to the frontend. */
public class SearchResultItem {
    private final String documentId;
    private final String title;
    private final String language;
    private final int matchCount;
    private final double score;
    private final String snippet;
    private final Map<String, Integer> keywordFrequencies;
    private final List<String> matchedKeywords;

    public SearchResultItem(String documentId, String title, String language, int matchCount,
                             double score, String snippet, Map<String, Integer> keywordFrequencies,
                             List<String> matchedKeywords) {
        this.documentId = documentId;
        this.title = title;
        this.language = language;
        this.matchCount = matchCount;
        this.score = score;
        this.snippet = snippet;
        this.keywordFrequencies = keywordFrequencies;
        this.matchedKeywords = matchedKeywords;
    }

    public String getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public String getLanguage() { return language; }
    public int getMatchCount() { return matchCount; }
    public double getScore() { return score; }
    public String getSnippet() { return snippet; }
    public Map<String, Integer> getKeywordFrequencies() { return keywordFrequencies; }
    public List<String> getMatchedKeywords() { return matchedKeywords; }
}
