import { formatMoney } from './format';

// Departure bands. The keys match the backend's TimeOfDay enum.
export const TIME_BANDS = [
  { key: 'NIGHT', label: 'Night', hours: '00:00 - 06:00' },
  { key: 'MORNING', label: 'Morning', hours: '06:00 - 12:00' },
  { key: 'AFTERNOON', label: 'Afternoon', hours: '12:00 - 18:00' },
  { key: 'EVENING', label: 'Evening', hours: '18:00 - 24:00' },
];

// Quick budget choices (per traveller). "Custom" lets the user type any amount.
export const BUDGET_CHOICES = [300, 500, 1000, 1500, 2000];

const TIME_MODES = { ANY: 'ANY', CUSTOM: 'CUSTOM' };

/** Turns the form's departure-time choice into the URL/API parameters the backend understands. */
export function timeParams({ timeMode, timeFrom, timeTo }) {
  if (timeMode === TIME_MODES.ANY) return {};
  if (timeMode === TIME_MODES.CUSTOM) return { departAfter: timeFrom || undefined, departBefore: timeTo || undefined };
  return { timeOfDay: timeMode };
}

/** Reads the same parameters back from a URLSearchParams into form state. */
export function readFilters(params) {
  const timeOfDay = params.get('timeOfDay');
  const departAfter = params.get('departAfter') || '';
  const departBefore = params.get('departBefore') || '';
  let timeMode = TIME_MODES.ANY;
  if (timeOfDay) timeMode = timeOfDay;
  else if (departAfter || departBefore) timeMode = TIME_MODES.CUSTOM;
  const maxBudget = params.get('maxBudget');
  return {
    timeMode,
    timeFrom: departAfter.slice(0, 5),
    timeTo: departBefore.slice(0, 5),
    maxBudget: maxBudget ? Number(maxBudget) : null,
  };
}

export function describeTime({ timeMode, timeFrom, timeTo }) {
  if (timeMode === TIME_MODES.ANY) return null;
  if (timeMode === TIME_MODES.CUSTOM) {
    if (timeFrom && timeTo) return `Departing ${timeFrom} - ${timeTo}`;
    return timeFrom ? `Departing after ${timeFrom}` : `Departing before ${timeTo}`;
  }
  const band = TIME_BANDS.find((b) => b.key === timeMode);
  return band ? `${band.label} (${band.hours})` : null;
}

export function describeBudget(maxBudget) {
  return maxBudget ? `Up to ${formatMoney(maxBudget)} per traveller` : null;
}

export { TIME_MODES };
