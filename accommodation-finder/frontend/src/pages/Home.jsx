import { Suspense, lazy, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { favoriteApi, roomApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useRealtime, useSubscription } from '../context/RealtimeContext';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import PageTransition from '../components/PageTransition';
import SearchFilters from '../components/SearchFilters';
import RoomCard from '../components/RoomCard';
import Pagination from '../components/Pagination';
import EmptyState from '../components/EmptyState';
import { CardSkeletons } from '../components/Loader';
import { listVariants } from '../motion/tokens';

// Leaflet plus the tile layer is the single heaviest thing on this page and it
// is below the fold on anything narrower than a laptop, so it arrives after the
// listings rather than blocking them.
const MapView = lazy(() => import('../components/MapView'));

const EMPTY_FILTERS = {
  query: '',
  city: '',
  minPrice: '',
  maxPrice: '',
  roomType: '',
  minSize: '',
  availableOnly: true,
  billsIncluded: false,
  sort: 'createdAt,desc',
};

const PAGE_SIZE = 12;

/** How many filters differ from the defaults, for the reset button's label. */
function countActive(filters) {
  return Object.keys(EMPTY_FILTERS).filter(
    (key) => key !== 'sort' && filters[key] !== EMPTY_FILTERS[key],
  ).length;
}

export default function Home() {
  useDocumentTitle('Find your next place');

  const { isAuthenticated } = useAuth();
  const { connected } = useRealtime();
  const toast = useToast();

  // `filters` is what the controls show; `applied` is what the server has been
  // asked for. Splitting the two is what lets the Search button skip the
  // debounce without the inputs stuttering.
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [applied, setApplied] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [cities, setCities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [highlight, setHighlight] = useState(null); // { id, at }
  const [freshIds, setFreshIds] = useState(() => new Set());

  useEffect(() => {
    const timer = window.setTimeout(() => setApplied(filters), 350);
    return () => window.clearTimeout(timer);
  }, [filters]);

  // Resetting the page in an effect meant a filter change fired two searches:
  // one on the old page, then a second once the reset landed. Doing it during
  // render lets React re-run this render before any effect sees the stale page.
  const lastApplied = useRef(applied);
  if (lastApplied.current !== applied) {
    lastApplied.current = applied;
    if (page !== 0) setPage(0);
  }

  const params = useMemo(() => {
    const next = { page, size: PAGE_SIZE, sort: applied.sort };
    if (applied.query) next.query = applied.query;
    if (applied.city) next.city = applied.city;
    if (applied.minPrice) next.minPrice = applied.minPrice;
    if (applied.maxPrice) next.maxPrice = applied.maxPrice;
    if (applied.roomType) next.roomType = applied.roomType;
    if (applied.minSize) next.minSize = applied.minSize;
    if (applied.availableOnly) next.availableOnly = true;
    if (applied.billsIncluded) next.billsIncluded = true;
    return next;
  }, [applied, page]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setResult(await roomApi.search(params));
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [params]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    let cancelled = false;
    roomApi
      .cities()
      .then((data) => {
        if (!cancelled) setCities(data);
      })
      .catch(() => {
        if (!cancelled) setCities([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // --- live updates ---------------------------------------------------

  useSubscription('/topic/rooms/availability', (event) => {
    setResult((current) => {
      if (!current?.content.some((room) => room.id === event.roomId)) return current;
      return {
        ...current,
        content: current.content.map((room) =>
          room.id === event.roomId ? { ...room, available: event.available } : room,
        ),
      };
    });
    // A timestamp, not the id: two updates to the same listing must each
    // replay the highlight, and a repeated id would look unchanged.
    setHighlight({ id: event.roomId, at: Date.now() });
  });

  useSubscription('/topic/rooms/new', (room) => {
    toast.info(`New listing in ${room.city}: ${room.title}`);

    // Only inject it into the grid when it would pass the filters on screen.
    if (page !== 0) return;
    if (applied.city && !room.city?.toLowerCase().includes(applied.city.toLowerCase())) return;
    if (applied.roomType && room.roomType !== applied.roomType) return;
    if (applied.availableOnly && !room.available) return;
    if (applied.maxPrice && Number(room.price) > Number(applied.maxPrice)) return;
    if (applied.minPrice && Number(room.price) < Number(applied.minPrice)) return;

    setFreshIds((current) => new Set(current).add(room.id));
    setHighlight({ id: room.id, at: Date.now() });
    setResult((current) =>
      current
        ? {
            ...current,
            content: [room, ...current.content.filter((item) => item.id !== room.id)].slice(
              0,
              current.size || PAGE_SIZE,
            ),
            totalElements: (current.totalElements || 0) + 1,
          }
        : current,
    );
  });

  // --- favourites -----------------------------------------------------

  const toggleFavorite = useCallback(
    async (room) => {
      try {
        const { favorite } = await favoriteApi.toggle(room.id);
        setResult((current) =>
          current
            ? {
                ...current,
                content: current.content.map((item) =>
                  item.id === room.id ? { ...item, favorite } : item,
                ),
              }
            : current,
        );
        toast.success(favorite ? 'Saved to your list' : 'Removed from your list');
      } catch (requestError) {
        toast.error(toMessage(requestError));
      }
    },
    [toast],
  );

  const rooms = result?.content ?? [];
  const availableCount = rooms.filter((room) => room.available).length;
  const activeCount = countActive(filters);
  const showSkeletons = loading && !result;

  function updateFilters(next) {
    setFilters(next);
  }

  function searchNow() {
    setApplied(filters);
  }

  function resetFilters() {
    setFilters(EMPTY_FILTERS);
    setApplied(EMPTY_FILTERS);
  }

  return (
    <PageTransition>
      <section className="hero">
        <div className="hero__inner">
          <span className="hero__eyebrow">
            <span className={`status-dot ${connected ? 'is-live' : 'is-down'}`} />
            {connected ? 'Streaming live availability' : 'Reconnecting to live updates'}
          </span>

          <h1>Find your next place, the moment it goes live</h1>
          <p>
            Search verified rooms, studios and flats around Paris. Availability updates stream in
            over a WebSocket, so what you see is what is actually free right now.
          </p>

          <form
            className="searchbar"
            role="search"
            onSubmit={(event) => {
              event.preventDefault();
              searchNow();
            }}
          >
            <input
              type="search"
              aria-label="Search listings"
              placeholder="Try 'studio near EPITA' or a street name"
              value={filters.query}
              onChange={(event) => updateFilters({ ...filters, query: event.target.value })}
            />
            <input
              type="search"
              aria-label="City"
              list="hero-cities"
              placeholder="City"
              value={filters.city}
              onChange={(event) => updateFilters({ ...filters, city: event.target.value })}
            />
            <datalist id="hero-cities">
              {cities.map((city) => (
                <option key={city} value={city} />
              ))}
            </datalist>
            <input
              type="number"
              min="0"
              step="50"
              inputMode="numeric"
              aria-label="Maximum price per month"
              placeholder="Max €/month"
              value={filters.maxPrice}
              onChange={(event) => updateFilters({ ...filters, maxPrice: event.target.value })}
            />
            <button type="submit" className="btn btn--accent">
              Search
            </button>
          </form>

          <div className="hero__stats">
            <div className="hero__stat">
              <strong>{result?.totalElements ?? '—'}</strong>
              <span>listings match your filters</span>
            </div>
            <div className="hero__stat">
              <strong>{availableCount}</strong>
              <span>free on this page</span>
            </div>
            <div className="hero__stat">
              <strong>{cities.length}</strong>
              <span>cities covered</span>
            </div>
          </div>
        </div>
      </section>

      <main className="page">
        <div className="page-header">
          <div>
            <h2>Listings</h2>
            <p aria-live="polite">
              {result
                ? `${result.totalElements} result${result.totalElements === 1 ? '' : 's'}`
                : 'Searching…'}
            </p>
          </div>
          <div className="live-strip">
            <span className={`status-dot ${connected ? 'is-live' : 'is-down'}`} />
            {connected ? 'Live updates active' : 'Reconnecting to live updates…'}
          </div>
        </div>

        <SearchFilters
          filters={filters}
          cities={cities}
          activeCount={activeCount}
          onChange={updateFilters}
          onReset={resetFilters}
        />

        <div className="results-layout">
          <div className="results-column">
            {showSkeletons && <CardSkeletons />}

            {error && (
              <EmptyState
                icon="&#9888;"
                title="Could not load listings"
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
                title="Nothing matches those filters"
                description="Try widening the price range or clearing the city."
                action={
                  <button type="button" className="btn btn--ghost" onClick={resetFilters}>
                    Reset filters
                  </button>
                }
              />
            )}

            {/* Dimming the grid while the next page loads reads as progress
                without the layout collapsing back to a spinner. */}
            {rooms.length > 0 && (
              <motion.div
                className={`room-grid ${loading ? 'is-refreshing' : ''}`}
                variants={listVariants}
                initial="initial"
                animate="animate"
              >
                <AnimatePresence mode="popLayout" initial={false}>
                  {rooms.map((room) => (
                    <RoomCard
                      key={room.id}
                      room={room}
                      canFavorite={isAuthenticated}
                      onToggleFavorite={toggleFavorite}
                      highlight={highlight?.id === room.id ? highlight.at : null}
                      isNew={freshIds.has(room.id)}
                    />
                  ))}
                </AnimatePresence>
              </motion.div>
            )}

            <Pagination
              page={result?.page ?? 0}
              totalPages={result?.totalPages ?? 0}
              onChange={setPage}
            />
          </div>

          <aside className="map-column">
            <Suspense fallback={<div className="skeleton" style={{ height: '100%' }} />}>
              <MapView rooms={rooms} />
            </Suspense>
          </aside>
        </div>
      </main>
    </PageTransition>
  );
}
