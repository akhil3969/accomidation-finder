import { useEffect, useState } from 'react';
import { prefersDark } from './palette';

/** Re-renders the charts when the OS flips between light and dark. */
export default function useChartTheme() {
  const [dark, setDark] = useState(prefersDark);

  useEffect(() => {
    if (!window.matchMedia) return undefined;
    const query = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (event) => setDark(event.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  return {
    dark,
    ink: dark ? '#ffffff' : '#0b0b0b',
    inkMuted: '#898781',
    grid: dark ? '#2c2c2a' : '#e1e0d9',
    axis: dark ? '#383835' : '#c3c2b7',
    surface: dark ? '#151a30' : '#ffffff',
  };
}
