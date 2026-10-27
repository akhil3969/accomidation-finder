import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, m } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { useInbox } from '../context/InboxContext';
import useMediaQuery from '../hooks/useMediaQuery';
import { DURATION, EASE, fadeVariants } from '../motion/tokens';
import NotificationBell from './NotificationBell';
import AccountMenu from './AccountMenu';

/** The three things anyone comes here for. Everything else is one click deeper. */
const MAIN_LINKS = [
  { to: '/', label: 'Find a room', end: true },
  { to: '/cost', label: 'Real cost' },
  { to: '/guides', label: 'Guides' },
];

function accountLinks({ isLandlord, isAdmin }) {
  return [
    { to: '/bookings', label: 'My bookings' },
    { to: '/favorites', label: 'Saved rooms' },
    { to: '/checklist', label: 'My documents' },
    ...(isLandlord ? [{ to: '/dashboard', label: 'Landlord dashboard' }] : []),
    ...(isAdmin ? [{ to: '/admin', label: 'Admin' }] : []),
    { to: '/profile', label: 'Profile and settings' },
  ];
}

const sheetVariants = {
  initial: { opacity: 0, y: -12 },
  animate: { opacity: 1, y: 0, transition: { duration: DURATION.base, ease: EASE } },
  exit: { opacity: 0, y: -12, transition: { duration: DURATION.fast, ease: EASE } },
};

function UnreadBadge({ count }) {
  if (!count) return null;
  return (
    <m.span
      /* Keyed on the count so every increment replays the pop. */
      key={count}
      className="nav-badge"
      initial={{ scale: 0.4, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      transition={{ type: 'spring', stiffness: 520, damping: 20 }}
    >
      {count > 9 ? '9+' : count}
    </m.span>
  );
}

export default function Navbar() {
  const { user, isAuthenticated, isLandlord, isAdmin, signOut } = useAuth();
  const { unread } = useInbox();
  const navigate = useNavigate();
  const location = useLocation();
  // Keep in step with the 900px nav media query in index.css.
  const isCompact = useMediaQuery('(max-width: 900px)');
  const [open, setOpen] = useState(false);

  // Navigating with the sheet open used to leave it hanging over the new page.
  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

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

  const personal = accountLinks({ isLandlord, isAdmin });
  const linkClass = ({ isActive }) => `nav-link ${isActive ? 'is-active' : ''}`;

  const messagesLink = (
    <NavLink to="/messages" className={linkClass} aria-label={unread ? `Messages, ${unread} unread` : undefined}>
      Messages
      <UnreadBadge count={unread} />
    </NavLink>
  );

  // Phones and small tablets: one plain list, biggest tap targets, no nesting.
  const sheet = (
    <>
      {MAIN_LINKS.map((link) => (
        <NavLink key={link.to} to={link.to} end={link.end} className={linkClass}>
          {link.label}
        </NavLink>
      ))}
      {isAuthenticated ? (
        <>
          {messagesLink}
          <span className="nav-divider" aria-hidden="true" />
          {personal.map((link) => (
            <NavLink key={link.to} to={link.to} className={linkClass}>
              {link.label}
            </NavLink>
          ))}
          <button type="button" className="btn btn--subtle btn--block" onClick={handleSignOut}>
            Sign out
          </button>
        </>
      ) : (
        <>
          <span className="nav-divider" aria-hidden="true" />
          <Link to="/register" className="btn btn--primary btn--block">
            Create an account
          </Link>
          <Link to="/login" className="btn btn--subtle btn--block">
            Sign in
          </Link>
        </>
      )}
    </>
  );

  return (
    <header className="navbar">
      <div className="navbar__inner">
        <Link to="/" className="brand" aria-label="AccomFinder, home">
          <span className="brand__mark" aria-hidden="true">
            &#8962;
          </span>
          AccomFinder
        </Link>

        {isCompact ? (
          <>
            <div className="nav-compact-actions">
              {isAuthenticated && <NotificationBell />}
              <button
                type="button"
                className="nav-toggle"
                aria-expanded={open}
                aria-controls="primary-navigation"
                aria-label={open ? 'Close menu' : 'Open menu'}
                onClick={() => setOpen((value) => !value)}
              >
                {open ? 'Close' : 'Menu'}
              </button>
            </div>
            <AnimatePresence>
              {open && (
                <>
                  <m.button
                    key="scrim"
                    type="button"
                    className="nav-scrim"
                    aria-label="Close menu"
                    tabIndex={-1}
                    variants={fadeVariants}
                    initial="initial"
                    animate="animate"
                    exit="exit"
                    onClick={() => setOpen(false)}
                  />
                  <m.nav
                    key="sheet"
                    id="primary-navigation"
                    aria-label="Main"
                    className="nav-sheet"
                    variants={sheetVariants}
                    initial="initial"
                    animate="animate"
                    exit="exit"
                  >
                    {sheet}
                  </m.nav>
                </>
              )}
            </AnimatePresence>
          </>
        ) : (
          <>
            <nav id="primary-navigation" aria-label="Main" className="nav-main">
              {MAIN_LINKS.map((link) => (
                <NavLink key={link.to} to={link.to} end={link.end} className={linkClass}>
                  {link.label}
                </NavLink>
              ))}
            </nav>
            <div className="nav-actions">
              {isAuthenticated ? (
                <>
                  {messagesLink}
                  <NotificationBell />
                  <AccountMenu user={user} links={personal} onSignOut={handleSignOut} />
                </>
              ) : (
                <>
                  <NavLink to="/login" className={linkClass}>
                    Sign in
                  </NavLink>
                  <Link to="/register" className="btn btn--primary btn--sm">
                    Create an account
                  </Link>
                </>
              )}
            </div>
          </>
        )}
      </div>
    </header>
  );
}
