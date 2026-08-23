import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { newcomerApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import Loader, { ButtonSpinner } from '../components/Loader';
import { todayIso } from '../utils/format';
import PageTransition from '../components/PageTransition';

/** A short, common list first, then anywhere. Ordered by who actually arrives here. */
const COMMON_COUNTRIES = [
  'India', 'China', 'Morocco', 'Algeria', 'Tunisia', 'Senegal', 'Cameroon', 'Brazil',
  'Colombia', 'Mexico', 'United States', 'Canada', 'Lebanon', 'Vietnam', 'Turkey',
  'Nigeria', 'Ivory Coast', 'Russia', 'Ukraine', 'Iran', 'Pakistan', 'Bangladesh',
  'Italy', 'Spain', 'Germany', 'Portugal', 'Romania', 'Poland', 'Belgium', 'Ireland',
];

const STEPS = ['Where you are from', 'About you', 'Your studies', 'Money'];

/**
 * Four short steps, asked once.
 *
 * Every question here changes something the person will actually see - the aid
 * estimate, which documents appear on their checklist, the travel time on each
 * listing. Nothing is collected because it might be interesting later.
 */
export default function ArrivalWizard() {
  useDocumentTitle('Set up your profile');

  const navigate = useNavigate();
  const toast = useToast();

  const [step, setStep] = useState(0);
  const [direction, setDirection] = useState(1);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    nationality: '',
    euNational: null,
    dateOfBirth: '',
    arrivalDate: todayIso(),
    student: true,
    scholarshipHolder: false,
    university: '',
    monthlyIncome: '',
    hasFrenchGuarantor: false,
  });

  useEffect(() => {
    let cancelled = false;
    newcomerApi
      .profile()
      .then((profile) => {
        if (cancelled) return;
        setForm((current) => ({
          ...current,
          nationality: profile.nationality || '',
          euNational: profile.euNational,
          dateOfBirth: profile.dateOfBirth || '',
          arrivalDate: profile.arrivalDate || todayIso(),
          student: profile.student,
          scholarshipHolder: profile.scholarshipHolder,
          university: profile.university || '',
          monthlyIncome: profile.monthlyIncome ?? '',
          hasFrenchGuarantor: profile.hasFrenchGuarantor,
        }));
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const set = (key, value) => setForm((current) => ({ ...current, [key]: value }));

  function goTo(next) {
    setDirection(next > step ? 1 : -1);
    setStep(next);
  }

  async function finish() {
    setSaving(true);
    try {
      await newcomerApi.saveProfile({
        ...form,
        monthlyIncome: form.monthlyIncome === '' ? null : Number(form.monthlyIncome),
        dateOfBirth: form.dateOfBirth || null,
        arrivalDate: form.arrivalDate || null,
      });
      toast.success('All set. Every listing now shows what it would really cost you.');
      navigate('/checklist');
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <Loader label="Loading your profile…" />;

  return (
    <PageTransition className="page page--narrow">
      <div className="page-header">
        <div>
          <h1>Let us make this useful for you</h1>
          <p>
            Four short questions. They change what you see - what each flat really costs after
            housing benefit, which documents you actually need, and how far each place is from
            your campus.
          </p>
        </div>
      </div>

      <div className="wizard-steps" aria-hidden="true">
        {STEPS.map((label, index) => (
          <span
            key={label}
            className={`wizard-dot ${index === step ? 'is-active' : ''} ${index < step ? 'is-done' : ''}`}
          />
        ))}
      </div>

      <div className="card">
        <div className="card__body stack" style={{ minHeight: 340, overflow: 'hidden' }}>
          <div className="small muted">
            Step {step + 1} of {STEPS.length} · {STEPS[step]}
          </div>

          {/* One step leaves before the next arrives, sliding in the direction
              of travel so going Back is visibly going back. */}
          <AnimatePresence mode="wait" custom={direction} initial={false}>
          <motion.div
            key={step}
            className="stack"
            custom={direction}
            initial={{ opacity: 0, x: direction * 28 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: direction * -28 }}
            transition={{ duration: 0.24, ease: [0.22, 0.61, 0.36, 1] }}
          >

          {/* ---------------------------------------------------- step 1 */}
          {step === 0 && (
            <>
              <h2>Which country are you coming from?</h2>
              <p className="small muted">
                This decides which paperwork applies to you. People from the EU need far less of
                it, and there is no point showing you steps you do not have to take.
              </p>

              <div className="field">
                <label htmlFor="nationality">Country</label>
                <input
                  id="nationality"
                  list="countries"
                  placeholder="Start typing"
                  value={form.nationality}
                  onChange={(event) => set('nationality', event.target.value)}
                />
                <datalist id="countries">
                  {COMMON_COUNTRIES.map((country) => (
                    <option key={country} value={country} />
                  ))}
                </datalist>
                <span className="field__hint">
                  Any country is fine - the list is just the quickest few.
                </span>
              </div>

              <div className="field">
                <label htmlFor="arrival">When did you arrive, or when do you plan to?</label>
                <input
                  id="arrival"
                  type="date"
                  value={form.arrivalDate}
                  onChange={(event) => set('arrivalDate', event.target.value)}
                />
              </div>
            </>
          )}

          {/* ---------------------------------------------------- step 2 */}
          {step === 1 && (
            <>
              <h2>Your date of birth</h2>
              <p className="small muted">
                Only used to check one thing: Visale, the free government guarantor, is open to
                people aged 18 to 30. If you qualify, it is the most useful thing on this site.
              </p>
              <div className="field">
                <label htmlFor="dob">Date of birth</label>
                <input
                  id="dob"
                  type="date"
                  value={form.dateOfBirth}
                  onChange={(event) => set('dateOfBirth', event.target.value)}
                />
              </div>

              <div className="field">
                <label>Do you already have a guarantor living in France?</label>
                <span className="field__hint" style={{ marginBottom: '0.4rem' }}>
                  Someone with a French income who would sign to cover your rent. For most people
                  arriving from abroad the answer is no, and that is completely normal.
                </span>
                <div className="stack" style={{ gap: '0.5rem' }}>
                  <button type="button"
                          className={`option-card ${!form.hasFrenchGuarantor ? 'is-selected' : ''}`}
                          onClick={() => set('hasFrenchGuarantor', false)}>
                    <div>
                      <div className="option-card__title">No, I do not</div>
                      <div className="option-card__desc">
                        We will show you how to get the free state guarantee instead.
                      </div>
                    </div>
                  </button>
                  <button type="button"
                          className={`option-card ${form.hasFrenchGuarantor ? 'is-selected' : ''}`}
                          onClick={() => set('hasFrenchGuarantor', true)}>
                    <div>
                      <div className="option-card__title">Yes, I have one</div>
                      <div className="option-card__desc">
                        Good - that already puts you ahead of most applicants.
                      </div>
                    </div>
                  </button>
                </div>
              </div>
            </>
          )}

          {/* ---------------------------------------------------- step 3 */}
          {step === 2 && (
            <>
              <h2>Are you a student?</h2>
              <p className="small muted">
                Students are assessed differently for housing benefit, and get access to schemes
                nobody else does.
              </p>

              <div className="stack" style={{ gap: '0.5rem' }}>
                <button type="button" className={`option-card ${form.student ? 'is-selected' : ''}`}
                        onClick={() => set('student', true)}>
                  <div>
                    <div className="option-card__title">Yes, I am studying</div>
                    <div className="option-card__desc">
                      Including exchange students and apprentices.
                    </div>
                  </div>
                </button>
                <button type="button" className={`option-card ${!form.student ? 'is-selected' : ''}`}
                        onClick={() => set('student', false)}>
                  <div>
                    <div className="option-card__title">No, I am working or looking for work</div>
                    <div className="option-card__desc">
                      Different rent limits apply to you, and we will use those.
                    </div>
                  </div>
                </button>
              </div>

              {form.student && (
                <>
                  <div className="field">
                    <label htmlFor="university">Where do you study?</label>
                    <input
                      id="university"
                      placeholder="e.g. EPITA, Le Kremlin-Bicetre"
                      value={form.university}
                      onChange={(event) => set('university', event.target.value)}
                    />
                    <span className="field__hint">
                      We look the address up so every listing can show you how long the journey
                      really takes.
                    </span>
                  </div>

                  <label className="checkbox">
                    <input
                      type="checkbox"
                      checked={form.scholarshipHolder}
                      onChange={(event) => set('scholarshipHolder', event.target.checked)}
                    />
                    I hold a scholarship (bourse)
                  </label>
                  <span className="field__hint">
                    A scholarship lowers the income the state assumes you have, which increases
                    your housing benefit. Worth ticking if it applies.
                  </span>
                </>
              )}
            </>
          )}

          {/* ---------------------------------------------------- step 4 */}
          {step === 3 && (
            <>
              <h2>Do you have any monthly income?</h2>
              <p className="small muted">
                Leave it empty if you have none - that is the normal answer on arrival and it
                does not disqualify you from anything. Students with no income are assessed on a
                fixed assumed figure instead.
              </p>

              <div className="field">
                <label htmlFor="income">Net monthly income in euros (optional)</label>
                <input
                  id="income"
                  type="number"
                  min="0"
                  step="50"
                  placeholder="Leave empty if none"
                  value={form.monthlyIncome}
                  onChange={(event) => set('monthlyIncome', event.target.value)}
                />
              </div>

              <div className="card" style={{ background: 'var(--surface-2)', border: 'none' }}>
                <div className="card__body">
                  <strong>That is everything.</strong>
                  <p className="small muted" style={{ margin: '0.3rem 0 0' }}>
                    From here on, every listing shows what it would really cost you after housing
                    benefit, whether the free guarantor covers it, and how far it is from your
                    campus. Your checklist is filtered to the documents you actually need.
                  </p>
                </div>
              </div>
            </>
          )}
          </motion.div>
          </AnimatePresence>
        </div>

        <div className="card__footer">
          {step > 0 && (
            <button type="button" className="btn btn--ghost" onClick={() => goTo(step - 1)}>
              Back
            </button>
          )}
          <div className="spacer" />
          {step < STEPS.length - 1 ? (
            <button type="button" className="btn btn--primary" onClick={() => goTo(step + 1)}>
              Continue
            </button>
          ) : (
            <button type="button" className="btn btn--accent" onClick={finish} disabled={saving}>
              {saving && <ButtonSpinner />}
              {saving ? 'Saving…' : 'Finish'}
            </button>
          )}
        </div>
      </div>

      <p className="small muted center" style={{ marginTop: '1rem' }}>
        You can change any of this later, and skip anything you are unsure about.
      </p>
    </PageTransition>
  );
}
