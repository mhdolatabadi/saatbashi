import type { CSSProperties } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { CalendarIcon, ClockIcon } from './icons';
import './AppShell.css';

const tabs = [
  { to: '/app/calendar', label: 'تقویم', Icon: CalendarIcon },
  { to: '/app/prayer-times', label: 'اوقات شرعی', Icon: ClockIcon },
];

export function AppShell() {
  const { pathname } = useLocation();
  const active = tabs.findIndex((tab) => pathname.startsWith(tab.to));
  // Keyed by path so every navigation plays the page entrance.
  return (
    <div className="app-shell">
      <main key={pathname} className="app-shell__content page-enter">
        <Outlet />
      </main>
      <nav className="app-shell__nav" aria-label="بخش‌ها" style={{ '--active': active, '--tab-count': tabs.length } as CSSProperties}>
        {active >= 0 && <span className="app-shell__indicator" aria-hidden="true" />}
        {tabs.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => 'app-shell__tab' + (isActive ? ' app-shell__tab--active' : '')}
          >
            <Icon />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
