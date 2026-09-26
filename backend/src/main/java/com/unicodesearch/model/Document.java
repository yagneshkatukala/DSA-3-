package com.unicodesearch.model;

import com.unicodesearch.util.UnicodeUtil;

/**
 * A single corpus document, kept in three deliberately separate forms:
 *
 *   originalContent   - text exactly as read from disk (line breaks intact)
 *   normalizedContent - NFC form, case + line breaks preserved (snippets/display)
 *   searchableContent - normalizedContent with Latin letters case-folded.
 *                       Same length as normalizedContent, so match positions
 *                       found here are valid indexes into normalizedContent.
 *
 * All matchers and the inverted index use searchableContent; snippets and
 * the document viewer use the other two. Original text is never lowercased.
 */
public class Document {
    private final String id;
    private final String title;
    private final String language;
    private final String originalContent;
    private final String normalizedContent;
    private final String searchableContent;
    private final String searchableTitle;

    public Document(String id, String title, String language, String originalContent) {
        this.id = id;
        this.title = title;
        this.language = language;
        this.originalContent = originalContent == null ? "" : originalContent;
        this.normalizedContent = UnicodeUtil.normalize(this.originalContent);
        this.searchableContent = UnicodeUtil.foldCase(this.normalizedContent);
        this.searchableTitle = UnicodeUtil.toSearchable(title);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getLanguage() { return language; }

    public String getOriginalContent() { return originalContent; }
    public String getNormalizedContent() { return normalizedContent; }
    public String getSearchableContent() { return searchableContent; }
    public String getSearchableTitle() { return searchableTitle; }

    /** Backward-compatible alias: the text pattern matchers run on. */
    public String getContent() { return searchableContent; }

    /** Backward-compatible alias: the original text for display. */
    public String getRawContent() { return originalContent; }
}
