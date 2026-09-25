import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { formatMoney } from '../utils/format';

export default function AdminPage() {
  const [tab, setTab] = useState('overview');
  return (
    <div className="container dashboard-page">
      <h1>Admin</h1>
      <div className="dashboard-layout">
        <nav className="dashboard-tabs">
          {['overview', 'users', 'stations', 'operators', 'bookings', 'broadcast'].map((t) => (
            <button key={t} className={`dashboard-tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
              {t[0].toUpperCase() + t.slice(1)}
            </button>
          ))}
        </nav>
        <div className="dashboard-content">
          {tab === 'overview' && <Overview />}
          {tab === 'users' && <UsersPanel />}
          {tab === 'stations' && <StationsPanel />}
          {tab === 'operators' && <OperatorsPanel />}
          {tab === 'bookings' && <AdminBookings />}
          {tab === 'broadcast' && <BroadcastPanel />}
        </div>
      </div>
    </div>
  );
}

function Overview() {
  const [d, setD] = useState(null);
  useEffect(() => { api.get('/api/admin/dashboard').then(setD); }, []);
  if (!d) return <div className="skeleton" style={{ height: 200 }} />;
  return (
    <div>
      <div className="stat-grid">
        <Stat label="Total users" value={d.totalUsers} />
        <Stat label="Passengers" value={d.totalPassengers} />
        <Stat label="Operators" value={d.totalOperators} />
        <Stat label="Total bookings" value={d.totalBookings} />
        <Stat label="Confirmed bookings" value={d.confirmedBookings} />
        <Stat label="Active journeys today" value={d.activeJourneysToday} />
        <Stat label="Revenue" value={formatMoney(d.totalRevenue)} />
        <Stat label="Refunded" value={formatMoney(d.totalRefunded)} />
      </div>
      <div className="card" style={{ padding: 20 }}>
        <h3>Transport distribution (today)</h3>
        {Object.entries(d.transportDistribution).map(([mode, count]) => (
          <div key={mode} className="list-row"><span>{mode}</span><span>{count}</span></div>
        ))}
      </div>
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

function UsersPanel() {
  const [q, setQ] = useState('');
  const [users, setUsers] = useState(null);

  function load() {
    api.get(`/api/admin/users?q=${encodeURIComponent(q)}&size=25`).then((r) => setUsers(r.items));
  }
  useEffect(load, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function setEnabled(id, enabled) {
    await api.put(`/api/admin/users/${id}/status`, { enabled });
    load();
  }

  return (
    <div>
      <form onSubmit={(e) => { e.preventDefault(); load(); }} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <input placeholder="Search by name or email" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="btn btn-dark btn-sm" type="submit">Search</button>
      </form>
      {!users ? <div className="skeleton" style={{ height: 160 }} /> : (
        <table className="table">
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td><td>{u.email}</td><td>{u.role}</td>
                <td><span className={`pill ${u.enabled ? 'pill-teal' : 'pill-red'}`}>{u.enabled ? 'Active' : 'Disabled'}</span></td>
                <td><button className="btn btn-ghost btn-sm" onClick={() => setEnabled(u.id, !u.enabled)}>{u.enabled ? 'Disable' : 'Enable'}</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function StationsPanel() {
  const [stations, setStations] = useState(null);
  const [form, setForm] = useState({ code: '', name: '', city: '', state: '', kind: 'HUB', latitude: '', longitude: '', transferMinutes: 30 });
  const [error, setError] = useState(null);

  function load() { api.get('/api/admin/stations').then(setStations); }
  useEffect(load, []);

  async function submit(e) {
    e.preventDefault();
    setError(null);
    try {
      await api.post('/api/admin/stations', { ...form, latitude: Number(form.latitude), longitude: Number(form.longitude), transferMinutes: Number(form.transferMinutes) });
      setForm({ code: '', name: '', city: '', state: '', kind: 'HUB', latitude: '', longitude: '', transferMinutes: 30 });
      load();
    } catch (err) { setError(err.message); }
  }

  return (
    <div>
      <form className="card" style={{ padding: 18, marginBottom: 20 }} onSubmit={submit}>
        <h3>Add station</h3>
        {error && <p className="banner banner-error">{error}</p>}
        <div className="search-form-row">
          <input placeholder="Code" required value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <input placeholder="Name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input placeholder="City" required value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
          <input placeholder="State" required value={form.state} onChange={(e) => setForm({ ...form, state: e.target.value })} />
        </div>
        <div className="search-form-row" style={{ marginTop: 10 }}>
          <select value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value })}>
            <option value="RAIL">Rail</option><option value="BUS">Bus</option><option value="HUB">Hub</option>
          </select>
          <input placeholder="Latitude" required value={form.latitude} onChange={(e) => setForm({ ...form, latitude: e.target.value })} />
          <input placeholder="Longitude" required value={form.longitude} onChange={(e) => setForm({ ...form, longitude: e.target.value })} />
          <input placeholder="Transfer minutes" type="number" value={form.transferMinutes} onChange={(e) => setForm({ ...form, transferMinutes: e.target.value })} />
        </div>
        <button className="btn btn-primary btn-sm" style={{ marginTop: 12 }} type="submit">Add station</button>
      </form>
      {!stations ? <div className="skeleton" style={{ height: 160 }} /> : (
        <table className="table">
          <thead><tr><th>Code</th><th>Name</th><th>City</th><th>Kind</th></tr></thead>
          <tbody>{stations.map((s) => <tr key={s.id}><td>{s.code}</td><td>{s.name}</td><td>{s.city}</td><td>{s.kind}</td></tr>)}</tbody>
        </table>
      )}
    </div>
  );
}

function OperatorsPanel() {
  const [operators, setOperators] = useState(null);
  const [form, setForm] = useState({ code: '', name: '', mode: 'BUS', contactEmail: '' });
  const [error, setError] = useState(null);

  function load() { api.get('/api/admin/operators').then(setOperators); }
  useEffect(load, []);

  async function submit(e) {
    e.preventDefault();
    setError(null);
    try {
      await api.post('/api/admin/operators', form);
      setForm({ code: '', name: '', mode: 'BUS', contactEmail: '' });
      load();
    } catch (err) { setError(err.message); }
  }

  return (
    <div>
      <form className="card" style={{ padding: 18, marginBottom: 20 }} onSubmit={submit}>
        <h3>Add operator</h3>
        {error && <p className="banner banner-error">{error}</p>}
        <div className="search-form-row">
          <input placeholder="Code" required value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <input placeholder="Name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <select value={form.mode} onChange={(e) => setForm({ ...form, mode: e.target.value })}>
            <option value="BUS">Bus</option><option value="TRAIN">Train</option>
          </select>
          <input placeholder="Contact email" value={form.contactEmail} onChange={(e) => setForm({ ...form, contactEmail: e.target.value })} />
        </div>
        <button className="btn btn-primary btn-sm" style={{ marginTop: 12 }} type="submit">Add operator</button>
      </form>
      {!operators ? <div className="skeleton" style={{ height: 160 }} /> : (
        <table className="table">
          <thead><tr><th>Code</th><th>Name</th><th>Mode</th><th>Status</th></tr></thead>
          <tbody>{operators.map((o) => <tr key={o.id}><td>{o.code}</td><td>{o.name}</td><td>{o.mode}</td><td>{o.active ? 'Active' : 'Inactive'}</td></tr>)}</tbody>
        </table>
      )}
    </div>
  );
}

function AdminBookings() {
  const [bookings, setBookings] = useState(null);
  useEffect(() => { api.get('/api/admin/bookings?size=30').then((r) => setBookings(r.items)); }, []);
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

function BroadcastPanel() {
  const [form, setForm] = useState({ title: '', message: '' });
  const [result, setResult] = useState(null);

  async function submit(e) {
    e.preventDefault();
    const res = await api.post('/api/admin/notifications/broadcast', form);
    setResult(`Sent to ${res.recipients} passengers.`);
    setForm({ title: '', message: '' });
  }

  return (
    <form className="card" style={{ padding: 20, maxWidth: 480 }} onSubmit={submit}>
      <h3>Broadcast a service alert</h3>
      {result && <p className="banner banner-info">{result}</p>}
      <div className="field"><label>Title</label><input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
      <div className="field" style={{ marginTop: 12 }}><label>Message</label><textarea required rows={4} value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} /></div>
      <button className="btn btn-primary btn-sm" style={{ marginTop: 12 }} type="submit">Send to all passengers</button>
    </form>
  );
}
