import { useNavigate } from 'react-router-dom';
import RouteLine from './RouteLine';
import { shortClassLabel, sortClasses } from '../utils/classLabels';
import { formatDuration, formatMoney, formatTime } from '../utils/format';

const TAG_LABELS = {
  RECOMMENDED: 'Recommended',
  FASTEST: 'Fastest',
  CHEAPEST: 'Cheapest',
  FEWEST_TRANSFERS: 'Fewest transfers',
  MOST_COMFORTABLE: 'Most comfortable',
};

export default function JourneyCard({ journey, passengers }) {
  const navigate = useNavigate();
  const notBookable = !journey.bookable;
  // Classes only line up neatly on a direct service; a connecting journey shows its per-leg classes once
  // a traveller opens it, after picking which leg matters most to them.
  const directClasses = journey.segments.length === 1 ? sortClasses(journey.segments[0].classes) : [];

  return (
    <article className="card journey-card">
      <div className="journey-card-tags">
        {journey.tags.map((tag) => (
          <span key={tag} className={`pill ${tag === 'RECOMMENDED' ? 'pill-amber' : 'pill-teal'}`}>
            {TAG_LABELS[tag] || tag}
          </span>
        ))}
        {journey.multimodal && <span className="pill pill-ink">Multimodal</span>}
      </div>

      <div className="journey-card-times">
        <div>
          <div className="journey-time">{formatTime(journey.departure)}</div>
          <div className="journey-sub">{journey.segments[0].board.name}</div>
        </div>
        <div className="journey-mid">
          <div className="journey-duration">{formatDuration(journey.durationMinutes)}</div>
          <RouteLine segments={journey.segments} />
          <div className="journey-transfers">
            {journey.transfers === 0 ? 'Direct' : `${journey.transfers} ${journey.transfers === 1 ? 'change' : 'changes'}`}
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="journey-time">{formatTime(journey.arrival)}</div>
          <div className="journey-sub">{journey.segments[journey.segments.length - 1].alight.name}</div>
        </div>
      </div>

      <ul className="journey-highlights">
        {journey.highlights.map((h, i) => (
          <li key={i}>{h}</li>
        ))}
      </ul>

      {directClasses.length > 0 && (
        <div className="journey-classes" aria-label="Available classes">
          {directClasses.map((c) => (
            <div key={c.coachClass} className={`class-chip-mini ${c.availableSeats <= 0 ? 'class-chip-mini-sold-out' : ''}`}>
              <span className="class-chip-mini-label">{shortClassLabel(c.coachClass)}</span>
              <span className="class-chip-mini-fare">
                {c.availableSeats > 0 ? formatMoney(c.fare) : 'Sold out'}
              </span>
            </div>
          ))}
        </div>
      )}

      <div className="journey-card-footer">
        <div>
          <div className="journey-fare">{formatMoney(journey.farePerPassenger)}</div>
          <div className="journey-sub">
            {directClasses.length > 0 ? 'starting fare' : 'per traveller'} &middot; {journey.availableSeats} seats left
          </div>
        </div>
        <button
          className="btn btn-primary"
          disabled={notBookable}
          onClick={() => navigate(`/journeys/${encodeURIComponent(journey.key)}/book?passengers=${passengers}`)}
        >
          {notBookable ? 'Sold out' : 'Select'}
        </button>
      </div>
    </article>
  );
}
