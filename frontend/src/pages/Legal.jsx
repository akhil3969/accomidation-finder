import { Link } from 'react-router-dom';
import useDocumentTitle from '../hooks/useDocumentTitle';
import PageTransition from '../components/PageTransition';

/**
 * Privacy policy and terms. Registration records which version a person accepted
 * (app.legal.terms-version on the backend) - bump both together when this changes.
 *
 * Written to describe what the code actually does. It is a starting point, not legal
 * advice: have it reviewed before real people rely on it.
 */
const VERSION = '2026-10';

function Privacy() {
  return (
    <>
      <h1>Privacy policy</h1>
      <p className="muted small">Version {VERSION}</p>

      <h2>Who is responsible</h2>
      <p>
        AccomFinder is operated by its author (see the contact link in the footer). That person is the data controller
        for the information described below.
      </p>

      <h2>What we hold, and why</h2>
      <ul>
        <li>
          <strong>Your account</strong> - name, email, password (stored only as a one-way hash), phone, bio and role.
          Needed to run your account (GDPR art. 6(1)(b)).
        </li>
        <li>
          <strong>Your newcomer profile</strong>, if you fill it in - nationality, date of birth, arrival date, study
          details, income and guarantor situation. Used only to estimate housing aid and Visale eligibility for you.
        </li>
        <li>
          <strong>Bookings, messages, reviews, favourites and reports</strong> you create, so the service works.
        </li>
        <li>
          <strong>Notifications</strong> about bookings and messages, and the emails we send you about them.
        </li>
        <li>
          <strong>Consent record</strong> - when you accepted these terms and which version.
        </li>
      </ul>

      <h2>How long we keep it</h2>
      <ul>
        <li>Your account and its content: until you delete your account.</li>
        <li>Read notifications: 90 days, then deleted automatically.</li>
        <li>Email verification and password-reset links: deleted 7 days after they expire.</li>
        <li>
          After you delete your account: bookings and conversations you shared with someone else remain for them, with
          your name replaced by &ldquo;Deleted user&rdquo; and your messages replaced by a removal notice.
        </li>
      </ul>

      <h2>Who else sees it</h2>
      <p>
        The landlord or tenant you deal with sees your name and what you send them. Our email provider delivers the
        emails we send you. Listing addresses (not yours) are sent to the French government address service and to
        OpenStreetMap to place them on the map. We do not sell data and we run no advertising or analytics.
      </p>

      <h2>Cookies</h2>
      <p>
        We set no cookies. Your sign-in is kept in your browser&apos;s local storage because the site cannot work without
        it, so no consent banner is needed.
      </p>

      <h2>Your rights</h2>
      <p>
        From your <Link to="/profile">profile</Link> you can download everything we hold about you as a file (rights of
        access and portability) and delete your account (right to erasure). You can correct your details there at any
        time. You can also complain to the CNIL (cnil.fr).
      </p>
    </>
  );
}

function Terms() {
  return (
    <>
      <h1>Terms of use</h1>
      <p className="muted small">Version {VERSION}</p>

      <h2>What this is</h2>
      <p>
        AccomFinder helps students find accommodation and talk to landlords. We are not a party to any rental agreement
        and do not handle rent or deposits.
      </p>

      <h2>Your account</h2>
      <ul>
        <li>Give your real name and an email address you control. You must confirm that address to book or list.</li>
        <li>Keep your password to yourself. If you think someone else has it, reset it - that signs out every device.</li>
      </ul>

      <h2>Listings and behaviour</h2>
      <ul>
        <li>Landlords must only list property they are entitled to rent, at the real price, with real photos.</li>
        <li>
          Never ask for, or send, a deposit or rent before the property has been visited and a lease signed. This is the
          pattern of nearly every rental scam.
        </li>
        <li>Harassment, discrimination and spam lead to removal. Report anything suspicious with the Report button.</li>
      </ul>

      <h2>Estimates</h2>
      <p>
        Housing-aid and Visale figures are estimates computed from published official figures. Only CAF and Action
        Logement can tell you what you will actually receive.
      </p>

      <h2>Ending</h2>
      <p>
        You can delete your account at any time from your profile. We may suspend accounts that break these terms.
      </p>

      <p className="small muted">
        See also the <Link to="/privacy">privacy policy</Link>.
      </p>
    </>
  );
}

export default function Legal({ kind }) {
  useDocumentTitle(kind === 'terms' ? 'Terms of use' : 'Privacy policy');
  return (
    <PageTransition className="page page--narrow">
      <article className="card">
        <div className="card__body prose">{kind === 'terms' ? <Terms /> : <Privacy />}</div>
      </article>
    </PageTransition>
  );
}
