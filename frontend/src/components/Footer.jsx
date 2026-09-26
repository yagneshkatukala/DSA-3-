export default function Footer() {
  return (
    <footer style={{ borderTop: '1px solid var(--line)', marginTop: 'auto' }}>
      <div className="shell" style={{ padding: '24px 32px', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <p style={{ fontSize: 13 }}>Unicode Search Engine — a DSA project for multilingual text search.</p>
        <p className="mono" style={{ fontSize: 12 }}>KMP · Rabin–Karp · Z · Aho–Corasick · Levenshtein DP · Max-Flow · Inverted Index</p>
      </div>
    </footer>
  );
}
