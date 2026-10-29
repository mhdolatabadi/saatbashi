import type { CSSProperties } from 'react';
import { toPersianDigits } from '../lib/persianDate';
import type { DayTrack as Track } from '../lib/dayTrack';
import './DayTrack.css';

/** The day on a bar: prayer marks at their true times and the part of the day already gone. */
export function DayTrack({ track }: { track: Track }) {
  return (
    <div
      className={'day-track' + (track.marks.some((m) => m.lane === 1) ? ' has-upper' : '')}
      style={{ '--progress': track.progress } as CSSProperties}
    >
      <div className="day-track__bar" role="progressbar" aria-label={track.night ? 'گذر شب' : 'گذر روز'}
        aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(track.progress * 100)}>
        <span className="day-track__fill" />
      </div>
      <ol className="day-track__marks">
        {track.marks.map((mark) => (
          <li
            key={mark.key}
            className={`day-track__mark is-${mark.state} lane-${mark.lane}`}
            style={{ '--at': mark.position } as CSSProperties}
          >
            <span>{mark.label}</span>
            <span dir="ltr">{toPersianDigits(mark.time)}</span>
          </li>
        ))}
      </ol>
    </div>
  );
}
