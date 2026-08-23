import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { toMessage } from '../api/client';
import useDocumentTitle from '../hooks/useDocumentTitle';
import useForm, { compact, passwordStrength, rules } from '../hooks/useForm';
import PageTransition from '../components/PageTransition';
import Field, { TextField } from '../components/Field';
import { ButtonSpinner } from '../components/Loader';

function validate(values) {
  return compact({
    name: rules.required(values.name, 'Your name') || rules.minLength(values.name, 2, 'Your name'),
    email: rules.required(values.email, 'Email') || rules.email(values.email),
    password:
      rules.required(values.password, 'Password') || rules.minLength(values.password, 8, 'Password'),
    confirm:
      rules.required(values.confirm, 'The confirmation') ||
      (values.confirm !== values.password ? 'The two passwords do not match' : undefined),
    // The backend stores whatever it is given, so only obvious nonsense is
    // rejected here - phone formats vary far too much to be strict about.
    phone:
      values.phone && !/^[+\d][\d\s().-]{5,}$/.test(values.phone.trim())
        ? 'That does not look like a phone number'
        : undefined,
  });
}

export default function Register() {
  useDocumentTitle('Create an account');

  const { register } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const form = useForm({
    initialValues: {
      name: '',
      email: '',
      password: '',
      confirm: '',
      phone: '',
      role: 'TENANT',
    },
    validate,
    onSubmit: async (values) => {
      try {
        const user = await register({
          name: values.name.trim(),
          email: values.email.trim(),
          password: values.password,
          phone: values.phone.trim() || null,
          role: values.role,
        });
        toast.success(`Account created. Welcome, ${user.name.split(' ')[0]}!`);
        navigate(values.role === 'LANDLORD' ? '/dashboard' : '/', { replace: true });
      } catch (error) {
        throw Object.assign(new Error(toMessage(error)), { userMessage: toMessage(error) });
      }
    },
  });

  const strength = passwordStrength(form.values.password);

  return (
    <PageTransition className="page page--auth">
      <div className="card" style={{ width: 'min(560px, 100%)' }}>
        <div className="card__body stack">
          <div>
            <h1 style={{ fontSize: '1.6rem' }}>Create your account</h1>
            <p className="muted small" style={{ margin: 0 }}>
              It takes about thirty seconds.
            </p>
          </div>

          <form className="stack" onSubmit={form.handleSubmit} noValidate>
            <TextField
              id="name"
              label="Full name"
              required
              autoComplete="name"
              value={form.values.name}
              error={form.errorFor('name')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />

            <div className="form-grid">
              <TextField
                id="email"
                label="Email"
                type="email"
                required
                autoComplete="email"
                value={form.values.email}
                error={form.errorFor('email')}
                onChange={form.handleChange}
                onBlur={form.handleBlur}
              />
              <TextField
                id="phone"
                label="Phone"
                type="tel"
                autoComplete="tel"
                hint="Optional."
                value={form.values.phone}
                error={form.errorFor('phone')}
                onChange={form.handleChange}
                onBlur={form.handleBlur}
              />
            </div>

            <div className="form-grid">
              <Field
                id="password"
                label="Password"
                required
                error={form.errorFor('password')}
                hint="At least 8 characters."
              >
                {(a11y) => (
                  <>
                    <input
                      {...a11y}
                      type="password"
                      autoComplete="new-password"
                      value={form.values.password}
                      onChange={form.handleChange}
                      onBlur={form.handleBlur}
                    />
                    {form.values.password && (
                      <div
                        className="strength"
                        role="meter"
                        aria-valuemin={0}
                        aria-valuemax={4}
                        aria-valuenow={strength.score}
                        aria-label={`Password strength: ${strength.label}`}
                      >
                        {[1, 2, 3, 4].map((step) => (
                          <span
                            key={step}
                            className={`strength__bar ${
                              step <= strength.score ? `is-on-${strength.score}` : ''
                            }`}
                          />
                        ))}
                      </div>
                    )}
                  </>
                )}
              </Field>

              <TextField
                id="confirm"
                label="Confirm password"
                type="password"
                required
                autoComplete="new-password"
                value={form.values.confirm}
                error={form.errorFor('confirm')}
                onChange={form.handleChange}
                onBlur={form.handleBlur}
              />
            </div>

            <Field id="role" label="I want to">
              {() => (
                <div className="chip-row">
                  <button
                    type="button"
                    className={`chip ${form.values.role === 'TENANT' ? 'is-active' : ''}`}
                    aria-pressed={form.values.role === 'TENANT'}
                    onClick={() => form.setValue('role', 'TENANT')}
                  >
                    Find a place
                  </button>
                  <button
                    type="button"
                    className={`chip ${form.values.role === 'LANDLORD' ? 'is-active' : ''}`}
                    aria-pressed={form.values.role === 'LANDLORD'}
                    onClick={() => form.setValue('role', 'LANDLORD')}
                  >
                    List my property
                  </button>
                </div>
              )}
            </Field>

            {form.formError && (
              <motion.div
                className="form-alert"
                role="alert"
                initial={{ opacity: 0, y: -6 }}
                animate={{ opacity: 1, y: 0 }}
              >
                {form.formError}
              </motion.div>
            )}

            <button type="submit" className="btn btn--accent btn--block" disabled={form.submitting}>
              {form.submitting && <ButtonSpinner />}
              {form.submitting ? 'Creating account…' : 'Create account'}
            </button>
          </form>

          <p className="small center muted" style={{ margin: 0 }}>
            Already registered? <Link to="/login">Sign in</Link>
          </p>
        </div>
      </div>
    </PageTransition>
  );
}
