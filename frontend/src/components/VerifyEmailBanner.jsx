import { useState } from 'react';
import { authApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

/** Shown until the address is confirmed - booking, messaging and listing all need it. */
export default function VerifyEmailBanner() {
  const { user } = useAuth();
  const toast = useToast();
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  if (!user || user.emailVerified !== false) return null;

  async function resend() {
    setSending(true);
    try {
      await authApi.resendVerification();
      setSent(true);
      toast.success(`A new link is on its way to ${user.email}`);
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="notice-bar" role="status">
      <span>
        <strong>Confirm your email</strong> to book rooms, message landlords and publish listings. We
        sent a link to {user.email}.
      </span>
      <button type="button" className="btn btn--sm btn--ghost" onClick={resend} disabled={sending || sent}>
        {sent ? 'Sent' : sending ? 'Sending…' : 'Resend link'}
      </button>
    </div>
  );
}
