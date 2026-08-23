import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { newcomerApi } from '../api/endpoints';
import useDocumentTitle from '../hooks/useDocumentTitle';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import PageTransition from '../components/PageTransition';

/**
 * Renders the guide body.
 *
 * The bodies are stored as light markdown so an admin can edit them without
 * touching code. Rather than pulling in a markdown library for headings, bold and
 * paragraphs, the three cases are handled here - and because it builds React
 * elements rather than setting innerHTML, an admin cannot accidentally (or
 * deliberately) inject script into a published page.
 */
function renderBody(body) {
  if (!body) return null;

  return body
    .split(/\n\s*\n/)
    .map((block) => block.trim())
    .filter(Boolean)
    .map((block, index) => {
      if (block.startsWith('## ')) {
        return <h2 key={index}>{block.slice(3)}</h2>;
      }
      const parts = block.split(/(\*\*[^*]+\*\*)/g).filter(Boolean);
      return (
        <p key={index}>
          {parts.map((part, partIndex) =>
            part.startsWith('**') && part.endsWith('**') ? (
              <strong key={partIndex}>{part.slice(2, -2)}</strong>
            ) : (
              <span key={partIndex}>{part}</span>
            ),
          )}
        </p>
      );
    });
}

export default function GuideDetail() {
  const { slug } = useParams();
  const [guide, setGuide] = useState(null);
  const [loading, setLoading] = useState(true);

  useDocumentTitle(guide?.title);

  useEffect(() => {
    setLoading(true);
    newcomerApi.guide(slug).then(setGuide).catch(() => setGuide(null)).finally(() => setLoading(false));
  }, [slug]);

  if (loading) return <Loader label="Loading..." />;

  if (!guide) {
    return (
      <PageTransition className="page page--narrow">
        <EmptyState
          title="We could not find that guide"
          action={<Link to="/guides" className="btn btn--primary">All guides</Link>}
        />
      </PageTransition>
    );
  }

  return (
    <PageTransition className="page page--narrow">
      <Link to="/guides" className="small">&larr; All guides</Link>

      <div className="page-header" style={{ marginTop: '0.75rem' }}>
        <div>
          <span className="badge badge--neutral">{guide.category}</span>
          <h1 style={{ marginTop: '0.5rem' }}>{guide.title}</h1>
          <p>{guide.summary}</p>
        </div>
      </div>

      <article className="card">
        <div className="card__body prose">{renderBody(guide.body)}</div>
        {(guide.officialLink || guide.lastVerified) && (
          <div className="card__footer" style={{ justifyContent: 'space-between' }}>
            <span className="small muted">
              {guide.lastVerified ? `Checked against the official source on ${guide.lastVerified}` : ''}
            </span>
            {guide.officialLink && (
              <a className="btn btn--sm btn--ghost" href={guide.officialLink}
                 target="_blank" rel="noreferrer">
                Official page
              </a>
            )}
          </div>
        )}
      </article>
    </PageTransition>
  );
}
