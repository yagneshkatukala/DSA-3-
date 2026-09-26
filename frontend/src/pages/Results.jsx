import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { search } from '../services/api';
import ResultCard from '../components/ResultCard';

export default function Results() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const query = params.get('query') || '';
  const language = params.get('language') || 'all';
  const algorithm = params.get('algorithm') || 'auto';
  const mode = params.get('mode') || '';
  const searchMode = params.get('searchMode') || 'exact';

  useEffect(() => {
    if (!query) {
      navigate('/');
      return;
    }
    setLoading(true);
    setError(null);
    search({ query, language, algorithm, mode, searchMode })
      .then(setData)
      .catch((e) => setError(e))
      .finally(() => setLoading(false));
  }, [query, language, algorithm, mode, searchMode]);

  return (
    <div className="shell" style={{ paddingTop: 56, paddingBottom: 96 }}>
      <div style={{ marginBottom: 40 }}>
        <p className="mono" style={{ fontSize: 12, color: 'var(--muted)' }}>search results</p>
        <h1 className="indic" style={{ fontSize: 36, marginTop: 10 }}>{query}</h1>
      </div>

      {loading && <p>Running search…</p>}

      {error && (
        <div className="panel" style={{ padding: 24, borderColor: 'var(--maroon)' }}>
          {error.status === 400 ? (
            <p style={{ color: 'var(--maroon)' }}>{error.message}</p>
          ) : (
            <p style={{ color: 'var(--maroon)' }}>
              Couldn't reach the search backend ({error.message}). Make sure the Java API server is running on port 8080.
            </p>
          )}
        </div>
      )}

      {data && !loading && !error && (
        <>
          <div className="hairline" style={{ marginBottom: 28 }} />
          <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap', marginBottom: 40, fontSize: 13 }}>
            <Stat label="Algorithm" value={data.algorithm.toUpperCase()} accent />
            <Stat label="Language" value={data.language} />
            <Stat label="Mode" value={`${data.mode} · ${data.searchMode}`} />
            <Stat label="Keywords" value={`${data.keywordCount} (AND)`} mono />
            <Stat label="Execution time" value={`${data.executionTimeMs} ms`} mono />
            <Stat label="Candidates scanned" value={`${data.documentsScanned} / ${data.totalDocuments}`} mono />
            <Stat label="Results" value={data.totalResults} mono />
          </div>

          {data.algorithmNote && <p style={{ fontSize: 12, marginBottom: 16 }}>{data.algorithmNote}</p>}
          {data.corrections && data.corrections.length > 0 && (
            <div className="panel" style={{ padding: 16, marginBottom: 16 }}>
              {data.corrections.map((c) => (
                <p key={c.original}>Fuzzy search: <b>{c.original}</b> → <b style={{ color: 'var(--saffron)' }}>{c.corrected}</b> (edit distance {c.editDistance})</p>
              ))}
            </div>
          )}
          {data.keywordDocumentCounts && data.keywords.length > 1 && (
            <p className="mono" style={{ fontSize: 12, marginBottom: 16 }}>
              documents per keyword: {Object.entries(data.keywordDocumentCounts).slice(0, 12).map(([k, v]) => `${k}=${v}`).join(' · ')}{data.keywords.length > 12 ? ' · …' : ''}
            </p>
          )}

          {data.results.length === 0 ? (
            <div className="panel" style={{ padding: 32 }}>
              <p>{data.message || 'No documents contain all of the keywords.'}</p>
              {data.suggestions && Object.keys(data.suggestions).length > 0 && (
                <div style={{ marginTop: 16 }}>
                  {Object.entries(data.suggestions).map(([kw, list]) => (
                    <p key={kw} style={{ marginTop: 6 }}>
                      Did you mean for <b>{kw}</b>:{' '}
                      {list.map((s) => (
                        <a key={s.term} href={`/results?${new URLSearchParams({ query: data.query.split(/\s+/).map((w) => (w.toLowerCase() === kw ? s.term : w)).join(' '), language, algorithm, mode, searchMode: 'exact' })}`}
                          style={{ color: 'var(--saffron)', marginRight: 12 }}>{s.term}</a>
                      ))}
                    </p>
                  ))}
                </div>
              )}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {data.results.map((r, i) => (
                <ResultCard key={r.documentId} result={r} index={i} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

function Stat({ label, value, accent, mono }) {
  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>{label}</div>
      <div className={mono ? 'mono' : ''} style={{ fontSize: 15, color: accent ? 'var(--saffron)' : 'var(--paper)', textTransform: accent ? 'none' : 'capitalize' }}>
        {value}
      </div>
    </div>
  );
}
