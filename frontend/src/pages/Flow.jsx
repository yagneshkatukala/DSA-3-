import { useState } from 'react';
import { getFlow } from '../services/api';

const ALGOS = [
  { value: 'dinic', label: 'Dinic' },
  { value: 'edmondskarp', label: 'Edmonds–Karp' },
  { value: 'fordfulkerson', label: 'Ford–Fulkerson' },
];

export default function Flow() {
  const [query, setQuery] = useState('');
  const [language, setLanguage] = useState('all');
  const [algorithm, setAlgorithm] = useState('dinic');
  const [demand, setDemand] = useState(1);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  function run(e) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    setError(null);
    getFlow({ query: query.trim(), language, algorithm, demand })
      .then(setData)
      .catch((err) => { setData(null); setError(err.message); })
      .finally(() => setLoading(false));
  }

  return (
    <div className="shell" style={{ paddingTop: 56, paddingBottom: 96 }}>
      <p className="mono" style={{ fontSize: 12, color: 'var(--muted)' }}>dsa analysis · network flow (CO4)</p>
      <h1 style={{ fontSize: 34, marginTop: 10, marginBottom: 12 }}>Keyword–Document Matching</h1>
      <p style={{ marginBottom: 32, maxWidth: 720, lineHeight: 1.6 }}>
        Source → keyword nodes → document nodes → sink. An edge keyword→document exists when the inverted index says the
        document contains the keyword. Max flow = the largest set of keywords that can each be given a distinct witness
        document; the min cut shows the bottleneck. This analysis is separate from normal search.
      </p>

      <form onSubmit={run} className="panel" style={{ padding: 24, display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 90px auto', gap: 16, alignItems: 'end', marginBottom: 40 }}>
        <div>
          <label className="field-label" htmlFor="fq">Keywords (space-separated, max 50)</label>
          <input id="fq" type="text" className="indic" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="india water river" />
        </div>
        <div>
          <label className="field-label" htmlFor="fl">Language</label>
          <select id="fl" value={language} onChange={(e) => setLanguage(e.target.value)}>
            {['all', 'english', 'telugu', 'hindi', 'tamil', 'bengali'].map((l) => <option key={l} value={l}>{l}</option>)}
          </select>
        </div>
        <div>
          <label className="field-label" htmlFor="fa">Flow algorithm</label>
          <select id="fa" value={algorithm} onChange={(e) => setAlgorithm(e.target.value)}>
            {ALGOS.map((a) => <option key={a.value} value={a.value}>{a.label}</option>)}
          </select>
        </div>
        <div>
          <label className="field-label" htmlFor="fd">Demand</label>
          <input id="fd" type="number" min="1" max="50" value={demand} onChange={(e) => setDemand(Number(e.target.value) || 1)} style={{ width: '100%', padding: '10px 12px', background: 'var(--panel-raised)', border: '1px solid var(--line)', color: 'var(--paper)' }} />
        </div>
        <button className="btn" type="submit" disabled={loading}>{loading ? 'Running…' : 'Analyse'}</button>
      </form>

      {error && <p style={{ color: 'var(--maroon)' }}>{error}</p>}

      {data && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <div className="panel" style={{ padding: 24, display: 'flex', gap: 32, flexWrap: 'wrap' }}>
            <Stat label="Keyword nodes" value={data.keywordNodes} />
            <Stat label="Document nodes" value={data.documentNodes} />
            <Stat label="Keyword→doc edges" value={data.keywordDocumentEdges} />
            <Stat label="Total edges" value={data.totalEdges} />
            <Stat label="Max flow" value={`${data.maxFlow} / ${data.requestedFlow}`} accent />
            <Stat label="Min-cut capacity" value={data.minCut.capacity} />
            <Stat label={data.algorithm} value={`${data.executionTimeMs} ms`} />
          </div>

          <p>
            {data.fullyMatched
              ? 'Every keyword can be assigned its own distinct document(s).'
              : 'Not every keyword can be fully assigned — the bottleneck keywords are listed below.'}
            {' '}Max-flow = min-cut: {String(data.minCut.equalsMaxFlow)} · all algorithms agree: {String(data.algorithmsAgree)}
          </p>

          <div className="panel" style={{ padding: 24 }}>
            <h3 style={{ fontSize: 16, marginBottom: 12 }}>Algorithm comparison (same network)</h3>
            {data.algorithmRuns.map((r) => (
              <div key={r.algorithm} className="mono" style={{ fontSize: 13, padding: '4px 0' }}>
                {r.algorithm}: flow {r.maxFlow} · {r.augmentationsOrPhases} augmentations/phases · {r.executionTimeMs} ms
              </div>
            ))}
          </div>

          {data.unmatchedKeywords.length > 0 && (
            <div className="panel" style={{ padding: 24, borderColor: 'var(--maroon)' }}>
              <h3 style={{ fontSize: 16, marginBottom: 12 }}>Bottleneck keywords</h3>
              {data.unmatchedKeywords.map((u) => (
                <div key={u.keyword} className="mono" style={{ fontSize: 13 }}>
                  {u.keyword}: assigned {u.assigned} of {u.demand} · only {u.documentsContainingKeyword} document(s) contain it
                </div>
              ))}
            </div>
          )}

          <div className="panel" style={{ padding: 24 }}>
            <h3 style={{ fontSize: 16, marginBottom: 12 }}>Matching ({data.matchingSize} pairs)</h3>
            <div style={{ maxHeight: 320, overflowY: 'auto' }}>
              {data.matching.map((m, i) => (
                <div key={i} className="mono indic" style={{ fontSize: 13, padding: '3px 0' }}>
                  {m.keyword} → {m.title} <span style={{ color: 'var(--muted)' }}>({m.documentId})</span>
                </div>
              ))}
            </div>
          </div>

          <div className="panel" style={{ padding: 24 }}>
            <h3 style={{ fontSize: 16, marginBottom: 12 }}>Min cut ({data.minCut.cutEdgeCount} edges)</h3>
            <div style={{ maxHeight: 240, overflowY: 'auto' }}>
              {data.minCut.cutEdges.map((c, i) => (
                <div key={i} className="mono" style={{ fontSize: 13 }}>{c.from} → {c.to} (cap {c.capacity})</div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value, accent }) {
  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>{label}</div>
      <div className="mono" style={{ fontSize: 15, color: accent ? 'var(--saffron)' : 'var(--paper)' }}>{value}</div>
    </div>
  );
}
