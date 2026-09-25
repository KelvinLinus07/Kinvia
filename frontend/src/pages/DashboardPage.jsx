import { useState } from 'react';
import BookingsPanel from '../components/dashboard/BookingsPanel';
import ProfilePanel from '../components/dashboard/ProfilePanel';
import PreferencesPanel from '../components/dashboard/PreferencesPanel';
import SavedJourneysPanel from '../components/dashboard/SavedJourneysPanel';
import NotificationsPanel from '../components/dashboard/NotificationsPanel';

const TABS = [
  { key: 'upcoming', label: 'Upcoming trips' },
  { key: 'previous', label: 'Previous trips' },
  { key: 'saved', label: 'Saved journeys' },
  { key: 'notifications', label: 'Notifications' },
  { key: 'preferences', label: 'Travel preferences' },
  { key: 'profile', label: 'Profile' },
];

export default function DashboardPage() {
  const [tab, setTab] = useState('upcoming');

  return (
    <div className="container dashboard-page">
      <h1>My trips</h1>
      <div className="dashboard-layout">
        <nav className="dashboard-tabs">
          {TABS.map((t) => (
            <button key={t.key} className={`dashboard-tab ${tab === t.key ? 'active' : ''}`} onClick={() => setTab(t.key)}>
              {t.label}
            </button>
          ))}
        </nav>
        <div className="dashboard-content">
          {tab === 'upcoming' && <BookingsPanel scope="UPCOMING" />}
          {tab === 'previous' && <BookingsPanel scope="PREVIOUS" />}
          {tab === 'saved' && <SavedJourneysPanel />}
          {tab === 'notifications' && <NotificationsPanel />}
          {tab === 'preferences' && <PreferencesPanel />}
          {tab === 'profile' && <ProfilePanel />}
        </div>
      </div>
    </div>
  );
}
