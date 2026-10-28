import { useEffect, useState, type CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import { APPROXIMATE_NOTE, formatDuration } from '../lib/prayerTimes';
import { toPersianDigits } from '../lib/persianDate';
import { hijriLabel, solarLabel } from '../lib/dateLabels';
import { dayTiles, dayTrack } from '../lib/dayTrack';
import { minutesAt, placeLabel, timesFor, usePlace } from '../lib/place';
import { CityPicker } from '../components/PrayerTimes';
import { LocationIcon } from '../components/icons';
import './LandingPage.css';
import './PrayerTimesPage.css';
import { ClientOnly } from '../components/ClientOnly';

const LUNAR_NOTE = 'تاریخ قمری محاسبه‌ای است و ممکن است با تقویم رسمی (رؤیت هلال) یک روز اختلاف داشته باشد.';

function PrayerTimesPageView() {
  const { place, selectCity, useMyLocation, locationError, cities } = usePlace();
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const interval = setInterval(() => setNow(new Date()), 30_000);
    return () => clearInterval(interval);
  }, []);

  const times = timesFor(place, now);
  const nowMinutes = minutesAt(place, now);
  const track = dayTrack(times, nowMinutes);
  const { next } = track;

  return (
    <>
      <section className="sky-hero" aria-labelledby="next-prayer">
        <div className="sky-hero__bar">
          <CityPicker cities={cities} value={place.kind === 'city' ? place.city.slug : ''} onChange={selectCity} />
          <span className="sky-hero__hijri">{hijriLabel(now)}</span>
        </div>
        <p className="sky-hero__date">{solarLabel(now)}</p>
        <p className="sky-hero__kicker">{next.label}{next.tomorrow && ' فردا'}</p>
        <div id="next-prayer" className="sky-hero__next">
          <span className="sky-hero__name">{next.shortLabel}</span>
          <span className="sky-hero__time" dir="ltr">{toPersianDigits(next.time)}</span>
        </div>
        <p className="sky-hero__left" aria-live="polite">{formatDuration(next.minutesLeft)} دیگر</p>

        <div className="day-track" style={{ '--progress': track.progress } as CSSProperties}>
          <div className="day-track__bar" role="progressbar" aria-label={track.night ? 'گذر شب' : 'گذر روز'}
            aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(track.progress * 100)}>
            <span className="day-track__fill" />
          </div>
          <ol className="day-track__marks">
            {track.marks.map((mark) => (
              <li key={mark.key} className={`day-track__mark is-${mark.state}`} style={{ '--at': mark.position } as CSSProperties}>
                <span>{mark.label}</span>
                <span dir="ltr">{toPersianDigits(mark.time)}</span>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="prayer-sheet glass" aria-label="اوقات امروز">
        <div className="prayer-sheet__head">
          <span className="prayer-sheet__place">
            {placeLabel(place)}
            {locationError && ' — دسترسی به موقعیت مکانی داده نشد'}
          </span>
          <button type="button" className="prayer-sheet__location" onClick={useMyLocation}>
            <LocationIcon />
            موقعیت من
          </button>
        </div>
        <ul className="prayer-tiles">
          {dayTiles(times, nowMinutes).map(({ key, label, time, state }) => (
            <li key={key} className={`prayer-tile is-${state}`}>
              <span className="prayer-tile__label">
                {label}
                {state === 'next' && <span className="prayer-next-label">بعدی</span>}
              </span>
              <span className="prayer-tile__time" dir="ltr">{toPersianDigits(time)}</span>
            </li>
          ))}
        </ul>
      </section>

      <p className="prayer-times-page__note">
        {APPROXIMATE_NOTE} {LUNAR_NOTE} اوقات شهرهای دیگر را در <Link to="/prayer-times">اوقات شرعی شهرهای ایران</Link> ببینید.
      </p>
    </>
  );
}

export function PrayerTimesPage() {
  return (
    <div className="prayer-times-page">
      <h1 className="prayer-times-page__title">اوقات شرعی امروز</h1>
      <ClientOnly fallback={<div className="glass page-placeholder page-placeholder--prayer" aria-hidden="true" />}>
        <PrayerTimesPageView />
      </ClientOnly>
    </div>
  );
}
