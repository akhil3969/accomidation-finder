import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { userApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useConfirm } from '../context/ConfirmContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import useForm, { compact, passwordStrength, rules } from '../hooks/useForm';
import PageTransition from '../components/PageTransition';
import Field, { TextAreaField, TextField } from '../components/Field';
import { ButtonSpinner } from '../components/Loader';
import { formatDate, initials } from '../utils/format';

export default function Profile() {
  useDocumentTitle('Your profile');

  const { user, updateUser, becomeLandlord, signOut } = useAuth();
  const toast = useToast();
  const confirm = useConfirm();
  const navigate = useNavigate();
  const [upgrading, setUpgrading] = useState(false);
  const [savingPrefs, setSavingPrefs] = useState(false);
  const [exporting, setExporting] = useState(false);

  const profileForm = useForm({
    initialValues: {
      name: user?.name || '',
      phone: user?.phone || '',
      bio: user?.bio || '',
      avatarUrl: user?.avatarUrl || '',
    },
    validate: (values) =>
      compact({
        name: rules.required(values.name, 'Your name') || rules.minLength(values.name, 2, 'Your name'),
        avatarUrl: rules.url(values.avatarUrl),
        bio: rules.maxLength(values.bio, 500, 'Your bio'),
      }),
    onSubmit: async (values) => {
      try {
        updateUser(await userApi.updateMe(values));
        toast.success('Profile updated');
      } catch (error) {
        const message = toMessage(error);
        throw Object.assign(new Error(message), { userMessage: message });
      }
    },
  });

  const passwordForm = useForm({
    initialValues: { currentPassword: '', newPassword: '', confirm: '' },
    validate: (values) =>
      compact({
        currentPassword: rules.required(values.currentPassword, 'Your current password'),
        newPassword:
          rules.required(values.newPassword, 'A new password') ||
          rules.minLength(values.newPassword, 8, 'The new password') ||
          (values.newPassword === values.currentPassword
            ? 'The new password must be different'
            : undefined),
        confirm:
          values.confirm !== values.newPassword ? 'The two new passwords do not match' : undefined,
      }),
    onSubmit: async (values) => {
      try {
        await userApi.changePassword({
          currentPassword: values.currentPassword,
          newPassword: values.newPassword,
        });
        passwordForm.reset({ currentPassword: '', newPassword: '', confirm: '' });
        toast.success('Password changed');
      } catch (error) {
        const message = toMessage(error);
        throw Object.assign(new Error(message), { userMessage: message });
      }
    },
  });

  const strength = passwordStrength(passwordForm.values.newPassword);

  const deleteForm = useForm({
    initialValues: { password: '' },
    validate: (values) => compact({ password: rules.required(values.password, 'Your password') }),
    onSubmit: async (values) => {
      const sure = await confirm({
        title: 'Delete your account?',
        message: 'This erases your profile, favourites, reviews and the messages you sent. It cannot be undone.',
        detail: 'Open booking requests are cancelled and the other person is told. Past bookings stay on their side as "Deleted user".',
        confirmLabel: 'Delete my account',
        tone: 'danger',
      });
      if (!sure) return;
      try {
        await userApi.deleteAccount(values.password);
        signOut();
        toast.success('Your account has been deleted');
        navigate('/', { replace: true });
      } catch (error) {
        const message = toMessage(error);
        throw Object.assign(new Error(message), { userMessage: message });
      }
    },
  });

  async function toggleEmailNotifications(event) {
    const enabled = event.target.checked;
    setSavingPrefs(true);
    try {
      updateUser(await userApi.updateMe({ emailNotifications: enabled }));
      toast.success(enabled ? 'Email notifications on' : 'Email notifications off');
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setSavingPrefs(false);
    }
  }

  async function downloadData() {
    setExporting(true);
    try {
      const blob = await userApi.exportData();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'accomfinder-my-data.json';
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setExporting(false);
    }
  }

  async function upgrade() {
    setUpgrading(true);
    try {
      await becomeLandlord();
      toast.success('You can now publish listings');
    } catch (error) {
      toast.error(toMessage(error));
    } finally {
      setUpgrading(false);
    }
  }

  return (
    <PageTransition className="page page--narrow stack">
      <div className="page-header">
        <div className="row" style={{ flexWrap: 'nowrap' }}>
          <span className="avatar avatar--lg">{initials(user?.name)}</span>
          <div style={{ minWidth: 0 }}>
            <h1 style={{ marginBottom: 0 }}>{user?.name}</h1>
            <p className="muted small" style={{ margin: 0 }}>
              {user?.email} · {user?.role} · member since {formatDate(user?.createdAt)}
            </p>
          </div>
        </div>
      </div>

      {user?.role === 'TENANT' && (
        <div className="card">
          <div className="card__body row row--between">
            <div>
              <strong>Have a place to rent out?</strong>
              <div className="small muted">Upgrade to a landlord account to publish listings.</div>
            </div>
            <button type="button" className="btn btn--accent" onClick={upgrade} disabled={upgrading}>
              {upgrading && <ButtonSpinner />}
              Become a landlord
            </button>
          </div>
        </div>
      )}

      <form className="card" onSubmit={profileForm.handleSubmit} noValidate>
        <div className="card__header">
          <h3>Profile details</h3>
        </div>
        <div className="card__body stack">
          <div className="form-grid">
            <TextField
              id="name"
              label="Full name"
              required
              autoComplete="name"
              value={profileForm.values.name}
              error={profileForm.errorFor('name')}
              onChange={profileForm.handleChange}
              onBlur={profileForm.handleBlur}
            />
            <TextField
              id="phone"
              label="Phone"
              type="tel"
              autoComplete="tel"
              value={profileForm.values.phone}
              onChange={profileForm.handleChange}
              onBlur={profileForm.handleBlur}
            />
          </div>
          <TextField
            id="avatarUrl"
            label="Avatar URL"
            type="url"
            placeholder="https://…"
            value={profileForm.values.avatarUrl}
            error={profileForm.errorFor('avatarUrl')}
            onChange={profileForm.handleChange}
            onBlur={profileForm.handleBlur}
          />
          <TextAreaField
            id="bio"
            label="About you"
            rows={3}
            maxLength={500}
            hint={`${profileForm.values.bio.length}/500`}
            value={profileForm.values.bio}
            error={profileForm.errorFor('bio')}
            onChange={profileForm.handleChange}
            onBlur={profileForm.handleBlur}
          />
          {profileForm.formError && (
            <div className="form-alert" role="alert">
              {profileForm.formError}
            </div>
          )}
        </div>
        <div className="card__footer">
          <button type="submit" className="btn btn--primary" disabled={profileForm.submitting}>
            {profileForm.submitting && <ButtonSpinner />}
            {profileForm.submitting ? 'Saving…' : 'Save profile'}
          </button>
        </div>
      </form>

      <form className="card" onSubmit={passwordForm.handleSubmit} noValidate>
        <div className="card__header">
          <h3>Change password</h3>
        </div>
        <div className="card__body stack">
          <TextField
            id="currentPassword"
            label="Current password"
            type="password"
            required
            autoComplete="current-password"
            value={passwordForm.values.currentPassword}
            error={passwordForm.errorFor('currentPassword')}
            onChange={passwordForm.handleChange}
            onBlur={passwordForm.handleBlur}
          />
          <div className="form-grid">
            <Field
              id="newPassword"
              label="New password"
              required
              hint="At least 8 characters."
              error={passwordForm.errorFor('newPassword')}
            >
              {(a11y) => (
                <>
                  <input
                    {...a11y}
                    type="password"
                    autoComplete="new-password"
                    value={passwordForm.values.newPassword}
                    onChange={passwordForm.handleChange}
                    onBlur={passwordForm.handleBlur}
                  />
                  {passwordForm.values.newPassword && (
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
              label="Confirm new password"
              type="password"
              required
              autoComplete="new-password"
              value={passwordForm.values.confirm}
              error={passwordForm.errorFor('confirm')}
              onChange={passwordForm.handleChange}
              onBlur={passwordForm.handleBlur}
            />
          </div>
          {passwordForm.formError && (
            <div className="form-alert" role="alert">
              {passwordForm.formError}
            </div>
          )}
        </div>
        <div className="card__footer">
          <button type="submit" className="btn btn--ghost" disabled={passwordForm.submitting}>
            {passwordForm.submitting && <ButtonSpinner />}
            {passwordForm.submitting ? 'Updating…' : 'Update password'}
          </button>
        </div>
      </form>

      <div className="card">
        <div className="card__header">
          <h3>Email notifications</h3>
        </div>
        <div className="card__body stack">
          <label className="checkbox" htmlFor="emailNotifications">
            <input
              id="emailNotifications"
              type="checkbox"
              checked={user?.emailNotifications !== false}
              disabled={savingPrefs}
              onChange={toggleEmailNotifications}
            />
            <span>Email me about booking updates and messages I have not seen on the site</span>
          </label>
          <p className="small muted" style={{ margin: 0 }}>
            Anything you read here within a few minutes is never emailed. Password and account emails always send.
          </p>
        </div>
      </div>

      <div className="card">
        <div className="card__header">
          <h3>Your data</h3>
        </div>
        <div className="card__body stack">
          <div className="row row--between">
            <div>
              <strong>Download everything we hold about you</strong>
              <div className="small muted">Profile, bookings, listings, messages, reviews and more, as a JSON file.</div>
            </div>
            <button type="button" className="btn btn--ghost" onClick={downloadData} disabled={exporting}>
              {exporting && <ButtonSpinner />}
              {exporting ? 'Preparing…' : 'Download my data'}
            </button>
          </div>
          <p className="small muted" style={{ margin: 0 }}>
            See the <Link to="/privacy">privacy policy</Link> for what is kept and for how long.
          </p>
        </div>
      </div>

      <form className="card card--danger" onSubmit={deleteForm.handleSubmit} noValidate>
        <div className="card__header">
          <h3>Delete account</h3>
        </div>
        <div className="card__body stack">
          <p className="small muted" style={{ margin: 0 }}>
            Permanently erases your account and personal data. Enter your password to confirm it is you.
          </p>
          <TextField
            id="deletePassword"
            name="password"
            label="Password"
            type="password"
            autoComplete="current-password"
            value={deleteForm.values.password}
            error={deleteForm.errorFor('password')}
            onChange={deleteForm.handleChange}
            onBlur={deleteForm.handleBlur}
          />
          {deleteForm.formError && (
            <div className="form-alert" role="alert">
              {deleteForm.formError}
            </div>
          )}
        </div>
        <div className="card__footer">
          <button type="submit" className="btn btn--danger" disabled={deleteForm.submitting}>
            {deleteForm.submitting && <ButtonSpinner />}
            {deleteForm.submitting ? 'Deleting…' : 'Delete my account'}
          </button>
        </div>
      </form>
    </PageTransition>
  );
}
