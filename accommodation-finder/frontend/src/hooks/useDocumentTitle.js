import { useEffect } from 'react';

export default function useDocumentTitle(title) {
  useEffect(() => {
    const previous = document.title;
    document.title = title ? `${title} - AccomFinder` : 'AccomFinder';
    return () => {
      document.title = previous;
    };
  }, [title]);
}
