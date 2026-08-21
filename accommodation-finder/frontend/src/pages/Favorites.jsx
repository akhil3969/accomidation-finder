import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { favoriteApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import PageTransition from '../components/PageTransition';
import { CardSkeletons } from '../components/Loader';
import RoomCard from '../components/RoomCard';
import EmptyState from '../components/EmptyState';
import { listVariants } from '../motion/tokens';

export default function Favorites() {
  useDocumentTitle('Saved listings');

  const toast = useToast();
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Everything on this page is saved by definition, but the list endpoint
      // does not set the flag, so the heart used to render hollow here.
      const saved = await favoriteApi.list();
      setRooms(saved.map((room) => ({ ...room, favorite: true })));
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const remove = useCallback(
    async (room) => {
      // Drop it from the grid straight away and put it back if the server
      // disagrees - waiting for the round trip made the heart feel broken.
      setRooms((current) => current.filter((item) => item.id !== room.id));
      try {
        await favoriteApi.toggle(room.id);
        toast.success('Removed from your list');
      } catch (requestError) {
        setRooms((current) =>
          current.some((item) => item.id === room.id) ? current : [room, ...current],
        );
        toast.error(toMessage(requestError));
      }
    },
    [toast],
  );

  return (
    <PageTransition className="page">
      <div className="page-header">
        <div>
          <h1>Saved listings</h1>
          <p>
            {loading
              ? 'Loading your saved listings…'
              : `${rooms.length} listing${rooms.length === 1 ? '' : 's'} saved.`}
          </p>
        </div>
      </div>

      {loading && <CardSkeletons count={3} />}

      {!loading && error && (
        <EmptyState
          icon="&#9888;"
          title="Could not load your saved listings"
          description={error}
          action={
            <button type="button" className="btn btn--primary" onClick={load}>
              Try again
            </button>
          }
        />
      )}

      {!loading && !error && rooms.length === 0 && (
        <EmptyState
          icon="&#9825;"
          title="Nothing saved yet"
          description="Tap the heart on any listing to keep it here."
          action={
            <Link to="/" className="btn btn--primary">
              Browse listings
            </Link>
          }
        />
      )}

      {!loading && !error && rooms.length > 0 && (
        <motion.div className="room-grid" variants={listVariants} initial="initial" animate="animate">
          <AnimatePresence mode="popLayout" initial={false}>
            {rooms.map((room) => (
              <RoomCard key={room.id} room={room} canFavorite onToggleFavorite={remove} />
            ))}
          </AnimatePresence>
        </motion.div>
      )}
    </PageTransition>
  );
}
