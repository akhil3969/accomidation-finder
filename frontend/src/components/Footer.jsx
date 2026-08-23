const YEAR = new Date().getFullYear();

export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer__inner">
        <span>
          &copy; {YEAR} AccomFinder &middot; real-time accommodation search, built with Spring Boot,
          WebSocket and React.
        </span>
        <span className="row" style={{ gap: '1rem' }}>
          <a
            href="https://github.com/akhil3969/accommodation-finder"
            target="_blank"
            rel="noreferrer"
          >
            Source on GitHub
          </a>
          <a href="/swagger-ui.html" target="_blank" rel="noreferrer">
            API docs
          </a>
        </span>
      </div>
    </footer>
  );
}
