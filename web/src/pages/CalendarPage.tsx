import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  daysInMonth,
  firstDayOfWeek,
  gregorianMonthNames,
  monthName,
  toArabicDigits,
  toGregorianDate,
  today,
  toPersianDigits,
  weekdayIndexSaturdayFirst,
  weekdayName,
} from '../lib/persianDate';
import { hijriMonthName, toHijri } from '../lib/hijriDate';
import { isHoliday, occasionsOn } from '../data/occasions';
import { timesFor, usePlace } from '../lib/place';
import { ChevronIcon } from '../components/icons';
import { SkyPage } from '../components/SkyPage';
import './CalendarPage.css';
import { ClientOnly } from '../components/ClientOnly';

const TITLE = 'تقویم';
const weekdayLabels = ['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'];
const LUNAR_NOTE = 'تاریخ‌های قمری محاسبه‌ای‌اند و ممکن است با تقویم رسمی (رؤیت هلال) یک روز اختلاف داشته باشند.';

interface DayCell {
  day: number;
  hijriDay: number;
  holiday: boolean;
}

/** "جمادی‌الاول – جمادی‌الثانی ۱۴۴۸", or "ژانویه ۲۰۲۶ – فوریه ۲۰۲۶" across a year change. */
function span(first: [string, number], last: [string, number]): string {
  if (first[0] === last[0] && first[1] === last[1]) return `${first[0]} ${toPersianDigits(first[1])}`;
  if (first[1] === last[1]) return `${first[0]} – ${last[0]} ${toPersianDigits(last[1])}`;
  return `${first[0]} ${toPersianDigits(first[1])} – ${last[0]} ${toPersianDigits(last[1])}`;
}

function TodayTimes() {
  const { place } = usePlace();
  const times = timesFor(place);
  const tiles = [
    { label: 'اذان صبح', time: times.fajr },
    { label: 'اذان ظهر', time: times.dhuhr },
    { label: 'اذان مغرب', time: times.maghrib },
  ];
  return (
    <section className="calendar-today" aria-labelledby="calendar-today-title">
      <div className="calendar-today__head">
        <h2 id="calendar-today-title">اوقات امروز {place.kind === 'city' && `به افق ${place.city.name}`}</h2>
        <Link to="/app/prayer-times">همه‌ی اوقات</Link>
      </div>
      <ul className="calendar-today__tiles">
        {tiles.map(({ label, time }) => (
          <li key={label} className="sky-card">
            <span>{label}</span>
            <b dir="ltr">{toPersianDigits(time)}</b>
          </li>
        ))}
      </ul>
    </section>
  );
}

function CalendarPageView() {
  const currentDate = useMemo(() => today(), []);
  const [displayedYear, setDisplayedYear] = useState(currentDate.year);
  const [displayedMonth, setDisplayedMonth] = useState(currentDate.month);
  const [direction, setDirection] = useState(0);

  const goToPreviousMonth = () => {
    setDirection(-1);
    if (displayedMonth === 1) {
      setDisplayedMonth(12);
      setDisplayedYear((y) => y - 1);
    } else {
      setDisplayedMonth((m) => m - 1);
    }
  };

  const goToNextMonth = () => {
    setDirection(1);
    if (displayedMonth === 12) {
      setDisplayedMonth(1);
      setDisplayedYear((y) => y + 1);
    } else {
      setDisplayedMonth((m) => m + 1);
    }
  };

  const month = useMemo(() => {
    const count = daysInMonth(displayedYear, displayedMonth);
    const leadingBlanks = weekdayIndexSaturdayFirst(firstDayOfWeek(displayedYear, displayedMonth));
    const cells: (DayCell | null)[] = Array.from({ length: leadingBlanks }, () => null);
    const occasions: { day: number; title: string; holiday: boolean }[] = [];
    for (let day = 1; day <= count; day++) {
      const date = toGregorianDate(displayedYear, displayedMonth, day);
      const dayOccasions = occasionsOn(date);
      cells.push({ day, hijriDay: toHijri(date).day, holiday: isHoliday(date) });
      for (const o of dayOccasions) occasions.push({ day, title: o.title, holiday: o.holiday });
    }
    const first = toGregorianDate(displayedYear, displayedMonth, 1);
    const last = toGregorianDate(displayedYear, displayedMonth, count);
    const h1 = toHijri(first);
    const h2 = toHijri(last);
    return {
      cells,
      occasions,
      hijriSpan: span([hijriMonthName(h1.month), h1.year], [hijriMonthName(h2.month), h2.year]),
      gregorianSpan: span(
        [gregorianMonthNames[first.getMonth()], first.getFullYear()],
        [gregorianMonthNames[last.getMonth()], last.getFullYear()],
      ),
    };
  }, [displayedYear, displayedMonth]);

  const isDisplayingCurrentMonth =
    displayedYear === currentDate.year && displayedMonth === currentDate.month;

  const todayLabel =
    `امروز ${weekdayName(currentDate.dayOfWeek)} ` +
    `${toPersianDigits(currentDate.day)} ${monthName(currentDate.month)} ` +
    toPersianDigits(currentDate.year);

  const monthKey = `${displayedYear}-${displayedMonth}`;

  const hero = (
    <>
      <div className="calendar-hero__bar">
        <button type="button" className="sky-chip calendar-nav-button" onClick={goToNextMonth} aria-label="ماه بعد">
          <ChevronIcon className="calendar-nav-button__icon--flip" />
        </button>
        <span className="sky-pill calendar-hero__hijri">{month.hijriSpan}</span>
        <button type="button" className="sky-chip calendar-nav-button" onClick={goToPreviousMonth} aria-label="ماه قبل">
          <ChevronIcon />
        </button>
      </div>
      <p key={monthKey} className="calendar-hero__month" aria-live="polite">
        <span className="calendar-hero__name">{monthName(displayedMonth)}</span>
        <span className="calendar-hero__year">{toPersianDigits(displayedYear)}</span>
      </p>
      <p className="calendar-hero__sub">{month.gregorianSpan}</p>
      <p className="calendar-hero__today">{todayLabel}</p>
    </>
  );

  return (
    <SkyPage title={TITLE} hero={hero}>
      <section className="calendar-sheet" aria-label={`${monthName(displayedMonth)} ${toPersianDigits(displayedYear)}`}>
        <div className="calendar-weekdays" aria-hidden="true">
          {weekdayLabels.map((label, index) => (
            <div key={label} className={'calendar-weekdays__cell' + (index === 6 ? ' is-holiday' : '')}>
              {label}
            </div>
          ))}
        </div>

        <div
          key={monthKey}
          className={'calendar-grid' + (direction > 0 ? ' is-from-next' : direction < 0 ? ' is-from-prev' : '')}
        >
          {month.cells.map((cell, index) =>
            cell === null ? (
              <div key={index} className="calendar-day" aria-hidden="true" />
            ) : (
              <div
                key={index}
                className={
                  'calendar-day' +
                  (isDisplayingCurrentMonth && cell.day === currentDate.day ? ' is-today' : cell.holiday ? ' is-holiday' : '')
                }
                aria-current={isDisplayingCurrentMonth && cell.day === currentDate.day ? 'date' : undefined}
              >
                <span className="calendar-day__solar">{toPersianDigits(cell.day)}</span>
                <span className="calendar-day__hijri">{toArabicDigits(cell.hijriDay)}</span>
              </div>
            ),
          )}
        </div>
      </section>

      <TodayTimes />

      {month.occasions.length > 0 && (
        <section className="calendar-occasions sky-card" aria-labelledby="calendar-occasions-title">
          <h2 id="calendar-occasions-title">مناسبت‌های {monthName(displayedMonth)}</h2>
          <ul>
            {month.occasions.map((o, i) => (
              <li key={i} className={o.holiday ? 'is-holiday' : undefined}>
                <span className="calendar-occasions__day">{toPersianDigits(o.day)} {monthName(displayedMonth)}</span>
                <span className="calendar-occasions__title">{o.title}</span>
                {o.holiday && <span className="calendar-occasions__tag">تعطیل</span>}
              </li>
            ))}
          </ul>
        </section>
      )}

      <p className="calendar-page__note">{LUNAR_NOTE}</p>
    </SkyPage>
  );
}

export function CalendarPage() {
  return (
    <ClientOnly
      fallback={
        <SkyPage title={TITLE} hero={<div className="sky-placeholder" aria-hidden="true" />}>
          <div className="sky-placeholder--ground" aria-hidden="true" />
        </SkyPage>
      }
    >
      <CalendarPageView />
    </ClientOnly>
  );
}
