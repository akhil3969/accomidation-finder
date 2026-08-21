import { motion } from 'framer-motion';
import { DURATION, EASE } from '../motion/tokens';

/**
 * Reveals its children the first time they scroll into view.
 *
 * `once` is deliberately the default: re-animating a section every time it
 * passes the viewport edge is the kind of effect that looks impressive in a
 * demo and becomes irritating on the second scroll.
 */
export default function Reveal({ children, delay = 0, y = 20, className, as = 'div' }) {
  const Component = motion[as] || motion.div;

  return (
    <Component
      className={className}
      initial={{ opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-60px' }}
      transition={{ duration: DURATION.slow, ease: EASE, delay }}
    >
      {children}
    </Component>
  );
}
