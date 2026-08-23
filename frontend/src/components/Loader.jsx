import { motion } from 'framer-motion';
import { fadeVariants, listVariants } from '../motion/tokens';

export default function Loader({ label = 'Loading...' }) {
  return (
    <motion.div className="loader" variants={fadeVariants} initial="initial" animate="animate">
      <div className="spinner" role="progressbar" aria-label={label} />
      <span className="small">{label}</span>
    </motion.div>
  );
}

/**
 * Placeholder grid shown while the first page of results is in flight.
 *
 * It reserves the same height the real cards will take, so the page does not
 * jump when the data lands.
 */
export function CardSkeletons({ count = 6 }) {
  return (
    <motion.div className="room-grid" variants={listVariants} initial="initial" animate="animate">
      {Array.from({ length: count }, (_, index) => (
        <motion.div
          key={index}
          className="skeleton skeleton--card"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: index * 0.04, duration: 0.2 }}
        />
      ))}
    </motion.div>
  );
}

/** Inline spinner for a button that is mid-request. */
export function ButtonSpinner() {
  return <span className="btn__spinner" aria-hidden="true" />;
}
