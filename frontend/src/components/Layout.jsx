import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="app-shell">
      <header className="top-nav">
        <div className="container top-nav-inner">
          <Link to="/" className="brand">
            Kinvia
          </Link>
          <nav className="top-nav-links">
            <NavLink to="/search" className="nav-link">Search</NavLink>
            {user && <NavLink to="/dashboard" className="nav-link">My trips</NavLink>}
            {user?.role === 'ADMIN' && <NavLink to="/admin" className="nav-link">Admin</NavLink>}
            {user?.role === 'OPERATOR' && <NavLink to="/operator" className="nav-link">Operator</NavLink>}
          </nav>
          <div className="top-nav-auth">
            {user ? (
              <>
                <span className="top-nav-user">{user.fullName.split(' ')[0]}</span>
                <button
                  className="btn btn-ghost btn-sm"
                  onClick={() => {
                    logout();
                    navigate('/');
                  }}
                >
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost btn-sm">Sign in</Link>
                <Link to="/register" className="btn btn-primary btn-sm">Create account</Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="container">
          <span>Kinvia &mdash; journeys, joined up.</span>
          <span className="site-footer-note">Development build. Payments are simulated.</span>
        </div>
      </footer>
    </div>
  );
}
