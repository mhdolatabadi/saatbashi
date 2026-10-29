import { midnight, type PrayerTimes } from './prayerTimes';

export type TrackKey = keyof PrayerTimes | 'midnight';

const labels: Record<TrackKey, { full: string; short: string }> = {
  fajr: { full: 'اذان صبح', short: 'صبح' },
  sunrise: { full: 'طلوع آفتاب', short: 'طلوع' },
  dhuhr: { full: 'اذان ظهر', short: 'ظهر' },
  asr: { full: 'اذان عصر', short: 'عصر' },
  sunset: { full: 'غروب آفتاب', short: 'غروب' },
  maghrib: { full: 'اذان مغرب', short: 'مغرب' },
  isha: { full: 'اذان عشا', short: 'عشا' },
  midnight: { full: 'نیمه‌شب شرعی', short: 'نیمه‌شب' },
};

/** Today's rows in order, as the prayer page lists them. */
export const dayRows: TrackKey[] = ['fajr', 'sunrise', 'dhuhr', 'asr', 'sunset', 'maghrib', 'isha'];

export type MarkState = 'past' | 'next' | 'future';

export interface TrackMark {
  key: TrackKey;
  label: string;
  time: string;
  /** 0..1 along the track. */
  position: number;
  state: MarkState;
  /** 0: label below the bar; 1: above it, when the mark sits too close to its neighbour below. */
  lane: 0 | 1;
}

/** Labels closer than this share no lane: about one label width on a phone. */
export const MIN_LABEL_GAP = 0.16;

export interface DayTrack {
  next: { key: TrackKey; label: string; shortLabel: string; time: string; minutesLeft: number; tomorrow: boolean };
  /** True between Isha and dawn: the track then runs from Isha to the next dawn. */
  night: boolean;
  /** 0..1, how far along the track now is. */
  progress: number;
  marks: TrackMark[];
}

function toMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + m;
}


/** The next row after [nowMinutes] (minute of the day at the place), or tomorrow's dawn after the last one. */
export function nextRow(times: PrayerTimes, nowMinutes: number): DayTrack['next'] {
  const key = dayRows.find((k) => toMinutes(times[k as keyof PrayerTimes]) > nowMinutes);
  const tomorrow = key === undefined;
  const k = (key ?? 'fajr') as keyof PrayerTimes;
  const at = toMinutes(times[k]) + (tomorrow ? 1440 : 0);
  return { key: k, label: labels[k].full, shortLabel: labels[k].short, time: times[k], minutesLeft: at - nowMinutes, tomorrow };
}

/**
 * The day track shown under the next prayer: from dawn to Isha with the five adhans during the day,
 * and from Isha through Shar'i midnight to the next dawn at night.
 */
export function dayTrack(times: PrayerTimes, nowMinutes: number): DayTrack {
  const next = nextRow(times, nowMinutes);
  const fajr = toMinutes(times.fajr);
  const isha = toMinutes(times.isha);
  const night = nowMinutes < fajr || nowMinutes >= isha;

  // Night minutes are measured from Isha on a clock that runs past 24:00.
  const nightClock = (m: number) => (m < isha ? m + 1440 : m);
  const points: { key: TrackKey; at: number; time: string }[] = night
    ? [
        { key: 'isha', at: isha, time: times.isha },
        { key: 'midnight', at: nightClock(toMinutes(midnight(times))), time: midnight(times) },
        { key: 'fajr', at: fajr + 1440, time: times.fajr },
      ]
    : (['fajr', 'dhuhr', 'asr', 'maghrib', 'isha'] as const).map((key) => ({ key, at: toMinutes(times[key]), time: times[key] }));

  const start = points[0].at;
  const span = points[points.length - 1].at - start;
  const now = night ? nightClock(nowMinutes) : nowMinutes;
  const nextIndex = points.findIndex((p) => p.at > now);

  // Marks sit at their true place in time; a label that would run into the one before it moves above the bar.
  const lastInLane = [-Infinity, -Infinity];
  const marks = points.map((p, i): TrackMark => {
    const position = (p.at - start) / span;
    const lane: 0 | 1 = position - lastInLane[0] < MIN_LABEL_GAP && position - lastInLane[1] >= MIN_LABEL_GAP ? 1 : 0;
    lastInLane[lane] = position;
    return {
      key: p.key,
      label: labels[p.key].short,
      time: p.time,
      position,
      state: i === nextIndex ? 'next' : i < nextIndex || nextIndex === -1 ? 'past' : 'future',
      lane,
    };
  });

  return { next, night, progress: Math.min(Math.max((now - start) / span, 0), 1), marks };
}

/** Each of today's rows plus Shar'i midnight, marked past, next or upcoming. Midnight is never dimmed. */
export function dayTiles(times: PrayerTimes, nowMinutes: number): { key: TrackKey; label: string; time: string; state: MarkState }[] {
  const next = nextRow(times, nowMinutes);
  const rows = dayRows.map((key) => {
    const time = times[key as keyof PrayerTimes];
    const state: MarkState = key === next.key && !next.tomorrow ? 'next' : toMinutes(time) <= nowMinutes ? 'past' : 'future';
    return { key, label: labels[key].full, time, state };
  });
  // After Isha the next row is tomorrow's dawn: today's dawn tile stands in for it.
  if (next.tomorrow) rows[0].state = 'next';
  return [...rows, { key: 'midnight', label: labels.midnight.full, time: midnight(times), state: 'future' }];
}
