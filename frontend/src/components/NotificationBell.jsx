import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, m } from 'framer-motion';
import { notificationApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import { useSubscription } from '../context/RealtimeContext';
import { DURATION, EASE } from '../motion/tokens';
import { timeAgo } from '../utils/format';

const ICONS = {
  BOOKING_REQUESTED: '📅',
  BOOKING_CONFIRMED: '✅',
  BOOKING_REJECTED: '✖️',
  BOOKING_CANCELLED: '↩️',
  BOOKING_COMPLETED: '⭐',
  NEW_MESSAGE: '💬',
};

const panelVariants = {
  initial: { opacity: 0, y: -6, scale: 0.98 },
  animate: { opacity: 1, y: 0, scale: 1, transition: { duration: DURATION.fast, ease: EASE } },
  exit: { opacity: 0, y: -6, scale: 0.98, transition: { duration: DURATION.fast, ease: EASE } },
};

/**
 * The bell. Its contents come from the server, not from the socket, so a booking
 * request that arrived while the landlord was offline is waiting here when they
 * return. The socket only makes new ones appear without a refresh.
 */
export default function NotificationBell() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState(null);
  const rootRef = useRef(null);

  const refreshCount = useCallback(async () => {
    try {
      const data = await notificationApi.unreadCount();
      setUnread(data.unread ?? 0);
    } catch {
      /* the number simply stays as it was */
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) refreshCount();
  }, [isAuthenticated, refreshCount]);

  useSubscription(isAuthenticated ? '/user/queue/notifications' : null, (notification) => {
    setItems((current) => {
      if (!current) return current;
      // A collapsed chat notification comes back with the same id - replace, don't duplicate.
      return [notification, ...current.filter((n) => n.id !== notification.id)];
    });
    refreshCount();
  });

  async function toggle() {
    const next = !open;
    setOpen(next);
    if (next) {
      try {
        const page = await notificationApi.list({ size: 15 });
        setItems(page.content);
      } catch {
        setItems([]);
      }
    }
  }

  // Click outside or Escape closes it, like every other menu.
  useEffect(() => {
    if (!open) return undefined;
    const onPointer = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false);
    };
    const onKey = (event) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  async function openNotification(notification) {
    setOpen(false);
    if (!notification.read) {
      setItems((current) => current?.map((n) => (n.id === notification.id ? { ...n, read: true } : n)));
      setUnread((count) => Math.max(0, count - 1));
      notificationApi.markRead(notification.id).catch(() => refreshCount());
    }
    if (notification.link) navigate(notification.link);
  }

  async function markAll() {
    setItems((current) => current?.map((n) => ({ ...n, read: true })));
    setUnread(0);
    try {
      await notificationApi.markAllRead();
    } catch {
      refreshCount();
    }
  }

  if (!isAuthenticated) return null;

  return (
    <div className="bell" ref={rootRef}>
      <button
        type="button"
        className="bell__button nav-link"
        aria-haspopup="true"
        aria-expanded={open}
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        onClick={toggle}
      >
        <span aria-hidden="true">🔔</span>
        {unread > 0 && (
          <m.span
            key={unread}
            className="nav-badge"
            initial={{ scale: 0.4, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: 'spring', stiffness: 520, damping: 20 }}
            aria-hidden="true"
          >
            {unread > 9 ? '9+' : unread}
          </m.span>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <m.div
            className="bell__panel"
            role="dialog"
            aria-label="Notifications"
            variants={panelVariants}
            initial="initial"
            animate="animate"
            exit="exit"
          >
            <div className="bell__header">
              <strong>Notifications</strong>
              {unread > 0 && (
                <button type="button" className="btn btn--link btn--sm" onClick={markAll}>
                  Mark all read
                </button>
              )}
            </div>

            {items === null ? (
              <p className="bell__empty muted small">Loading…</p>
            ) : items.length === 0 ? (
              <p className="bell__empty muted small">
                Nothing yet. Booking updates and new messages will show up here.
              </p>
            ) : (
              <ul className="bell__list">
                {items.map((n) => (
                  <li key={n.id}>
                    <button
                      type="button"
                      className={`bell__item ${n.read ? '' : 'is-unread'}`}
                      onClick={() => openNotification(n)}
                    >
                      <span className="bell__icon" aria-hidden="true">
                        {ICONS[n.type] || '🔔'}
                      </span>
                      <span className="bell__text">
                        <span className="bell__title">{n.title}</span>
                        {n.body && <span className="bell__body">{n.body}</span>}
                        <span className="bell__time">{timeAgo(n.createdAt)}</span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </m.div>
        )}
      </AnimatePresence>
    </div>
  );
}
