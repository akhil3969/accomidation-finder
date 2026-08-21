/** Compact numeric pagination that always shows at most seven buttons. */
export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;

  const windowSize = 5;
  let start = Math.max(0, page - Math.floor(windowSize / 2));
  const end = Math.min(totalPages, start + windowSize);
  start = Math.max(0, end - windowSize);
  const pages = Array.from({ length: end - start }, (_, index) => start + index);

  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="pagination__page"
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        aria-label="Previous page"
      >
        &#8249;
      </button>

      {start > 0 && (
        <button type="button" className="pagination__page" onClick={() => onChange(0)}>
          1
        </button>
      )}
      {start > 1 && (
        <span className="pagination__gap" aria-hidden="true">
          &hellip;
        </span>
      )}

      {pages.map((value) => (
        <button
          key={value}
          type="button"
          className={`pagination__page ${value === page ? 'is-active' : ''}`}
          onClick={() => onChange(value)}
          aria-current={value === page ? 'page' : undefined}
          aria-label={`Page ${value + 1}`}
        >
          {value + 1}
        </button>
      ))}

      {end < totalPages - 1 && (
        <span className="pagination__gap" aria-hidden="true">
          &hellip;
        </span>
      )}
      {end < totalPages && (
        <button type="button" className="pagination__page" onClick={() => onChange(totalPages - 1)}>
          {totalPages}
        </button>
      )}

      <button
        type="button"
        className="pagination__page"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="Next page"
      >
        &#8250;
      </button>
    </nav>
  );
}
