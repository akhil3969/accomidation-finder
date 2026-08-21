import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { newcomerApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import { itemVariants, listVariants } from '../motion/tokens';
import PageTransition from '../components/PageTransition';

/**
 * The paperwork, made visible.
 *
 * French landlords all want the same bundle of documents, and nobody writes the
 * list down because everyone who grew up here already knows it. Arriving without
 * it means losing flats for reasons nobody explains.
 */
export default function Checklist() {
  useDocumentTitle('Your documents');

  const toast = useToast();
  const [view, setView] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    try {
      setView(await newcomerApi.checklist());
      setError(null);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function toggle(entry) {
    if (busy) return;
    setBusy(entry.itemId);

    // Tick the box now. Waiting for the round trip made every tick feel like
    // the click had missed.
    const optimistic = !entry.done;
    setView((current) =>
      current
        ? {
            ...current,
            completedItems: current.completedItems + (optimistic ? 1 : -1),
            entries: current.entries.map((item) =>
              item.itemId === entry.itemId ? { ...item, done: optimistic } : item,
            ),
          }
        : current,
    );

    try {
      setView(await newcomerApi.toggleChecklist(entry.itemId, optimistic, entry.note));
    } catch (requestError) {
      toast.error(toMessage(requestError));
      load();
    } finally {
      setBusy(null);
    }
  }

  if (loading) return <Loader label="Loading your checklist…" />;

  if (error || !view) {
    return (
      <PageTransition className="page page--narrow">
        <EmptyState
          icon="&#9888;"
          title="Could not load your checklist"
          description={error || 'Please try again.'}
          action={
            <button type="button" className="btn btn--primary" onClick={load}>
              Try again
            </button>
          }
        />
      </PageTransition>
    );
  }

  if (view.entries.length === 0) {
    return (
      <PageTransition className="page page--narrow">
        <EmptyState
          title="Nothing here yet"
          description="Tell us a little about yourself and we will build your list."
          action={<Link to="/start" className="btn btn--primary">Set up your profile</Link>}
        />
      </PageTransition>
    );
  }

  const percentage = Math.round((view.completedItems / view.totalItems) * 100);
  const categories = [...new Set(view.entries.map((entry) => entry.category))];

  return (
    <PageTransition className="page page--narrow">
      <div className="page-header">
        <div>
          <h1>What you need before you can rent</h1>
          <p>
            This is the list every French landlord expects. Nobody writes it down, which is
            exactly why it costs newcomers flats.
          </p>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div className="card__body">
          <div className="row row--between" style={{ marginBottom: '0.5rem' }}>
            <strong>
              {view.completedItems} of {view.totalItems} done
            </strong>
            <span className="small muted">{percentage}%</span>
          </div>
          <div className="progress-bar" role="progressbar" aria-valuenow={percentage}
               aria-valuemin={0} aria-valuemax={100}>
            <motion.div
              className="progress-bar__fill"
              initial={false}
              animate={{ width: `${percentage}%` }}
              transition={{ duration: 0.42, ease: [0.22, 0.61, 0.36, 1] }}
            />
          </div>
          <p className="small" style={{ margin: '0.7rem 0 0' }}>{view.encouragement}</p>
        </div>
      </div>

      {categories.map((category) => (
        <section key={category} style={{ marginBottom: '1.75rem' }}>
          <h2 style={{ fontSize: '1.05rem' }}>{category}</h2>
          <motion.div
            className="stack"
            variants={listVariants}
            initial="initial"
            whileInView="animate"
            viewport={{ once: true, margin: '-40px' }}
          >
            {view.entries
              .filter((entry) => entry.category === category)
              .map((entry) => (
                <motion.article
                  key={entry.itemId}
                  layout
                  variants={itemVariants}
                  className={`checklist-item ${entry.done ? 'is-done' : ''}`}
                >
                  <button
                    type="button"
                    className={`checklist-check ${entry.done ? 'is-done' : ''}`}
                    aria-pressed={entry.done}
                    aria-label={entry.done ? `Mark ${entry.title} as not done` : `Mark ${entry.title} as done`}
                    disabled={busy === entry.itemId}
                    onClick={() => toggle(entry)}
                  >
                    ✓
                  </button>

                  <div>
                    <div className="row" style={{ gap: '0.4rem' }}>
                      <span className="checklist-item__title">{entry.title}</span>
                      {entry.essential && !entry.done && (
                        <span className="badge badge--warning">Essential</span>
                      )}
                    </div>

                    <p className="checklist-item__body" style={{ margin: '0.25rem 0 0' }}>
                      {entry.explanation}
                    </p>

                    {entry.ifYouCannot && !entry.done && (
                      <div className="checklist-item__fallback">
                        <strong>If you cannot: </strong>
                        {entry.ifYouCannot}
                      </div>
                    )}

                    {entry.officialLink && (
                      <a className="small" href={entry.officialLink} target="_blank" rel="noreferrer"
                         style={{ display: 'inline-block', marginTop: '0.5rem' }}>
                        Official page
                      </a>
                    )}
                  </div>
                </motion.article>
              ))}
          </motion.div>
        </section>
      ))}

      <div className="card">
        <div className="card__body row row--between">
          <div>
            <strong>Not sure what any of this means?</strong>
            <div className="small muted">
              The guides explain each of these in plain language.
            </div>
          </div>
          <Link to="/guides" className="btn btn--ghost">Read the guides</Link>
        </div>
      </div>
    </PageTransition>
  );
}
