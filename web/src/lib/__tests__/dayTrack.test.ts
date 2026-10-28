import { describe, expect, it } from 'vitest';
import { dayTiles, dayTrack, nextRow } from '../dayTrack';
import type { PrayerTimes } from '../prayerTimes';

const times: PrayerTimes = {
  fajr: '04:30',
  sunrise: '06:00',
  dhuhr: '12:00',
  asr: '15:30',
  sunset: '18:00',
  maghrib: '18:20',
  isha: '19:30',
};

describe('next row', () => {
  it('picks the next row of the day, sunrise and sunset included', () => {
    expect(nextRow(times, 5 * 60)).toMatchObject({ key: 'sunrise', shortLabel: 'طلوع', minutesLeft: 60, tomorrow: false });
    expect(nextRow(times, 15 * 60)).toMatchObject({ key: 'asr', label: 'اذان عصر', time: '15:30', minutesLeft: 30 });
  });
  it("counts to tomorrow's dawn after Isha", () => {
    expect(nextRow(times, 23 * 60)).toMatchObject({ key: 'fajr', minutesLeft: 330, tomorrow: true });
  });
});

describe('day track', () => {
  it('runs from dawn to Isha through the five adhans during the day', () => {
    const track = dayTrack(times, 15 * 60);
    expect(track.night).toBe(false);
    expect(track.marks.map((m) => m.key)).toEqual(['fajr', 'dhuhr', 'asr', 'maghrib', 'isha']);
    expect(track.marks.map((m) => m.state)).toEqual(['past', 'past', 'next', 'future', 'future']);
    expect(track.marks[0].position).toBe(0);
    expect(track.marks[4].position).toBe(1);
    expect(track.progress).toBeCloseTo((15 * 60 - 270) / (1170 - 270));
  });
  it('runs from Isha through midnight to the next dawn at night', () => {
    // Shar'i midnight is halfway from sunset (18:00) to the next dawn (04:30): 23:15.
    const evening = dayTrack(times, 21 * 60);
    expect(evening.night).toBe(true);
    expect(evening.marks.map((m) => [m.key, m.time, m.state])).toEqual([
      ['isha', '19:30', 'past'],
      ['midnight', '23:15', 'next'],
      ['fajr', '04:30', 'future'],
    ]);
    const beforeDawn = dayTrack(times, 2 * 60);
    expect(beforeDawn.marks.map((m) => m.state)).toEqual(['past', 'past', 'next']);
    expect(beforeDawn.progress).toBeCloseTo((2 * 60 + 1440 - 1170) / (270 + 1440 - 1170));
  });
});

describe('day tiles', () => {
  it('dims elapsed rows and marks the next one', () => {
    const tiles = dayTiles(times, 15 * 60);
    expect(tiles.map((t) => t.state)).toEqual(['past', 'past', 'past', 'next', 'future', 'future', 'future', 'future']);
    expect(tiles[7]).toMatchObject({ key: 'midnight', time: '23:15' });
  });
  it("marks dawn as next after Isha, standing in for tomorrow's", () => {
    expect(dayTiles(times, 23 * 60).map((t) => t.state)).toEqual(['next', 'past', 'past', 'past', 'past', 'past', 'past', 'future']);
  });
});
