const ALGORITHMS = [
  { name: 'KMP', detail: 'Single-keyword exact matching using an LPS/failure-function array — O(n + m).' },
  { name: 'Rabin–Karp', detail: 'Rolling-hash single-keyword matching with collision verification — average O(n + m).' },
  { name: 'Aho–Corasick', detail: 'Trie + failure links; matches all query keywords in one linear text pass.' },
  { name: 'Z-Algorithm', detail: 'Z-array of pattern + separator + text; linear-time single-keyword matching — O(n + m).' },
  { name: 'Levenshtein DP', detail: 'Edit-distance dynamic programming over the index dictionary for typo-tolerant "did you mean" search (CO3).' },
  { name: 'Network Flow', detail: 'Keyword–document bipartite graph: Ford-Fulkerson, Edmonds-Karp, Dinic max flow, matching and min-cut (CO4).' },
  { name: 'Inverted Index', detail: 'HashMap<term, postings> narrows every query to a small candidate set before pattern matching runs.' },
];

export default function About() {
  return (
    <div className="shell" style={{ paddingTop: 56, paddingBottom: 96, maxWidth: 760 }}>
      <p className="mono" style={{ fontSize: 12, color: 'var(--muted)' }}>about the project</p>
      <h1 style={{ fontSize: 34, marginTop: 10, marginBottom: 24 }}>Unicode Search Engine</h1>
      <p style={{ fontSize: 16, lineHeight: 1.7, marginBottom: 32 }}>
        A Unicode-aware text search engine for multilingual content — Telugu, Hindi,
        Tamil, Bengali and English — built to demonstrate core data structures and
        algorithms rather than lean on a pre-built search library. The web interface is a
        thin client; every match, ranking score and execution time you see comes from a
        Java backend that implements KMP, Rabin–Karp, Aho–Corasick, a Trie and an
        inverted index by hand.
      </p>

      <h3 style={{ fontSize: 18, marginBottom: 16 }}>Problem statement</h3>
      <p style={{ marginBottom: 32, lineHeight: 1.7 }}>
        Multilingual content online is growing quickly, but naive substring
        search scales poorly once corpora and Unicode complexity grow. This
        project builds a fast, scalable, Unicode-aware search pipeline using
        classical pattern-matching and indexing algorithms, demonstrated over a
        500-document sample corpus (100 articles each across five languages).
      </p>

      <h3 style={{ fontSize: 18, marginBottom: 16 }}>Algorithms & data structures</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 0, marginBottom: 32 }}>
        {ALGORITHMS.map((a, i) => (
          <div key={a.name} style={{ padding: '16px 0', borderTop: i === 0 ? '1px solid var(--line)' : 'none', borderBottom: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', gap: 24 }}>
              <div className="mono" style={{ width: 140, flexShrink: 0, color: 'var(--saffron)', fontSize: 14 }}>{a.name}</div>
              <div style={{ fontSize: 14, color: 'var(--paper-dim)' }}>{a.detail}</div>
            </div>
          </div>
        ))}
      </div>

      <h3 style={{ fontSize: 18, marginBottom: 16 }}>Technology stack</h3>
      <p style={{ lineHeight: 1.7 }}>
        React + Vite on the frontend. The backend is plain Java (JDK 17+) running
        on the built-in <code className="mono">HttpServer</code>, with every algorithm
        hand-implemented — no Elasticsearch, Lucene, Solr, or regex-based matching.
      </p>
    </div>
  );
}
