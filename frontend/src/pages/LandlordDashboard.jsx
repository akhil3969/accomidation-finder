import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { bookingApi, roomApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import { useConfirm } from '../context/ConfirmContext';
import { useSubscription } from '../context/RealtimeContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import StatTile from '../components/StatTile';
import StatusBadge from '../components/StatusBadge';
import Modal from '../components/Modal';
import { formatDate, formatPrice, formatPricePrecise } from '../utils/format';
import PageTransition from '../components/PageTransition';

export default function LandlordDashboard() {
  useDocumentTitle('Landlord dashboard');

  const toast = useToast();
  const confirm = useConfirm();
  const [stats, setStats] = useState(null);
  const [listings, setListings] = useState([]);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [decision, setDecision] = useState(null); // { booking, status }
  const [responseText, setResponseText] = useState('');
  const [deciding, setDeciding] = useState(false);

  const load = useCallback(async () => {
    try {
      const [statsData, roomsPage, requestsPage] = await Promise.all([
        roomApi.myStats(),
        roomApi.mine({ page: 0, size: 50 }),
        bookingApi.requests({ page: 0, size: 20 }),
      ]);
      setStats(statsData);
      setListings(roomsPage.content);
      setRequests(requestsPage.content);
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // A new request lands without a refresh.
  useSubscription('/user/queue/bookings', (booking) => {
    setRequests((current) => {
      const exists = current.some((item) => item.id === booking.id);
      if (exists) {
        return current.map((item) => (item.id === booking.id ? booking : item));
      }
      toast.info(`New booking request for "${booking.roomTitle}"`);
      return [booking, ...current];
    });
    roomApi.myStats().then(setStats).catch(() => {});
  });

  async function toggleAvailability(room) {
    try {
      const updated = await roomApi.setAvailability(room.id, !room.available);
      setListings((current) => current.map((item) => (item.id === room.id ? updated : item)));
      toast.success(updated.available ? 'Listing is live again' : 'Listing marked as taken');
    } catch (error) {
      toast.error(toMessage(error));
    }
  }

  async function removeListing(room) {
    const agreed = await confirm({
      title: 'Delete this listing?',
      message: `"${room.title}" will disappear from search immediately.`,
      detail: 'Requests already made against it are cancelled. This cannot be undone.',
      confirmLabel: 'Delete listing',
      tone: 'danger',
    });
    if (!agreed) return;
    try {
      await roomApi.remove(room.id);
      setListings((current) => current.filter((item) => item.id !== room.id));
      toast.success('Listing deleted');
    } catch (error) {
      toast.error(toMessage(error));
    }
  }

  async function confirmDecision() {
    if (!decision || deciding) return;
    setDeciding(true);
    try {
      const updated = await bookingApi.decide(decision.booking.id, decision.status, responseText || null);
      setRequests((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      toast.success(`Booking ${updated.status.toLowerCase()}`);
      setDecision(null);
      setResponseText('');
      roomApi.myStats().then(setStats).catch(() => {});
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setDeciding(false);
    }
  }

  if (loading) return <Loader label="Loading your dashboard..." />;

  return (
    <PageTransition className="page">
      <div className="page-header">
        <div>
          <h1>Landlord dashboard</h1>
          <p>Manage your listings and respond to booking requests.</p>
        </div>
        <Link to="/dashboard/rooms/new" className="btn btn--accent">
          + New listing
        </Link>
      </div>

      {stats && (
        <div className="stat-row">
          <StatTile label="Listings" value={stats.totalListings} tone="primary" />
          <StatTile label="Available now" value={stats.availableListings} tone="success" />
          <StatTile label="Pending requests" value={stats.pendingBookings} tone="accent" />
          <StatTile label="Confirmed stays" value={stats.confirmedBookings} />
          <StatTile label="Unread messages" value={stats.unreadMessages} />
        </div>
      )}

      <section className="stack" style={{ marginBottom: '2rem' }}>
        <h2>Booking requests</h2>
        {requests.length === 0 ? (
          <EmptyState icon="&#128237;" title="No requests yet" description="They will appear here the moment a tenant applies." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Listing</th>
                  <th>Tenant</th>
                  <th>Dates</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <AnimatePresence initial={false}>
                  {requests.map((booking) => (
                    <motion.tr
                      key={booking.id}
                      layout
                      initial={{ opacity: 0, backgroundColor: 'var(--primary-soft)' }}
                      animate={{ opacity: 1, backgroundColor: 'rgba(0,0,0,0)' }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: 0.34, ease: [0.22, 0.61, 0.36, 1] }}
                    >
                    <td>
                      <Link to={`/rooms/${booking.roomId}`}>{booking.roomTitle}</Link>
                      {booking.message && <div className="small muted">&ldquo;{booking.message}&rdquo;</div>}
                    </td>
                    <td>{booking.tenant?.name}</td>
                    <td className="small">
                      {formatDate(booking.checkIn)}
                      <br />
                      {formatDate(booking.checkOut)}
                    </td>
                    <td>{formatPricePrecise(booking.totalPrice)}</td>
                    <td>
                      <StatusBadge status={booking.status} />
                    </td>
                    <td>
                      {booking.status === 'PENDING' && (
                        <div className="row" style={{ gap: '0.35rem' }}>
                          <button
                            type="button"
                            className="btn btn--sm btn--success"
                            onClick={() => setDecision({ booking, status: 'CONFIRMED' })}
                          >
                            Accept
                          </button>
                          <button
                            type="button"
                            className="btn btn--sm btn--ghost"
                            onClick={() => setDecision({ booking, status: 'REJECTED' })}
                          >
                            Decline
                          </button>
                        </div>
                      )}
                      {booking.status === 'CONFIRMED' && (
                        <button
                          type="button"
                          className="btn btn--sm btn--subtle"
                          onClick={() => setDecision({ booking, status: 'COMPLETED' })}
                        >
                          Mark completed
                        </button>
                      )}
                    </td>
                    </motion.tr>
                  ))}
                </AnimatePresence>
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="stack">
        <h2>Your listings</h2>
        {listings.length === 0 ? (
          <EmptyState
            icon="&#127968;"
            title="No listings yet"
            description="Publish your first room and it appears live for every visitor instantly."
            action={
              <Link to="/dashboard/rooms/new" className="btn btn--primary">
                Create a listing
              </Link>
            }
          />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>City</th>
                  <th>Price</th>
                  <th>Status</th>
                  <th>Rating</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {listings.map((room) => (
                  <tr key={room.id}>
                    <td>
                      <Link to={`/rooms/${room.id}`}>{room.title}</Link>
                    </td>
                    <td>{room.city}</td>
                    <td>{formatPrice(room.price)}</td>
                    <td>
                      <span className={`badge ${room.available ? 'badge--success' : 'badge--danger'}`}>
                        {room.available ? 'Available' : 'Taken'}
                      </span>
                    </td>
                    <td className="small">
                      {room.reviewCount ? `${room.averageRating} (${room.reviewCount})` : '-'}
                    </td>
                    <td>
                      <div className="row" style={{ gap: '0.35rem' }}>
                        <button type="button" className="btn btn--sm btn--subtle" onClick={() => toggleAvailability(room)}>
                          {room.available ? 'Mark taken' : 'Mark available'}
                        </button>
                        <Link to={`/dashboard/rooms/${room.id}/edit`} className="btn btn--sm btn--ghost">
                          Edit
                        </Link>
                        <button type="button" className="btn btn--sm btn--danger" onClick={() => removeListing(room)}>
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <Modal
        open={Boolean(decision)}
        title={
          decision?.status === 'CONFIRMED'
            ? 'Accept this booking'
            : decision?.status === 'REJECTED'
              ? 'Decline this booking'
              : 'Mark this stay completed'
        }
        onClose={() => setDecision(null)}
        footer={
          <>
            <button type="button" className="btn btn--ghost" onClick={() => setDecision(null)}>
              Cancel
            </button>
            <button
              type="button"
              className={`btn ${decision?.status === 'REJECTED' ? 'btn--danger' : 'btn--primary'}`}
              disabled={deciding}
              onClick={confirmDecision}
            >
              {deciding ? 'Saving…' : 'Confirm'}
            </button>
          </>
        }
      >
          <div className="stack">
            <p className="small muted" style={{ margin: 0 }}>
              {decision?.booking.tenant?.name} · {formatDate(decision?.booking.checkIn)} to{' '}
              {formatDate(decision?.booking.checkOut)}
            </p>
            {decision?.status === 'CONFIRMED' && (
              <p className="small" style={{ margin: 0 }}>
                Accepting takes the listing off the market. You can re-open it any time.
              </p>
            )}
            <div className="field">
              <label htmlFor="response">Message to the tenant (optional)</label>
              <textarea
                id="response"
                rows={3}
                value={responseText}
                onChange={(event) => setResponseText(event.target.value)}
              />
            </div>
          </div>
      </Modal>
    </PageTransition>
  );
}
