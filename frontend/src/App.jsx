import { Suspense, lazy, useEffect } from 'react';
import { Route, Routes, useLocation } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedRoute from './components/ProtectedRoute';
import ErrorBoundary from './components/ErrorBoundary';
import Loader from './components/Loader';
import Home from './pages/Home';

/**
 * Everything except the landing page is code split.
 *
 * The map, the charts and the admin console together are most of the bundle,
 * and a visitor who lands on search and books a room never touches any of
 * them. Splitting here takes the first paint down to the shell plus Home.
 */
const RoomDetail = lazy(() => import('./pages/RoomDetail'));
const Login = lazy(() => import('./pages/Login'));
const Register = lazy(() => import('./pages/Register'));
const Favorites = lazy(() => import('./pages/Favorites'));
const MyBookings = lazy(() => import('./pages/MyBookings'));
const Messages = lazy(() => import('./pages/Messages'));
const Profile = lazy(() => import('./pages/Profile'));
const LandlordDashboard = lazy(() => import('./pages/LandlordDashboard'));
const RoomEditor = lazy(() => import('./pages/RoomEditor'));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));
const ArrivalWizard = lazy(() => import('./pages/ArrivalWizard'));
const Checklist = lazy(() => import('./pages/Checklist'));
const Guides = lazy(() => import('./pages/Guides'));
const GuideDetail = lazy(() => import('./pages/GuideDetail'));
const AidCalculator = lazy(() => import('./pages/AidCalculator'));
const NotFound = lazy(() => import('./pages/NotFound'));

/** A route change should start you at the top of the new page, not halfway down the old one. */
function ScrollToTop({ pathname }) {
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }, [pathname]);
  return null;
}

export default function App() {
  const location = useLocation();

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>

      <Navbar />
      <ScrollToTop pathname={location.pathname} />

      <div className="app-shell__content" id="main-content">
        <ErrorBoundary resetKey={location.pathname}>
          <Suspense fallback={<Loader label="Loading..." />}>
            {/* mode="wait" lets the outgoing page finish leaving before the next
                one arrives, so the two never overlap and shift the layout. */}
            <AnimatePresence mode="wait" initial={false}>
              <Routes location={location} key={location.pathname}>
                {/* public */}
                <Route path="/" element={<Home />} />
                <Route path="/rooms/:id" element={<RoomDetail />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/guides" element={<Guides />} />
                <Route path="/guides/:slug" element={<GuideDetail />} />
                <Route path="/cost" element={<AidCalculator />} />

                {/* signed in */}
                <Route
                  path="/start"
                  element={
                    <ProtectedRoute>
                      <ArrivalWizard />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/checklist"
                  element={
                    <ProtectedRoute>
                      <Checklist />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/favorites"
                  element={
                    <ProtectedRoute>
                      <Favorites />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/bookings"
                  element={
                    <ProtectedRoute>
                      <MyBookings />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/messages"
                  element={
                    <ProtectedRoute>
                      <Messages />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/profile"
                  element={
                    <ProtectedRoute>
                      <Profile />
                    </ProtectedRoute>
                  }
                />

                {/* landlord */}
                <Route
                  path="/dashboard"
                  element={
                    <ProtectedRoute roles={['LANDLORD', 'ADMIN']}>
                      <LandlordDashboard />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard/rooms/new"
                  element={
                    <ProtectedRoute roles={['LANDLORD', 'ADMIN']}>
                      <RoomEditor />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard/rooms/:id/edit"
                  element={
                    <ProtectedRoute roles={['LANDLORD', 'ADMIN']}>
                      <RoomEditor />
                    </ProtectedRoute>
                  }
                />

                {/* admin */}
                <Route
                  path="/admin"
                  element={
                    <ProtectedRoute roles={['ADMIN']}>
                      <AdminDashboard />
                    </ProtectedRoute>
                  }
                />

                <Route path="*" element={<NotFound />} />
              </Routes>
            </AnimatePresence>
          </Suspense>
        </ErrorBoundary>
      </div>

      <Footer />
    </div>
  );
}
