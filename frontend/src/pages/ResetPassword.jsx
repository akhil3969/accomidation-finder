import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import useForm, { compact, rules } from '../hooks/useForm';
import PageTransition from '../components/PageTransition';
import { TextField } from '../components/Field';
import { ButtonSpinner } from '../components/Loader';

export default function ResetPassword() {
  useDocumentTitle('Choose a new password');

  const [params] = useSearchParams();
  const token = params.get('token');
  const { signOut } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const form = useForm({
    initialValues: { password: '', confirm: '' },
    validate: (values) =>
      compact({
        password: rules.required(values.password, 'A new password') || rules.minLength(values.password, 8, 'The password'),
        confirm: values.confirm !== values.password ? 'The two passwords do not match' : undefined,
      }),
    onSubmit: async (values) => {
      try {
        await authApi.resetPassword(token, values.password);
        // The reset signed out every device, including this one.
        signOut();
        toast.success('Password changed - sign in with your new one');
        navigate('/login', { replace: true });
      } catch (error) {
        throw Object.assign(new Error(toMessage(error)), { userMessage: toMessage(error) });
      }
    },
  });

  return (
    <PageTransition className="page page--auth">
      <div className="card" style={{ width: 'min(440px, 100%)' }}>
        <div className="card__body stack">
          <div>
            <h1 style={{ fontSize: '1.6rem' }}>Choose a new password</h1>
            <p className="muted small" style={{ margin: 0 }}>
              This signs you out everywhere else, so anyone using your old password loses access.
            </p>
          </div>

          {!token ? (
            <div className="form-alert" role="alert">
              This link is missing its token. <Link to="/forgot-password">Request a new one</Link>.
            </div>
          ) : (
            <form className="stack" onSubmit={form.handleSubmit} noValidate>
              <TextField
                id="password"
                label="New password"
                type="password"
                autoComplete="new-password"
                hint="At least 8 characters."
                autoFocus
                required
                value={form.values.password}
                error={form.errorFor('password')}
                onChange={form.handleChange}
                onBlur={form.handleBlur}
              />
              <TextField
                id="confirm"
                label="Confirm new password"
                type="password"
                autoComplete="new-password"
                required
                value={form.values.confirm}
                error={form.errorFor('confirm')}
                onChange={form.handleChange}
                onBlur={form.handleBlur}
              />
              {form.formError && (
                <div className="form-alert" role="alert">
                  {form.formError}
                </div>
              )}
              <button type="submit" className="btn btn--primary btn--block" disabled={form.submitting}>
                {form.submitting && <ButtonSpinner />}
                {form.submitting ? 'Saving…' : 'Set new password'}
              </button>
            </form>
          )}
        </div>
      </div>
    </PageTransition>
  );
}
