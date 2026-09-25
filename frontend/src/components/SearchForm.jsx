import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import StationInput from './StationInput';
import { BUDGET_CHOICES, TIME_BANDS, TIME_MODES, timeParams } from '../utils/searchOptions';
import { formatMoney } from '../utils/format';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

const CUSTOM_BUDGET = 'CUSTOM';

function initialBudgetChoice(maxBudget) {
  if (!maxBudget) return '';
  return BUDGET_CHOICES.includes(maxBudget) ? String(maxBudget) : CUSTOM_BUDGET;
}

export default function SearchForm({ dark, initial }) {
  const navigate = useNavigate();
  const [origin, setOrigin] = useState(initial?.origin || null);
  const [destination, setDestination] = useState(initial?.destination || null);
  const [date, setDate] = useState(initial?.date || todayIso());
  const [passengers, setPassengers] = useState(initial?.passengers || 1);
  const [transport, setTransport] = useState(initial?.transport || 'ANY');
  const [timeMode, setTimeMode] = useState(initial?.timeMode || TIME_MODES.ANY);
  const [timeFrom, setTimeFrom] = useState(initial?.timeFrom || '');
  const [timeTo, setTimeTo] = useState(initial?.timeTo || '');
  const [budgetChoice, setBudgetChoice] = useState(initialBudgetChoice(initial?.maxBudget));
  const [customBudget, setCustomBudget] = useState(
    initial?.maxBudget && !BUDGET_CHOICES.includes(initial.maxBudget) ? String(initial.maxBudget) : '',
  );
  const [error, setError] = useState(null);

  function validate() {
    if (!origin || !destination) return 'Choose an origin and a destination from the suggestions.';
    if (origin.id === destination.id) return 'Origin and destination must be different.';
    if (!date || date < todayIso()) return 'Choose a travel date from today onwards.';
    if (timeMode === TIME_MODES.CUSTOM && !timeFrom && !timeTo) return 'Enter a start or an end time, or pick a time of day.';
    if (budgetChoice === CUSTOM_BUDGET && !(Number(customBudget) > 0)) return 'Enter a budget greater than zero.';
    return null;
  }

  function submit(event) {
    event.preventDefault();
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }
    setError(null);
    const maxBudget = budgetChoice === CUSTOM_BUDGET ? Number(customBudget) : Number(budgetChoice) || undefined;
    const params = new URLSearchParams({
      originId: origin.id,
      destinationId: destination.id,
      originName: origin.name,
      destinationName: destination.name,
      date,
      passengers,
      transport,
    });
    Object.entries({ ...timeParams({ timeMode, timeFrom, timeTo }), maxBudget }).forEach(([key, value]) => {
      if (value !== undefined && value !== '') params.set(key, value);
    });
    navigate(`/search?${params.toString()}`);
  }

  return (
    <form className={`search-form ${dark ? 'search-form-dark' : ''}`} onSubmit={submit}>
      <div className="search-form-row">
        <StationInput id="origin" label="From" value={origin} onChange={setOrigin} placeholder="Station, city or code" />
        <StationInput id="destination" label="To" value={destination} onChange={setDestination} placeholder="Station, city or code" />
      </div>
      <div className="search-form-row">
        <div className="field">
          <label htmlFor="date">Date</label>
          <input id="date" type="date" min={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="passengers">Travellers</label>
          <select id="passengers" value={passengers} onChange={(e) => setPassengers(Number(e.target.value))}>
            {[1, 2, 3, 4, 5, 6].map((n) => (
              <option key={n} value={n}>{n} {n === 1 ? 'traveller' : 'travellers'}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="transport">Transport</label>
          <select id="transport" value={transport} onChange={(e) => setTransport(e.target.value)}>
            <option value="ANY">Any</option>
            <option value="TRAIN">Train</option>
            <option value="BUS">Bus</option>
          </select>
        </div>
      </div>

      <div className="search-form-row search-form-filters">
        <div className="field search-form-time">
          <label id="time-label">Departure time</label>
          <div className="chip-row" role="group" aria-labelledby="time-label">
            <button type="button" className={`chip ${timeMode === TIME_MODES.ANY ? 'chip-on' : ''}`} onClick={() => setTimeMode(TIME_MODES.ANY)}>
              Any time
            </button>
            {TIME_BANDS.map((band) => (
              <button
                type="button"
                key={band.key}
                className={`chip ${timeMode === band.key ? 'chip-on' : ''}`}
                onClick={() => setTimeMode(band.key)}
                title={band.hours}
              >
                {band.label}
                <span className="chip-sub">{band.hours}</span>
              </button>
            ))}
            <button type="button" className={`chip ${timeMode === TIME_MODES.CUSTOM ? 'chip-on' : ''}`} onClick={() => setTimeMode(TIME_MODES.CUSTOM)}>
              Custom
            </button>
          </div>
          {timeMode === TIME_MODES.CUSTOM && (
            <div className="time-range">
              <input type="time" aria-label="Depart after" value={timeFrom} onChange={(e) => setTimeFrom(e.target.value)} />
              <span>to</span>
              <input type="time" aria-label="Depart before" value={timeTo} onChange={(e) => setTimeTo(e.target.value)} />
            </div>
          )}
        </div>

        <div className="field search-form-budget">
          <label htmlFor="budget">Max budget (per traveller)</label>
          <select id="budget" value={budgetChoice} onChange={(e) => setBudgetChoice(e.target.value)}>
            <option value="">No limit</option>
            {BUDGET_CHOICES.map((amount) => (
              <option key={amount} value={amount}>Up to {formatMoney(amount)}</option>
            ))}
            <option value={CUSTOM_BUDGET}>Custom amount</option>
          </select>
          {budgetChoice === CUSTOM_BUDGET && (
            <input
              type="number"
              min="1"
              step="1"
              inputMode="numeric"
              placeholder="Amount in rupees"
              aria-label="Custom maximum budget in rupees"
              value={customBudget}
              onChange={(e) => setCustomBudget(e.target.value)}
            />
          )}
        </div>
      </div>

      {error && <p className="field-error" role="alert">{error}</p>}
      <button type="submit" className="btn btn-primary btn-block search-form-submit">
        Search journeys
      </button>
    </form>
  );
}
