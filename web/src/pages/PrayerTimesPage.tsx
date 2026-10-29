import { useEffect, useState, type CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import { APPROXIMATE_NOTE, formatDuration } from '../lib/prayerTimes';
import { toPersianDigits } from '../lib/persianDate';
import { hijriLabel, solarLabel } from '../lib/dateLabels';
import { dayTiles, dayTrack } from '../lib/dayTrack';
import { minutesAt, placeLabel, timesFor, usePlace } from '../lib/place';
import { CityPicker } from '../components/PrayerTimes';
import { LocationIcon } from '../components/icons';
import { SkyPage } from '../components/SkyPage';
import './LandingPage.css';
import './PrayerTimesPage.css';
import { ClientOnly } from '../components/ClientOnly';

const TITLE = 'اوقات شرعی امروز';
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

  const hero = (
    <>
      <div className="sky-hero__bar">
        <CityPicker cities={cities} value={place.kind === 'city' ? place.city.slug : ''} onChange={selectCity} />
        <span className="sky-pill">{hijriLabel(now)}</span>
      </div>
      <div className="next-prayer">
        <p className="next-prayer__date">{solarLabel(now)}</p>
        <p className="next-prayer__line">
          <span className="next-prayer__name">{next.shortLabel}</span>
          <span className="next-prayer__time" dir="ltr">{toPersianDigits(next.time)}</span>
        </p>
        <p className="next-prayer__left" aria-live="polite">
          {next.label}{next.tomorrow && ' فردا'}، {formatDuration(next.minutesLeft)} دیگر
        </p>
      </div>
      <div className="day-track" style={{ '--progress': track.progress } as CSSProperties}>
        <div className="day-track__bar" role="progressbar" aria-label={track.night ? 'گذر شب' : 'گذر روز'}
          aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(track.progress * 100)}>
          <span className="day-track__fill" />
        </div>
        <ol className="day-track__marks" style={{ '--count': track.marks.length } as CSSProperties}>
          {track.marks.map((mark) => (
            <li key={mark.key} className={`day-track__mark is-${mark.state}`}>
              <span>{mark.label}</span>
              <span dir="ltr">{toPersianDigits(mark.time)}</span>
            </li>
          ))}
        </ol>
      </div>
    </>
  );

  return (
    <SkyPage title={TITLE} hero={hero}>
      <ul className="prayer-tiles" aria-label="اوقات امروز">
        {dayTiles(times, nowMinutes).map(({ key, label, time, state }) => (
          <li key={key} className={`prayer-tile sky-card is-${state}`}>
            <span className="prayer-tile__label">
              {label}
              {state === 'next' && <span className="prayer-next-label">بعدی</span>}
            </span>
            <span className="prayer-tile__time" dir="ltr">{toPersianDigits(time)}</span>
          </li>
        ))}
      </ul>
      <div className="prayer-place sky-card">
        <span className="prayer-place__label">
          {placeLabel(place)}
          {locationError && ' — دسترسی به موقعیت مکانی داده نشد'}
        </span>
        <button type="button" className="prayer-place__location" onClick={useMyLocation}>
          <LocationIcon />
          موقعیت من
        </button>
      </div>
      <p className="prayer-times-page__note">
        {APPROXIMATE_NOTE} {LUNAR_NOTE} اوقات شهرهای دیگر را در <Link to="/prayer-times">اوقات شرعی شهرهای ایران</Link> ببینید.
      </p>
    </SkyPage>
  );
}

export function PrayerTimesPage() {
  return (
    <ClientOnly
      fallback={
        <SkyPage title={TITLE} hero={<div className="sky-placeholder" aria-hidden="true" />}>
          <div className="sky-placeholder--ground" aria-hidden="true" />
        </SkyPage>
      }
    >
      <PrayerTimesPageView />
    </ClientOnly>
  );
}
