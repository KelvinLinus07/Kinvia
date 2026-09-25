import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, qs } from '../api/client';
import JourneyCard from '../components/JourneyCard';
import SearchForm from '../components/SearchForm';
import { useAuth } from '../context/AuthContext';
import { formatDate } from '../utils/format';
import { describeBudget, describeTime, readFilters, timeParams } from '../utils/searchOptions';

const RANKINGS = [
  { value: 'BALANCED', label: 'Balanced' },
  { value: 'FASTEST', label: 'Fastest' },
  { value: 'CHEAPEST', label: 'Cheapest' },
  { value: 'FEWEST_TRANSFERS', label: 'Fewest transfers' },
  { value: 'COMFORTABLE', label: 'Most comfortable' },
];

export default function SearchResultsPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [ranking, setRanking] = useState('BALANCED');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saveMessage, setSaveMessage] = useState(null);

  const originId = params.get('originId');
  const destinationId = params.get('destinationId');
  const date = params.get('date');
  const passengers = params.get('passengers') || '1';
  const transport = params.get('transport') || 'ANY';
  const filters = readFilters(params);
  const timeLabel = describeTime(filters);
  const budgetLabel = describeBudget(filters.maxBudget);
  const filtersKey = `${filters.timeMode}|${filters.timeFrom}|${filters.timeTo}|${filters.maxBudget}`;

  const initial = useMemo(
    () => ({
      origin: originId ? { id: originId, name: params.get('originName') } : null,
      destination: destinationId ? { id: destinationId, name: params.get('destinationName') } : null,
      date,
      passengers: Number(passengers),
      transport,
      ...filters,
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [params.toString()],
  );

  /** Drops one or more filters from the URL, which re-runs the search. */
  function clearFilters(...names) {
    const next = new URLSearchParams(params);
    names.forEach((name) => next.delete(name));
    navigate(`/search?${next.toString()}`);
  }

  useEffect(() => {
    if (!originId || !destinationId || !date) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    api
      .get(
        `/api/journeys/search${qs({
          originId,
          destinationId,
          date,
          passengers,
          transport: transport === 'ANY' ? undefined : transport,
          ranking,
          ...timeParams(filters),
          maxBudget: filters.maxBudget || undefined,
        })}`,
      )
      .then(setResult)
      .catch((err) => {
        setResult(null);
        setError(err.message);
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [originId, destinationId, date, passengers, transport, ranking, filtersKey]);

  async function saveThisSearch() {
    try {
      await api.post('/api/users/me/saved-journeys', {
        label: `${result.origin.name} to ${result.destination.name}`,
        originId: result.origin.id,
        destinationId: result.destination.id,
        transportPreference: transport,
        ranking,
        passengers: Number(passengers),
      });
      setSaveMessage('Saved to your dashboard.');
    } catch (err) {
      setSaveMessage(err.message);
    }
  }

  return (
    <div className="container search-page">
      <div className="search-page-form">
        <SearchForm initial={initial} />
      </div>

      {!originId || !destinationId ? (
        <p className="banner banner-info" style={{ marginTop: 24 }}>Choose an origin and a destination above to search.</p>
      ) : (
        <div className="search-results-layout" style={{ marginTop: 32 }}>
          <aside className="search-filters card" style={{ padding: 20 }}>
            <div className="search-filter-group">
              <h4>Sort by</h4>
              {RANKINGS.map((r) => (
                <label className="radio-row" key={r.value}>
                  <input type="radio" name="ranking" checked={ranking === r.value} onChange={() => setRanking(r.value)} />
                  {r.label}
                </label>
              ))}
            </div>
            {result && (
              <div className="search-filter-group">
                <h4>Trip</h4>
                <p style={{ margin: 0, fontSize: '0.85rem' }}>
                  {result.origin.name} &rarr; {result.destination.name}
                  <br />
                  {formatDate(date)} &middot; {passengers} {Number(passengers) === 1 ? 'traveller' : 'travellers'}
                </p>
                {user && (
                  <button className="btn btn-ghost btn-sm" style={{ marginTop: 8 }} onClick={saveThisSearch}>
                    Save this search
                  </button>
                )}
                {saveMessage && <p className="journey-sub">{saveMessage}</p>}
              </div>
            )}
          </aside>

          <div className="journey-list">
            {(timeLabel || budgetLabel) && (
              <div className="active-filters">
                {timeLabel && (
                  <span className="pill pill-teal">
                    {timeLabel}
                    <button type="button" aria-label="Remove departure time filter" onClick={() => clearFilters('timeOfDay', 'departAfter', 'departBefore')}>&times;</button>
                  </span>
                )}
                {budgetLabel && (
                  <span className="pill pill-teal">
                    {budgetLabel}
                    <button type="button" aria-label="Remove budget filter" onClick={() => clearFilters('maxBudget')}>&times;</button>
                  </span>
                )}
              </div>
            )}
            {loading && [1, 2, 3].map((i) => <div key={i} className="skeleton" style={{ height: 150 }} />)}
            {error && <p className="banner banner-error">{error}</p>}
            {!loading && result && result.journeys.length > 0 && (
              <p className="result-count">
                {result.journeys.length} {result.journeys.length === 1 ? 'journey' : 'journeys'}
                {result.matchedBeforeFilters > result.journeys.length && ` of ${result.matchedBeforeFilters} for this route`}
              </p>
            )}
            {!loading && result && result.journeys.length === 0 && (
              <div className="card empty-state">
                {result.matchedBeforeFilters > 0 ? (
                  <>
                    <h3>No journeys found for your selected time and budget.</h3>
                    <p>
                      {result.matchedBeforeFilters} {result.matchedBeforeFilters === 1 ? 'journey runs' : 'journeys run'} on
                      this route that day. Widen the departure time or raise the budget to see them.
                    </p>
                    <div className="empty-state-actions">
                      {timeLabel && <button className="btn btn-ghost btn-sm" onClick={() => clearFilters('timeOfDay', 'departAfter', 'departBefore')}>Any departure time</button>}
                      {budgetLabel && <button className="btn btn-ghost btn-sm" onClick={() => clearFilters('maxBudget')}>No budget limit</button>}
                      <button className="btn btn-primary btn-sm" onClick={() => clearFilters('timeOfDay', 'departAfter', 'departBefore', 'maxBudget')}>Clear all filters</button>
                    </div>
                  </>
                ) : (
                  <>
                    <h3>No journeys found</h3>
                    <p>
                      Nothing is available from {result.origin.name} to {result.destination.name} on {formatDate(date)} for{' '}
                      {passengers} {Number(passengers) === 1 ? 'traveller' : 'travellers'}. Try another date, or a different transport type.
                    </p>
                  </>
                )}
              </div>
            )}
            {!loading && result?.journeys.map((journey) => (
              <JourneyCard key={journey.key} journey={journey} passengers={passengers} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
