package com.unicodesearch.ranking;

import com.unicodesearch.algorithm.KMP;
import com.unicodesearch.model.Document;

import java.util.List;
import java.util.Map;

/**
 * Transparent, explainable relevance scoring (deliberately NOT a
 * black-box ML ranker, so every score can be justified in a viva).
 *
 * score = (keyword frequency weight)
 *       + (title match bonus)
 *       + (exact/full-word match bonus)
 *       + (keyword coverage bonus, for multi-keyword queries)
 *
 * Weights are simple, fixed constants chosen so that:
 *   - raw match frequency matters, but doesn't dominate everything
 *   - a title hit is a strong relevance signal
 *   - matching MORE of the requested keywords (multi-keyword mode)
 *     outranks matching one keyword many times
 * Final score is normalized to a 0-100 scale for display.
 *
 * Ranking happens AFTER filtering: a document that does not contain every
 * query keyword has already been rejected, so it can never outrank (or even
 * appear next to) a valid document.
 */
public class RankingEngine {

    private static final double FREQUENCY_WEIGHT = 4.0;
    private static final double TITLE_MATCH_BONUS = 20.0;
    private static final double EXACT_MATCH_BONUS = 10.0;
    private static final double COVERAGE_WEIGHT = 15.0;

    public double score(Document doc, Map<String, Integer> keywordFrequencies,
                         List<String> queryKeywords, boolean exactMatchFound) {

        int totalFrequency = keywordFrequencies.values().stream().mapToInt(Integer::intValue).sum();
        double freqScore = Math.min(totalFrequency, 15) * FREQUENCY_WEIGHT; // diminishing-returns cap

        boolean titleMatch = false;
        for (String kw : queryKeywords) {
            // case-folded title vs. case-folded keyword, matched with KMP (no String.contains)
            if (kw != null && !kw.isEmpty() && KMP.search(doc.getSearchableTitle(), kw).getMatchCount() > 0) {
                titleMatch = true;
                break;
            }
        }

        int matchedKeywordCount = (int) keywordFrequencies.values().stream().filter(f -> f > 0).count();
        double coverageRatio = queryKeywords.isEmpty() ? 0 : (double) matchedKeywordCount / queryKeywords.size();

        double raw = freqScore
                + (titleMatch ? TITLE_MATCH_BONUS : 0)
                + (exactMatchFound ? EXACT_MATCH_BONUS : 0)
                + (coverageRatio * COVERAGE_WEIGHT);

        double maxPossible = (15 * FREQUENCY_WEIGHT) + TITLE_MATCH_BONUS + EXACT_MATCH_BONUS + COVERAGE_WEIGHT;
        double normalized = Math.min(100.0, (raw / maxPossible) * 100.0);
        return Math.round(normalized * 100.0) / 100.0;
    }
}
