import { useEffect, useState } from 'react';
import { animate, motion, useReducedMotion } from 'framer-motion';
import { DURATION, EASE } from '../motion/tokens';

/**
 * Counts from the previous value to the new one.
 *
 * These tiles sit on dashboards that are updated by WebSocket frames, so a
 * number changing is information: rolling it makes the change impossible to
 * miss where a silent swap of the digits is easy to scroll past.
 */
function useCountUp(target) {
  const reduced = useReducedMotion();
  const numeric = typeof target === 'number' && Number.isFinite(target);
  const [display, setDisplay] = useState(numeric ? target : 0);

  useEffect(() => {
    if (!numeric || reduced) return undefined;
    const controls = animate(display, target, {
      duration: 0.5,
      ease: EASE,
      onUpdate: (value) => setDisplay(value),
    });
    return () => controls.stop();
    // Chasing `display` here would restart the animation on every frame.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target, numeric, reduced]);

  if (!numeric) return target;
  if (reduced) return target;
  return Math.round(display).toLocaleString();
}

export default function StatTile({ label, value, tone = '', hint }) {
  const shown = useCountUp(value);

  return (
    <motion.div
      className={`stat-tile ${tone ? `stat-tile--${tone}` : ''}`}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: DURATION.base, ease: EASE }}
    >
      <div className="stat-tile__label">{label}</div>
      <div className="stat-tile__value">{shown}</div>
      {hint && <div className="small muted">{hint}</div>}
    </motion.div>
  );
}
