import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [working, setWorking] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setWorking(true);
    setError(null);
    try {
      await login(email, password);
      navigate(location.state?.from || '/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setWorking(false);
    }
  }

  return (
    <div className="auth-page container">
      <form className="card auth-card" onSubmit={submit}>
        <h2>Welcome back</h2>
        <p>Sign in to manage your bookings and continue planning journeys.</p>
        {error && <p className="banner banner-error">{error}</p>}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <button className="btn btn-primary btn-block" disabled={working} type="submit">
          {working ? 'Signing in\u2026' : 'Sign in'}
        </button>
        <p className="auth-switch">
          New to Kinvia? <Link to="/register">Create an account</Link>
        </p>
        <p className="journey-sub" style={{ marginTop: 12 }}>
          Try it: <code>traveller@kinvia.dev</code> / <code>Kinvia@Dev1</code>
        </p>
      </form>
    </div>
  );
}
