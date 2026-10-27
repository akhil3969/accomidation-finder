import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { DURATION, EASE } from '../motion/tokens';
import { initials } from '../utils/format';

const panelVariants = {
  initial: { opacity: 0, y: -6, scale: 0.98 },
  animate: { opacity: 1, y: 0, scale: 1, transition: { duration: DURATION.fast, ease: EASE } },
  exit: { opacity: 0, y: -6, scale: 0.98, transition: { duration: DURATION.fast, ease: EASE } },
};

/**
 * Everything personal behind one button, so the bar itself stays at three or four
 * words. Keyboard: Enter/Space opens it, Escape closes it and returns focus.
 */
export default function AccountMenu({ user, links, onSignOut }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const buttonRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const onPointer = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false);
    };
    const onKey = (event) => {
      if (event.key === 'Escape') {
        setOpen(false);
        buttonRef.current?.focus();
      }
    };
    document.addEventListener('mousedown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  return (
    <div className="menu" ref={rootRef}>
      <button
        ref={buttonRef}
        type="button"
        className="menu__button"
        aria-haspopup="true"
        aria-expanded={open}
        aria-label={`Account menu for ${user?.name}`}
        onClick={() => setOpen((value) => !value)}
      >
        <span className="avatar avatar--sm" aria-hidden="true">
          {initials(user?.name)}
        </span>
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            className="menu__panel"
            variants={panelVariants}
            initial="initial"
            animate="animate"
            exit="exit"
          >
            <div className="menu__who">
              <strong>{user?.name}</strong>
              <span>{user?.email}</span>
            </div>
            <nav aria-label="Your account">
              <ul className="menu__list">
                {links.map((link) => (
                  <li key={link.to}>
                    <Link to={link.to} className="menu__item" onClick={() => setOpen(false)}>
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
            <button
              type="button"
              className="menu__item menu__item--quiet"
              onClick={() => {
                setOpen(false);
                onSignOut();
              }}
            >
              Sign out
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
