import { useCallback, useEffect, useState } from 'react';
import { contentApi } from '../../api/endpoints';
import { toMessage } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { useConfirm } from '../../context/ConfirmContext';
import Loader from '../../components/Loader';
import Modal from '../../components/Modal';

const EMPTY_GUIDE = {
  slug: '', title: '', summary: '', body: '', category: 'Practical', icon: 'reading', officialLink: '',
};

/**
 * Editing the words and the official figures.
 *
 * The aid parameters are the important half. Every one of them is set by decree
 * and changes yearly; when CAF publishes new ceilings in October, somebody fixes
 * them here and the estimates are correct again the same day.
 */
export default function ContentTab() {
  const toast = useToast();
  const confirm = useConfirm();
  const [guides, setGuides] = useState([]);
  const [items, setItems] = useState([]);
  const [parameters, setParameters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [section, setSection] = useState('parameters');
  const [editing, setEditing] = useState(null);
  const [drafts, setDrafts] = useState({});

  const load = useCallback(async () => {
    try {
      const [guideList, itemList, parameterList] = await Promise.all([
        contentApi.guides(),
        contentApi.checklist(),
        contentApi.aidParameters(),
      ]);
      setGuides(guideList);
      setItems(itemList);
      setParameters(parameterList);
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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

  async function saveGuide() {
    const payload = editing;
    await act(
      payload.id ? contentApi.updateGuide(payload.id, payload) : contentApi.createGuide(payload),
      payload.id ? 'Guide updated' : 'Guide created',
    );
    setEditing(null);
  }

  if (loading) return <Loader label="Loading content..." />;

  return (
    <div className="stack" style={{ gap: '1.25rem' }}>
      <div className="chip-row">
        <button type="button" className={`chip ${section === 'parameters' ? 'is-active' : ''}`}
                onClick={() => setSection('parameters')}>
          Official figures ({parameters.length})
        </button>
        <button type="button" className={`chip ${section === 'guides' ? 'is-active' : ''}`}
                onClick={() => setSection('guides')}>
          Guides ({guides.length})
        </button>
        <button type="button" className={`chip ${section === 'checklist' ? 'is-active' : ''}`}
                onClick={() => setSection('checklist')}>
          Checklist ({items.length})
        </button>
      </div>

      {/* ------------------------------------------------- official figures */}
      {section === 'parameters' && (
        <>
          <div className="card" style={{ background: 'var(--warning-soft)', border: 'none' }}>
            <div className="card__body">
              <strong>These numbers decide what every user is told they will pay.</strong>
              <p className="small" style={{ margin: '0.3rem 0 0' }}>
                They are set by decree and change at least once a year - CAF ceilings usually in
                October, Visale limits in January. Check them against the source link, correct
                anything that has moved, and saving marks it as verified today.
              </p>
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>What it is</th>
                  <th>Value</th>
                  <th>In force from</th>
                  <th>Last checked</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {parameters.map((parameter) => (
                  <tr key={parameter.id}>
                    <td>
                      {parameter.label}
                      <div className="small muted">{parameter.key}</div>
                    </td>
                    <td>
                      <input
                        type="number"
                        step="0.01"
                        defaultValue={parameter.value}
                        style={{ maxWidth: 110, fontVariantNumeric: 'tabular-nums' }}
                        onChange={(event) =>
                          setDrafts({ ...drafts, [parameter.id]: event.target.value })}
                      />
                    </td>
                    <td className="small">{parameter.effectiveFrom || '-'}</td>
                    <td className="small muted">{parameter.lastVerified || 'never'}</td>
                    <td>
                      <div className="row" style={{ gap: '0.3rem' }}>
                        <button
                          type="button"
                          className="btn btn--sm btn--primary"
                          disabled={drafts[parameter.id] === undefined}
                          onClick={() => act(
                            contentApi.updateAidParameter(parameter.id, {
                              ...parameter,
                              value: Number(drafts[parameter.id]),
                            }),
                            'Figure updated and marked as verified today')}
                        >
                          Save
                        </button>
                        {parameter.source && (
                          <a className="btn btn--sm btn--ghost" href={parameter.source}
                             target="_blank" rel="noreferrer">
                            Source
                          </a>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* ------------------------------------------------------------ guides */}
      {section === 'guides' && (
        <>
          <div className="row row--end">
            <button type="button" className="btn btn--accent btn--sm"
                    onClick={() => setEditing({ ...EMPTY_GUIDE })}>
              + New guide
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Category</th>
                  <th>Checked</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {guides.map((guide) => (
                  <tr key={guide.id}>
                    <td>
                      {guide.title}
                      <div className="small muted">{guide.summary}</div>
                    </td>
                    <td><span className="badge badge--neutral">{guide.category}</span></td>
                    <td className="small muted">{guide.lastVerified || '-'}</td>
                    <td>
                      <div className="row" style={{ gap: '0.3rem' }}>
                        <button type="button" className="btn btn--sm btn--ghost"
                                onClick={() => setEditing(guide)}>
                          Edit
                        </button>
                        <button type="button" className="btn btn--sm btn--danger"
                                onClick={async () => {
                                  const agreed = await confirm({
                                    title: 'Delete this guide?',
                                    message: `"${guide.title}" will be removed for everyone.`,
                                    confirmLabel: 'Delete guide',
                                    tone: 'danger',
                                  });
                                  if (agreed) act(contentApi.deleteGuide(guide.id), 'Guide deleted');
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
        </>
      )}

      {/* --------------------------------------------------------- checklist */}
      {section === 'checklist' && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Document</th>
                <th>Category</th>
                <th>Shown to</th>
                <th>Essential</th>
                <th>Active</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>
                    {item.title}
                    <div className="small muted">{item.explanation?.slice(0, 90)}...</div>
                  </td>
                  <td><span className="badge badge--neutral">{item.category}</span></td>
                  <td className="small">
                    {item.nonEuOnly ? 'Non-EU only' : item.studentsOnly ? 'Students only' : 'Everyone'}
                  </td>
                  <td>{item.essential ? 'Yes' : 'No'}</td>
                  <td>
                    <button type="button" className="btn btn--sm btn--ghost"
                            onClick={() => act(
                              contentApi.updateItem(item.id, { ...item, active: !item.active }),
                              item.active ? 'Hidden from users' : 'Shown to users')}>
                      {item.active ? 'Active' : 'Hidden'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={Boolean(editing)}
        size="lg"
        title={editing?.id ? 'Edit guide' : 'New guide'}
        onClose={() => setEditing(null)}
        footer={
          <>
            <button type="button" className="btn btn--ghost" onClick={() => setEditing(null)}>
              Cancel
            </button>
            <button type="button" className="btn btn--primary" onClick={saveGuide}>
              Save
            </button>
          </>
        }
      >
        {/* Modal is always mounted so it can animate closed, so the body has to
            tolerate `editing` being null; Modal holds the last open render
            while it leaves. */}
        {editing && (
          <div className="stack">
            <div className="form-grid">
              <div className="field">
                <label htmlFor="g-title">Title</label>
                <input id="g-title" value={editing.title}
                       onChange={(event) => setEditing({ ...editing, title: event.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="g-slug">URL slug</label>
                <input id="g-slug" value={editing.slug}
                       onChange={(event) => setEditing({ ...editing, slug: event.target.value })} />
              </div>
            </div>
            <div className="field">
              <label htmlFor="g-summary">One-line summary</label>
              <input id="g-summary" value={editing.summary || ''}
                     onChange={(event) => setEditing({ ...editing, summary: event.target.value })} />
            </div>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="g-category">Category</label>
                <input id="g-category" value={editing.category || ''}
                       onChange={(event) => setEditing({ ...editing, category: event.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="g-link">Official link</label>
                <input id="g-link" value={editing.officialLink || ''}
                       onChange={(event) => setEditing({ ...editing, officialLink: event.target.value })} />
              </div>
            </div>
            <div className="field">
              <label htmlFor="g-body">Body</label>
              <textarea id="g-body" rows={12} value={editing.body || ''}
                        onChange={(event) => setEditing({ ...editing, body: event.target.value })} />
              <span className="field__hint">
                Blank line between paragraphs. **bold** works, and a line starting with ## is a heading.
              </span>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
