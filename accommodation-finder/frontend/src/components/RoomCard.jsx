import { forwardRef, memo, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import Rating from './Rating';
import { itemVariants } from '../motion/tokens';
import { ROOM_TYPE_LABELS, formatPrice, placeholderImage } from '../utils/format';

/**
 * `forwardRef` is not optional here: AnimatePresence's popLayout mode measures
 * each leaving child through a ref, and a plain function component silently
 * fails that measurement (and warns) when a card is removed from the grid.
 */
const RoomCard = forwardRef(function RoomCard(
  { room, onToggleFavorite, canFavorite, highlight, isNew },
  ref,
) {
  const [flash, setFlash] = useState(false);
  const [pendingFavorite, setPendingFavorite] = useState(false);

  // Briefly ring the card when a live update touches it. `highlight` carries a
  // timestamp rather than a boolean so two updates in a row to the same listing
  // each replay the effect - with a boolean the second was swallowed.
  useEffect(() => {
    if (!highlight) return undefined;
    setFlash(true);
    const timer = window.setTimeout(() => setFlash(false), 1400);
    return () => window.clearTimeout(timer);
  }, [highlight]);

  async function handleFavorite() {
    if (pendingFavorite) return;
    setPendingFavorite(true);
    try {
      await onToggleFavorite?.(room);
    } finally {
      setPendingFavorite(false);
    }
  }

  const cover = room.images?.[0] || placeholderImage(room.id);

  return (
    <motion.article
      ref={ref}
      layout="position"
      variants={itemVariants}
      whileHover={{ y: -5 }}
      transition={{ duration: 0.18, ease: [0.22, 0.61, 0.36, 1] }}
      className={`room-card ${flash ? 'is-flashing' : ''}`}
    >
      <Link to={`/rooms/${room.id}`} className="room-card__media" aria-label={room.title}>
        <img src={cover} alt="" loading="lazy" decoding="async" />
      </Link>

      {isNew && <span className="room-card__new">Just listed</span>}

      {canFavorite && (
        <motion.button
          type="button"
          className={`icon-btn room-card__fav ${room.favorite ? 'is-active' : ''}`}
          aria-label={room.favorite ? 'Remove from saved' : 'Save this listing'}
          aria-pressed={Boolean(room.favorite)}
          disabled={pendingFavorite}
          whileTap={{ scale: 0.8 }}
          onClick={handleFavorite}
        >
          <motion.span
            key={room.favorite ? 'on' : 'off'}
            initial={{ scale: 0.6, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: 'spring', stiffness: 520, damping: 18 }}
          >
            {room.favorite ? '♥' : '♡'}
          </motion.span>
        </motion.button>
      )}

      <div className="room-card__body">
        <div className="row" style={{ gap: '0.35rem' }}>
          <span className={`badge ${room.available ? 'badge--success' : 'badge--danger'}`}>
            {room.available ? 'Available' : 'Taken'}
          </span>
          <span className="badge badge--neutral">
            {ROOM_TYPE_LABELS[room.roomType] || room.roomType}
          </span>
          {room.landlordVerified && <span className="badge badge--info">Verified</span>}
          {room.riskLevel === 'high' && <span className="badge badge--danger">Check carefully</span>}
        </div>

        <h3 className="room-card__title">
          <Link to={`/rooms/${room.id}`}>{room.title}</Link>
        </h3>

        <div className="room-card__meta">
          {[room.city, room.sizeSqm ? `${room.sizeSqm} m²` : null, room.bedrooms ? `${room.bedrooms} bed` : null]
            .filter(Boolean)
            .join(' · ')}
        </div>

        <Rating value={room.averageRating} count={room.reviewCount} />

        <div className="room-card__footer">
          <div className="room-card__price">
            {formatPrice(room.price)} <span>/ month</span>
            {room.chargesAmount > 0 && <span> + {formatPrice(room.chargesAmount)} bills</span>}
          </div>
          <div className="spacer" />
          <Link to={`/rooms/${room.id}`} className="btn btn--sm btn--primary">
            View
          </Link>
        </div>
      </div>
    </motion.article>
  );
});

/**
 * A live availability flip re-renders the whole result page, and without this
 * every card in the grid would re-render with it. Comparing the fields the card
 * actually draws keeps that to the one card that changed.
 */
export default memo(RoomCard, (previous, next) =>
  previous.room === next.room &&
  previous.highlight === next.highlight &&
  previous.isNew === next.isNew &&
  previous.canFavorite === next.canFavorite &&
  previous.onToggleFavorite === next.onToggleFavorite);
