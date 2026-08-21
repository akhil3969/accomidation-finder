import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { bookingApi, favoriteApi, reviewApi, roomApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useSubscription } from '../context/RealtimeContext';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import useForm, { compact, rules } from '../hooks/useForm';
import PageTransition from '../components/PageTransition';
import Reveal from '../components/Reveal';
import Loader, { ButtonSpinner } from '../components/Loader';
import Rating, { RatingInput } from '../components/Rating';
import EmptyState from '../components/EmptyState';
import Field, { TextAreaField } from '../components/Field';
import AffordabilityPanel from '../components/AffordabilityPanel';
import VisalePanel from '../components/VisalePanel';
import NeighbourhoodPanel from '../components/NeighbourhoodPanel';
import SafetyPanel from '../components/SafetyPanel';
import { DURATION, EASE } from '../motion/tokens';
import {
  ROOM_TYPE_LABELS,
  formatDate,
  formatPrice,
  formatPricePrecise,
  initials,
  nightsBetween,
  placeholderImage,
  todayIso,
} from '../utils/format';

const MapView = lazy(() => import('../components/MapView'));

/** Do two closed date ranges touch at all? */
function overlaps(fromA, toA, fromB, toB) {
  return new Date(fromA) < new Date(toB) && new Date(fromB) < new Date(toA);
}

export default function RoomDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const toast = useToast();

  const [room, setRoom] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [ranges, setRanges] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeImage, setActiveImage] = useState(0);
  const [favPending, setFavPending] = useState(false);

  useDocumentTitle(room?.title);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [detail, roomReviews, bookedRanges] = await Promise.all([
        roomApi.byId(id),
        reviewApi.forRoom(id).catch(() => []),
        bookingApi.ranges(id).catch(() => []),
      ]);
      setRoom(detail);
      setReviews(roomReviews);
      setRanges(bookedRanges);
      setActiveImage(0);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  // Availability for this specific listing, pushed by the backend.
  useSubscription(`/topic/rooms/${id}/availability`, (event) => {
    setRoom((current) => (current ? { ...current, available: event.available } : current));
    toast.info(
      event.available ? 'This listing just became available' : 'This listing was just taken',
    );
  });

  const isOwner = Boolean(room && user && room.landlord?.id === user.id);

  // ------------------------------------------------------------- booking

  const bookingForm = useForm({
    initialValues: {
      checkIn: todayIso(14),
      checkOut: todayIso(194),
      guests: 1,
      message: '',
    },
    validate: (values) => {
      const today = todayIso();
      const nights = nightsBetween(values.checkIn, values.checkOut);
      const clash = ranges.find(
        (range) =>
          values.checkIn &&
          values.checkOut &&
          overlaps(values.checkIn, values.checkOut, range.checkIn, range.checkOut),
      );

      return compact({
        checkIn:
          rules.required(values.checkIn, 'A move-in date') ||
          (values.checkIn < today ? 'Move-in cannot be in the past' : undefined),
        checkOut:
          rules.required(values.checkOut, 'A move-out date') ||
          (nights <= 0 ? 'Move-out must be after move-in' : undefined) ||
          (clash
            ? `Those dates clash with a stay already held (${formatDate(clash.checkIn)} to ${formatDate(clash.checkOut)})`
            : undefined),
        guests: rules.number(values.guests, {
          min: 1,
          max: room?.maxGuests || 10,
          label: 'Guests',
        }),
        message: rules.maxLength(values.message, 1000, 'Your message'),
      });
    },
    onSubmit: async (values) => {
      await bookingApi.request({
        roomId: Number(id),
        checkIn: values.checkIn,
        checkOut: values.checkOut,
        guests: Number(values.guests),
        message: values.message.trim() || null,
      });
      toast.success('Request sent. The landlord has been notified in real time.');
      navigate('/bookings');
    },
  });

  const nights = nightsBetween(bookingForm.values.checkIn, bookingForm.values.checkOut);
  const estimate = useMemo(() => {
    if (!room?.price || !nights) return 0;
    return (Number(room.price) * nights) / 30;
  }, [room?.price, nights]);

  function handleBookingSubmit(event) {
    event.preventDefault();
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/rooms/${id}` } });
      return;
    }
    bookingForm.handleSubmit(event);
  }

  // ----------------------------------------------------------- favourite

  async function handleFavorite() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/rooms/${id}` } });
      return;
    }
    if (favPending) return;
    setFavPending(true);
    // Flip immediately, roll back if the server says otherwise.
    const previous = room.favorite;
    setRoom((current) => ({ ...current, favorite: !previous }));
    try {
      const { favorite } = await favoriteApi.toggle(room.id);
      setRoom((current) => ({ ...current, favorite }));
      toast.success(favorite ? 'Saved to your list' : 'Removed from your list');
    } catch (requestError) {
      setRoom((current) => ({ ...current, favorite: previous }));
      toast.error(toMessage(requestError));
    } finally {
      setFavPending(false);
    }
  }

  // -------------------------------------------------------------- review

  const reviewForm = useForm({
    initialValues: { rating: 5, comment: '' },
    validate: (values) =>
      compact({
        comment:
          rules.required(values.comment, 'A few words') ||
          rules.minLength(values.comment, 10, 'Your review') ||
          rules.maxLength(values.comment, 2000, 'Your review'),
      }),
    onSubmit: async (values) => {
      try {
        const created = await reviewApi.create(id, values);
        setReviews((current) => [created, ...current]);
        reviewForm.reset({ rating: 5, comment: '' });
        toast.success('Thanks for your review');
        setRoom(await roomApi.byId(id));
      } catch (requestError) {
        const message = toMessage(requestError);
        throw Object.assign(new Error(message), { userMessage: message });
      }
    },
  });

  if (loading) {
    return (
      <PageTransition>
        <Loader label="Loading listing…" />
      </PageTransition>
    );
  }

  if (error || !room) {
    return (
      <PageTransition className="page">
        <EmptyState
          icon="&#9888;"
          title="Listing unavailable"
          description={error || 'This listing may have been removed.'}
          action={
            <>
              <button type="button" className="btn btn--ghost" onClick={load}>
                Try again
              </button>
              <Link to="/" className="btn btn--primary">
                Back to search
              </Link>
            </>
          }
        />
      </PageTransition>
    );
  }

  const images = room.images?.length ? room.images : [placeholderImage(room.id)];
  const alreadyReviewed = reviews.some((item) => item.author?.id === user?.id);

  return (
    <PageTransition className="page">
      {/* A high risk score belongs at the very top. The cost panel on the right
          is the most eye-catching thing on this page, and it would be wrong to
          let an attractive number land before the warning that the listing may
          not be real. */}
      {room.riskLevel === 'high' && (
        <div className="safety-banner safety-banner--high" style={{ marginBottom: '1.25rem' }}>
          <span className="safety-banner__icon" aria-hidden="true">
            ⚠
          </span>
          <div>
            <strong>Take your time with this one.</strong> Several things about this listing match
            the pattern of rental fraud. Read the safety notes below before you contact anyone, and
            never send money before you have seen the place.
          </div>
        </div>
      )}

      <div className="page-header">
        <div>
          <div className="row" style={{ gap: '0.4rem', marginBottom: '0.35rem' }}>
            <AnimatePresence mode="wait" initial={false}>
              <motion.span
                key={room.available ? 'free' : 'taken'}
                className={`badge ${room.available ? 'badge--success' : 'badge--danger'}`}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                transition={{ duration: DURATION.fast, ease: EASE }}
              >
                {room.available ? 'Available now' : 'Currently taken'}
              </motion.span>
            </AnimatePresence>
            <span className="badge badge--neutral">{ROOM_TYPE_LABELS[room.roomType]}</span>
            {room.billsIncluded && <span className="badge badge--info">Bills included</span>}
          </div>
          <h1>{room.title}</h1>
          <p className="muted">
            {[room.address, room.postalCode, room.city, room.country].filter(Boolean).join(', ')}
          </p>
          <Rating value={room.averageRating} count={room.reviewCount} size="lg" />
        </div>

        <div className="row">
          <motion.button
            type="button"
            className={`icon-btn ${room.favorite ? 'is-active' : ''}`}
            onClick={handleFavorite}
            whileTap={{ scale: 0.85 }}
            aria-pressed={Boolean(room.favorite)}
            aria-label={room.favorite ? 'Remove from saved' : 'Save this listing'}
          >
            {room.favorite ? '♥' : '♡'}
          </motion.button>
          {isOwner && (
            <Link to={`/dashboard/rooms/${room.id}/edit`} className="btn btn--ghost btn--sm">
              Edit listing
            </Link>
          )}
        </div>
      </div>

      <div className="detail-layout">
        <div className="stack" style={{ gap: '1.5rem' }}>
          <section className="gallery">
            <div className="gallery__main">
              <AnimatePresence mode="wait" initial={false}>
                <motion.img
                  key={images[activeImage]}
                  src={images[activeImage]}
                  alt={room.title}
                  initial={{ opacity: 0, scale: 1.02 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: DURATION.base, ease: EASE }}
                />
              </AnimatePresence>
            </div>
            {images.length > 1 && (
              <div className="gallery__thumbs">
                {images.map((image, index) => (
                  <button
                    key={image}
                    type="button"
                    className={index === activeImage ? 'is-active' : ''}
                    onClick={() => setActiveImage(index)}
                    aria-label={`Photo ${index + 1} of ${images.length}`}
                    aria-current={index === activeImage ? 'true' : undefined}
                  >
                    <img src={image} alt="" loading="lazy" />
                  </button>
                ))}
              </div>
            )}
          </section>

          <SafetyPanel roomId={room.id} />

          <Reveal>
            <section className="card">
              <div className="card__body stack">
                <h2>About this place</h2>
                <p style={{ whiteSpace: 'pre-line' }}>
                  {room.description || 'No description provided.'}
                </p>

                <div className="spec-grid">
                  <div className="spec">
                    <span>Size</span>
                    <strong>{room.sizeSqm ? `${room.sizeSqm} m²` : '—'}</strong>
                  </div>
                  <div className="spec">
                    <span>Bedrooms</span>
                    <strong>{room.bedrooms ?? '—'}</strong>
                  </div>
                  <div className="spec">
                    <span>Bathrooms</span>
                    <strong>{room.bathrooms ?? '—'}</strong>
                  </div>
                  <div className="spec">
                    <span>Max guests</span>
                    <strong>{room.maxGuests ?? '—'}</strong>
                  </div>
                  <div className="spec">
                    <span>Available from</span>
                    <strong>{formatDate(room.availableFrom)}</strong>
                  </div>
                  <div className="spec">
                    <span>Minimum stay</span>
                    <strong>{room.minStayMonths ? `${room.minStayMonths} months` : '—'}</strong>
                  </div>
                  <div className="spec">
                    <span>Furnished</span>
                    <strong>{room.furnished ? 'Yes' : 'No'}</strong>
                  </div>
                  <div className="spec">
                    <span>Deposit</span>
                    <strong>{room.deposit ? formatPrice(room.deposit) : '—'}</strong>
                  </div>
                </div>

                {room.amenities?.length > 0 && (
                  <>
                    <h3>What this place offers</h3>
                    <div className="amenity-list">
                      {room.amenities.map((amenity) => (
                        <span key={amenity} className="amenity">
                          {amenity}
                        </span>
                      ))}
                    </div>
                  </>
                )}
              </div>
            </section>
          </Reveal>

          {room.latitude && room.longitude && (
            <Reveal>
              <section className="card">
                <div className="card__header">
                  <h3>Where you will be</h3>
                  <span className="muted small">{room.city}</span>
                </div>
                <div className="card__body">
                  <div className="detail-map">
                    <Suspense fallback={<div className="skeleton" style={{ height: '100%' }} />}>
                      <MapView
                        rooms={[room]}
                        center={[Number(room.latitude), Number(room.longitude)]}
                        zoom={15}
                        /* An inline map that grabs the wheel traps the page
                           scroll halfway down the article. */
                        scrollZoom={false}
                      />
                    </Suspense>
                  </div>
                </div>
              </section>
            </Reveal>
          )}

          <NeighbourhoodPanel roomId={room.id} />

          <Reveal>
            <section className="card">
              <div className="card__header">
                <h3>Reviews ({reviews.length})</h3>
                <Rating value={room.averageRating} count={room.reviewCount} />
              </div>
              <div className="card__body stack">
                {reviews.length === 0 && (
                  <p className="muted small">No reviews yet for this listing.</p>
                )}

                <AnimatePresence initial={false}>
                  {reviews.map((item) => (
                    <motion.div
                      key={item.id}
                      layout="position"
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: DURATION.base, ease: EASE }}
                      className="row"
                      style={{ alignItems: 'flex-start', gap: '0.75rem', flexWrap: 'nowrap' }}
                    >
                      <span className="avatar">{initials(item.author?.name)}</span>
                      <div style={{ minWidth: 0 }}>
                        <strong>{item.author?.name}</strong>{' '}
                        <span className="rating__stars" aria-label={`${item.rating} out of 5`}>
                          <span className="rating__stars-track">★★★★★</span>
                          <span
                            className="rating__stars-fill"
                            style={{ width: `${(item.rating / 5) * 100}%` }}
                          >
                            ★★★★★
                          </span>
                        </span>
                        <div className="small muted">{formatDate(item.createdAt)}</div>
                        <p style={{ margin: '0.25rem 0 0' }}>{item.comment}</p>
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>

                {isAuthenticated && !isOwner && !alreadyReviewed && (
                  <form
                    className="stack"
                    onSubmit={reviewForm.handleSubmit}
                    noValidate
                    style={{ borderTop: '1px solid var(--border)', paddingTop: '1rem' }}
                  >
                    <h4 style={{ margin: 0 }}>Leave a review</h4>
                    <p className="small muted" style={{ margin: 0 }}>
                      Reviews are open to guests with a confirmed or completed stay here.
                    </p>
                    <RatingInput
                      value={reviewForm.values.rating}
                      onChange={(rating) => reviewForm.setValue('rating', rating)}
                    />
                    <TextAreaField
                      id="comment"
                      label="Your review"
                      placeholder="How was your stay?"
                      rows={3}
                      value={reviewForm.values.comment}
                      error={reviewForm.errorFor('comment')}
                      onChange={reviewForm.handleChange}
                      onBlur={reviewForm.handleBlur}
                    />
                    {reviewForm.formError && (
                      <div className="form-alert" role="alert">
                        {reviewForm.formError}
                      </div>
                    )}
                    <div className="row row--end">
                      <button
                        type="submit"
                        className="btn btn--primary btn--sm"
                        disabled={reviewForm.submitting}
                      >
                        {reviewForm.submitting && <ButtonSpinner />}
                        Post review
                      </button>
                    </div>
                  </form>
                )}
              </div>
            </section>
          </Reveal>
        </div>

        {/* ------------------------------------------------ booking panel */}
        <aside className="booking-panel stack">
          <AffordabilityPanel roomId={room.id} />

          <div className="card">
            <div className="card__body stack">
              <div>
                <div className="room-card__price" style={{ fontSize: '1.6rem' }}>
                  {formatPrice(room.price)} <span>/ month</span>
                </div>
                {room.deposit && (
                  <span className="small muted">Deposit {formatPrice(room.deposit)}</span>
                )}
              </div>

              {isOwner ? (
                <p className="small muted" style={{ margin: 0 }}>
                  This is your own listing. Manage requests from your dashboard.
                </p>
              ) : (
                <form className="stack" onSubmit={handleBookingSubmit} noValidate>
                  <div className="form-grid">
                    <Field
                      id="checkIn"
                      label="Move in"
                      required
                      error={bookingForm.errorFor('checkIn')}
                    >
                      {(a11y) => (
                        <input
                          {...a11y}
                          type="date"
                          min={todayIso()}
                          value={bookingForm.values.checkIn}
                          onChange={bookingForm.handleChange}
                          onBlur={bookingForm.handleBlur}
                        />
                      )}
                    </Field>
                    <Field
                      id="checkOut"
                      label="Move out"
                      required
                      error={bookingForm.errorFor('checkOut')}
                    >
                      {(a11y) => (
                        <input
                          {...a11y}
                          type="date"
                          min={bookingForm.values.checkIn}
                          value={bookingForm.values.checkOut}
                          onChange={bookingForm.handleChange}
                          onBlur={bookingForm.handleBlur}
                        />
                      )}
                    </Field>
                  </div>

                  <Field
                    id="guests"
                    label="Guests"
                    error={bookingForm.errorFor('guests')}
                    hint={room.maxGuests ? `This place sleeps up to ${room.maxGuests}.` : undefined}
                  >
                    {(a11y) => (
                      <input
                        {...a11y}
                        type="number"
                        min="1"
                        max={room.maxGuests || 10}
                        inputMode="numeric"
                        value={bookingForm.values.guests}
                        onChange={bookingForm.handleChange}
                        onBlur={bookingForm.handleBlur}
                      />
                    )}
                  </Field>

                  <TextAreaField
                    id="message"
                    label="Message to the landlord"
                    rows={3}
                    placeholder="Introduce yourself briefly"
                    value={bookingForm.values.message}
                    error={bookingForm.errorFor('message')}
                    onChange={bookingForm.handleChange}
                    onBlur={bookingForm.handleBlur}
                  />

                  <AnimatePresence initial={false}>
                    {nights > 0 && (
                      <motion.div
                        className="spec"
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: DURATION.fast, ease: EASE }}
                      >
                        <span>{nights} nights, pro-rated</span>
                        <strong>{formatPricePrecise(estimate)}</strong>
                      </motion.div>
                    )}
                  </AnimatePresence>

                  {bookingForm.formError && (
                    <div className="form-alert" role="alert">
                      {bookingForm.formError}
                    </div>
                  )}

                  <button
                    type="submit"
                    className="btn btn--accent btn--block"
                    disabled={bookingForm.submitting || !room.available}
                  >
                    {bookingForm.submitting && <ButtonSpinner />}
                    {room.available
                      ? bookingForm.submitting
                        ? 'Sending request…'
                        : 'Request to book'
                      : 'Currently unavailable'}
                  </button>

                  <p className="small muted center" style={{ margin: 0 }}>
                    You will not be charged. The landlord confirms first.
                  </p>
                </form>
              )}
            </div>
          </div>

          <div className="card">
            <div className="card__body stack">
              <h3 style={{ margin: 0 }}>Hosted by</h3>
              <div className="row" style={{ flexWrap: 'nowrap' }}>
                <span className="avatar avatar--lg">{initials(room.landlord?.name)}</span>
                <div style={{ minWidth: 0 }}>
                  <strong>{room.landlord?.name}</strong>
                  <div className="small muted">
                    {room.landlord?.bio || 'Landlord on AccomFinder'}
                  </div>
                </div>
              </div>
              {!isOwner && (
                <button
                  type="button"
                  className="btn btn--ghost btn--block"
                  onClick={() =>
                    isAuthenticated
                      ? navigate('/messages', {
                          state: {
                            roomId: room.id,
                            userId: room.landlord?.id,
                            roomTitle: room.title,
                          },
                        })
                      : navigate('/login', { state: { from: `/rooms/${id}` } })
                  }
                >
                  Message the landlord
                </button>
              )}
            </div>
          </div>

          <VisalePanel roomId={room.id} />

          {ranges.length > 0 && (
            <div className="card">
              <div className="card__header">
                <h3>Already held</h3>
              </div>
              <div className="card__body stack" style={{ gap: '0.4rem' }}>
                {ranges.map((range) => (
                  <div key={`${range.checkIn}-${range.checkOut}`} className="row small muted">
                    <span>
                      {formatDate(range.checkIn)} – {formatDate(range.checkOut)}
                    </span>
                    <div className="spacer" />
                    <span className="badge badge--neutral">{range.status}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </aside>
      </div>
    </PageTransition>
  );
}
