import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const navLinkCls = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
  }`;

export default function Navbar() {
  const { isAuthenticated, isAdmin, logout, roles } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const handleLogout = async () => {
    setOpen(false);
    await logout();
    navigate('/login');
  };

  const close = () => setOpen(false);

  return (
    <header className="sticky top-0 z-40 border-b border-gray-200 bg-white/80 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
        <Link to="/" onClick={close} className="flex items-center gap-2 text-lg font-bold text-gray-900">
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-600 text-sm text-white">S</span>
          Shop
        </Link>

        {/* Desktop nav */}
        <nav className="hidden items-center gap-1 md:flex">
          <NavLink to="/" className={navLinkCls} end>
            Products
          </NavLink>
          {isAuthenticated && (
            <>
              <NavLink to="/cart" className={navLinkCls}>
                Cart
              </NavLink>
              <NavLink to="/orders" className={navLinkCls}>
                Orders
              </NavLink>
              {isAdmin && (
                <NavLink to="/admin/users" className={navLinkCls}>
                  Admin
                </NavLink>
              )}
              <button
                onClick={handleLogout}
                className="ml-1 rounded-md px-3 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-100 hover:text-gray-900"
              >
                Logout
              </button>
            </>
          )}
          {!isAuthenticated && (
            <>
              <NavLink to="/login" className={navLinkCls}>
                Login
              </NavLink>
              <NavLink to="/register" className={navLinkCls}>
                Register
              </NavLink>
            </>
          )}
        </nav>

        {/* Mobile toggle */}
        <button
          onClick={() => setOpen((v) => !v)}
          className="rounded-md p-2 text-gray-700 hover:bg-gray-100 md:hidden"
          aria-label="Toggle menu"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            {open ? (
              <path strokeLinecap="round" d="M6 6l12 12M18 6L6 18" />
            ) : (
              <path strokeLinecap="round" d="M4 7h16M4 12h16M4 17h16" />
            )}
          </svg>
        </button>
      </div>

      {/* Mobile menu */}
      {open && (
        <nav className="border-t border-gray-200 bg-white px-4 py-2 md:hidden">
          <NavLink to="/" onClick={close} className={navLinkCls} end>
            Products
          </NavLink>
          {isAuthenticated && (
            <>
              <NavLink to="/cart" onClick={close} className={navLinkCls}>
                Cart
              </NavLink>
              <NavLink to="/orders" onClick={close} className={navLinkCls}>
                Orders
              </NavLink>
              {isAdmin && (
                <NavLink to="/admin/users" onClick={close} className={navLinkCls}>
                  Admin
                </NavLink>
              )}
              <button
                onClick={handleLogout}
                className="block w-full rounded-md px-3 py-2 text-left text-sm font-medium text-gray-600 hover:bg-gray-100"
              >
                Logout
              </button>
            </>
          )}
          {!isAuthenticated && (
            <>
              <NavLink to="/login" onClick={close} className={navLinkCls}>
                Login
              </NavLink>
              <NavLink to="/register" onClick={close} className={navLinkCls}>
                Register
              </NavLink>
            </>
          )}
        </nav>
      )}
    </header>
  );
}
