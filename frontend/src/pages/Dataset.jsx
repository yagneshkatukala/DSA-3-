import { useEffect, useState } from 'react';
import { getStatistics, rebuildIndex } from '../services/api';

export default function Dataset() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(null);
  const [rebuilding, setRebuilding] = useState(false);

  function load() {
    getStatistics().then(setStats).catch((e) => setError(e.message));
  }

  useEffect(load, []);

  async function handleRebuild() {
    setRebuilding(true);
    try {
      await rebuildIndex();
      load();
    } finally {
      setRebuilding(false);
    }
  }

  return (
    <div className="shell" style={{ paddingTop: 56, paddingBottom: 96 }}>
      <p className="mono" style={{ fontSize: 12, color: 'var(--muted)' }}>dataset & index</p>
      <h1 style={{ fontSize: 34, marginTop: 10, marginBottom: 32 }}>Corpus & inverted index statistics</h1>

      {error && (
        <div className="panel" style={{ padding: 24, borderColor: 'var(--maroon)' }}>
          <p style={{ color: 'var(--maroon)' }}>Couldn't reach the backend ({error}). Start the Java API server on port 8080.</p>
        </div>
      )}

      {stats && (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
            <StatCard label="Documents" value={stats.documentCount} />
            <StatCard label="Languages" value={stats.languageCount ?? stats.languages.length} />
            <StatCard label="Indexed terms" value={stats.indexedTerms} />
            <StatCard label="Postings" value={stats.totalPostings} />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 40 }}>
            <StatCard label="Characters" value={stats.totalCharacters} />
            <StatCard label="Total searches" value={stats.totalSearches ?? 0} />
            <StatCard label="Max keywords / query" value={stats.maxKeywordsPerQuery ?? '—'} />
          </div>

          <div className="panel" style={{ padding: 32, marginBottom: 32 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h3 style={{ fontSize: 18 }}>Documents by language</h3>
              <span className="mono" style={{ fontSize: 12, color: 'var(--teal)' }}>index status: {stats.indexStatus}</span>
            </div>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              {stats.languages.map((l) => (
                <span key={l} className="mono" style={{ fontSize: 12, padding: '8px 14px', border: '1px solid var(--line)', textTransform: 'capitalize' }}>
                  {l}
                  {stats.documentsByLanguage && stats.documentsByLanguage[l] !== undefined && (
                    <> · {stats.documentsByLanguage[l]}</>
                  )}
                </span>
              ))}
            </div>
            {stats.lastIndexBuild && (
              <p style={{ fontSize: 12, marginTop: 20, color: 'var(--muted)' }}>
                Last index build: <span className="mono">{stats.lastIndexBuild}</span>
              </p>
            )}
          </div>

          <button className="btn btn-ghost" onClick={handleRebuild} disabled={rebuilding}>
            {rebuilding ? 'Rebuilding index…' : 'Rebuild index from corpus/'}
          </button>
          <p style={{ fontSize: 12, marginTop: 10 }}>
            Re-reads every .txt file under corpus/ and reconstructs the inverted index from scratch — use this after adding new documents.
          </p>
        </>
      )}
    </div>
  );
}

function StatCard({ label, value }) {
  return (
    <div className="panel" style={{ padding: 24 }}>
      <div className="mono" style={{ fontSize: 28, color: 'var(--saffron)' }}>{value}</div>
      <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 8 }}>{label}</div>
    </div>
  );
}
