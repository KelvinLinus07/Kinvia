import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { formatMoney } from '../utils/format';

export default function OperatorPage() {
  const [tab, setTab] = useState('overview');
  return (
    <div className="container dashboard-page">
      <h1>Operator desk</h1>
      <div className="dashboard-layout">
        <nav className="dashboard-tabs">
          {['overview', 'vehicles', 'routes', 'trips', 'bookings'].map((t) => (
            <button key={t} className={`dashboard-tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
              {t[0].toUpperCase() + t.slice(1)}
            </button>
          ))}
        </nav>
        <div className="dashboard-content">
          {tab === 'overview' && <OperatorOverview />}
          {tab === 'vehicles' && <VehiclesPanel />}
          {tab === 'routes' && <RoutesPanel />}
          {tab === 'trips' && <TripsPanel />}
          {tab === 'bookings' && <OperatorBookings />}
        </div>
      </div>
    </div>
  );
}

function OperatorOverview() {
  const [d, setD] = useState(null);
  useEffect(() => { api.get('/api/operator/dashboard').then(setD); }, []);
  if (!d) return <div className="skeleton" style={{ height: 160 }} />;
  return (
    <div className="stat-grid">
      <Stat label="Vehicles" value={d.totalVehicles} />
      <Stat label="Routes" value={d.totalRoutes} />
      <Stat label="Trips today" value={d.tripsToday} />
      <Stat label="Upcoming bookings" value={d.upcomingBookings} />
    </div>
  );
}

function Stat({ label, value }) {
  return (
    <div className="card stat-card">
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

function VehiclesPanel() {
  const [vehicles, setVehicles] = useState(null);
  useEffect(() => { api.get('/api/operator/vehicles?size=30').then((r) => setVehicles(r.items)); }, []);
  if (!vehicles) return <div className="skeleton" style={{ height: 160 }} />;
  return (
    <table className="table">
      <thead><tr><th>Number</th><th>Name</th><th>Mode</th><th>Coaches</th><th>Status</th></tr></thead>
      <tbody>
        {vehicles.map((v) => (
          <tr key={v.id}>
            <td>{v.number}</td><td>{v.name}</td><td>{v.mode}</td>
            <td>{v.coaches.map((c) => c.code).join(', ')}</td><td>{v.status}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function RoutesPanel() {
  const [routes, setRoutes] = useState(null);
  useEffect(() => { api.get('/api/operator/routes?size=30').then((r) => setRoutes(r.items)); }, []);
  if (!routes) return <div className="skeleton" style={{ height: 160 }} />;
  return (
    <table className="table">
      <thead><tr><th>Code</th><th>Name</th><th>Stops</th><th>Status</th></tr></thead>
      <tbody>
        {routes.map((r) => (
          <tr key={r.id}>
            <td>{r.code}</td><td>{r.name}</td>
            <td>{r.stops.map((s) => s.stationCode).join(' \u2192 ')}</td>
            <td>{r.active ? 'Active' : 'Inactive'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TripsPanel() {
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [trips, setTrips] = useState(null);

  function load() {
    api.get(`/api/operator/trips?date=${date}&size=30`).then((r) => setTrips(r.items));
  }
  useEffect(load, [date]); // eslint-disable-line react-hooks/exhaustive-deps

  async function updateStatus(id, status, delayMinutes) {
    await api.put(`/api/operator/trips/${id}/status`, { status, delayMinutes: delayMinutes || 0, note: '' });
    load();
  }

  return (
    <div>
      <div className="field" style={{ maxWidth: 220, marginBottom: 16 }}>
        <label>Date</label>
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </div>
      {!trips ? <div className="skeleton" style={{ height: 160 }} /> : (
        <table className="table">
          <thead><tr><th>Route</th><th>Vehicle</th><th>Departure</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {trips.map((t) => (
              <tr key={t.id}>
                <td>{t.routeName}</td><td>{t.vehicleName}</td>
                <td>{new Date(t.departure).toLocaleString()}</td>
                <td><span className="pill pill-ink">{t.status}{t.delayMinutes ? ` +${t.delayMinutes}m` : ''}</span></td>
                <td style={{ display: 'flex', gap: 6 }}>
                  <button className="btn btn-ghost btn-sm" onClick={() => updateStatus(t.id, 'DELAYED', 20)}>+20m delay</button>
                  <button className="btn btn-danger btn-sm" onClick={() => updateStatus(t.id, 'CANCELLED', 0)}>Cancel</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function OperatorBookings() {
  const [bookings, setBookings] = useState(null);
  useEffect(() => { api.get('/api/operator/bookings?size=30').then((r) => setBookings(r.items)); }, []);
  if (!bookings) return <div className="skeleton" style={{ height: 160 }} />;
  return (
    <table className="table">
      <thead><tr><th>Reference</th><th>Route</th><th>Status</th><th>Amount</th></tr></thead>
      <tbody>
        {bookings.map((b) => (
          <tr key={b.id}>
            <td>{b.reference}</td><td>{b.origin.name} &rarr; {b.destination.name}</td>
            <td><span className="pill pill-ink">{b.status}</span></td><td>{formatMoney(b.totalAmount, b.currency)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
