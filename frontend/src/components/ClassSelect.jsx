import { availabilityFor, shortClassLabel, sortClasses } from '../utils/classLabels';
import { formatMoney } from '../utils/format';

/**
 * Renders the classes actually offered for one segment as selectable cards, priced and counted from the
 * real seat inventory the backend returns. A class with too few seats for the whole party is shown but
 * disabled, so a choice here always carries through cleanly to seat selection.
 */
export default function ClassSelect({ classes, passengerCount, selectedClass, onSelect }) {
  if (!classes || classes.length === 0) {
    return <p className="banner banner-error">No travel classes are available for this train right now.</p>;
  }

  return (
    <div className="class-options">
      {sortClasses(classes).map((offer) => {
        const availability = availabilityFor(offer.availableSeats, passengerCount);
        const selected = selectedClass === offer.coachClass;
        return (
          <button
            key={offer.coachClass}
            type="button"
            className={`class-option ${selected ? 'selected' : ''} ${availability.selectable ? '' : 'disabled'}`}
            disabled={!availability.selectable}
            aria-pressed={selected}
            title={availability.selectable ? undefined : 'Not enough seats left for every traveller in this class'}
            onClick={() => onSelect(offer)}
          >
            <div className="class-option-top">
              <span className="class-option-code">{shortClassLabel(offer.coachClass)}</span>
              <span className={`pill ${availability.pillClass}`}>{availability.text}</span>
            </div>
            <div className="class-option-name">{offer.label}</div>
            <div className="class-option-fare">{formatMoney(offer.fare)}</div>
            <div className="class-option-sub">
              {offer.availableSeats} {offer.availableSeats === 1 ? 'seat' : 'seats'} left &middot; per traveller
            </div>
          </button>
        );
      })}
    </div>
  );
}
