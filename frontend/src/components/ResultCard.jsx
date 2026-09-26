import SnippetText from './SnippetText';

export default function ResultCard({ result, index }) {
  return (
    <article className="panel" style={{ padding: 24, display: 'flex', gap: 20 }}>
      <div className="mono" style={{ color: 'var(--muted)', fontSize: 13, minWidth: 24 }}>
        {String(index + 1).padStart(2, '0')}
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', flexWrap: 'wrap', gap: 8 }}>
          <h3 style={{ fontSize: 19 }}>{result.title}</h3>
          <span className="mono" style={{ fontSize: 12, color: 'var(--teal)', textTransform: 'capitalize' }}>{result.language}</span>
        </div>
        <p className="indic" style={{ fontSize: 15, lineHeight: 1.7, color: 'var(--paper)', whiteSpace: 'pre-line' }}>
          <SnippetText text={result.snippet} />
        </p>
        <div style={{ display: 'flex', gap: 20, fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>
          <span>Relevance <b className="mono" style={{ color: 'var(--saffron)' }}>{result.score}%</b></span>
          <span>Matches <b className="mono" style={{ color: 'var(--paper)' }}>{result.matchCount}</b></span>
          {result.matchedKeywords && result.matchedKeywords.length > 0 && (
            <span>Matched keywords <b className="mono" style={{ color: 'var(--paper)' }}>{result.matchedKeywords.length}</b>
              <span className="mono" style={{ marginLeft: 6 }}>({result.matchedKeywords.slice(0, 6).join(', ')}{result.matchedKeywords.length > 6 ? ', …' : ''})</span>
            </span>
          )}
        </div>
      </div>
    </article>
  );
}
