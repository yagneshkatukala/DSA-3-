package com.unicodesearch.flow;

/** Outcome of one max-flow run. */
public record FlowResult(String algorithm, int maxFlow, int augmentations, double executionTimeMs) { }
