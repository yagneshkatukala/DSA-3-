import { useState } from 'react';
import { getPerformance } from '../services/api';

const LANGUAGES = ['all', 'telugu', 'hindi', 'tamil', 'bengali', 'english'];

const BAR_COLOR = {
  Naive: 'var(--maroon)',
  KMP: 'var(--saffron)',
  'Rabin-Karp': 'var(--teal)',
  'Z-Algorithm': '#6fbf73',
  'Aho-Corasick': '#8f7fe0',
};

export default function Performance() {
  const [query, setQuery] = useState('');
  const [language, setLanguage] = useState('all');
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  function runComparison(e) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    setError(null);
    getPerformance({ query: query.trim(), language })
      .then(setData)
      .catch((e2) => setError(e2.message))
      .finally(() => setLoading(false));
  }

  const maxTime = data ? Math.max(...data.comparison.map((c) => c.executionTimeMs), 0.001) : 0;

  return (
    <div className="shell" style={{ paddingTop: 56, paddingBottom: 96 }}>
      <p className="mono" style={{ fontSize: 12, color: 'var(--muted)' }}>algorithm performance</p>
      <h1 style={{ fontSize: 34, marginTop: 10, marginBottom: 32 }}>Naive · KMP · Rabin–Karp · Z · Aho–Corasick</h1>

      <form onSubmit={runComparison} className="panel" style={{ padding: 24, display: 'flex', gap: 16, alignItems: 'end', marginBottom: 40 }}>
        <div style={{ flex: 1 }}>
          <label className="field-label" htmlFor="pq">Keyword to benchmark</label>
          <input id="pq" type="text" className="indic" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="తెలుగు" />
        </div>
        <div style={{ width: 180 }}>
          <label className="field-label" htmlFor="pl">Language</label>
          <select id="pl" value={language} onChange={(e) => setLanguage(e.target.value)}>
            {LANGUAGES.map((l) => <option key={l} value={l}>{l === 'all' ? 'All languages' : l}</option>)}
          </select>
        </div>
        <button className="btn" type="submit" disabled={loading}>{loading ? 'Running…' : 'Run comparison'}</button>
      </form>

      {error && <p style={{ color: 'var(--maroon)' }}>{error}</p>}

      {data && (
        <div className="panel" style={{ padding: 32 }}>
          <p style={{ marginBottom: 24, fontSize: 13 }}>
            Candidate documents come from the inverted index (of {data.comparison[0]?.totalDocuments} total), not a full corpus scan.
            Query: <span className="indic">"{data.query}"</span>. Times are measured live with <code className="mono">System.nanoTime()</code> on this run — nothing here is precomputed.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            {data.comparison.map((row) => (
              <div key={row.algorithm}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 6 }}>
                  <span>{row.algorithm}</span>
                  <span className="mono">{row.executionTimeMs} ms · {row.matches} matches · {row.documentsScanned} docs · {row.scope}</span>
                </div>
                <div style={{ height: 10, background: 'var(--panel-raised)', border: '1px solid var(--line)' }}>
                  <div
                    style={{
                      height: '100%',
                      width: `${Math.max(4, (row.executionTimeMs / maxTime) * 100)}%`,
                      background: BAR_COLOR[row.algorithm] || 'var(--teal)',
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {!data && !error && (
        <div className="panel" style={{ padding: 32, color: 'var(--muted)' }}>
          <p>Enter a keyword above to benchmark Naive substring search against KMP, Rabin–Karp and Aho–Corasick on the current corpus.</p>
        </div>
      )}
    </div>
  );
}
