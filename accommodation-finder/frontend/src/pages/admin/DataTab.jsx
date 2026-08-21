import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi, moderationApi } from '../../api/endpoints';
import { toMessage } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { useConfirm } from '../../context/ConfirmContext';
import Loader from '../../components/Loader';
import Pagination from '../../components/Pagination';
import { formatDate, formatPrice } from '../../utils/format';

/** Browse and manage every account and listing without opening the database. */
export default function DataTab() {
  const toast = useToast();
  const confirm = useConfirm();
  const [users, setUsers] = useState([]);
  const [rooms, setRooms] = useState(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [section, setSection] = useState('users');

  const load = useCallback(async () => {
    try {
      const [userList, roomPage] = await Promise.all([
        adminApi.users(),
        adminApi.rooms({ page, size: 25 }),
      ]);
      setUsers(userList);
      setRooms(roomPage);
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  async function act(promise, message) {
    try {
      await promise;
      toast.success(message);
      load();
    } catch (error) {
      toast.error(toMessage(error));
    }
  }

  if (loading) return <Loader label="Loading data..." />;

  const needle = search.trim().toLowerCase();
  const visibleUsers = needle
    ? users.filter((user) =>
        [user.name, user.email, user.nationality].some((field) =>
          (field || '').toLowerCase().includes(needle),
        ))
    : users;

  return (
    <div className="stack" style={{ gap: '1.25rem' }}>
      <div className="row row--between">
        <div className="chip-row">
          <button type="button" className={`chip ${section === 'users' ? 'is-active' : ''}`}
                  onClick={() => setSection('users')}>
            People ({users.length})
          </button>
          <button type="button" className={`chip ${section === 'rooms' ? 'is-active' : ''}`}
                  onClick={() => setSection('rooms')}>
            Listings ({rooms?.totalElements ?? 0})
          </button>
        </div>

        <div className="row">
          {section === 'rooms' && (
            <button type="button" className="btn btn--sm btn--ghost"
                    onClick={() => act(adminApi.rescoreAll(), 'Every listing re-scored')}>
              Re-run scam scoring
            </button>
          )}
          <input type="search" placeholder="Filter" value={search} style={{ maxWidth: 220 }}
                 onChange={(event) => setSearch(event.target.value)} />
        </div>
      </div>

      {section === 'users' && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>From</th>
                <th>Role</th>
                <th>Verified</th>
                <th>Listings</th>
                <th>Joined</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visibleUsers.map((user) => (
                <tr key={user.id} style={{ opacity: user.enabled ? 1 : 0.5 }}>
                  <td>{user.name}</td>
                  <td className="small muted">{user.email}</td>
                  <td className="small">{user.nationality || '-'}</td>
                  <td>
                    <select
                      value={user.role}
                      onChange={(event) =>
                        act(adminApi.setRole(user.id, event.target.value), 'Role updated')}
                      style={{ padding: '0.2rem 0.4rem', fontSize: '0.8rem' }}
                    >
                      <option value="TENANT">Tenant</option>
                      <option value="LANDLORD">Landlord</option>
                      <option value="ADMIN">Admin</option>
                    </select>
                  </td>
                  <td>
                    {/* An account that has never been through verification has
                        no status at all, which used to throw here and take the
                        whole admin table down. */}
                    <span className={`badge ${user.verificationStatus === 'VERIFIED' ? 'badge--success' : 'badge--neutral'}`}>
                      {(user.verificationStatus || 'UNVERIFIED').replace(/_/g, ' ').toLowerCase()}
                    </span>
                  </td>
                  <td style={{ fontVariantNumeric: 'tabular-nums' }}>{user.listingCount}</td>
                  <td className="small">{formatDate(user.createdAt)}</td>
                  <td>
                    <div className="row" style={{ gap: '0.3rem' }}>
                      <button type="button" className="btn btn--sm btn--ghost"
                              onClick={() => act(adminApi.setEnabled(user.id, !user.enabled),
                                user.enabled ? 'Account disabled' : 'Account enabled')}>
                        {user.enabled ? 'Disable' : 'Enable'}
                      </button>
                      <button type="button" className="btn btn--sm btn--danger"
                              onClick={async () => {
                                const agreed = await confirm({
                                  title: 'Delete this account?',
                                  message: `${user.name} and everything they created will be removed.`,
                                  detail: 'Listings, bookings and messages go with the account. This cannot be undone.',
                                  confirmLabel: 'Delete account',
                                  tone: 'danger',
                                });
                                if (agreed) {
                                  act(adminApi.deleteUser(user.id), 'Account deleted');
                                }
                              }}>
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {section === 'rooms' && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>City</th>
                  <th>Rent</th>
                  <th>Landlord</th>
                  <th>State</th>
                  <th>Risk</th>
                  <th>Reports</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rooms?.content
                  ?.filter((room) => !needle || room.title.toLowerCase().includes(needle)
                    || (room.city || '').toLowerCase().includes(needle))
                  .map((room) => (
                    <tr key={room.id} style={{ opacity: room.hidden ? 0.5 : 1 }}>
                      <td><Link to={`/rooms/${room.id}`}>{room.title}</Link></td>
                      <td>{room.city}</td>
                      <td style={{ fontVariantNumeric: 'tabular-nums' }}>{formatPrice(room.price)}</td>
                      <td className="small">{room.landlordName}</td>
                      <td>
                        <div className="row" style={{ gap: '0.25rem' }}>
                          {room.hidden && <span className="badge badge--danger">hidden</span>}
                          {room.verified && <span className="badge badge--success">verified</span>}
                          {!room.hidden && !room.verified && (
                            <span className="badge badge--neutral">live</span>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className={`badge ${
                          room.riskScore >= 55 ? 'badge--danger'
                            : room.riskScore >= 25 ? 'badge--warning' : 'badge--neutral'}`}>
                          {room.riskScore ?? 0}
                        </span>
                      </td>
                      <td style={{ fontVariantNumeric: 'tabular-nums' }}>{room.reportCount}</td>
                      <td>
                        <div className="row" style={{ gap: '0.3rem' }}>
                          <button type="button" className="btn btn--sm btn--subtle"
                                  onClick={() => act(
                                    moderationApi.setVerified(room.id, !room.verified),
                                    room.verified ? 'Verification removed' : 'Listing verified')}>
                            {room.verified ? 'Unverify' : 'Verify'}
                          </button>
                          <button type="button" className="btn btn--sm btn--ghost"
                                  onClick={() => act(
                                    moderationApi.setHidden(room.id, !room.hidden, 'Admin action'),
                                    room.hidden ? 'Listing restored' : 'Listing hidden')}>
                            {room.hidden ? 'Restore' : 'Hide'}
                          </button>
                          <button type="button" className="btn btn--sm btn--danger"
                                  onClick={async () => {
                                    const agreed = await confirm({
                                      title: 'Delete this listing?',
                                      message: `"${room.title}" will be removed for everyone.`,
                                      confirmLabel: 'Delete listing',
                                      tone: 'danger',
                                    });
                                    if (agreed) {
                                      act(adminApi.deleteRoom(room.id), 'Listing deleted');
                                    }
                                  }}>
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
          <Pagination page={rooms?.page ?? 0} totalPages={rooms?.totalPages ?? 0} onChange={setPage} />
        </>
      )}
    </div>
  );
}
