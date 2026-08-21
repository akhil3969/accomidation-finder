import { useEffect, useState } from 'react';

/** Delays a rapidly changing value so we do not hit the API on every keystroke. */
export default function useDebouncedValue(value, delay = 400) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
