import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { useAuth } from '../../context/AuthContext';

export default function ProfilePanel() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ fullName: user.fullName, phone: user.phone || '' });
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' });
  const [passengers, setPassengers] = useState([]);
  const [newPassenger, setNewPassenger] = useState({ fullName: '', age: '', gender: 'MALE', mobile: '' });
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  function loadPassengers() {
    api.get('/api/users/me/passengers').then(setPassengers);
  }
  useEffect(loadPassengers, []);

  async function saveProfile(e) {
    e.preventDefault();
    setError(null);
    try {
      const updated = await api.put('/api/users/me', form);
      setUser(updated);
      setMessage('Profile updated.');
    } catch (err) {
      setError(err.message);
    }
  }

  async function changePassword(e) {
    e.preventDefault();
    setError(null);
    try {
      await api.post('/api/users/me/password', passwords);
      setPasswords({ currentPassword: '', newPassword: '' });
      setMessage('Password changed.');
    } catch (err) {
      setError(err.message);
    }
  }

  async function addPassenger(e) {
    e.preventDefault();
    try {
      await api.post('/api/users/me/passengers', { ...newPassenger, age: Number(newPassenger.age) });
      setNewPassenger({ fullName: '', age: '', gender: 'MALE', mobile: '' });
      loadPassengers();
    } catch (err) {
      setError(err.message);
    }
  }

  async function removePassenger(id) {
    await api.delete(`/api/users/me/passengers/${id}`);
    loadPassengers();
  }

  return (
    <div className="panel-stack">
      {message && <p className="banner banner-info">{message}</p>}
      {error && <p className="banner banner-error">{error}</p>}

      <form className="card" style={{ padding: 20 }} onSubmit={saveProfile}>
        <h3>Profile</h3>
        <div className="search-form-row">
          <div className="field">
            <label>Full name</label>
            <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          </div>
          <div className="field">
            <label>Phone</label>
            <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </div>
        </div>
        <button className="btn btn-primary btn-sm" style={{ marginTop: 12 }} type="submit">Save</button>
      </form>

      <form className="card" style={{ padding: 20 }} onSubmit={changePassword}>
        <h3>Change password</h3>
        <div className="search-form-row">
          <div className="field">
            <label>Current password</label>
            <input type="password" value={passwords.currentPassword} onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })} />
          </div>
          <div className="field">
            <label>New password</label>
            <input type="password" minLength={8} value={passwords.newPassword} onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} />
          </div>
        </div>
        <button className="btn btn-primary btn-sm" style={{ marginTop: 12 }} type="submit">Update password</button>
      </form>

      <div className="card" style={{ padding: 20 }}>
        <h3>Saved travellers</h3>
        {passengers.map((p) => (
          <div key={p.id} className="list-row">
            <span>{p.fullName} &middot; {p.age}, {p.gender.toLowerCase()}</span>
            <button className="btn btn-ghost btn-sm" onClick={() => removePassenger(p.id)}>Remove</button>
          </div>
        ))}
        <form className="search-form-row" style={{ marginTop: 12 }} onSubmit={addPassenger}>
          <input placeholder="Full name" required value={newPassenger.fullName} onChange={(e) => setNewPassenger({ ...newPassenger, fullName: e.target.value })} />
          <input placeholder="Age" type="number" required value={newPassenger.age} onChange={(e) => setNewPassenger({ ...newPassenger, age: e.target.value })} />
          <select value={newPassenger.gender} onChange={(e) => setNewPassenger({ ...newPassenger, gender: e.target.value })}>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>
          <button className="btn btn-dark btn-sm" type="submit">Add traveller</button>
        </form>
      </div>
    </div>
  );
}
