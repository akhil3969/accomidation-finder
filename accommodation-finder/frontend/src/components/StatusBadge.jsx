import { AnimatePresence, motion } from 'framer-motion';
import { STATUS_LABELS } from '../utils/format';
import { spring } from '../motion/tokens';

const TONE = {
  PENDING: 'warning',
  CONFIRMED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'neutral',
  COMPLETED: 'info',
};

/**
 * A booking status that visibly changes.
 *
 * Statuses flip from a WebSocket frame while the page is open, so the badge is
 * keyed on the status: the old one leaves and the new one arrives, which makes
 * a landlord's decision impossible to miss.
 */
export default function StatusBadge({ status }) {
  return (
    <span className="status-badge">
      <AnimatePresence mode="wait" initial={false}>
        <motion.span
          key={status}
          className={`badge badge--${TONE[status] || 'neutral'}`}
          initial={{ opacity: 0, y: -6, scale: 0.9 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 6, scale: 0.9 }}
          transition={spring}
        >
          {STATUS_LABELS[status] || status}
        </motion.span>
      </AnimatePresence>
    </span>
  );
}
