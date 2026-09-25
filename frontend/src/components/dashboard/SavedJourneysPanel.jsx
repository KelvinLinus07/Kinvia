import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api/client';

export default function SavedJourneysPanel() {
  const [journeys, setJourneys] = useState(null);
  const navigate = useNavigate();

  function load() {
    api.get('/api/users/me/saved-journeys').then(setJourneys);
  }
  useEffect(load, []);

  async function remove(id) {
    await api.delete(`/api/users/me/saved-journeys/${id}`);
    load();
  }

  function searchAgain(journey) {
    const params = new URLSearchParams({
      originId: journey.origin.id,
      destinationId: journey.destination.id,
      originName: journey.origin.name,
      destinationName: journey.destination.name,
      date: new Date().toISOString().slice(0, 10),
      passengers: journey.passengers,
      transport: journey.transportPreference,
    });
    navigate(`/search?${params.toString()}`);
  }

  if (!journeys) return <div className="skeleton" style={{ height: 120 }} />;
  if (journeys.length === 0) return <p>No saved journeys yet. Save one from the search results page.</p>;

  return (
    <div className="journey-list">
      {journeys.map((j) => (
        <div key={j.id} className="card" style={{ padding: 18, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <strong>{j.label}</strong>
            <div className="journey-sub">{j.origin.name} &rarr; {j.destination.name} &middot; {j.passengers} traveller(s)</div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-primary btn-sm" onClick={() => searchAgain(j)}>Search again</button>
            <button className="btn btn-ghost btn-sm" onClick={() => remove(j.id)}>Remove</button>
          </div>
        </div>
      ))}
    </div>
  );
}
