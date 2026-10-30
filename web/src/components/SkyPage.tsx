import type { ReactNode } from 'react';
import './SkyPage.css';

/** Sun by day, a low sun at dusk, moon and stars at night; the stylesheet shows the one for the current period. */
function SkyScene() {
  return (
    <div className="sky-scene" aria-hidden="true">
      <svg className="sky-scene__stars" width="100%" height="220">
        <defs>
          <pattern id="sky-stars" width="390" height="220" patternUnits="userSpaceOnUse">
            <circle cx="40" cy="40" r="1.4" fill="#fff" opacity=".8" />
            <circle cx="120" cy="24" r="1" fill="#fff" opacity=".6" />
            <circle cx="210" cy="60" r="1.2" fill="#fff" opacity=".7" />
            <circle cx="300" cy="30" r="1" fill="#fff" opacity=".5" />
            <circle cx="80" cy="110" r="1" fill="#fff" opacity=".5" />
            <circle cx="250" cy="130" r="1.3" fill="#fff" opacity=".6" />
            <circle cx="350" cy="90" r="1" fill="#fff" opacity=".7" />
            <circle cx="170" cy="150" r="1" fill="#fff" opacity=".4" />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#sky-stars)" />
      </svg>
      <svg className="sky-scene__sun" width="76" height="76" viewBox="0 0 76 76">
        <circle cx="38" cy="38" r="36" fill="#fff3d6" opacity=".35" />
        <circle cx="38" cy="38" r="21" fill="#ffe2a3" />
      </svg>
      <svg className="sky-scene__moon" width="44" height="44" viewBox="0 0 44 44">
        <path d="M14 38a18 18 0 1 0 14 -30a14 14 0 1 1 -14 30z" fill="#f4e9c8" />
      </svg>
    </div>
  );
}

/**
 * The live-sky layout: a sky hero on top and the page's ground below it. [top] sits above the title, for a site
 * header; [className] can widen the content column (see SkyPage.css).
 */
export function SkyPage({
  title,
  top,
  hero,
  children,
  className,
}: {
  title?: string;
  top?: ReactNode;
  hero: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={'sky-page' + (className ? ` ${className}` : '')}>
      <section className="sky-hero">
        <SkyScene />
        {top}
        {title && <h1 className="sky-hero__title">{title}</h1>}
        {hero}
      </section>
      <div className="sky-ground">{children}</div>
    </div>
  );
}
