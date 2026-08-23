import { motion } from 'framer-motion';
import { pageVariants } from '../motion/tokens';

/**
 * Wraps a route's content so it fades and lifts into place.
 *
 * Every page mounts inside one of these, and <App> keys the whole set on the
 * pathname, so navigating anywhere gives the same short cross-fade rather than
 * the hard swap React Router does on its own.
 */
export default function PageTransition({ children, className }) {
  return (
    <motion.div
      className={className}
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
    >
      {children}
    </motion.div>
  );
}
