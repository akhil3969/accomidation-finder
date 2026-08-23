import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { useRealtime } from '../context/RealtimeContext';
import { useInbox } from '../context/InboxContext';
import useMediaQuery from '../hooks/useMediaQuery';
import { DURATION, EASE, fadeVariants } from '../motion/tokens';
import { initials } from '../utils/format';

const PUBLIC_LINKS = [
  { to: '/', label: 'Search', end: true },
  { to: '/cost', label: 'Real cost' },
  { to: '/guides', label: 'Guides' },
];

const MEMBER_LINKS = [
  { to: '/checklist', label: 'My documents' },
  { to: '/favorites', label: 'Saved' },
  { to: '/bookings', label: 'My bookings' },
  { to: '/messages', label: 'Messages', badge: true },
];

const sheetVariants = {
  initial: { opacity: 0, y: -12 },
  animate: { opacity: 1, y: 0, transition: { duration: DURATION.base, ease: EASE } },
  exit: { opacity: 0, y: -12, transition: { duration: DURATION.fast, ease: EASE } },
};

export default function Navbar() {
  const { user, isAuthenticated, isLandlord, isAdmin, signOut } = useAuth();
  const { connected } = useRealtime();
  const { unread } = useInbox();
  const navigate = useNavigate();
  const location = useLocation();
  const isCompact = useMediaQuery('(max-width: 860px)');
  const [open, setOpen] = useState(false);

  // Navigating with the sheet open used to leave it hanging over the new page.
  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  // Escape is the expected way out of anything that overlays the page.
  useEffect(() => {
    if (!open) return undefined;
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open]);

  function handleSignOut() {
    signOut();
    setOpen(false);
    navigate('/');
  }

  const links = [
    ...PUBLIC_LINKS,
    ...(isAuthenticated ? MEMBER_LINKS : []),
    ...(isLandlord ? [{ to: '/dashboard', label: 'Dashboard' }] : []),
    ...(isAdmin ? [{ to: '/admin', label: 'Admin' }] : []),
  ];

  const navContent = (
    <>
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          end={link.end}
          className={({ isActive }) => `nav-link ${isActive ? 'is-active' : ''}`}
        >
          {link.label}
          {link.badge && unread > 0 && (
            <motion.span
              /* Keyed on the count so every increment replays the pop rather
                 than silently swapping the digit. */
              key={unread}
              className="nav-badge"
              initial={{ scale: 0.4, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ type: 'spring', stiffness: 520, damping: 20 }}
              aria-label={`${unread} unread`}
            >
              {unread > 9 ? '9+' : unread}
            </motion.span>
          )}
        </NavLink>
      ))}

      <span className="nav-divider" aria-hidden="true" />

      {/* On desktop this is a bare dot with a tooltip; in the mobile sheet
          there is room for the words, and a lone orange dot on its own line
          explains nothing. */}
      <span
        className="nav-status"
        role="status"
        title={connected ? 'Live updates active' : 'Reconnecting to live updates'}
      >
        <span className={`status-dot ${connected ? 'is-live' : 'is-down'}`} aria-hidden="true" />
        <span className="nav-status__label">
          {connected ? 'Live updates active' : 'Reconnecting…'}
        </span>
      </span>

      {isAuthenticated ? (
        <>
          <Link to="/profile" className="nav-link" title={user?.name} aria-label="Your profile">
            <span className="avatar avatar--sm">{initials(user?.name)}</span>
          </Link>
          <button type="button" className="btn btn--sm btn--ghost" onClick={handleSignOut}>
            Sign out
          </button>
        </>
      ) : (
        <>
          <NavLink to="/login" className="nav-link">
            Sign in
          </NavLink>
          <Link to="/register" className="btn btn--sm btn--accent">
            Get started
          </Link>
        </>
      )}
    </>
  );

  return (
    <header className="navbar">
      <div className="navbar__inner">
        <Link to="/" className="brand">
          <span className="brand__mark" aria-hidden="true">
            &#127968;
          </span>
          AccomFinder
        </Link>

        <button
          type="button"
          className="nav-toggle"
          aria-expanded={open}
          aria-controls="primary-navigation"
          aria-label={open ? 'Close navigation' : 'Open navigation'}
          onClick={() => setOpen((value) => !value)}
        >
          {open ? '✕' : '☰'}
        </button>

        {/* On a narrow screen the links are a sheet that mounts, so it can
            animate in and out; on desktop they are simply always there. */}
        {isCompact ? (
          <AnimatePresence>
            {open && (
              <>
                <motion.button
                  key="scrim"
                  type="button"
                  className="nav-scrim"
                  aria-label="Close navigation"
                  variants={fadeVariants}
                  initial="initial"
                  animate="animate"
                  exit="exit"
                  onClick={() => setOpen(false)}
                />
                <motion.nav
                  key="sheet"
                  id="primary-navigation"
                  className="nav-links"
                  variants={sheetVariants}
                  initial="initial"
                  animate="animate"
                  exit="exit"
                >
                  {navContent}
                </motion.nav>
              </>
            )}
          </AnimatePresence>
        ) : (
          <nav id="primary-navigation" className="nav-links">
            {navContent}
          </nav>
        )}
      </div>
    </header>
  );
}
