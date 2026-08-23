import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { aidApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { compact, rules } from '../hooks/useForm';
import Field from '../components/Field';
import { ButtonSpinner } from '../components/Loader';
import { formatPrice } from '../utils/format';
import PageTransition from '../components/PageTransition';

/**
 * The estimator on its own, open to anyone.
 *
 * Somebody deciding whether they can afford to come to France at all should be
 * able to get an answer before they create an account. Gating this behind a
 * signup would be gating the most useful thing on the site.
 */
/**
 * Checked here rather than at the server, because a bad postcode comes back as
 * a zone lookup failure and a bad rent comes back as a nonsensical estimate -
 * neither of which tells the person which box to correct.
 */
function validate(values) {
  return compact({
    rent:
      rules.required(values.rent, 'A monthly rent') ||
      rules.number(values.rent, { min: 1, max: 100000, label: 'The rent' }),
    charges: rules.number(values.charges, { min: 0, max: 10000, label: 'Charges' }),
    postcode: rules.required(values.postcode, 'A postcode') || rules.frenchPostcode(values.postcode),
    age: rules.number(values.age, { min: 16, max: 99, label: 'Your age' }),
    monthlyIncome: rules.number(values.monthlyIncome, { min: 0, max: 100000, label: 'Income' }),
  });
}

export default function AidCalculator() {
  useDocumentTitle('What will rent really cost me?');

  const toast = useToast();
  const [form, setForm] = useState({
    rent: '700',
    charges: '',
    postcode: '75013',
    student: true,
    scholarship: false,
    sharedFlat: false,
    monthlyIncome: '',
    age: '',
  });
  const [estimate, setEstimate] = useState(null);
  const [visale, setVisale] = useState(null);
  const [loading, setLoading] = useState(false);
  const [touched, setTouched] = useState({});

  const errors = useMemo(() => validate(form), [form]);
  const errorFor = (key) => (touched[key] ? errors[key] : undefined);
  const touch = (key) => () => setTouched((current) => ({ ...current, [key]: true }));

  const set = (key, value) => setForm((current) => ({ ...current, [key]: value }));

  async function calculate(event) {
    event.preventDefault();
    if (Object.keys(errors).length) {
      // Reveal every complaint at once rather than one per submit attempt.
      setTouched(Object.fromEntries(Object.keys(errors).map((key) => [key, true])));
      return;
    }
    setLoading(true);
    try {
      const payload = {
        rent: Number(form.rent),
        charges: form.charges === '' ? 0 : Number(form.charges),
        postcode: form.postcode,
        student: form.student,
        scholarship: form.scholarship,
        sharedFlat: form.sharedFlat,
        monthlyIncome: form.monthlyIncome === '' ? null : Number(form.monthlyIncome),
        age: form.age === '' ? null : Number(form.age),
      };
      const [aid, guarantee] = await Promise.all([
        aidApi.estimate(payload),
        aidApi.visale(payload),
      ]);
      setEstimate(aid);
      setVisale(guarantee);
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageTransition className="page">
      <div className="page-header">
        <div>
          <h1>What will rent really cost you?</h1>
          <p>
            The French state pays part of most students&rsquo; rent, and provides a free guarantor if
            your family lives abroad. Neither happens automatically. Put a rent in below and see
            what you would actually pay.
          </p>
        </div>
      </div>

      <div className="detail-layout">
        <form className="card" onSubmit={calculate}>
          <div className="card__header">
            <h3>Your situation</h3>
          </div>
          <div className="card__body stack">
            <div className="form-grid">
              <Field id="rent" label="Monthly rent (€)" required error={errorFor('rent')}>
                {(a11y) => (
                  <input {...a11y} type="number" min="1" inputMode="numeric" value={form.rent}
                         onBlur={touch('rent')}
                         onChange={(event) => set('rent', event.target.value)} />
                )}
              </Field>
              <Field id="charges" label="Charges on top (€)" error={errorFor('charges')}>
                {(a11y) => (
                  <input {...a11y} type="number" min="0" inputMode="numeric" placeholder="0 if included"
                         value={form.charges} onBlur={touch('charges')}
                         onChange={(event) => set('charges', event.target.value)} />
                )}
              </Field>
            </div>

            <div className="form-grid">
              <Field id="postcode" label="Postcode" required error={errorFor('postcode')}
                     hint="Decides the rent ceiling the state applies. Paris is treated differently to a small town.">
                {(a11y) => (
                  <input {...a11y} maxLength={5} inputMode="numeric" value={form.postcode}
                         onBlur={touch('postcode')}
                         onChange={(event) => set('postcode', event.target.value)} />
                )}
              </Field>
              <Field id="age" label="Your age" error={errorFor('age')}>
                {(a11y) => (
                  <input {...a11y} type="number" min="16" max="99" inputMode="numeric"
                         placeholder="For the guarantor check" value={form.age}
                         onBlur={touch('age')}
                         onChange={(event) => set('age', event.target.value)} />
                )}
              </Field>
            </div>

            <Field id="monthlyIncome" label="Monthly income (€)" error={errorFor('monthlyIncome')}>
              {(a11y) => (
                <input {...a11y} type="number" min="0" inputMode="numeric"
                       placeholder="Leave empty if none" value={form.monthlyIncome}
                       onBlur={touch('monthlyIncome')}
                       onChange={(event) => set('monthlyIncome', event.target.value)} />
              )}
            </Field>

            <div className="stack" style={{ gap: '0.5rem' }}>
              <label className="checkbox">
                <input type="checkbox" checked={form.student}
                       onChange={(event) => set('student', event.target.checked)} />
                I am a student
              </label>
              <label className="checkbox">
                <input type="checkbox" checked={form.scholarship}
                       onChange={(event) => set('scholarship', event.target.checked)} />
                I hold a scholarship (bourse)
              </label>
              <label className="checkbox">
                <input type="checkbox" checked={form.sharedFlat}
                       onChange={(event) => set('sharedFlat', event.target.checked)} />
                It is a shared flat (colocation)
              </label>
            </div>
          </div>
          <div className="card__footer">
            <button type="submit" className="btn btn--accent btn--block" disabled={loading}>
              {loading && <ButtonSpinner />}
              {loading ? 'Working it out…' : 'Show me the real cost'}
            </button>
          </div>
        </form>

        <div className="stack">
          {!estimate && (
            <div className="card">
              <div className="card__body">
                <p className="small muted" style={{ margin: 0 }}>
                  Fill in the form and the answer appears here, along with every figure used to
                  get it - so you can check it rather than take our word for it.
                </p>
              </div>
            </div>
          )}

          {estimate && (
            <>
              <div className="aid-panel">
                <div className="small muted">Estimated real cost</div>
                <div className="aid-headline">
                  <span className="aid-headline__was">{formatPrice(estimate.monthlyRent)}</span>
                  <span className="aid-headline__now">
                    {formatPrice(estimate.effectiveMonthlyCost)}
                  </span>
                  <span className="aid-headline__unit">/ month</span>
                </div>
                <p className="small" style={{ marginTop: '0.5rem', marginBottom: 0 }}>
                  {estimate.likelyEligible
                    ? `Housing benefit should be worth ${formatPrice(estimate.estimateLow)} to ${formatPrice(estimate.estimateHigh)} a month.`
                    : 'Housing benefit is unlikely to help much at this rent and income.'}
                </p>
              </div>

              <div className="card">
                <div className="card__header">
                  <h3>How that is worked out</h3>
                  <span className="small muted">{estimate.zoneLabel}</span>
                </div>
                <div className="card__body">
                  <div className="aid-steps">
                    {(estimate.breakdown || []).map((step) => (
                      <div key={step.label} className="aid-step">
                        <span className="aid-step__label">{step.label}</span>
                        <span className="aid-step__value">{step.value}</span>
                        <span className="aid-step__why">{step.explanation}</span>
                      </div>
                    ))}
                  </div>
                  <ul className="note-list" style={{ marginTop: '1rem' }}>
                    {(estimate.notes || []).map((note) => (
                      <li key={note}>{note}</li>
                    ))}
                  </ul>
                </div>
                <div className="card__footer" style={{ justifyContent: 'space-between' }}>
                  <span className="small muted">
                    Official figures last checked {estimate.parametersVerifiedOn}
                  </span>
                  <a className="btn btn--sm btn--ghost" href={estimate.officialSimulatorUrl}
                     target="_blank" rel="noreferrer">
                    Confirm officially
                  </a>
                </div>
              </div>
            </>
          )}

          {visale && (
            <div className="card">
              <div className="card__header">
                <h3>Free state guarantor</h3>
                <span className={`badge ${visale.eligible ? 'badge--success' : 'badge--warning'}`}>
                  {visale.eligible ? 'You qualify' : 'Not at this rent'}
                </span>
              </div>
              <div className="card__body stack">
                <p className="small" style={{ margin: 0 }}>{visale.whatItIs}</p>
                {visale.passed?.length > 0 && (
                  <ul className="note-list check-list">
                    {visale.passed.map((item) => <li key={item}>{item}</li>)}
                  </ul>
                )}
                {visale.blockers?.length > 0 && (
                  <ul className="note-list block-list">
                    {visale.blockers.map((item) => <li key={item}>{item}</li>)}
                  </ul>
                )}
                <a className="btn btn--ghost btn--block" href={visale.officialUrl}
                   target="_blank" rel="noreferrer">
                  Apply on visale.fr
                </a>
              </div>
            </div>
          )}

          <div className="card">
            <div className="card__body row row--between">
              <div>
                <strong>Want this on every listing?</strong>
                <div className="small muted">
                  Tell us a little about yourself once and it appears everywhere.
                </div>
              </div>
              <Link to="/start" className="btn btn--primary btn--sm">Set up</Link>
            </div>
          </div>
        </div>
      </div>
    </PageTransition>
  );
}
