import { useEffect, useState } from 'react';

/**
 * True once the element comes within `margin` of the viewport, and true forever after.
 * Lets heavy, below-the-fold pieces (the map) skip the first load entirely on
 * small screens while still being ready before anyone scrolls to them.
 */
export default function useNearViewport(ref, margin = '300px') {
  const [near, setNear] = useState(false);

  useEffect(() => {
    if (near) return undefined;
    const element = ref.current;
    if (!element || typeof IntersectionObserver === 'undefined') {
      setNear(true);
      return undefined;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) setNear(true);
      },
      { rootMargin: margin },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [ref, margin, near]);

  return near;
}
