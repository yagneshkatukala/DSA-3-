import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const LANGUAGES = [
  { value: 'all', label: 'All languages' },
  { value: 'telugu', label: 'Telugu' },
  { value: 'hindi', label: 'Hindi' },
  { value: 'tamil', label: 'Tamil' },
  { value: 'bengali', label: 'Bengali' },
  { value: 'english', label: 'English' },
];

const ALGORITHMS = [
  { value: 'auto', label: 'Automatic' },
  { value: 'kmp', label: 'KMP' },
  { value: 'rabinkarp', label: 'Rabin–Karp' },
  { value: 'zalgorithm', label: 'Z-Algorithm' },
  { value: 'naive', label: 'Naive' },
  { value: 'ahocorasick', label: 'Aho–Corasick' },
];

const SPECIMENS = [
  { lang: 'Telugu', sample: 'తెలుగు భాష' },
  { lang: 'Hindi', sample: 'हिन्दी भाषा' },
  { lang: 'Tamil', sample: 'தமிழ் மொழி' },
  { lang: 'Bengali', sample: 'বাংলা ভাষা' },
  { lang: 'English', sample: 'English text' },
];

const MAX_KEYWORDS = 50;

export default function Home() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [language, setLanguage] = useState('all');
  const [mode, setMode] = useState('single');
  const [algorithm, setAlgorithm] = useState('auto');
  const [searchMode, setSearchMode] = useState('exact');

  const keywordCount = query.trim() ? query.trim().split(/\s+/).length : 0;
  const tooManyKeywords = keywordCount > MAX_KEYWORDS;

  function handleSubmit(e) {
    e.preventDefault();
    if (!query.trim() || tooManyKeywords) return;
    const params = new URLSearchParams({ query: query.trim(), language, mode, algorithm, searchMode });
    navigate(`/results?${params.toString()}`);
  }

  return (
    <div className="shell" style={{ paddingTop: 72, paddingBottom: 96 }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1.1fr 0.9fr', gap: 64, alignItems: 'start' }}>
        <div>
          <p className="mono" style={{ color: 'var(--muted)', fontSize: 13, marginBottom: 20 }}>
            unicode-aware · multilingual · built on hand-written DSA
          </p>
          <h1 style={{ fontSize: 52, lineHeight: 1.08, maxWidth: 520 }}>
            Search across India's scripts, one algorithm at a time.
          </h1>
          <p style={{ marginTop: 24, fontSize: 17, maxWidth: 460, lineHeight: 1.6 }}>
            Every query here runs through pattern-matching structures built from
            scratch in Java — KMP, Rabin–Karp and Aho–Corasick working over an
            inverted index — with no shortcuts through regex or built-in search.
          </p>

          <div style={{ display: 'flex', gap: 36, marginTop: 56 }}>
            {SPECIMENS.map((s) => (
              <div key={s.lang}>
                <div className="indic" style={{ fontSize: 22, color: 'var(--paper)' }}>{s.sample}</div>
                <div className="mono" style={{ fontSize: 11, color: 'var(--muted)', marginTop: 6 }}>{s.lang}</div>
              </div>
            ))}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="panel" style={{ padding: 32, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div>
            <label className="field-label" htmlFor="query">Search query</label>
            <input
              id="query"
              type="text"
              className="indic"
              placeholder="తెలుగు, भारत, தமிழ், বাংলা, India…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              autoFocus
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}>
              <span className="mono" style={{ fontSize: 11, color: tooManyKeywords ? 'var(--maroon)' : 'var(--muted)' }}>
                {keywordCount} / {MAX_KEYWORDS} keywords
              </span>
              {tooManyKeywords && (
                <span className="mono" style={{ fontSize: 11, color: 'var(--maroon)' }}>
                  Too many keywords — max {MAX_KEYWORDS}
                </span>
              )}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <div>
              <label className="field-label" htmlFor="language">Language</label>
              <select id="language" value={language} onChange={(e) => setLanguage(e.target.value)}>
                {LANGUAGES.map((l) => <option key={l.value} value={l.value}>{l.label}</option>)}
              </select>
            </div>
            <div>
              <label className="field-label" htmlFor="algorithm">Algorithm</label>
              <select id="algorithm" value={algorithm} onChange={(e) => setAlgorithm(e.target.value)}>
                {ALGORITHMS.map((a) => <option key={a.value} value={a.value}>{a.label}</option>)}
              </select>
            </div>
          </div>

          <div>
            <span className="field-label">Search mode</span>
            <div style={{ display: 'flex', gap: 10 }}>
              {[{ v: 'single', l: 'Single keyword' }, { v: 'multi', l: 'Multi keyword' }].map((m) => (
                <button
                  type="button"
                  key={m.v}
                  onClick={() => setMode(m.v)}
                  className={mode === m.v ? 'btn' : 'btn btn-ghost'}
                  style={{ flex: 1, justifyContent: 'center', padding: '10px 12px', fontSize: 13 }}
                >
                  {m.l}
                </button>
              ))}
            </div>
          </div>

          <div>
            <span className="field-label">Match type</span>
            <div style={{ display: 'flex', gap: 10 }}>
              {[{ v: 'exact', l: 'Exact search' }, { v: 'fuzzy', l: 'Fuzzy (typo-tolerant)' }].map((m) => (
                <button type="button" key={m.v} onClick={() => setSearchMode(m.v)}
                  className={searchMode === m.v ? 'btn' : 'btn btn-ghost'}
                  style={{ flex: 1, justifyContent: 'center', padding: '10px 12px', fontSize: 13 }}>
                  {m.l}
                </button>
              ))}
            </div>
          </div>

          <button type="submit" className="btn" style={{ justifyContent: 'center', marginTop: 8 }} disabled={tooManyKeywords}>
            Search
          </button>
          <p style={{ fontSize: 12, color: 'var(--muted)' }}>
            Separate keywords with spaces (up to {MAX_KEYWORDS}). Two or more keywords use AND: a document must contain every one, found in a single Aho–Corasick pass. Fuzzy mode uses Levenshtein edit distance over the index dictionary.
          </p>
        </form>
      </div>
    </div>
  );
}
