package com.unicodesearch.index;

import com.unicodesearch.algorithm.KMP;
import com.unicodesearch.model.Document;
import com.unicodesearch.model.Posting;
import com.unicodesearch.util.UnicodeUtil;

import java.util.*;

/**
 * Inverted index: term -> list of postings (which documents contain the
 * term, how often, and at which positions).  The set of keys of this map is
 * the search DICTIONARY, generated entirely from the corpus.
 *
 * Instead of running a pattern matcher over every document, the engine
 * first consults this HashMap and only runs the character-level algorithms
 * (KMP / Rabin-Karp / Z / Aho-Corasick) on the small candidate set.
 *
 * Terms are stored in SEARCHABLE form (NFC + Latin case folding) -- the same
 * representation the matchers scan -- so "India", "INDIA" and "india" are one
 * dictionary entry and one query keyword.
 *
 * Candidate retrieval comes in two flavours:
 *
 *  1. Exact-token:   getCandidateDocuments / ...ForAny / ...ForAll
 *     A document qualifies if it contains the keyword as a whole indexed term.
 *
 *  2. Containment:   getDocumentsContaining / getCandidateDocumentsForAllContaining
 *     A document qualifies if it contains the keyword ANYWHERE inside a word.
 *     The dictionary is scanned with KMP for every term that contains the
 *     keyword, and the postings of those terms are unioned. This is exactly
 *     the set of documents a substring matcher would report, so the index
 *     is a lossless filter -- important for agglutinative Indic languages
 *     where a keyword is often a prefix/stem of longer inflected words.
 *
 * Construction: O(total corpus size).
 * Exact lookup: O(1) average + O(postings).
 * Containment lookup: O(sum of dictionary term lengths) per keyword
 *     (one KMP pass over each term) + O(postings of matching terms).
 * ALL (AND) candidates: sets are intersected smallest-first, O(sum of set sizes).
 */
public class InvertedIndex {

    private final Map<String, List<Posting>> index = new HashMap<>();
    private final Map<String, Document> documentsById = new LinkedHashMap<>();
    private int totalPostings = 0;

    /**
     * Cache: keyword -> documents containing it. The dictionary scan is the
     * costliest step of a query, and repeated keywords are very common, so
     * results are memoized (cleared on every rebuild; bounded to stay small).
     */
    private static final int CACHE_LIMIT = 5000;
    private final Map<String, Set<String>> containingCache = new java.util.concurrent.ConcurrentHashMap<>();

    public void build(List<Document> documents) {
        index.clear();
        documentsById.clear();
        totalPostings = 0;
        containingCache.clear();

        for (Document doc : documents) {
            documentsById.put(doc.getId(), doc);

            // One posting per (term, document) pair with all occurrence offsets.
            Map<String, Posting> postingForTermInThisDoc = new LinkedHashMap<>();
            for (UnicodeUtil.Token token : UnicodeUtil.tokenizeWithOffsets(doc.getSearchableContent())) {
                postingForTermInThisDoc
                        .computeIfAbsent(token.text(), k -> new Posting(doc.getId()))
                        .addOccurrence(token.start());
            }

            for (Map.Entry<String, Posting> entry : postingForTermInThisDoc.entrySet()) {
                index.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
                totalPostings++;
            }
        }
    }

    // ------------------------------------------------------------------
    // Exact-token candidate retrieval
    // ------------------------------------------------------------------

    /** Documents containing the given (searchable-form) term as a whole indexed token. */
    public Set<String> getCandidateDocuments(String term) {
        List<Posting> postings = index.get(term);
        if (postings == null) return Collections.emptySet();
        Set<String> docs = new LinkedHashSet<>();
        for (Posting p : postings) docs.add(p.getDocumentId());
        return docs;
    }

    /** OR semantics: union of each term's postings. */
    public Set<String> getCandidateDocumentsForAny(List<String> terms) {
        Set<String> result = new LinkedHashSet<>();
        for (String term : terms) result.addAll(getCandidateDocuments(term));
        return result;
    }

    /**
     * AND semantics: intersection of each term's postings, i.e.
     * postings(t1) INTERSECT postings(t2) INTERSECT ... INTERSECT postings(tN).
     * Smallest posting set first, early exit when the running set is empty.
     */
    public Set<String> getCandidateDocumentsForAll(List<String> terms) {
        List<Set<String>> sets = new ArrayList<>();
        for (String term : terms) sets.add(getCandidateDocuments(term));
        return intersect(sets);
    }

    // ------------------------------------------------------------------
    // Containment (substring-in-word) candidate retrieval
    // ------------------------------------------------------------------

    /** Every dictionary term that contains the keyword (found with KMP over the dictionary). */
    public List<String> getDictionaryTermsContaining(String keyword) {
        List<String> terms = new ArrayList<>();
        if (keyword == null || keyword.isEmpty()) return terms;
        int[] lps = KMP.buildLPS(keyword); // built ONCE, reused for every dictionary term
        for (String term : index.keySet()) {
            if (KMP.contains(term, keyword, lps)) terms.add(term);
        }
        Collections.sort(terms);
        return terms;
    }

    /** Documents containing the keyword anywhere inside a word (see class comment). */
    public Set<String> getDocumentsContaining(String keyword) {
        Set<String> cached = containingCache.get(keyword);
        if (cached != null) return cached;
        Set<String> docs = new LinkedHashSet<>();
        for (String term : getDictionaryTermsContaining(keyword)) {
            for (Posting p : index.get(term)) docs.add(p.getDocumentId());
        }
        if (containingCache.size() < CACHE_LIMIT) containingCache.put(keyword, docs);
        return docs;
    }

    /** True if the keyword occurs inside at least one dictionary term. */
    public boolean dictionaryContains(String keyword) {
        if (index.containsKey(keyword)) return true;
        if (containingCache.containsKey(keyword)) return !containingCache.get(keyword).isEmpty();
        int[] lps = KMP.buildLPS(keyword);
        for (String term : index.keySet()) {
            if (KMP.contains(term, keyword, lps)) return true;
        }
        return false;
    }

    /**
     * AND candidates for a multi-keyword query using containment semantics.
     * Result = docsContaining(k1) INTERSECT ... INTERSECT docsContaining(kN).
     * Also fills perKeywordCounts (keyword -> number of documents containing it)
     * when a non-null map is supplied.
     */
    public Set<String> getCandidateDocumentsForAllContaining(List<String> keywords,
                                                             Map<String, Integer> perKeywordCounts) {
        List<Set<String>> sets = new ArrayList<>();
        for (String kw : keywords) {
            Set<String> docs = getDocumentsContaining(kw);
            if (perKeywordCounts != null) perKeywordCounts.put(kw, docs.size());
            sets.add(docs);
        }
        return intersect(sets);
    }

    private static Set<String> intersect(List<Set<String>> sets) {
        if (sets.isEmpty()) return new LinkedHashSet<>();
        List<Set<String>> ordered = new ArrayList<>(sets);
        ordered.sort(Comparator.comparingInt(Set::size));
        Set<String> result = new LinkedHashSet<>(ordered.get(0));
        for (int i = 1; i < ordered.size() && !result.isEmpty(); i++) {
            result.retainAll(ordered.get(i));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Dictionary access
    // ------------------------------------------------------------------

    /** The dictionary: every distinct indexed term. */
    public Set<String> getDictionary() {
        return Collections.unmodifiableSet(index.keySet());
    }

    /** Number of documents in which the term occurs as a whole token. */
    public int getDocumentFrequency(String term) {
        List<Posting> postings = index.get(term);
        return postings == null ? 0 : postings.size();
    }

    public Document getDocument(String id) {
        return documentsById.get(id);
    }

    public Collection<Document> getAllDocuments() {
        return documentsById.values();
    }

    public int getVocabularySize() {
        return index.size();
    }

    public int getTotalPostings() {
        return totalPostings;
    }

    public int getDocumentCount() {
        return documentsById.size();
    }

    public long getTotalCharacters() {
        long total = 0;
        for (Document d : documentsById.values()) total += d.getSearchableContent().length();
        return total;
    }
}
