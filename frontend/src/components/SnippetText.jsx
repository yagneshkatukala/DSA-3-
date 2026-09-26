// Renders backend snippets where matched keywords are wrapped in [[ ]]
// markers as <mark> elements for visual highlighting.
export default function SnippetText({ text }) {
  if (!text) return null;
  const parts = text.split(/(\[\[[^\]]+\]\])/g);
  return (
    <span className="indic">
      {parts.map((part, i) => {
        const match = part.match(/^\[\[([^\]]+)\]\]$/);
        if (match) {
          return <mark key={i}>{match[1]}</mark>;
        }
        return <span key={i}>{part}</span>;
      })}
    </span>
  );
}
