import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { moderationApi } from '../../api/endpoints';
import { toMessage } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import Loader from '../../components/Loader';
import EmptyState from '../../components/EmptyState';
import Modal from '../../components/Modal';
import { formatDateTime } from '../../utils/format';

const STATUSES = [
  { value: 'OPEN', label: 'Open' },
  { value: 'UNDER_REVIEW', label: 'Under review' },
  { value: 'ACTION_TAKEN', label: 'Action taken' },
  { value: 'DISMISSED', label: 'Dismissed' },
];

export default function ModerationTab() {
  const toast = useToast();
  const [reports, setReports] = useState(null);
  const [verifications, setVerifications] = useState([]);
  const [highRisk, setHighRisk] = useState([]);
  const [audit, setAudit] = useState(null);
  const [filter, setFilter] = useState('OPEN');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(null);
  const [decision, setDecision] = useState({ status: 'ACTION_TAKEN', moderatorNote: '', hideListing: false, banUser: false });

  const load = useCallback(async () => {
    try {
      const [reportPage, pending, risky, auditPage] = await Promise.all([
        moderationApi.reports({ status: filter || undefined, page: 0, size: 20 }),
        moderationApi.verifications(),
        moderationApi.highRisk(),
        moderationApi.audit({ page: 0, size: 15 }),
      ]);
      setReports(reportPage);
      setVerifications(pending);
      setHighRisk(risky);
      setAudit(auditPage);
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  useEffect(() => {
    load();
  }, [load]);

  async function resolve() {
    try {
      await moderationApi.resolve(working.id, decision);
      toast.success('Report resolved');
      setWorking(null);
      setDecision({ status: 'ACTION_TAKEN', moderatorNote: '', hideListing: false, banUser: false });
      load();
    } catch (error) {
      toast.error(toMessage(error));
    }
  }

  async function decideVerification(user, approve) {
    try {
      await moderationApi.decideVerification(user.id, approve, approve ? 'Documents checked' : 'Could not verify');
      toast.success(approve ? `${user.name} verified` : 'Verification rejected');
      load();
    } catch (error) {
      toast.error(toMessage(error));
    }
  }

  async function toggleHidden(room) {
    try {
      await moderationApi.setHidden(room.id, !room.hidden, 'Flagged by risk score');
      toast.success(room.hidden ? 'Listing restored' : 'Listing hidden from search');
      load();
    } catch (error) {
      toast.error(toMessage(error));
    }
  }

  if (loading) return <Loader label="Loading the queue..." />;

  return (
    <div className="stack" style={{ gap: '2rem' }}>
      {/* ------------------------------------------------ verification queue */}
      <section>
        <h2>Landlords waiting to be verified</h2>
        <p className="small muted">
          A verified badge is the strongest signal a nervous newcomer has. Check the documents,
          then approve or reject.
        </p>
        {verifications.length === 0 ? (
          <EmptyState title="Nobody waiting" description="Every landlord has been dealt with." />
        ) : (
          <div className="stack">
            {verifications.map((user) => (
              <div key={user.id} className="card">
                <div className="card__body row row--between">
                  <div>
                    <strong>{user.name}</strong>
                    <div className="small muted">{user.email} · {user.nationality || 'unknown origin'}</div>
                    {user.bio && <div className="small" style={{ marginTop: '0.3rem' }}>{user.bio}</div>}
                  </div>
                  <div className="row">
                    <button type="button" className="btn btn--sm btn--success"
                            onClick={() => decideVerification(user, true)}>
                      Approve
                    </button>
                    <button type="button" className="btn btn--sm btn--ghost"
                            onClick={() => decideVerification(user, false)}>
                      Reject
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ------------------------------------------------------------ reports */}
      <section>
        <div className="row row--between" style={{ marginBottom: '0.6rem' }}>
          <h2 style={{ margin: 0 }}>Reports</h2>
          <div className="chip-row">
            <button type="button" className={`chip ${filter === '' ? 'is-active' : ''}`}
                    onClick={() => setFilter('')}>
              All
            </button>
            {STATUSES.map((status) => (
              <button key={status.value} type="button"
                      className={`chip ${filter === status.value ? 'is-active' : ''}`}
                      onClick={() => setFilter(status.value)}>
                {status.label}
              </button>
            ))}
          </div>
        </div>

        {reports?.content?.length === 0 ? (
          <EmptyState title="Nothing reported" description="Quiet is good." />
        ) : (
          <div className="stack">
            {reports?.content?.map((report) => (
              <article key={report.id}
                       className={`report-card ${report.status !== 'OPEN' ? 'report-card--resolved' : ''}`}>
                <div className="row row--between">
                  <div>
                    <strong>{report.reasonLabel}</strong>
                    {report.roomId && (
                      <span className="small muted">
                        {' '}on{' '}
                        <Link to={`/rooms/${report.roomId}`}>{report.roomTitle}</Link>
                        {report.roomRiskScore != null && ` · risk ${report.roomRiskScore}`}
                      </span>
                    )}
                  </div>
                  <span className="badge badge--neutral">{report.status.replace('_', ' ')}</span>
                </div>

                {report.details && <p className="small" style={{ marginTop: '0.5rem' }}>{report.details}</p>}

                <div className="small muted">
                  Reported by {report.reporter?.name || 'a user'} on {formatDateTime(report.createdAt)}
                  {report.handledBy && ` · handled by ${report.handledBy}`}
                </div>

                {report.moderatorNote && (
                  <div className="small" style={{ marginTop: '0.4rem' }}>
                    <strong>Note:</strong> {report.moderatorNote}
                  </div>
                )}

                {report.status === 'OPEN' && (
                  <div className="row" style={{ marginTop: '0.7rem' }}>
                    <button type="button" className="btn btn--sm btn--primary"
                            onClick={() => setWorking(report)}>
                      Resolve
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>

      {/* -------------------------------------------------------- risky rooms */}
      <section>
        <h2>Listings the scam scorer flagged</h2>
        <p className="small muted">
          Scored automatically on the patterns that target newcomers. A high score is a reason
          to look, not proof of anything.
        </p>
        {highRisk.length === 0 ? (
          <EmptyState title="Nothing flagged" description="No listing scored above the threshold." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Listing</th>
                  <th>City</th>
                  <th>Landlord</th>
                  <th>Risk</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {highRisk.map((room) => (
                  <tr key={room.id}>
                    <td><Link to={`/rooms/${room.id}`}>{room.title}</Link></td>
                    <td>{room.city}</td>
                    <td className="small">{room.landlordName}</td>
                    <td>
                      <span className={`badge ${room.riskScore >= 55 ? 'badge--danger' : 'badge--warning'}`}>
                        {room.riskScore}
                      </span>
                    </td>
                    <td>
                      <button type="button" className="btn btn--sm btn--ghost"
                              onClick={() => toggleHidden(room)}>
                        {room.hidden ? 'Restore' : 'Hide'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* ------------------------------------------------------------- audit */}
      <section>
        <h2>What moderators have done</h2>
        <p className="small muted">
          Every decision is recorded. Moderation without a trail is just an unaccountable
          delete button.
        </p>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>When</th>
                <th>Who</th>
                <th>Action</th>
                <th>Target</th>
                <th>Reason</th>
              </tr>
            </thead>
            <tbody>
              {audit?.content?.map((entry) => (
                <tr key={entry.id}>
                  <td className="small">{formatDateTime(entry.createdAt)}</td>
                  <td className="small">{entry.moderator}</td>
                  <td><span className="badge badge--neutral">{entry.action.replace(/_/g, ' ')}</span></td>
                  <td className="small muted">{entry.targetType} #{entry.targetId}</td>
                  <td className="small">{entry.reason || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <Modal
        open={Boolean(working)}
        title="Resolve this report"
        onClose={() => setWorking(null)}
        footer={
          <>
            <button type="button" className="btn btn--ghost" onClick={() => setWorking(null)}>
              Cancel
            </button>
            <button type="button" className="btn btn--primary" onClick={resolve}>
              Save decision
            </button>
          </>
        }
      >
          <div className="stack">
            <div className="field">
              <label htmlFor="status">Outcome</label>
              <select id="status" value={decision.status}
                      onChange={(event) => setDecision({ ...decision, status: event.target.value })}>
                {STATUSES.map((status) => (
                  <option key={status.value} value={status.value}>{status.label}</option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="note">Note for the record</label>
              <textarea id="note" rows={3} value={decision.moderatorNote}
                        onChange={(event) => setDecision({ ...decision, moderatorNote: event.target.value })} />
            </div>
            <label className="checkbox">
              <input type="checkbox" checked={decision.hideListing}
                     onChange={(event) => setDecision({ ...decision, hideListing: event.target.checked })} />
              Hide the listing from search
            </label>
            <label className="checkbox">
              <input type="checkbox" checked={decision.banUser}
                     onChange={(event) => setDecision({ ...decision, banUser: event.target.checked })} />
              Disable the account behind it
            </label>
          </div>
      </Modal>
    </div>
  );
}
