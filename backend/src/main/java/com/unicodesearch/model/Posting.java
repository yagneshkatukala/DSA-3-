package com.unicodesearch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A posting in the inverted index: for a given term, which document it
 * appears in, how many times, and at which character positions.
 */
public class Posting {
    private final String documentId;
    private int frequency;
    private final List<Integer> positions = new ArrayList<>();

    public Posting(String documentId) {
        this.documentId = documentId;
    }

    public void addOccurrence(int position) {
        positions.add(position);
        frequency++;
    }

    public String getDocumentId() { return documentId; }
    public int getFrequency() { return frequency; }
    public List<Integer> getPositions() { return positions; }
}
