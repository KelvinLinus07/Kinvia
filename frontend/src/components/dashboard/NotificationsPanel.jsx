import { useEffect, useState } from 'react';
import { api } from '../../api/client';

export default function NotificationsPanel() {
  const [items, setItems] = useState(null);

  function load() {
    api.get('/api/notifications?page=0&size=30').then((res) => setItems(res.items));
  }
  useEffect(load, []);

  async function markRead(id) {
    await api.patch(`/api/notifications/${id}/read`);
    load();
  }

  if (!items) return <div className="skeleton" style={{ height: 160 }} />;
  if (items.length === 0) return <p>No notifications yet.</p>;

  return (
    <div className="journey-list">
      {items.map((n) => (
        <div key={n.id} className="card" style={{ padding: 16, opacity: n.read ? 0.65 : 1, display: 'flex', justifyContent: 'space-between', gap: 12 }}>
          <div>
            <strong>{n.title}</strong>
            <p style={{ margin: '4px 0 0' }}>{n.message}</p>
            <span className="journey-sub">{new Date(n.createdAt).toLocaleString()}</span>
          </div>
          {!n.read && (
            <button className="btn btn-ghost btn-sm" onClick={() => markRead(n.id)}>
              Mark read
            </button>
          )}
        </div>
      ))}
    </div>
  );
}
