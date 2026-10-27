import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import PageTransition from '../components/PageTransition';
import Loader from '../components/Loader';

export default function VerifyEmail() {
  useDocumentTitle('Confirm your email');

  const [params] = useSearchParams();
  const { user, updateUser } = useAuth();
  const [state, setState] = useState({ status: 'working', message: '' });
  // StrictMode runs effects twice in development; the token is single-use, so the
  // second call would fail and overwrite the success message.
  const attempted = useRef(false);

  useEffect(() => {
    if (attempted.current) return;
    attempted.current = true;
    const token = params.get('token');
    if (!token) {
      setState({ status: 'error', message: 'This link is missing its token.' });
      return;
    }
    authApi
      .verifyEmail(token)
      .then((verified) => {
        if (user && user.id === verified.id) updateUser({ ...user, emailVerified: true });
        setState({ status: 'done', message: '' });
      })
      .catch((error) => setState({ status: 'error', message: toMessage(error) }));
  }, [params, user, updateUser]);

  return (
    <PageTransition className="page page--auth">
      <div className="card" style={{ width: 'min(460px, 100%)' }}>
        <div className="card__body stack center">
          {state.status === 'working' && <Loader label="Confirming your email…" />}
          {state.status === 'done' && (
            <>
              <h1 style={{ fontSize: '1.5rem' }}>Email confirmed</h1>
              <p className="muted">You can now book rooms, message landlords and publish listings.</p>
              <Link to={user ? '/' : '/login'} className="btn btn--primary">
                {user ? 'Start searching' : 'Sign in'}
              </Link>
            </>
          )}
          {state.status === 'error' && (
            <>
              <h1 style={{ fontSize: '1.5rem' }}>That link did not work</h1>
              <p className="muted">{state.message}</p>
              <p className="small muted">
                Sign in and use <strong>Resend link</strong> in the banner at the top of the page to get a new one.
              </p>
              <Link to="/" className="btn btn--ghost">
                Back to search
              </Link>
            </>
          )}
        </div>
      </div>
    </PageTransition>
  );
}
