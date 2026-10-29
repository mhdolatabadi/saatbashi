import { describe, expect, it } from 'vitest';
import { faqs } from '../../data/faq';
import { canonicalPath, metaFor, prerenderRoutes, structuredDataFor } from '../../seo';

describe('search metadata', () => {
  it('normalizes query strings and trailing slashes consistently', () => {
    expect(canonicalPath('/calendar/?utm_source=test#today')).toBe('/calendar');
    expect(canonicalPath('/')).toBe('/');
  });
  it('keeps personal and missing pages out of search', () => {
    expect(metaFor('/app/home').noindex).toBe(true);
    expect(metaFor('/not-a-page').noindex).toBe(true);
    expect(metaFor('/calendar').noindex).not.toBe(true);
  });
  it('no longer publishes the removed religious texts', () => {
    expect(prerenderRoutes.some(path => path.startsWith('/app/texts'))).toBe(false);
    expect(metaFor('/app/texts/ziyarat_ashura').noindex).toBe(true);
  });
  it('has unique metadata and a page identity for every exported route', () => {
    const titles = prerenderRoutes.map(path => metaFor(path).title);
    expect(new Set(titles).size).toBe(titles.length);
    for (const path of prerenderRoutes) {
      expect(metaFor(path).description.length).toBeGreaterThan(30);
      const data = structuredDataFor(path, 'https://example.test');
      expect(data['@graph'][0].url).toBe(`https://example.test${path}`);
    }
  });
  it('gives city pages their real parent and omits personal breadcrumbs', () => {
    const city = structuredDataFor('/prayer-times/tehran', 'https://example.test');
    expect(JSON.stringify(city)).toContain('https://example.test/prayer-times');
    expect(city['@graph'][1]['@type']).toBe('BreadcrumbList');
    expect(structuredDataFor('/app/home', 'https://example.test')['@graph']).toHaveLength(1);
  });
  it('publishes an indexable privacy policy for the store listings', () => {
    expect(prerenderRoutes).toContain('/privacy');
    expect(metaFor('/privacy').noindex).not.toBe(true);
  });
  it('describes the site and its publisher on the home page', () => {
    // No SoftwareApplication: Google rejects it without ratings, which would show as an error in Search Console.
    const types = structuredDataFor('/', 'https://example.test')['@graph'].map(node => node['@type']);
    expect(types).toEqual(['WebSite', 'Organization']);
  });
  it('names breadcrumb steps briefly instead of repeating page titles', () => {
    const crumbs = structuredDataFor('/prayer-times/tehran/1405/aban', 'https://example.test')['@graph'][1] as
      { itemListElement: { name: string }[] };
    expect(crumbs.itemListElement.map(c => c.name)).toEqual(['ساعت‌باشی', 'اوقات شرعی', 'تهران', 'آبان ۱۴۰۵']);
    const month = structuredDataFor('/calendar/1405/mehr', 'https://example.test')['@graph'][1] as
      { itemListElement: { name: string }[] };
    expect(month.itemListElement.map(c => c.name)).toEqual(['ساعت‌باشی', 'تقویم', '۱۴۰۵', 'مهر']);
  });
  it('emits the visible FAQ of guide pages as FAQPage', () => {
    for (const [path, entries] of Object.entries(faqs)) {
      const faq = structuredDataFor(path, 'https://example.test')['@graph'].find(node => node['@type'] === 'FAQPage');
      expect(faq?.mainEntity).toEqual(entries.map(({ question, answer }) =>
        ({ '@type': 'Question', name: question, acceptedAnswer: { '@type': 'Answer', text: answer } })));
    }
  });
});
