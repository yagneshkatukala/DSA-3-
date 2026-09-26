import { NavLink } from 'react-router-dom';

const links = [
  { to: '/', label: 'Search' },
  { to: '/performance', label: 'Performance' },
  { to: '/flow', label: 'Network Flow' },
  { to: '/dataset', label: 'Dataset' },
  { to: '/about', label: 'About' },
];

export default function Header() {
  return (
    <header style={{ borderBottom: '1px solid var(--line)' }}>
      <div className="shell" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 76 }}>
        <NavLink to="/" style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
          <span style={{ fontFamily: 'var(--font-display)', fontSize: 22, fontWeight: 600 }}>Unicode Search Engine</span>
        </NavLink>
        <nav style={{ display: 'flex', gap: 28 }}>
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.to === '/'}
              style={({ isActive }) => ({
                fontSize: 14,
                color: isActive ? 'var(--saffron)' : 'var(--paper-dim)',
                borderBottom: isActive ? '1px solid var(--saffron)' : '1px solid transparent',
                paddingBottom: 4,
              })}
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}
