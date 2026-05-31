import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ServerSelectPage from '../pages/ServerSelectPage';

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isLoggedIn, ready } = useAuth();

  if (!isLoggedIn) return <Navigate to="/login" replace />;
  if (!ready) return <ServerSelectPage />;

  return <>{children}</>;
}
