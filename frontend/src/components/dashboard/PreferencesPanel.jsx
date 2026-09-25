import { useEffect, useState } from 'react';
import { api } from '../../api/client';

export default function PreferencesPanel() {
  const [prefs, setPrefs] = useState(null);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    api.get('/api/users/me/preferences').then(setPrefs);
  }, []);

  async function save(e) {
    e.preventDefault();
    const updated = await api.put('/api/users/me/preferences', prefs);
    setPrefs(updated);
    setMessage('Preferences saved.');
  }

  if (!prefs) return <div className="skeleton" style={{ height: 160 }} />;

  return (
    <form className="card" style={{ padding: 20, maxWidth: 480 }} onSubmit={save}>
      <h3>Travel preferences</h3>
      <p className="journey-sub">These are used to personalise search when you do not specify your own filters.</p>
      {message && <p className="banner banner-info">{message}</p>}
      <div className="field">
        <label>Preferred transport</label>
        <select value={prefs.transportPreference} onChange={(e) => setPrefs({ ...prefs, transportPreference: e.target.value })}>
          <option value="ANY">Any</option>
          <option value="TRAIN">Train</option>
          <option value="BUS">Bus</option>
        </select>
      </div>
      <div className="field" style={{ marginTop: 12 }}>
        <label>Default sort</label>
        <select value={prefs.ranking} onChange={(e) => setPrefs({ ...prefs, ranking: e.target.value })}>
          <option value="BALANCED">Balanced</option>
          <option value="FASTEST">Fastest</option>
          <option value="CHEAPEST">Cheapest</option>
          <option value="FEWEST_TRANSFERS">Fewest transfers</option>
          <option value="COMFORTABLE">Most comfortable</option>
        </select>
      </div>
      <div className="field" style={{ marginTop: 12 }}>
        <label>Maximum transfers</label>
        <select value={prefs.maxTransfers} onChange={(e) => setPrefs({ ...prefs, maxTransfers: Number(e.target.value) })}>
          <option value={0}>Direct only</option>
          <option value={1}>Up to 1 change</option>
          <option value={2}>Up to 2 changes</option>
        </select>
      </div>
      <label className="radio-row" style={{ marginTop: 12 }}>
        <input type="checkbox" checked={prefs.journeyReminders} onChange={(e) => setPrefs({ ...prefs, journeyReminders: e.target.checked })} />
        Send me a reminder before departure
      </label>
      <button className="btn btn-primary btn-sm" style={{ marginTop: 16 }} type="submit">Save preferences</button>
    </form>
  );
}
