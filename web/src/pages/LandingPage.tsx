import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import {
  daysInMonth,
  firstDayOfWeek,
  fromDate,
  gregorianMonthNames,
  monthName,
  toArabicDigits,
  toGregorianDate,
  toPersianDigits,
  weekdayIndexSaturdayFirst,
} from '../lib/persianDate';
import { hijriMonthName, toHijri } from '../lib/hijriDate';
import { minutesAt, placeLabel, timesFor, usePlace } from '../lib/place';
import { CityPicker, PrayerTimesTable } from '../components/PrayerTimes';
import { APPROXIMATE_NOTE, formatDuration } from '../lib/prayerTimes';
import { isHoliday, occasionsOn, type Occasion } from '../data/occasions';
import { ChevronIcon, LocationIcon } from '../components/icons';
import { ClientOnly } from '../components/ClientOnly';
import { SiteFooter, SiteHeader } from '../components/SiteChrome';
import { SkyPage } from '../components/SkyPage';
import { DayTrack } from '../components/DayTrack';
import { dayTrack } from '../lib/dayTrack';
import { DateConverter } from '../components/DateConverter';
import { gregorianLabel, hijriLabel, solarLabel } from '../lib/dateLabels';
import { BUILD_YEAR, yearPath } from '../lib/calendarPages';
import './LandingPage.css';

const weekdayHeaders = ['شنبه', 'یکشنبه', 'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنجشنبه', 'جمعه'];

function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function useNow(): Date {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);
  return now;
}

// --- Today --------------------------------------------------------------------------------------

/** Each digit remounts when it changes, so only the digits that tick roll into place. */
function Rolling({ value, className }: { value: number; className?: string }) {
  const digits = toPersianDigits(String(value).padStart(2, '0'));
  return (
    <span className={'ti-roll' + (className ? ` ${className}` : '')}>
      {[...digits].map((digit, index) => (
        <span key={`${index}-${digit}`} className="ti-roll__digit">
          {digit}
        </span>
      ))}
    </span>
  );
}

type PlaceState = ReturnType<typeof usePlace>;

/** On the sky: the clock, today in all three calendars, and the day's prayers on a track. */
function HomeHero({ now, place }: { now: Date; place: PlaceState['place'] }) {
  const track = dayTrack(timesFor(place, now), minutesAt(place, now));
  const { next } = track;
  return (
    <div className="home-hero">
      <div className="home-hero__clock" role="timer" aria-label={now.toLocaleTimeString('fa-IR')}>
        <Rolling value={now.getHours()} />
        <span className="home-hero__colon">:</span>
        <Rolling value={now.getMinutes()} />
        <span className="home-hero__seconds">
          <span className="home-hero__colon">:</span>
          <Rolling value={now.getSeconds()} />
        </span>
      </div>
      <dl className="home-hero__dates">
        <div className="home-hero__date home-hero__date--solar">
          <dt>هجری شمسی</dt>
          <dd>{solarLabel(now)}</dd>
        </div>
        <div className="home-hero__date">
          <dt>هجری قمری</dt>
          <dd>{hijriLabel(now)}</dd>
        </div>
        <div className="home-hero__date">
          <dt>میلادی</dt>
          <dd dir="ltr">{gregorianLabel(now)}</dd>
        </div>
      </dl>
      <p className="home-hero__next" aria-live="polite">
        <span className="sky-pill">
          {next.label}{next.tomorrow && ' فردا'} <span dir="ltr">{toPersianDigits(next.time)}</span>
        </span>
        <span>{formatDuration(next.minutesLeft)} دیگر، {placeLabel(place)}</span>
      </p>
      <DayTrack track={track} />
    </div>
  );
}

// --- Calendar -----------------------------------------------------------------------------------

interface Cell {
  date: Date;
  solarDay: number;
  inMonth: boolean;
}

function buildCells(year: number, month: number): Cell[] {
  const leading = weekdayIndexSaturdayFirst(firstDayOfWeek(year, month));
  const count = daysInMonth(year, month);
  const first = toGregorianDate(year, month, 1);
  const total = Math.ceil((leading + count) / 7) * 7;
  const cells: Cell[] = [];
  for (let i = 0; i < total; i++) {
    const date = new Date(first.getFullYear(), first.getMonth(), first.getDate() - leading + i, 12);
    cells.push({ date, solarDay: fromDate(date).day, inMonth: i >= leading && i < leading + count });
  }
  return cells;
}

function rangeLabel(first: Date, last: Date): { hijri: string; gregorian: string } {
  const h1 = toHijri(first);
  const h2 = toHijri(last);
  const hijri =
    h1.month === h2.month
      ? `${hijriMonthName(h1.month)} ${toPersianDigits(h1.year)}`
      : `${hijriMonthName(h1.month)}${h1.year !== h2.year ? ` ${toPersianDigits(h1.year)}` : ''} – ${hijriMonthName(h2.month)} ${toPersianDigits(h2.year)}`;
  const g1 = gregorianMonthNames[first.getMonth()];
  const g2 = gregorianMonthNames[last.getMonth()];
  const gregorian =
    first.getMonth() === last.getMonth()
      ? `${g1} ${toPersianDigits(first.getFullYear())}`
      : `${g1}${first.getFullYear() !== last.getFullYear() ? ` ${toPersianDigits(first.getFullYear())}` : ''} – ${g2} ${toPersianDigits(last.getFullYear())}`;
  return { hijri, gregorian };
}

function OccasionList({ items }: { items: { date?: Date; occasion: Occasion }[] }) {
  if (items.length === 0) return <p className="ti-occasions__empty">مناسبتی ثبت نشده است.</p>;
  return (
    <ul className="ti-occasions__list">
      {items.map(({ date, occasion }, index) => (
        <li key={index} className={'ti-occasion' + (occasion.holiday ? ' ti-occasion--holiday' : '')}>
          {date && (
            <span className="ti-occasion__day">
              {toPersianDigits(fromDate(date).day)} {monthName(fromDate(date).month)}
            </span>
          )}
          <span className="ti-occasion__title">
            {occasion.title}
            {occasion.holiday && <span className="ti-occasion__tag">تعطیل</span>}
          </span>
        </li>
      ))}
    </ul>
  );
}

function CalendarSection({ now }: { now: Date }) {
  const current = fromDate(now);
  const [view, setView] = useState({ year: current.year, month: current.month, direction: 0 });
  const [selected, setSelected] = useState<Date>(now);

  const cells = useMemo(() => buildCells(view.year, view.month), [view.year, view.month]);
  const monthDays = cells.filter((c) => c.inMonth);
  const range = rangeLabel(monthDays[0].date, monthDays[monthDays.length - 1].date);

  const shift = (delta: number) => {
    setView(({ year, month }) => {
      const index = year * 12 + (month - 1) + delta;
      return { year: Math.floor(index / 12), month: (index % 12) + 1, direction: delta };
    });
  };

  const goToday = () => {
    const delta = current.year * 12 + current.month - (view.year * 12 + view.month);
    setView({ year: current.year, month: current.month, direction: Math.sign(delta) });
    setSelected(now);
  };

  const monthOccasions = monthDays.flatMap(({ date }) =>
    occasionsOn(date).map((occasion) => ({ date, occasion })),
  );

  return (
    <>
      <section className="ti-card ti-calendar glass enter" style={{ animationDelay: '80ms' }} aria-label="تقویم">
        <header className="ti-calendar__header">
          <button type="button" className="ti-icon-button" onClick={() => shift(-1)} aria-label="ماه قبل">
            <ChevronIcon />
          </button>
          <div className="ti-calendar__titles">
            <h2 key={`${view.year}-${view.month}`} className="ti-calendar__month">
              {monthName(view.month)} {toPersianDigits(view.year)}
            </h2>
            <div className="ti-calendar__range">
              <span>{range.hijri}</span>
              <span className="ti-calendar__dot" aria-hidden="true" />
              <span>{range.gregorian}</span>
            </div>
          </div>
          <button type="button" className="ti-icon-button ti-icon-button--flip" onClick={() => shift(1)} aria-label="ماه بعد">
            <ChevronIcon />
          </button>
        </header>

        <div className="ti-calendar__weekdays" aria-hidden="true">
          {weekdayHeaders.map((label, index) => (
            <div key={label} className={'ti-calendar__weekday' + (index === 6 ? ' is-holiday' : '')}>
              <span className="ti-calendar__weekday-full">{label}</span>
              <span className="ti-calendar__weekday-short">{label[0]}</span>
            </div>
          ))}
        </div>
        <div
          key={`${view.year}-${view.month}`}
          className={
            'ti-calendar__grid' +
            (view.direction > 0 ? ' is-from-next' : view.direction < 0 ? ' is-from-prev' : '')
          }
          role="group"
          aria-label={`${monthName(view.month)} ${toPersianDigits(view.year)}`}
        >
          {cells.map((cell) => {
            const holiday = isHoliday(cell.date);
            const classes = [
              'ti-day',
              cell.inMonth ? '' : 'is-outside',
              holiday ? 'is-holiday' : '',
              sameDay(cell.date, now) ? 'is-today' : '',
              sameDay(cell.date, selected) ? 'is-selected' : '',
            ].filter(Boolean).join(' ');
            return (
              <button
                key={cell.date.toDateString()}
                type="button"
                className={classes}
                aria-label={solarLabel(cell.date)}
                aria-pressed={sameDay(cell.date, selected)}
                onClick={() => {
                  setSelected(cell.date);
                  if (!cell.inMonth) {
                    const p = fromDate(cell.date);
                    const delta = p.year * 12 + p.month - (view.year * 12 + view.month);
                    setView({ year: p.year, month: p.month, direction: Math.sign(delta) });
                  }
                }}
              >
                <span className="ti-day__solar">{toPersianDigits(cell.solarDay)}</span>
                <span className="ti-day__sub">
                  <span className="ti-day__hijri">{toArabicDigits(toHijri(cell.date).day)}</span>
                  <span className="ti-day__gregorian">{cell.date.getDate()}</span>
                </span>
              </button>
            );
          })}
        </div>

        <div className="ti-calendar__footer">
          <button type="button" className="ti-button" onClick={goToday}>
            برو به امروز
          </button>
          <span className="ti-calendar__legend">
            <span className="ti-legend-dot" aria-hidden="true" /> تعطیل رسمی
          </span>
        </div>

        <div key={selected.toDateString()} className="ti-selected">
          <div className="ti-selected__dates">
            <strong>{solarLabel(selected)}</strong>
            <span>{hijriLabel(selected)}</span>
            <span dir="ltr">{gregorianLabel(selected)}</span>
          </div>
          <OccasionList items={occasionsOn(selected).map((occasion) => ({ occasion }))} />
        </div>
      </section>

      <section className="ti-card ti-occasions glass enter" style={{ animationDelay: '160ms' }} aria-label="مناسبت‌های ماه">
        <h2 className="ti-card__title">مناسبت‌های {monthName(view.month)}</h2>
        <OccasionList items={monthOccasions.slice(0, 4)} />
        {monthOccasions.length > 4 && (
          <details key={`${view.year}-${view.month}`} className="ti-occasions__more">
            <summary>نمایش {toPersianDigits(monthOccasions.length - 4)} مناسبت دیگر</summary>
            <OccasionList items={monthOccasions.slice(4)} />
          </details>
        )}
        <p className="ti-note">
          تاریخ‌های قمری محاسبه‌ای‌اند و ممکن است با تقویم رسمی کشور یک روز اختلاف داشته باشند.
        </p>
      </section>
    </>
  );
}

// --- Prayer times -------------------------------------------------------------------------------

/** The place is shared with the hero, so both follow the city picked here. */
function PrayerTimesCard({ now, placeState }: { now: Date; placeState: PlaceState }) {
  const { place, selectCity, useMyLocation, locationError, cities } = placeState;
  // A handful of trig calls; cheap enough to redo on every tick and it rolls over at midnight.
  const times = timesFor(place, now);

  return (
    <section className="ti-card ti-prayer glass enter" style={{ animationDelay: '120ms' }} aria-label="اوقات شرعی">
      <h2 className="ti-card__title">اوقات شرعی</h2>
      <div className="ti-prayer__place">
        <CityPicker cities={cities} value={place.kind === 'city' ? place.city.slug : ''} onChange={selectCity} />
        <button type="button" className="ti-link-button" onClick={useMyLocation}>
          <LocationIcon /> موقعیت من
        </button>
      </div>
      <p className="ti-prayer__label">{placeLabel(place)}</p>
      {locationError && <p className="ti-note ti-note--error">دسترسی به موقعیت مکانی داده نشد؛ اوقات به افق شهر انتخاب‌شده است.</p>}
      <PrayerTimesTable times={times} nowMinutes={minutesAt(place, now)} />
      <p className="ti-note">{APPROXIMATE_NOTE}</p>
      <Link to="/app/prayer-times" className="ti-more">
        جزئیات بیشتر
      </Link>
    </section>
  );
}

// --- Page ---------------------------------------------------------------------------------------

const ABOUT = (
  <section className="ti-about glass" aria-labelledby="ti-about-title">
    <h2 id="ti-about-title" className="ti-card__title">درباره‌ی ساعت‌باشی</h2>
    <p>
      ساعت‌باشی تقویم کامل شمسی را همراه با تاریخ قمری و میلادی هر روز نشان می‌دهد؛ جمعه‌ها و تعطیلات رسمی
      به رنگ قرمزند و مناسبت‌های هر ماه زیر تقویم آمده است. اوقات شرعی امروز (اذان صبح، طلوع آفتاب، اذان
      ظهر، غروب آفتاب، اذان مغرب و نیمه‌شب شرعی) به افق تهران یا موقعیت شما محاسبه می‌شود و با مبدل تاریخ
      می‌توانید تاریخ شمسی و میلادی را به هم تبدیل کنید.
    </p>
    <ul className="ti-about__links">
      <li><Link to={yearPath(BUILD_YEAR)}>تقویم {toPersianDigits(BUILD_YEAR)}</Link></li>
      <li><Link to="/app/prayer-times">اوقات شرعی امروز</Link></li>
      <li><Link to="/prayer-times">اوقات شرعی شهرهای ایران</Link></li>
      <li><Link to="/app/calendar">تقویم ماه جاری</Link></li>
    </ul>
  </section>
);

/** The site on the live sky: header and today on the sky, the calendar and tools on the ground below. */
function HomeFrame({ hero, children }: { hero: ReactNode; children: ReactNode }) {
  return (
    <SkyPage className="ti sky-page--site" top={<SiteHeader onSky />} hero={hero}>
      <main className="ti-main">
        <h1 className="sr-only">تقویم شمسی، قمری و میلادی، اوقات شرعی و مناسبت‌های امروز</h1>
        {children}
        {ABOUT}
      </main>
      <SiteFooter onGround />
    </SkyPage>
  );
}

/** Everything that depends on the current moment; rendered only in the browser. */
function LiveHome() {
  const now = useNow();
  const placeState = usePlace();
  return (
    <HomeFrame hero={<HomeHero now={now} place={placeState.place} />}>
      <div className="ti-layout">
        <div className="ti-layout__primary">
          <CalendarSection now={now} />
        </div>
        <aside className="ti-layout__aside">
          <PrayerTimesCard now={now} placeState={placeState} />
          <DateConverter style={{ animationDelay: '200ms' }} />
        </aside>
      </div>
    </HomeFrame>
  );
}

/** Same footprint as LiveHome, so nothing jumps when the live content replaces it. */
function HomePlaceholder() {
  return (
    <HomeFrame hero={<div className="home-hero home-hero--placeholder" aria-hidden="true" />}>
      <div className="ti-layout" aria-hidden="true">
        <div className="ti-layout__primary">
          <div className="ti-card glass ti-placeholder ti-placeholder--calendar" />
        </div>
        <div className="ti-layout__aside">
          <div className="ti-card glass ti-placeholder ti-placeholder--prayer" />
        </div>
      </div>
    </HomeFrame>
  );
}

export function LandingPage() {
  return (
    <ClientOnly fallback={<HomePlaceholder />}>
      <LiveHome />
    </ClientOnly>
  );
}
