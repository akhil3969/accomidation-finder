import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { newcomerApi, reportApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import Modal from './Modal';

const TONE = { high: 'high', caution: 'caution', normal: 'normal' };
const HEADLINE = {
  high: 'Several things about this listing look wrong',
  caution: 'A couple of things here are worth checking',
  normal: 'Nothing about this listing looks unusual',
};

/**
 * The safety read on a listing, plus the report button.
 *
 * Deliberately shows reassurances as well as warnings. A panel that only ever
 * says "be careful" gets ignored within a week, and then it is worse than useless
 * on the listing that genuinely is a scam.
 */
export default function SafetyPanel({ roomId }) {
  const { isAuthenticated } = useAuth();
  const toast = useToast();

  const [report, setReport] = useState(null);
  const [reasons, setReasons] = useState([]);
  const [reporting, setReporting] = useState(false);
  const [sending, setSending] = useState(false);
  const [form, setForm] = useState({ reason: 'SUSPECTED_SCAM', details: '' });

  useEffect(() => {
    let cancelled = false;
    newcomerApi
      .safety(roomId)
      .then((data) => {
        if (!cancelled) setReport(data);
      })
      .catch(() => {});
    reportApi
      .reasons()
      .then((data) => {
        if (!cancelled) setReasons(data);
      })
      .catch(() => {
        if (!cancelled) setReasons([]);
      });
    return () => {
      cancelled = true;
    };
  }, [roomId]);

  async function submitReport(event) {
    event.preventDefault();
    if (sending) return;
    setSending(true);
    try {
      await reportApi.submit({ roomId, reason: form.reason, details: form.details });
      toast.success('Thank you. Our team will look at this listing.');
      setReporting(false);
      setForm({ reason: 'SUSPECTED_SCAM', details: '' });
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setSending(false);
    }
  }

  // A partial or unexpected payload should not take the listing page down
  // with it - the panel simply has nothing to add.
  if (!report || typeof report !== 'object' || Array.isArray(report)) return null;

  const warnings = report.warnings || [];
  const reassurances = report.reassurances || [];
  const rules = report.rulesToRemember || [];
  const level = TONE[report.riskLevel] || 'normal';
  const METER_COLOR = {
    high: 'var(--danger)',
    caution: 'var(--warning)',
    normal: 'var(--success)',
  };

  return (
    <section className="card">
      <div className="card__header">
        <h3>Is this listing safe?</h3>
        {report.landlordVerified && <span className="badge badge--success">Landlord verified</span>}
      </div>

      <div className="card__body stack">
        <div className={`safety-banner safety-banner--${level}`}>
          <span className="safety-banner__icon" aria-hidden="true">
            {level === 'high' ? '⚠' : level === 'caution' ? '?' : '✓'}
          </span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <strong>{HEADLINE[report.riskLevel]}</strong>
            <div
              className="risk-meter"
              role="meter"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={report.riskScore}
              aria-label="Risk score"
            >
              {/* The bar grows into place rather than appearing at full width,
                  which makes a high score register as a high score. */}
              <motion.div
                className="risk-meter__fill"
                style={{ background: METER_COLOR[level] }}
                initial={{ width: 0 }}
                animate={{ width: `${Math.max(report.riskScore || 0, 4)}%` }}
                transition={{ duration: 0.4, ease: [0.22, 0.61, 0.36, 1] }}
              />
            </div>
            <span className="small muted">Risk score {report.riskScore} out of 100</span>
          </div>
        </div>

        {warnings.length > 0 && (
          <div>
            <h4 style={{ fontSize: '0.9rem', margin: '0 0 0.3rem' }}>What we noticed</h4>
            <ul className="note-list block-list">
              {warnings.map((warning) => (
                <li key={warning}>{warning}</li>
              ))}
            </ul>
          </div>
        )}

        {reassurances.length > 0 && (
          <div>
            <h4 style={{ fontSize: '0.9rem', margin: '0 0 0.3rem' }}>In its favour</h4>
            <ul className="note-list check-list">
              {reassurances.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
        )}

        <details>
          <summary className="small" style={{ cursor: 'pointer', fontWeight: 600 }}>
            Rules that keep you safe anywhere, not just here
          </summary>
          <ul className="note-list" style={{ marginTop: '0.6rem' }}>
            {rules.map((rule) => (
              <li key={rule}>{rule}</li>
            ))}
          </ul>
        </details>

        <button
          type="button"
          className="btn btn--ghost btn--sm"
          onClick={() => (isAuthenticated ? setReporting(true) : toast.info('Sign in to report a listing'))}
        >
          Report this listing
        </button>
      </div>

      <Modal
        open={reporting}
        title="Report this listing"
        onClose={() => setReporting(false)}
        footer={
          <>
            <button type="button" className="btn btn--ghost" onClick={() => setReporting(false)}>
              Cancel
            </button>
            <button type="submit" form="report-form" className="btn btn--danger" disabled={sending}>
              {sending ? 'Sending…' : 'Send report'}
            </button>
          </>
        }
      >
          <form id="report-form" className="stack" onSubmit={submitReport}>
            <p className="small muted" style={{ margin: 0 }}>
              Reports go to our moderation team. Nothing is shared with the landlord, and you
              will not be identified to them.
            </p>
            <div className="field">
              <label htmlFor="reason">What is wrong?</label>
              <select
                id="reason"
                value={form.reason}
                onChange={(event) => setForm({ ...form, reason: event.target.value })}
              >
                {reasons.map((reason) => (
                  <option key={reason.value} value={reason.value}>
                    {reason.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="details">Anything else we should know?</label>
              <textarea
                id="details"
                rows={4}
                placeholder="What happened, and when"
                value={form.details}
                onChange={(event) => setForm({ ...form, details: event.target.value })}
              />
            </div>
          </form>
      </Modal>
    </section>
  );
}
