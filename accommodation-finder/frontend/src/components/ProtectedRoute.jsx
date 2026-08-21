import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Loader from './Loader';

/**
 * Guards a route. `roles` restricts it further, e.g. roles={['LANDLORD','ADMIN']}.
 * While the session is being restored we show a loader rather than bouncing
 * the user to /login on every refresh.
 */
export default function ProtectedRoute({ children, roles }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <Loader label="Checking your session..." />;
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (roles && roles.length && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
