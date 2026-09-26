package com.unicodesearch.service;

import com.unicodesearch.algorithm.AhoCorasick;
import com.unicodesearch.algorithm.KMP;
import com.unicodesearch.algorithm.NaiveSearch;
import com.unicodesearch.algorithm.RabinKarp;
import com.unicodesearch.algorithm.ZAlgorithm;
import com.unicodesearch.index.InvertedIndex;
import com.unicodesearch.model.Document;
import com.unicodesearch.model.MatchResult;
import com.unicodesearch.model.SearchResultItem;
import com.unicodesearch.ranking.RankingEngine;
import com.unicodesearch.snippet.SnippetExtractor;
import com.unicodesearch.util.UnicodeUtil;

import java.util.*;

/**
 * Central orchestrator.
 *
 *   raw query
 *     -> Unicode preprocessing (NFC + Latin case folding)      UnicodeUtil
 *     -> tokenization into keywords (max 50)
 *     -> [fuzzy: correct absent keywords via Levenshtein DP]   FuzzySearch
 *     -> inverted-index candidates:  docs(k1) AND docs(k2) ... AND docs(kN)
 *     -> pattern matching on the searchable text
 *           1 keyword  : KMP | Rabin-Karp | Z | Naive (| Aho-Corasick if chosen)
 *           2+ keywords: Aho-Corasick, one pass per document
 *     -> AND filter: every keyword must have >= 1 match
 *     -> ranking -> multi-line snippets -> results
 *
 * Both the index terms and the matcher input use the SAME searchable
 * representation, which is what fixes the earlier case-mismatch bug
 * ("India" in a document vs. "india" in the index).
 */
public class SearchEngine {

    /** Hard cap on how many space-separated keywords a single query may contain. */
    public static final int MAX_KEYWORDS = 50;

    private static final int SUGGESTIONS_PER_KEYWORD = 5;

    private final InvertedIndex invertedIndex;
    private final RankingEngine rankingEngine = new RankingEngine();
    private final FuzzySearch fuzzySearch;

    public SearchEngine(InvertedIndex invertedIndex) {
        this.invertedIndex = invertedIndex;
        this.fuzzySearch = new FuzzySearch(invertedIndex);
    }

    public FuzzySearch getFuzzySearch() { return fuzzySearch; }
    public InvertedIndex getInvertedIndex() { return invertedIndex; }

    public static class SearchResponse {
        public String query;
        public String language;
        public String algorithm;
        public String requestedAlgorithm;
        public String algorithmNote;
        public String mode;
        public String searchMode = "exact";
        public double executionTimeMs;
        public int documentsScanned;
        public int totalDocuments;
        public int totalResults;
        public int keywordCount;
        public List<String> keywords = new ArrayList<>();
        public Map<String, Integer> keywordDocumentCounts = new LinkedHashMap<>();
        public List<Map<String, Object>> corrections = new ArrayList<>();
        public Map<String, List<Map<String, Object>>> suggestions = new LinkedHashMap<>();
        public String message;
        public List<SearchResultItem> results = Collections.emptyList();
        /** Non-null only when the query was rejected (e.g. too many keywords). */
        public String error;
    }

    /** Backward-compatible entry point: exact search. */
    public SearchResponse search(String rawQuery, String language, String algorithm, String mode) {
        return search(rawQuery, language, algorithm, mode, "exact");
    }

    /**
     * @param rawQuery   user's query (space-separated keywords)
     * @param language   "all" or a language folder name
     * @param algorithm  auto | kmp | rabinkarp | naive | zalgorithm | ahocorasick
     * @param mode       single | multi | "" (informational -- the effective mode is derived
     *                   from the number of keywords: 2+ keywords are ALWAYS AND-matched)
     * @param searchMode exact | fuzzy
     */
    public SearchResponse search(String rawQuery, String language, String algorithm, String mode, String searchMode) {
        long overallStart = System.nanoTime();

        SearchResponse response = new SearchResponse();
        response.query = rawQuery;
        response.language = (language == null || language.isBlank()) ? "all" : language;
        response.requestedAlgorithm = algorithm;
        response.searchMode = "fuzzy".equalsIgnoreCase(searchMode) ? "fuzzy" : "exact";
        response.totalDocuments = invertedIndex.getDocumentCount();

        // ---- 1. Unicode preprocessing + tokenization ----
        List<String> tokens = UnicodeUtil.tokenize(rawQuery);
        response.keywordCount = tokens.size();

        if (tokens.isEmpty()) {
            response.algorithm = resolveAlgorithm(algorithm, 1);
            response.mode = "single";
            response.message = "Enter at least one keyword.";
            response.executionTimeMs = elapsedMs(overallStart);
            return response;
        }

        // Reject (never truncate) queries over the keyword limit.
        if (tokens.size() > MAX_KEYWORDS) {
            response.algorithm = resolveAlgorithm(algorithm, 2);
            response.mode = "multi";
            response.executionTimeMs = elapsedMs(overallStart);
            response.error = "Too many keywords: " + tokens.size() + " provided. Maximum " + MAX_KEYWORDS
                    + " keywords allowed.";
            return response;
        }

        // A repeated keyword is the same requirement; keep first occurrences, in order.
        List<String> keywords = new ArrayList<>(new LinkedHashSet<>(tokens));

        // ---- 2. Dictionary check + fuzzy correction (CO3) ----
        boolean fuzzy = "fuzzy".equals(response.searchMode);
        List<String> searchKeywords = new ArrayList<>();
        for (String kw : keywords) {
            if (invertedIndex.dictionaryContains(kw)) {
                searchKeywords.add(kw);
                continue;
            }
            List<FuzzySearch.Suggestion> sugg = fuzzySearch.suggest(kw, SUGGESTIONS_PER_KEYWORD);
            if (!sugg.isEmpty()) {
                List<Map<String, Object>> sj = new ArrayList<>();
                for (FuzzySearch.Suggestion s : sugg) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("term", s.term());
                    m.put("distance", s.distance());
                    m.put("documentFrequency", s.documentFrequency());
                    sj.add(m);
                }
                response.suggestions.put(kw, sj);
            }
            if (fuzzy && !sugg.isEmpty()) {
                FuzzySearch.Suggestion best = sugg.get(0);
                Map<String, Object> corr = new LinkedHashMap<>();
                corr.put("original", kw);
                corr.put("corrected", best.term());
                corr.put("editDistance", best.distance());
                corr.put("alternatives", sugg.stream().skip(1).map(FuzzySearch.Suggestion::term).toList());
                response.corrections.add(corr);
                searchKeywords.add(best.term());
            } else {
                searchKeywords.add(kw); // stays absent -> zero results, handled gracefully below
            }
        }
        searchKeywords = new ArrayList<>(new LinkedHashSet<>(searchKeywords));
        response.keywords = searchKeywords;

        boolean multi = searchKeywords.size() >= 2;
        response.mode = multi ? "multi" : "single";
        String resolved = resolveAlgorithm(algorithm, searchKeywords.size());
        if (multi && !"ahocorasick".equals(resolved)) {
            response.algorithmNote = "Multi-keyword queries use Aho-Corasick (one pass, AND semantics); the selected "
                    + "single-pattern algorithm applies to one-keyword queries.";
            resolved = "ahocorasick";
        }
        response.algorithm = resolved;

        // ---- 3. Candidate documents from the inverted index: docs(k1) AND ... AND docs(kN) ----
        Map<String, Integer> perKeywordCounts = new LinkedHashMap<>();
        Set<String> candidateIds = invertedIndex.getCandidateDocumentsForAllContaining(searchKeywords, perKeywordCounts);
        response.keywordDocumentCounts = perKeywordCounts;

        List<Document> candidates = new ArrayList<>();
        for (String id : candidateIds) {
            Document doc = invertedIndex.getDocument(id);
            if (doc == null) continue;
            if (!"all".equalsIgnoreCase(response.language) && !doc.getLanguage().equalsIgnoreCase(response.language)) continue;
            candidates.add(doc);
        }

        // Exact whole-word hits per keyword (used only for the ranking bonus).
        Map<String, Set<String>> exactDocs = new HashMap<>();
        for (String kw : searchKeywords) exactDocs.put(kw, invertedIndex.getCandidateDocuments(kw));

        // ---- 4. Pattern matching + AND filter + ranking + snippets ----
        AhoCorasick automaton = "ahocorasick".equals(resolved) ? new AhoCorasick(searchKeywords) : null;
        List<SearchResultItem> results = new ArrayList<>();

        for (Document doc : candidates) {
            String text = doc.getSearchableContent();
            Map<String, List<Integer>> positions = new LinkedHashMap<>();

            if (automaton != null) {
                Map<String, List<Integer>> found = automaton.searchAll(text); // ONE pass, all keywords
                for (String kw : searchKeywords) positions.put(kw, found.getOrDefault(kw, Collections.emptyList()));
            } else {
                String kw = searchKeywords.get(0);
                MatchResult mr = switch (resolved) {
                    case "rabinkarp" -> RabinKarp.search(text, kw);
                    case "naive" -> NaiveSearch.search(text, kw);
                    case "zalgorithm" -> ZAlgorithm.search(text, kw);
                    default -> KMP.search(text, kw);
                };
                positions.put(kw, mr.getPositions());
            }

            // AND filter: reject the document if ANY keyword has zero matches.
            Map<String, Integer> freq = new LinkedHashMap<>();
            boolean hasAll = true;
            for (String kw : searchKeywords) {
                int c = positions.get(kw).size();
                freq.put(kw, c);
                if (c == 0) { hasAll = false; break; }
            }
            if (!hasAll) continue;

            int totalMatches = freq.values().stream().mapToInt(Integer::intValue).sum();
            boolean allExact = true;
            for (String kw : searchKeywords) {
                if (!exactDocs.get(kw).contains(doc.getId())) { allExact = false; break; }
            }

            double score = rankingEngine.score(doc, freq, searchKeywords, allExact);
            String snippet = SnippetExtractor.build(doc.getNormalizedContent(), positions);

            results.add(new SearchResultItem(doc.getId(), doc.getTitle(), doc.getLanguage(),
                    totalMatches, score, snippet, freq, new ArrayList<>(searchKeywords)));
        }

        results.sort(Comparator.comparingDouble(SearchResultItem::getScore).reversed()
                .thenComparing(Comparator.comparingInt(SearchResultItem::getMatchCount).reversed())
                .thenComparing(SearchResultItem::getTitle));

        response.documentsScanned = candidates.size();
        response.totalResults = results.size();
        response.results = results;
        response.message = buildMessage(response, searchKeywords, multi);
        response.executionTimeMs = elapsedMs(overallStart);
        return response;
    }

    private String buildMessage(SearchResponse r, List<String> keywords, boolean multi) {
        if (!r.corrections.isEmpty() && r.totalResults > 0) {
            StringBuilder sb = new StringBuilder("Fuzzy search corrected: ");
            for (int i = 0; i < r.corrections.size(); i++) {
                Map<String, Object> c = r.corrections.get(i);
                if (i > 0) sb.append(", ");
                sb.append(c.get("original")).append(" -> ").append(c.get("corrected"))
                        .append(" (edit distance ").append(c.get("editDistance")).append(")");
            }
            return sb.toString();
        }
        if (r.totalResults > 0) return null;

        List<String> absent = new ArrayList<>();
        for (String kw : keywords) if (!invertedIndex.dictionaryContains(kw)) absent.add(kw);
        if (!absent.isEmpty()) {
            return "No documents found: " + String.join(", ", absent) + (absent.size() == 1 ? " does" : " do")
                    + " not appear in the indexed dictionary."
                    + (r.suggestions.isEmpty() ? "" : " See the suggestions below, or try Fuzzy search.");
        }
        if (multi) return "Each keyword exists in the corpus, but no document contains ALL " + keywords.size()
                + " keywords" + ("all".equalsIgnoreCase(r.language) ? "." : " in the selected language.");
        return "No documents contain \"" + keywords.get(0) + "\""
                + ("all".equalsIgnoreCase(r.language) ? "." : " in the selected language.");
    }

    /** Normalizes an algorithm name; "auto" -> KMP for one keyword, Aho-Corasick for several. */
    static String resolveAlgorithm(String requested, int keywordCount) {
        if (requested == null || requested.isBlank() || "auto".equalsIgnoreCase(requested)) {
            return keywordCount > 1 ? "ahocorasick" : "kmp";
        }
        String a = requested.toLowerCase().replace("-", "").replace("_", "").replace(" ", "");
        return switch (a) {
            case "z", "zalgo", "zalgorithm" -> "zalgorithm";
            case "rabinkarp", "rk" -> "rabinkarp";
            case "aho", "ahocorasick" -> "ahocorasick";
            case "naive" -> "naive";
            default -> "kmp";
        };
    }

    private double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
