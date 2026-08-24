import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function PrivateRoute({
  children,
  requireRoles,
}: {
  children: ReactNode;
  requireRoles?: string[];
}) {
  const { isAuthenticated, roles } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (requireRoles && !requireRoles.some((r) => roles.includes(r))) {
    return (
      <div className="p-6">
        <h2>403 — Access denied</h2>
        <p>You don't have permission to view this page.</p>
      </div>
    );
  }
  return <>{children}</>;
}
