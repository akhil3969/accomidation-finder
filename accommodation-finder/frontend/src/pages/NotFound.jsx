import { Link } from 'react-router-dom';
import useDocumentTitle from '../hooks/useDocumentTitle';
import PageTransition from '../components/PageTransition';

export default function NotFound() {
  useDocumentTitle('Page not found');

  return (
    <PageTransition className="page page--narrow">
      <div className="empty-state">
        <div className="empty-state__icon" aria-hidden="true">
          &#128269;
        </div>
        <h1>That page does not exist</h1>
        <p className="muted">The link may be out of date, or the listing was removed.</p>
        <Link to="/" className="btn btn--primary">
          Back to search
        </Link>
      </div>
    </PageTransition>
  );
}
