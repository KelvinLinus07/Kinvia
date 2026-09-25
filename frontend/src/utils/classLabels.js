// Small, shared helpers for rendering travel classes consistently across the search results and the
// booking wizard. The backend is the source of truth for which classes exist and what they cost — this
// file only decides how to label, order and flag them for display.

/** Compact code shown on chips and radio cards, the way travellers already recognise these classes. */
const SHORT_LABELS = {
  SLEEPER: 'SL',
  CHAIR_CAR: 'CC',
  AC_3_TIER: '3A',
  AC_2_TIER: '2A',
  AC_1_TIER: '1A',
  SEATER: 'Seater',
  AC_SEATER: 'AC Seater',
  AC_SLEEPER: 'AC Sleeper',
};

/** Display order, lowest comfort first, matching how these classes are usually listed for booking. */
const CLASS_ORDER = {
  SLEEPER: 1,
  CHAIR_CAR: 2,
  AC_3_TIER: 3,
  AC_2_TIER: 4,
  AC_1_TIER: 5,
  SEATER: 0,
  AC_SEATER: 1,
  AC_SLEEPER: 2,
};

export function shortClassLabel(coachClass) {
  return SHORT_LABELS[coachClass] || coachClass;
}

/** Returns a new array; never mutates the classes coming from the API response. */
export function sortClasses(classes) {
  return [...classes].sort((a, b) => (CLASS_ORDER[a.coachClass] ?? 99) - (CLASS_ORDER[b.coachClass] ?? 99));
}

/**
 * Availability is read from real seat inventory (never invented). A class needs at least one seat per
 * traveller to be actually bookable as a group, so "RAC / Limited" is informational rather than a state
 * the traveller can select into — picking it would leave them unable to seat everyone at the seat map step.
 */
export function availabilityFor(availableSeats, passengerCount) {
  if (availableSeats <= 0) {
    return { state: 'unavailable', text: 'Not available', pillClass: 'pill-red', selectable: false };
  }
  if (availableSeats < passengerCount) {
    return { state: 'limited', text: 'RAC / Limited', pillClass: 'pill-amber', selectable: false };
  }
  return { state: 'available', text: 'Available', pillClass: 'pill-teal', selectable: true };
}
