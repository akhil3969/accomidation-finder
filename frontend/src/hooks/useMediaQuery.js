import { useEffect, useState } from 'react';

/**
 * Subscribes to a CSS media query from JavaScript.
 *
 * Used where a layout difference is not just a matter of styling - the mobile
 * navigation is a separate element that mounts and unmounts so it can animate,
 * where the desktop one is always there.
 */
export default function useMediaQuery(query) {
  const [matches, setMatches] = useState(
    () => typeof window !== 'undefined' && window.matchMedia(query).matches,
  );

  useEffect(() => {
    const list = window.matchMedia(query);
    const onChange = (event) => setMatches(event.matches);
    setMatches(list.matches);
    list.addEventListener('change', onChange);
    return () => list.removeEventListener('change', onChange);
  }, [query]);

  return matches;
}
