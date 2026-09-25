import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, qs } from '../../api/client';
import RouteLine from '../RouteLine';
import { formatDateTime, formatMoney } from '../../utils/format';

const STATUS_PILL = {
  CONFIRMED: 'pill-teal',
  HELD: 'pill-amber',
  PENDING: 'pill-amber',
  CANCELLED: 'pill-red',
  EXPIRED: 'pill-red',
  COMPLETED: 'pill-ink',
};

export default function BookingsPanel({ scope }) {
  const [bookings, setBookings] = useState(null);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  function load() {
    api
      .get(`/api/bookings${qs({ scope, page: 0, size: 20 })}`)
      .then((res) => setBookings(res.items))
      .catch((err) => setError(err.message));
  }

  useEffect(load, [scope]);

  async function cancel(id) {
    const preview = await api.get(`/api/bookings/${id}/cancellation-preview`);
    if (!preview.cancellable) {
      window.alert(preview.message);
      return;
    }
    if (!window.confirm(`${preview.message} Continue with cancellation?`)) return;
    await api.post(`/api/bookings/${id}/cancel`);
    load();
  }

  if (error) return <p className="banner banner-error">{error}</p>;
  if (!bookings) return <div className="skeleton" style={{ height: 160 }} />;
  if (bookings.length === 0) return <p>No {scope === 'UPCOMING' ? 'upcoming' : 'previous'} trips yet.</p>;

  return (
    <div className="journey-list">
      {bookings.map((b) => (
        <div key={b.id} className="card" style={{ padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
            <div>
              <strong>{b.origin.name} &rarr; {b.destination.name}</strong>
              <div className="journey-sub">{formatDateTime(b.departure)} &middot; Ref {b.reference}</div>
            </div>
            <span className={`pill ${STATUS_PILL[b.status] || 'pill-ink'}`}>{b.status}</span>
          </div>
          <RouteLine segments={b.modes.map((m) => ({ mode: m, board: {}, alight: {} }))} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 }}>
            <span>{formatMoney(b.totalAmount, b.currency)} &middot; {b.passengerCount} traveller(s)</span>
            <div style={{ display: 'flex', gap: 8 }}>
              {b.status === 'CONFIRMED' && (
                <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/tickets/${b.id}`)}>
                  Ticket
                </button>
              )}
              {b.cancellable && (
                <button className="btn btn-danger btn-sm" onClick={() => cancel(b.id)}>
                  Cancel
                </button>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
