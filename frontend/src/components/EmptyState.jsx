import { motion } from 'framer-motion';
import { DURATION, EASE } from '../motion/tokens';

export default function EmptyState({ icon = '\u{1F50D}', title, description, action, tone }) {
  return (
    <motion.div
      className={`empty-state ${tone ? `empty-state--${tone}` : ''}`}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: DURATION.slow, ease: EASE }}
    >
      <div className="empty-state__icon" aria-hidden="true">
        {icon}
      </div>
      <h3>{title}</h3>
      {description && <p className="small">{description}</p>}
      {action && <div className="row">{action}</div>}
    </motion.div>
  );
}
