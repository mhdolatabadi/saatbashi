import { cities } from './data/cities';
import { calendarYears, monthPath, monthSpan, monthTitle, prayerMonthPath, prayerYears, yearPath } from './lib/calendarPages';
import { monthName, toPersianDigits } from './lib/persianDate';
import { faqs } from './data/faq';


export const SITE_NAME = 'ساعت‌باشی';

/** Public Android builds (same page as the site's download link). */
export const ANDROID_RELEASE = 'https://github.com/mhdolatabadi/saatbashi/releases/latest';

export interface PageMeta {
  title: string;
  description: string;
  /** Personal pages are kept out of search results. */
  noindex?: boolean;
  /** schema.org type for the page's JSON-LD. */
  type: 'WebSite' | 'WebPage' | 'CollectionPage';
  /** Short name for breadcrumb trails («تهران», «آبان ۱۴۰۵»), shown by search engines instead of the full title. */
  crumb?: string;
}

const home: PageMeta = {
  title: 'ساعت‌باشی — تقویم شمسی، قمری و میلادی، اوقات شرعی و مناسبت‌ها',
  description:
    'تقویم کامل شمسی با تاریخ قمری و میلادی هر روز، تعطیلات رسمی و مناسبت‌های ماه، اوقات شرعی امروز و تبدیل تاریخ؛ به‌همراه پیگیری نماز و روزه‌ی قضا.',
  type: 'WebSite',
};

const pages: Record<string, PageMeta> = {
  '/': home,
  '/calendar': {
    title: 'تقویم شمسی؛ تاریخ امروز، مناسبت‌ها و تعطیلات | ساعت‌باشی',
    crumb: 'تقویم',
    description: 'راهنمای تقویم شمسی ایران: تاریخ امروز، تقویم ماه جاری، مناسبت‌ها، تعطیلات و نمایش هم‌زمان تاریخ میلادی و قمری.',
    type: 'WebPage',
  },
  '/date-converter': {
    title: 'تبدیل تاریخ شمسی به میلادی و قمری | ساعت‌باشی',
    crumb: 'تبدیل تاریخ',
    description: 'تبدیل آنلاین تاریخ شمسی به میلادی و قمری، میلادی به شمسی و قمری به شمسی، همراه با روز هفته.',
    type: 'WebPage',
  },
  '/app/home': {
    title: 'شمارنده‌ی نماز و روزه‌ی قضا | ساعت‌باشی',
    description: 'شمارنده‌ی ساده برای نمازها و روزه‌های قضا؛ اطلاعات فقط در مرورگر خود شما ذخیره می‌شود.',
    noindex: true,
    type: 'WebPage',
  },
  '/app/calendar': {
    title: 'تقویم شمسی ماه جاری | ساعت‌باشی',
    crumb: 'تقویم ماه جاری',
    description: 'تقویم ماه جاری شمسی با مشخص بودن امروز و جمعه‌ها.',
    type: 'WebPage',
  },
  '/app/prayer-times': {
    title: 'اوقات شرعی امروز؛ اذان صبح، ظهر و مغرب | ساعت‌باشی',
    crumb: 'اوقات شرعی امروز',
    description:
      'اوقات شرعی امروز به افق تهران یا موقعیت شما: اذان صبح، طلوع آفتاب، اذان ظهر، عصر، غروب آفتاب، اذان مغرب و عشا.',
    type: 'WebPage',
  },
};

pages['/prayer-times'] = {
  title: 'اوقات شرعی شهرهای ایران؛ اذان صبح، ظهر و مغرب امروز | ساعت‌باشی',
  crumb: 'اوقات شرعی',
  description: 'اوقات شرعی امروز مراکز استان‌های ایران: اذان صبح، طلوع آفتاب، اذان ظهر، غروب آفتاب، اذان مغرب و نیمه‌شب شرعی.',
  type: 'CollectionPage',
};

for (const city of cities) {
  pages[`/prayer-times/${city.slug}`] = {
    title: `اوقات شرعی ${city.name} امروز؛ اذان صبح، ظهر و مغرب | ساعت‌باشی`,
    crumb: city.name,
    description: `اوقات شرعی امروز به افق ${city.name}: اذان صبح، طلوع آفتاب، اذان ظهر، غروب آفتاب، اذان مغرب و نیمه‌شب شرعی؛ محاسبه‌ی تقریبی به روش تهران.`,
    type: 'WebPage',
  };
}

for (const year of calendarYears) {
  const y = toPersianDigits(year);
  pages[yearPath(year)] = {
    title: `تقویم ${y}؛ تعطیلات رسمی و مناسبت‌های سال ${y} | ساعت‌باشی`,
    crumb: y,
    description: `تقویم سال ${y} هجری شمسی ماه‌به‌ماه با تاریخ قمری و میلادی، فهرست تعطیلات رسمی ${y} و مناسبت‌های هر ماه.`,
    type: 'CollectionPage',
  };
  for (let month = 1; month <= 12; month++) {
    const title = monthTitle(year, month);
    const span = monthSpan(year, month);
    pages[monthPath(year, month)] = {
      title: `تقویم ${title}؛ مناسبت‌ها و تعطیلات | ساعت‌باشی`,
      crumb: monthName(month),
      description: `تقویم ${title} روزبه‌روز با تاریخ قمری (${span.hijri}) و میلادی (${span.gregorian})، تعطیلات رسمی و مناسبت‌های ماه.`,
      type: 'WebPage',
    };
  }
}

for (const city of cities) {
  for (const year of prayerYears) {
    for (let month = 1; month <= 12; month++) {
      const title = monthTitle(year, month);
      pages[prayerMonthPath(city.slug, year, month)] = {
        title: `اوقات شرعی ${city.name} ${title}؛ جدول اذان صبح، ظهر و مغرب | ساعت‌باشی`,
        crumb: title,
        description: `جدول ماهانه‌ی اوقات شرعی ${city.name} در ${title}: اذان صبح، طلوع آفتاب، اذان ظهر، غروب آفتاب، اذان مغرب و نیمه‌شب شرعی برای همه‌ی روزهای ماه؛ محاسبه‌ی تقریبی به روش تهران.`,
        type: 'WebPage',
      };
    }
  }
}

export const notFoundMeta: PageMeta = {
  title: 'صفحه پیدا نشد | ساعت‌باشی',
  description: 'صفحه‌ای که دنبالش بودید پیدا نشد.',
  noindex: true,
  type: 'WebPage',
};

export function metaFor(pathname: string): PageMeta {
  const clean = pathname.length > 1 ? pathname.replace(/\/+$/, '') : pathname;
  return pages[clean] ?? notFoundMeta;
}

/** Every route that is pre-rendered; indexable ones also go into the sitemap. */
export const prerenderRoutes = Object.keys(pages);

/** One canonical path for SSR and in-app navigation; query parameters never become canonicals. */
export function canonicalPath(pathname: string): string {
  const path = pathname.split(/[?#]/, 1)[0];
  return path.replace(/\/+$/, '') || '/';
}

export function structuredDataFor(pathname: string, origin: string) {
  const path = canonicalPath(pathname);
  const meta = metaFor(path);
  const site = `${origin}/`;
  const url = `${origin}${path}`;
  const page: Record<string, unknown> = {
    '@type': meta.type, '@id': `${url}#page`, url,
    name: meta.title, description: meta.description, inLanguage: 'fa',
    ...(path === '/' ? { name: SITE_NAME, alternateName: 'Saatbashi' } :
      { isPartOf: { '@id': `${site}#page` } }),
  };
  const graph: Record<string, unknown>[] = [page];
  if (path === '/') {
    page.publisher = { '@id': `${site}#organization` };
    graph.push(
      { '@type': 'Organization', '@id': `${site}#organization`, name: SITE_NAME, alternateName: 'Saatbashi',
        url: site, logo: `${origin}/icon-512.png` },
    );
  }
  const faq = faqs[path];
  if (faq) {
    page.mainEntity = { '@id': `${url}#faq` };
    graph.push({ '@type': 'FAQPage', '@id': `${url}#faq`, url, inLanguage: 'fa',
      mainEntity: faq.map(({ question, answer }) => ({ '@type': 'Question', name: question,
        acceptedAnswer: { '@type': 'Answer', text: answer } })) });
  }
  if (path !== '/' && !meta.noindex) {
    // Every ancestor that is itself a page: /calendar/1405/mehr → /calendar → /calendar/1405.
    const segments = path.split('/').filter(Boolean);
    const paths = ['/', ...segments.slice(0, -1).map((_, i) => `/${segments.slice(0, i + 1).join('/')}`)
      .filter((p) => p in pages && !pages[p].noindex)];

    paths.push(path);
    const breadcrumbId = `${url}#breadcrumb`;
    page.breadcrumb = { '@id': breadcrumbId };
    graph.push({ '@type': 'BreadcrumbList', '@id': breadcrumbId,
      itemListElement: paths.map((p, i) => ({ '@type': 'ListItem', position: i + 1,
        name: p === '/' ? SITE_NAME : metaFor(p).crumb ?? metaFor(p).title.split('|')[0].trim(), item: `${origin}${p}` })) });
  }
  return { '@context': 'https://schema.org', '@graph': graph };
}
