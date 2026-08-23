import { AnimatePresence, motion } from 'framer-motion';
import { DURATION, EASE } from '../motion/tokens';

/**
 * Height-animated disclosure.
 *
 * `height: auto` is the one exception to the transform-and-opacity-only rule in
 * this app: there is no compositor-friendly way to reveal content of unknown
 * size, and framer-motion measures it rather than transitioning to the keyword,
 * which is what browsers cannot do on their own.
 */
export default function Collapse({ open, children, className }) {
  return (
    <AnimatePresence initial={false}>
      {open && (
        <motion.div
          className={className}
          style={{ overflow: 'hidden' }}
          initial={{ height: 0, opacity: 0 }}
          animate={{ height: 'auto', opacity: 1 }}
          exit={{ height: 0, opacity: 0 }}
          transition={{ duration: DURATION.base, ease: EASE }}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
