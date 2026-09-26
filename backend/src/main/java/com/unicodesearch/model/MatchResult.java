package com.unicodesearch.model;

import java.util.List;

/** Output of a pattern-matching algorithm run against a single document. */
public class MatchResult {
    private final String keyword;
    private final List<Integer> positions;
    private final double executionTimeMs;

    public MatchResult(String keyword, List<Integer> positions, double executionTimeMs) {
        this.keyword = keyword;
        this.positions = positions;
        this.executionTimeMs = executionTimeMs;
    }

    public String getKeyword() { return keyword; }
    public List<Integer> getPositions() { return positions; }
    public int getMatchCount() { return positions.size(); }
    public double getExecutionTimeMs() { return executionTimeMs; }
}
