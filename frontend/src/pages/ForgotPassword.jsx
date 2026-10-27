import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import useDocumentTitle from '../hooks/useDocumentTitle';
import useForm, { compact, rules } from '../hooks/useForm';
import PageTransition from '../components/PageTransition';
import { TextField } from '../components/Field';
import { ButtonSpinner } from '../components/Loader';

export default function ForgotPassword() {
  useDocumentTitle('Reset your password');
  const [sentTo, setSentTo] = useState(null);

  const form = useForm({
    initialValues: { email: '' },
    validate: (values) => compact({ email: rules.required(values.email, 'Email') || rules.email(values.email) }),
    onSubmit: async (values) => {
      try {
        await authApi.forgotPassword(values.email.trim());
        setSentTo(values.email.trim());
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
            <h1 style={{ fontSize: '1.6rem' }}>Forgot your password?</h1>
            <p className="muted small" style={{ margin: 0 }}>
              Enter your email and we will send you a link to choose a new one.
            </p>
          </div>

          {sentTo ? (
            // Worded so it is true whether or not the account exists - the server
            // deliberately does not say, so nobody can probe which emails are registered.
            <div className="notice-bar notice-bar--inline" role="status">
              If an account exists for {sentTo}, a reset link is on its way. It works once, for one hour.
            </div>
          ) : (
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
              {form.formError && (
                <div className="form-alert" role="alert">
                  {form.formError}
                </div>
              )}
              <button type="submit" className="btn btn--primary btn--block" disabled={form.submitting}>
                {form.submitting && <ButtonSpinner />}
                {form.submitting ? 'Sending…' : 'Send reset link'}
              </button>
            </form>
          )}

          <p className="small center muted" style={{ margin: 0 }}>
            Remembered it? <Link to="/login">Sign in</Link>
          </p>
        </div>
      </div>
    </PageTransition>
  );
}
