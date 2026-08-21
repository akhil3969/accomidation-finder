/**
 * Motion vocabulary for the whole app.
 *
 * Everything here sits between 180ms and 400ms. Anything slower reads as lag on
 * a page whose whole promise is that it updates the instant something changes,
 * and anything that animates a property other than transform or opacity gets
 * handed to the compositor badly, so those two are all we move.
 */

export const EASE = [0.22, 0.61, 0.36, 1]; // gentle ease-out, no overshoot
export const EASE_IN_OUT = [0.4, 0, 0.2, 1];

export const DURATION = {
  fast: 0.18,
  base: 0.24,
  slow: 0.34,
};

export const spring = {
  type: 'spring',
  stiffness: 380,
  damping: 32,
  mass: 0.8,
};

/** Route-level transition: content lifts in, the outgoing view simply fades. */
export const pageVariants = {
  initial: { opacity: 0, y: 12 },
  animate: { opacity: 1, y: 0, transition: { duration: DURATION.slow, ease: EASE } },
  exit: { opacity: 0, y: -8, transition: { duration: DURATION.fast, ease: EASE } },
};

/** Parent of a list that should reveal its children one after another. */
export const listVariants = {
  initial: {},
  animate: { transition: { staggerChildren: 0.045, delayChildren: 0.02 } },
  exit: {},
};

/** A single card or row inside a staggered list. */
export const itemVariants = {
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0, transition: { duration: DURATION.base, ease: EASE } },
  exit: { opacity: 0, y: -8, scale: 0.97, transition: { duration: DURATION.fast, ease: EASE } },
};

export const fadeVariants = {
  initial: { opacity: 0 },
  animate: { opacity: 1, transition: { duration: DURATION.base, ease: EASE } },
  exit: { opacity: 0, transition: { duration: DURATION.fast, ease: EASE } },
};

/** Backdrop + panel pair used by modals and drawers. */
export const backdropVariants = {
  initial: { opacity: 0 },
  animate: { opacity: 1, transition: { duration: DURATION.fast, ease: EASE } },
  exit: { opacity: 0, transition: { duration: DURATION.fast, ease: EASE } },
};

export const panelVariants = {
  initial: { opacity: 0, y: 24, scale: 0.97 },
  animate: { opacity: 1, y: 0, scale: 1, transition: spring },
  exit: { opacity: 0, y: 12, scale: 0.98, transition: { duration: DURATION.fast, ease: EASE } },
};

/** Toasts slide in from the right edge they are docked to. */
export const toastVariants = {
  initial: { opacity: 0, x: 32, scale: 0.96 },
  animate: { opacity: 1, x: 0, scale: 1, transition: spring },
  exit: { opacity: 0, x: 32, scale: 0.96, transition: { duration: DURATION.fast, ease: EASE } },
};

/** Chat bubbles grow from the side they belong to. */
export const bubbleVariants = {
  initial: { opacity: 0, y: 10, scale: 0.96 },
  animate: { opacity: 1, y: 0, scale: 1, transition: spring },
  exit: { opacity: 0, transition: { duration: DURATION.fast } },
};

/** Hover/press feedback shared by every interactive card. */
export const hoverLift = {
  whileHover: { y: -4, transition: { duration: DURATION.fast, ease: EASE } },
  whileTap: { y: -1, scale: 0.995 },
};

export const tapScale = {
  whileHover: { scale: 1.02 },
  whileTap: { scale: 0.97 },
  transition: { duration: DURATION.fast, ease: EASE },
};
