import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router-dom';
import { AppShell } from '../AppShell';
import { PublicLayout, SiteFooter, SiteHeader } from '../SiteChrome';

describe('unobstructed page chrome', () => {
  it('keeps navigation without adding a repeated guide panel', () => {
    const html = renderToStaticMarkup(<MemoryRouter><AppShell /></MemoryRouter>);
    expect(html).toContain('aria-label="بخش‌ها"');
    expect(html).not.toContain('saatbashi-guide');
    expect(html).not.toContain('saatbashi-character.webp');
  });
  it('preserves public content and the compact brand mark', () => {
    const html = renderToStaticMarkup(<MemoryRouter><PublicLayout><main>محتوای صفحه</main></PublicLayout></MemoryRouter>);
    expect(html).toContain('محتوای صفحه');
    expect(html).toContain('/favicon.svg');
    expect(html).not.toContain('saatbashi-guide');
  });
  it('offers only calendar and prayer tabs with a two-column indicator', () => {
    const html = renderToStaticMarkup(<MemoryRouter initialEntries={['/app/calendar']}><AppShell /></MemoryRouter>);
    expect(html.match(/class="app-shell__tab/g)).toHaveLength(2);
    expect(html).toContain('--tab-count:2');
    expect(html).toContain('aria-current="page"');
    expect(html).not.toContain('/app/home');
    expect(html).not.toContain('قضا');
  });
  it('does not falsely highlight calendar on the retained legacy route', () => {
    const html = renderToStaticMarkup(<MemoryRouter initialEntries={['/app/home']}><AppShell /></MemoryRouter>);
    expect(html).not.toContain('app-shell__indicator');
    expect(html).not.toContain('aria-current="page"');
  });
  it('does not promote legacy counters in public navigation', () => {
    const html = renderToStaticMarkup(<MemoryRouter><PublicLayout><main>تقویم</main></PublicLayout></MemoryRouter>);
    expect(html).not.toContain('/app/home');
    expect(html).not.toContain('قضا');
    expect(html).toContain('/app/calendar');
    expect(html).toContain('/app/prayer-times');
    expect(html).toContain('href="/privacy"');
  });
  it('drops the glass pane when the header sits on the sky and the footer on the ground', () => {
    const header = renderToStaticMarkup(<MemoryRouter><SiteHeader onSky /></MemoryRouter>);
    const footer = renderToStaticMarkup(<MemoryRouter><SiteFooter onGround /></MemoryRouter>);
    expect(header).toContain('ti-header--sky');
    expect(header).not.toContain('glass');
    expect(footer).toContain('sky-card');
    expect(footer).not.toContain('glass');
  });
});
