import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { bookingApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import { useConfirm } from '../context/ConfirmContext';
import { useSubscription } from '../context/RealtimeContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import PageTransition from '../components/PageTransition';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';
import { itemVariants, listVariants } from '../motion/tokens';
import { formatDate, formatPricePrecise, placeholderImage } from '../utils/format';

export default function MyBookings() {
  useDocumentTitle('My bookings');

  const toast = useToast();
  const confirm = useConfirm();
  const [result, setResult] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelling, setCancelling] = useState(null);
  const [changedId, setChangedId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setResult(await bookingApi.mine({ page, size: 8 }));
      setError(null);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  // The landlord's decision arrives here without a refresh.
  useSubscription('/user/queue/bookings', (booking) => {
    setResult((current) => {
      if (!current?.content.some((item) => item.id === booking.id)) return current;
      toast.info(`Booking for "${booking.roomTitle}" is now ${booking.status.toLowerCase()}`);
      setChangedId(booking.id);
      return {
        ...current,
        content: current.content.map((item) => (item.id === booking.id ? booking : item)),
      };
    });
  });

  // Clear the highlight after it has been seen.
  useEffect(() => {
    if (!changedId) return undefined;
    const timer = window.setTimeout(() => setChangedId(null), 2000);
    return () => window.clearTimeout(timer);
  }, [changedId]);

  async function cancel(booking) {
    const agreed = await confirm({
      title: 'Cancel this request?',
      message: `Your request for "${booking.roomTitle}" will be withdrawn.`,
      detail: 'The landlord is told immediately. You can request the same dates again later.',
      confirmLabel: 'Cancel request',
      cancelLabel: 'Keep it',
      tone: 'danger',
    });
    if (!agreed) return;

    setCancelling(booking.id);
    try {
      const updated = await bookingApi.cancel(booking.id);
      setResult((current) =>
        current
          ? {
              ...current,
              content: current.content.map((item) => (item.id === updated.id ? updated : item)),
            }
          : current,
      );
      toast.success('Booking cancelled');
    } catch (requestError) {
      toast.error(toMessage(requestError));
    } finally {
      setCancelling(null);
    }
  }

  if (loading && !result) {
    return (
      <PageTransition>
        <Loader label="Loading your bookings…" />
      </PageTransition>
    );
  }

  const bookings = result?.content ?? [];

  return (
    <PageTransition className="page">
      <div className="page-header">
        <div>
          <h1>My bookings</h1>
          <p>Every request you have sent, and where it stands.</p>
        </div>
      </div>

      {error && (
        <EmptyState
          icon="&#9888;"
          title="Could not load bookings"
          description={error}
          action={
            <button type="button" className="btn btn--primary" onClick={load}>
              Try again
            </button>
          }
        />
      )}

      {!error && bookings.length === 0 && (
        <EmptyState
          icon="&#128197;"
          title="No bookings yet"
          description="When you request a room, it will show up here."
          action={
            <Link to="/" className="btn btn--primary">
              Browse listings
            </Link>
          }
        />
      )}

      <motion.div className="stack" variants={listVariants} initial="initial" animate="animate">
        <AnimatePresence mode="popLayout" initial={false}>
          {bookings.map((booking) => (
            <motion.article
              key={booking.id}
              layout
              variants={itemVariants}
              className={`card booking-row ${changedId === booking.id ? 'is-changed' : ''}`}
            >
              <div className="card__body booking-row__inner">
                <img
                  className="booking-row__thumb"
                  src={booking.roomImage || placeholderImage(booking.roomId)}
                  alt=""
                  loading="lazy"
                />

                <div className="booking-row__main">
                  <div className="row" style={{ gap: '0.5rem' }}>
                    <StatusBadge status={booking.status} />
                    <span className="small muted">Requested {formatDate(booking.requestedAt)}</span>
                  </div>
                  <h3 style={{ margin: '0.35rem 0 0.15rem' }}>
                    <Link to={`/rooms/${booking.roomId}`}>{booking.roomTitle}</Link>
                  </h3>
                  <div className="small muted">
                    {booking.roomCity} · {formatDate(booking.checkIn)} to{' '}
                    {formatDate(booking.checkOut)} · {booking.guests} guest
                    {booking.guests > 1 ? 's' : ''}
                  </div>
                  {booking.landlordResponse && (
                    <p className="small" style={{ marginTop: '0.5rem', marginBottom: 0 }}>
                      <strong>Landlord:</strong> {booking.landlordResponse}
                    </p>
                  )}
                </div>

                <div className="booking-row__side">
                  <strong className="tabular">{formatPricePrecise(booking.totalPrice)}</strong>
                  {['PENDING', 'CONFIRMED'].includes(booking.status) && (
                    <button
                      type="button"
                      className="btn btn--sm btn--ghost"
                      disabled={cancelling === booking.id}
                      onClick={() => cancel(booking)}
                    >
                      {cancelling === booking.id ? 'Cancelling…' : 'Cancel'}
                    </button>
                  )}
                </div>
              </div>
            </motion.article>
          ))}
        </AnimatePresence>
      </motion.div>

      <Pagination page={result?.page ?? 0} totalPages={result?.totalPages ?? 0} onChange={setPage} />
    </PageTransition>
  );
}
