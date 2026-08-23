import { motion } from 'framer-motion';

const STARS = '★★★★★';

/**
 * Read-only star rating.
 *
 * The stars are two stacked copies of the same string with the filled one
 * clipped to a percentage width. The previous version rounded to the nearest
 * half and then drew the half case with the same hollow glyph as an empty
 * star, so 3.5 and 3.0 were indistinguishable.
 */
export default function Rating({ value = 0, count, size = 'sm' }) {
  if (!count) {
    return <span className="rating muted small">No reviews yet</span>;
  }

  const clamped = Math.min(5, Math.max(0, Number(value) || 0));
  const percent = (clamped / 5) * 100;

  return (
    <span
      className={`rating ${size === 'lg' ? 'rating--lg' : 'small'}`}
      title={`${clamped.toFixed(1)} out of 5 from ${count} review${count === 1 ? '' : 's'}`}
    >
      <span className="rating__stars" aria-hidden="true">
        <span className="rating__stars-track">{STARS}</span>
        <span className="rating__stars-fill" style={{ width: `${percent}%` }}>
          {STARS}
        </span>
      </span>
      <strong>{clamped.toFixed(1)}</strong>
      <span className="muted">({count})</span>
    </span>
  );
}

/** Interactive picker used in the review form. */
export function RatingInput({ value, onChange }) {
  return (
    <div className="rating-input" role="radiogroup" aria-label="Your rating">
      {[1, 2, 3, 4, 5].map((star) => (
        <motion.button
          key={star}
          type="button"
          role="radio"
          aria-checked={value === star}
          aria-label={`${star} star${star > 1 ? 's' : ''}`}
          className={`rating-input__star ${star <= value ? 'is-on' : ''}`}
          whileHover={{ scale: 1.18 }}
          whileTap={{ scale: 0.88 }}
          onClick={() => onChange(star)}
        >
          {star <= value ? '★' : '☆'}
        </motion.button>
      ))}
    </div>
  );
}
