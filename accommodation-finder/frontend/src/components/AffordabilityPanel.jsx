import { useEffect, useState } from 'react';
import { aidApi } from '../api/endpoints';
import Collapse from './Collapse';
import { formatPrice } from '../utils/format';

/**
 * What this flat actually costs, once housing benefit is counted.
 *
 * The advertised rent is the number everyone compares on, and for a student in
 * France it is close to meaningless - the state pays a large slice of it. Showing
 * the real figure changes which flats someone even considers.
 */
export default function AffordabilityPanel({ roomId, compact = false }) {
  const [estimate, setEstimate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showWorking, setShowWorking] = useState(false);

  useEffect(() => {
    let cancelled = false;
    aidApi
      .forRoom(roomId)
      .then((data) => {
        if (!cancelled) setEstimate(data);
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [roomId]);

  if (loading) {
    return <div className="skeleton" style={{ height: compact ? 60 : 150 }} />;
  }
  if (!estimate || typeof estimate !== 'object' || Array.isArray(estimate)) return null;

  if (!estimate.likelyEligible) {
    return (
      <div className="aid-panel">
        <div className="small muted">
          Housing benefit is unlikely to make much difference on this one, so budget the full
          rent. It still costs nothing to check on the official simulator.
        </div>
        <a className="btn btn--sm btn--ghost" style={{ marginTop: '0.6rem' }}
           href={estimate.officialSimulatorUrl} target="_blank" rel="noreferrer">
          Check officially
        </a>
      </div>
    );
  }

  return (
    <div className="aid-panel">
      <div className="small muted" style={{ marginBottom: '0.2rem' }}>
        What you would actually pay
      </div>

      <div className="aid-headline">
        <span className="aid-headline__was">{formatPrice(estimate.monthlyRent)}</span>
        <span className="aid-headline__now">{formatPrice(estimate.effectiveMonthlyCost)}</span>
        <span className="aid-headline__unit">/ month</span>
      </div>

      <p className="small" style={{ margin: '0.5rem 0 0' }}>
        Housing benefit should cover roughly{' '}
        <strong>
          {formatPrice(estimate.estimateLow)} to {formatPrice(estimate.estimateHigh)}
        </strong>{' '}
        a month. That is money from the state, not a discount from the landlord - you apply for
        it yourself after you move in.
      </p>

      {!compact && (
        <>
          <button
            type="button"
            className="btn btn--link small"
            style={{ marginTop: '0.6rem' }}
            onClick={() => setShowWorking((value) => !value)}
            aria-expanded={showWorking}
          >
            {showWorking ? 'Hide the working' : 'Show me how this is worked out'}
          </button>

          <Collapse open={showWorking}>
            <div className="aid-steps">
              {(estimate.breakdown || []).map((step) => (
                <div key={step.label} className="aid-step">
                  <span className="aid-step__label">{step.label}</span>
                  <span className="aid-step__value">{step.value}</span>
                  <span className="aid-step__why">{step.explanation}</span>
                </div>
              ))}
            </div>

            <ul className="note-list" style={{ marginTop: '0.9rem' }}>
              {(estimate.notes || []).map((note) => (
                <li key={note}>{note}</li>
              ))}
            </ul>

            <p className="small muted" style={{ marginTop: '0.75rem', marginBottom: 0 }}>
              Based on {estimate.zoneLabel}. Official figures last checked{' '}
              {estimate.parametersVerifiedOn}.{' '}
              <a href={estimate.officialSimulatorUrl} target="_blank" rel="noreferrer">
                Confirm on the government simulator
              </a>
              .
            </p>
          </Collapse>
        </>
      )}
    </div>
  );
}
