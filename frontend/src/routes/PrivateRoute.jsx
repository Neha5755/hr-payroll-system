import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function PrivateRoute({ requireHr = false }) {
  const { user, isHrOrAdmin } = useAuth();

  if (!user) return <Navigate to="/login" replace />;
  if (requireHr && !isHrOrAdmin) return <Navigate to="/dashboard" replace />;

  return <Outlet />;
}
