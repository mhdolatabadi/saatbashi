import type { ReactNode } from 'react';
import './SkyPage.css';

/** Sun by day, a low sun at dusk, moon and stars at night; the stylesheet shows the one for the current period. */
function SkyScene() {
  return (
    <div className="sky-scene" aria-hidden="true">
      <svg className="sky-scene__sun" width="120" height="120" viewBox="0 0 120 120">
        <circle cx="60" cy="60" r="44" fill="#fff3d6" opacity=".35" />
        <circle cx="60" cy="60" r="26" fill="#ffe2a3" />
      </svg>
      <svg className="sky-scene__night" width="390" height="200" viewBox="0 0 390 200" preserveAspectRatio="xMinYMin slice">
        <circle cx="40" cy="40" r="1.4" fill="#fff" opacity=".8" />
        <circle cx="120" cy="24" r="1" fill="#fff" opacity=".6" />
        <circle cx="210" cy="60" r="1.2" fill="#fff" opacity=".7" />
        <circle cx="300" cy="30" r="1" fill="#fff" opacity=".5" />
        <circle cx="80" cy="110" r="1" fill="#fff" opacity=".5" />
        <circle cx="250" cy="130" r="1.3" fill="#fff" opacity=".6" />
        <circle cx="350" cy="90" r="1" fill="#fff" opacity=".7" />
        <circle cx="170" cy="150" r="1" fill="#fff" opacity=".4" />
        <path d="M70 70a18 18 0 1 0 14 -30a14 14 0 1 1 -14 30z" fill="#f4e9c8" />
      </svg>
    </div>
  );
}

/** The live-sky layout: a sky hero on top and the page's ground below it. */
export function SkyPage({ title, hero, children, variant }: { title: string; hero: ReactNode; children: ReactNode; variant?: 'calendar' }) {
  return (
    <div className={'sky-page' + (variant ? ` sky-page--${variant}` : '')}>
      <section className="sky-hero">
        <SkyScene />
        <h1 className="sky-hero__title">{title}</h1>
        {hero}
      </section>
      <div className="sky-ground">{children}</div>
    </div>
  );
}
