import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { newcomerApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import useDocumentTitle from '../hooks/useDocumentTitle';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import { itemVariants, listVariants } from '../motion/tokens';
import PageTransition from '../components/PageTransition';

// The API sends a category key, not a glyph. The previous map turned several of
// them into bare letters ("A", "H", "1"), which read as placeholder text.
const ICON = {
  money: '💶',
  guarantor: '🤝',
  reading: '📄',
  safety: '🛡️',
  rights: '⚖️',
  calendar: '🗓️',
  housing: '🏠',
};

export default function Guides() {
  useDocumentTitle('Guides');

  const [guides, setGuides] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setGuides(await newcomerApi.guides());
    } catch (requestError) {
      // Swallowing this left an empty page with no explanation and no retry.
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <Loader label="Loading guides…" />;

  const categories = [...new Set(guides.map((guide) => guide.category))];

  return (
    <PageTransition className="page">
      <div className="page-header">
        <div>
          <h1>How any of this works</h1>
          <p>
            Written for someone who has never dealt with the French system before. No
            assumptions, no jargon left unexplained.
          </p>
        </div>
      </div>

      {error && (
        <EmptyState
          icon="&#9888;"
          title="Could not load the guides"
          description={error}
          action={
            <button type="button" className="btn btn--primary" onClick={load}>
              Try again
            </button>
          }
        />
      )}

      {!error && guides.length === 0 && (
        <EmptyState title="No guides published yet" description="Check back shortly." />
      )}

      {categories.map((category) => (
        <section key={category} style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.05rem' }}>{category}</h2>
          <motion.div
            className="guide-grid"
            variants={listVariants}
            initial="initial"
            whileInView="animate"
            viewport={{ once: true, margin: '-40px' }}
          >
            {guides
              .filter((guide) => guide.category === category)
              .map((guide) => (
                <motion.div key={guide.slug} variants={itemVariants} whileHover={{ y: -4 }}>
                  <Link to={`/guides/${guide.slug}`} className="guide-card">
                    <div className="guide-card__icon" aria-hidden="true">
                      {ICON[guide.icon] || '📘'}
                    </div>
                    <h3>{guide.title}</h3>
                    <p>{guide.summary}</p>
                  </Link>
                </motion.div>
              ))}
          </motion.div>
        </section>
      ))}
    </PageTransition>
  );
}
