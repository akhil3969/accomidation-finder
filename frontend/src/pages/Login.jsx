import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { toMessage } from '../api/client';
import useDocumentTitle from '../hooks/useDocumentTitle';
import useForm, { compact, rules } from '../hooks/useForm';
import PageTransition from '../components/PageTransition';
import { TextField } from '../components/Field';
import { ButtonSpinner } from '../components/Loader';

const DEMO_ACCOUNTS = [
  { label: 'Tenant', email: 'akhil@epita.fr' },
  { label: 'Landlord', email: 'jean@landlord.fr' },
  { label: 'Admin', email: 'admin@accomfinder.app' },
];

function validate(values) {
  return compact({
    email: rules.required(values.email, 'Email') || rules.email(values.email),
    password: rules.required(values.password, 'Password'),
  });
}

export default function Login() {
  useDocumentTitle('Sign in');

  const { signIn } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const form = useForm({
    initialValues: { email: '', password: '' },
    validate,
    onSubmit: async (values) => {
      try {
        const user = await signIn(values);
        toast.success(`Welcome back, ${user.name.split(' ')[0]}`);
        navigate(location.state?.from || '/', { replace: true });
      } catch (error) {
        // useForm surfaces `userMessage`, so the server's wording reaches the
        // form rather than a generic "request failed".
        throw Object.assign(new Error(toMessage(error)), { userMessage: toMessage(error) });
      }
    },
  });

  return (
    <PageTransition className="page page--auth">
      <div className="card" style={{ width: 'min(440px, 100%)' }}>
        <div className="card__body stack">
          <div>
            <h1 style={{ fontSize: '1.6rem' }}>Welcome back</h1>
            <p className="muted small" style={{ margin: 0 }}>
              Sign in to book rooms, save favourites and chat with landlords.
            </p>
          </div>

          <form className="stack" onSubmit={form.handleSubmit} noValidate>
            <TextField
              id="email"
              label="Email"
              type="email"
              autoComplete="email"
              autoFocus
              required
              value={form.values.email}
              error={form.errorFor('email')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />

            <TextField
              id="password"
              label="Password"
              type="password"
              autoComplete="current-password"
              required
              value={form.values.password}
              error={form.errorFor('password')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />

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

            <button
              type="submit"
              className="btn btn--primary btn--block"
              disabled={form.submitting}
            >
              {form.submitting && <ButtonSpinner />}
              {form.submitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <div className="stack" style={{ gap: '0.4rem' }}>
            <span className="small muted">Demo accounts (password: password123)</span>
            <div className="chip-row">
              {DEMO_ACCOUNTS.map((account) => (
                <button
                  key={account.email}
                  type="button"
                  className="chip"
                  onClick={() => form.reset({ email: account.email, password: 'password123' })}
                >
                  {account.label}
                </button>
              ))}
            </div>
          </div>

          <p className="small center muted" style={{ margin: 0 }}>
            No account yet? <Link to="/register">Create one</Link>
          </p>
        </div>
      </div>
    </PageTransition>
  );
}
