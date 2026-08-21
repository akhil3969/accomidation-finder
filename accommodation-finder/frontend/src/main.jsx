import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { MotionConfig } from 'framer-motion';
import App from './App';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { RealtimeProvider } from './context/RealtimeContext';
import { InboxProvider } from './context/InboxContext';
import { ConfirmProvider } from './context/ConfirmContext';
import 'leaflet/dist/leaflet.css';
import './styles/index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {/* reducedMotion="user" makes every framer-motion animation in the app
        collapse to an instant state change when the operating system asks for
        it, which is the half the CSS media query cannot reach. */}
    <MotionConfig reducedMotion="user">
      {/* Opting in early silences the v7 upgrade warnings React Router logs on
          every page, and startTransition keeps the current page on screen
          while a lazily loaded route is fetched. */}
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <ToastProvider>
          <AuthProvider>
            <RealtimeProvider>
              <InboxProvider>
                <ConfirmProvider>
                  <App />
                </ConfirmProvider>
              </InboxProvider>
            </RealtimeProvider>
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>
    </MotionConfig>
  </React.StrictMode>,
);
