import { useEffect, useState } from 'react';
import { aidApi } from '../api/endpoints';
import Collapse from './Collapse';

/**
 * The guarantor question.
 *
 * Not having a French guarantor is the most common reason an international
 * student's application is rejected, and the free state scheme that solves it is
 * not widely known. Worth its own panel rather than a footnote.
 */
export default function VisalePanel({ roomId }) {
  const [check, setCheck] = useState(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    aidApi
      .visaleForRoom(roomId)
      .then((data) => {
        if (!cancelled) setCheck(data);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [roomId]);

  if (!check || typeof check !== 'object' || Array.isArray(check)) return null;

  const passed = check.passed || [];
  const blockers = check.blockers || [];
  const steps = check.howToApply || [];

  return (
    <div className="card">
      <div className="card__header">
        <div>
          <h3>Do you need a guarantor?</h3>
          <div className="small muted">Visale - the free state guarantee</div>
        </div>
        <span className={`badge ${check.eligible ? 'badge--success' : 'badge--warning'}`}>
          {check.eligible ? 'You qualify' : 'Check the details'}
        </span>
      </div>

      <div className="card__body stack">
        <p className="small" style={{ margin: 0 }}>{check.whyItMatters}</p>

        {passed.length > 0 && (
          <ul className="note-list check-list">
            {passed.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        )}

        {blockers.length > 0 && (
          <ul className="note-list block-list">
            {blockers.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        )}

        <button type="button" className="btn btn--link small" style={{ textAlign: 'left' }}
                onClick={() => setOpen((value) => !value)} aria-expanded={open}>
          {open ? 'Hide the steps' : 'How do I apply?'}
        </button>

        <Collapse open={open}>
          <ol className="stack" style={{ gap: '0.5rem', paddingLeft: '1.1rem', margin: 0 }}>
            {steps.map((step) => (
              <li key={step} className="small">{step}</li>
            ))}
          </ol>
        </Collapse>

        <a className="btn btn--ghost btn--block" href={check.officialUrl} target="_blank" rel="noreferrer">
          Apply on visale.fr
        </a>
      </div>
    </div>
  );
}
