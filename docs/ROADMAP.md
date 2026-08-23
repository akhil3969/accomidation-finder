# Roadmap to real users

Ranked by what actually blocks real people from using this safely, not by what is
interesting to build. Every item says **why it matters**, **where it lives** and a rough
size: **S** = a few hours, **M** = a day or two, **L** = about a week, **XL** = several weeks.

Do the tiers in order. Tier 0 is not optional.

---

# Tier 0 — Do not let a real user near it until these are done

### 0.1 Rotate the JWT secret and get it out of the repo — **S**

`backend/src/main/resources/application.yml` ships a working default secret, and that file
is public on GitHub. Anyone who reads it can forge a token for **any account, including
admin**, on any deployment that did not override it. That is a complete authentication
bypass.

- Generate a real one: `openssl rand -base64 48`
- Set it as `JWT_SECRET` in your host's environment, never in a file you commit
- Change the default in `application.yml` to something that *cannot work*, so a
  misconfigured deploy fails loudly instead of running insecurely:
  `secret: ${JWT_SECRET:}` — the blank check I added throws on startup
- Anyone who already cloned your repo has the old secret. Rotating invalidates every
  existing token, which is exactly what you want.

### 0.2 Notifications that reach people who are not looking at the site — **M**

**This is the bug that makes the product not work.** Right now a booking request only
appears if the landlord happens to have the tab open. Close the browser and the request is
invisible until they next sign in. No landlord will use that.

`NotificationService` (`service/NotificationService.java`) currently has exactly one
channel: the live WebSocket. It needs a second, persistent one.

- Add a `notifications` table: recipient, type, payload, `read`, `created_at`
- Write a row for every event *and* push it over the socket
- Add a bell icon with a dropdown in the navbar
- Then layer email on top (item 0.3)

### 0.3 Transactional email — **M**

Nothing in the system can send an email, which makes three things impossible:

- **Password reset.** There is no flow at all. Real users forget passwords within days and
  are then permanently locked out. This alone blocks launch.
- **Email verification.** Anyone can register as `landlord@sorbonne.fr` today. On a rental
  platform, unverified identity is how fraud starts.
- **Booking alerts** when the recipient is offline.

Add `spring-boot-starter-mail` plus a provider (Brevo and Resend both have usable free
tiers and work from France). Build: verify-email, reset-password (single-use, expiring
token in its own table), booking-status-changed, new-message-digest.

### 0.4 Fix the booking race condition — **S**

`BookingService.request` checks for overlapping dates and then inserts. Two requests
arriving at the same moment can both pass the check and both be written — two people
holding the same room.

- Simplest correct fix: a database constraint MySQL will enforce no matter what the
  application does, plus catching the resulting `DataIntegrityViolationException`
- Or `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the room row for the duration of the check
- Add `@Version` to `Room` and `Booking` for optimistic locking on concurrent edits

### 0.5 GDPR — you are handling EU personal data — **M**

Names, phone numbers, addresses and private messages of people in France. This is a legal
obligation, not a nice-to-have.

- Privacy policy and terms pages, accepted at registration with the timestamp stored
- **Right to export**: an endpoint returning everything you hold on a user as JSON
- **Right to erasure**: real account deletion — anonymise their bookings and messages
  rather than orphaning rows
- A cookie/consent notice if you add any analytics
- Document your retention period and actually enforce it

### 0.6 Turn off the things that leak in production — **S**

- `server.error.include-message: always` in `application.yml` sends exception text to the
  client. Override it to `never` in `application-prod.yml`.
- `SecurityConfig` line 64 permits `/h2-console/**` in *every* profile. Move that matcher
  behind a `@Profile("demo")` bean or delete it.
- Force HTTPS and add HSTS at your reverse proxy.
- Set `logging.level.org.hibernate.SQL: OFF` in prod — SQL logs contain personal data.

### 0.7 Rate limiting — **S**

`/api/auth/login` will accept unlimited guesses. Add Bucket4j (or nginx `limit_req`):
5 login attempts per IP per minute, 3 registrations per IP per hour, 20 messages per user
per minute. Also cap `size` on every paginated endpoint — nothing stops
`GET /api/rooms?size=1000000` today. Set `spring.data.web.pageable.max-page-size: 100`.

### 0.8 A way to create the first admin — **S**

Seeding is disabled in prod, so a production database has **no admin account** and no way
to make one except editing the database by hand. Add a one-shot bootstrap: if
`ADMIN_EMAIL` and `ADMIN_PASSWORD` are set and no admin exists, create one at startup.

---

# Tier 1 — The product is not usable for real users without these

### 1.1 Real image uploads — **M**

`RoomEditor` asks landlords to paste image URLs, one per line. No actual landlord will do
this. It is also an abuse vector: arbitrary third-party URLs rendered on your pages.

Use Cloudinary or S3 + CloudFront. Upload from the browser with a pre-signed URL so the
files never pass through your API. Validate MIME type and size server-side, strip EXIF
(holiday photos carry GPS coordinates), and generate thumbnails.

### 1.2 Address → coordinates automatically — **S**

`RoomEditor` has raw latitude and longitude number fields. Nobody knows their flat's
coordinates. Without them the listing never appears on the map.

Geocode the address server-side on save using the French government's free API
(`api-adresse.data.gouv.fr`, no key needed, excellent French coverage). Show the resulting
pin on a small map in the form so the landlord can drag it if it lands wrong.

### 1.3 Scam prevention — **L**

This is *the* problem in the Paris rental market, and it is what would make your app worth
using over the alternatives. Students routinely lose deposits to fake listings.

- Landlord verification: ID document check (Stripe Identity, or manual review to start)
  with a "Verified landlord" badge
- Duplicate-listing detection: same photos or address posted twice
- Flag listings that look like the classic scam pattern — far below market rate,
  landlord "abroad", pushing users to pay off-platform
- A **Report** button on every listing and profile, feeding a moderation queue
- Block/mute another user
- An in-app warning before any payment leaves the platform

### 1.4 French language — **M**

Your users are in Paris. The entire interface is English-only. Add `react-i18next`, extract
every string, and ship FR and EN. Not optional for the market you are in.

### 1.5 Saved searches with alerts — **M**

Your own original README promised "get notified when a matching room becomes available",
and it is the single most valuable feature in this domain — the whole problem is that good
listings vanish in hours.

Store the search criteria, run new listings against saved searches on publish, and notify
matches by email and push. This is the feature that brings users back.

### 1.6 Password reset UI — **S**

The backend work is in 0.3; this is the two screens: request-a-reset and set-a-new-password.

---

# Tier 2 — Reliability and operations

### 2.1 Flyway migrations — **M**

`database/schema.sql` and the JPA entities are maintained by hand and can drift. The prod
profile uses `ddl-auto: validate`, so the first drift is a failed deploy — and there is no
mechanism at all for changing a live schema without data loss.

Add `flyway-core`, move the current schema to `V1__initial_schema.sql`, and make every
future change a numbered migration. Do this **before** you have production data, because
retrofitting Flyway afterwards is genuinely painful.

### 2.2 Backups — **S**

There is no backup story. Automated daily `mysqldump` to object storage with 30-day
retention, and — this is the part people skip — **actually restore one** to prove it works.

### 2.3 Error tracking and uptime monitoring — **S**

Right now a production exception goes to a log file nobody reads. Add Sentry (free tier
covers this) to both the backend and the React app, plus UptimeRobot pinging a health
endpoint. You need `spring-boot-starter-actuator` — `SecurityConfig` already references
`/actuator/health` but the dependency is not in `pom.xml`.

### 2.4 CI/CD — **M**

GitHub Actions: on every push run `mvn test`, `npm run build`, and a linter; on merge to
main, deploy. Removes the "works on my machine" class of problem entirely.

### 2.5 Structured logging with request IDs — **S**

When a user reports a problem you currently cannot trace what happened. Add a filter that
puts a correlation ID in the MDC, log as JSON, ship to a log aggregator.

### 2.6 Scale past one server — **M**

The STOMP broker is in-memory: two instances means users connected to server A never
receive events published on server B. Swap `enableSimpleBroker` for
`enableStompBrokerRelay` backed by RabbitMQ. Only needed when you genuinely outgrow one
box — but know it is the ceiling.

---

# Tier 3 — Features users will ask for

| Feature | Why | Size |
|---|---|---|
| Two-sided reviews — landlords rate tenants | Landlords need it to trust applicants | M |
| Map-area search ("search this area" as you pan) | The natural way to look for housing | M |
| Availability calendar UI | The data exists (`/ranges`); no calendar renders it | M |
| Roommate matching for shared flats | Your `SHARED` room type implies it | L |
| Deposit handling / escrow | The thing users are most afraid of | XL |
| Document handling — dossier, guarantor | The French rental reality; every landlord asks | L |
| Virtual tours / video | Fewer wasted visits | M |
| iCal export of confirmed stays | Landlords manage across platforms | S |
| Price suggestions from comparable listings | Helps landlords price correctly | M |
| Progressive Web App — installable, push notifications | Students live on phones | M |
| Typing indicators and read receipts in chat | Expected in any chat in 2026 | S |
| Search history and recently viewed | Small, users notice it | S |

---

# Tier 4 — Performance

### 4.1 Split the frontend bundle — **S**

One 508 KB JavaScript file (158 KB gzipped) loads before anything renders, and Leaflet is
in it even for users who never see a map. Use `React.lazy` per route and a dynamic import
for the map. Should roughly halve first load.

### 4.2 Cache the cheap things — **S**

`/api/rooms/cities` runs a `DISTINCT` over every room on every page load. Add Spring's
cache abstraction with Caffeine, evicting when a room is created or deleted.

### 4.3 Real full-text search — **M**

`RoomSpecifications` builds `LIKE '%term%'`, which cannot use an index and will crawl once
you have real volume. Move to a MySQL `FULLTEXT` index, or Elasticsearch/Meilisearch if
you want typo tolerance and relevance ranking.

### 4.4 Image delivery — **S**

Serve WebP/AVIF through a CDN with responsive `srcset`. Listing photos will be the bulk of
your bandwidth.

### 4.5 Database indexes for real query patterns — **S**

The current indexes are single-column. Once you have traffic, look at the slow query log
and add composite indexes matching your actual filters — likely `(city, available, price)`.

---

# Tier 5 — Confidence in the code

### 5.1 Widen the backend tests — **L**

Four test classes is thin for something handling other people's money and personal data.
Target the paths where a bug costs someone real: every branch of the booking state
machine, the authorisation rules (can user A read user B's messages?), the search filter
combinations. Use Testcontainers so tests run against real MySQL rather than H2 — the
dialects differ in ways that hide bugs.

### 5.2 Any frontend tests at all — **M**

There are currently zero. Vitest plus React Testing Library for the forms and the auth
context; Playwright for one end-to-end path: register → search → book → chat.

### 5.3 Move the JWT out of localStorage — **M**

`api/client.js` stores the token in `localStorage`, which any successful XSS can read. An
httpOnly, SameSite=Strict cookie cannot be read by JavaScript. This means adding CSRF
protection back, which is why it is a real piece of work rather than a one-liner — but it
is the correct design for a site holding personal data.

### 5.4 A React error boundary — **S**

There is none, so one render error blanks the entire page. Wrap the router in a boundary
that shows something recoverable and reports to Sentry.

### 5.5 Linting and formatting in CI — **S**

ESLint + Prettier for the frontend, Checkstyle or Spotless for Java. Enforce in CI so
style stops being a discussion.

### 5.6 Accessibility pass — **M**

Run axe DevTools. Likely findings: colour contrast on muted text, the chat needs
`aria-live`, focus management when modals open and close, and full keyboard navigation of
the listing grid. This is also a legal requirement for some French services.

---

# Tier 6 — Growth

- **Deploy it somewhere real.** Railway or Render for the API, Vercel or Netlify for the
  frontend, PlanetScale or Railway for MySQL. A live URL changes everything.
- SEO: server-render or pre-render the listing pages, add structured data, generate a
  sitemap. Right now Google sees an empty `<div id="root">`.
- Analytics: Plausible or Umami — GDPR-friendly, unlike Google Analytics.
- Onboarding: an empty-state tour for first-time landlords.
- Referral: students share housing finds constantly.
- Partner with student unions and CROUS for listing supply. A marketplace with no listings
  is worth nothing, and supply is the harder side.

---

# If you only do six things

1. **0.1** Rotate the JWT secret — you are currently exploitable by anyone reading GitHub
2. **0.2 + 0.3** Persistent notifications and email — without them the product does not function
3. **0.5** GDPR basics — a legal obligation, not a feature
4. **1.1** Real image uploads — nobody pastes URLs
5. **1.2** Automatic geocoding — nobody knows their coordinates
6. **2.1** Flyway — do it before you have data you cannot afford to lose

---

# Suggested order

**Weeks 1–2** — Tier 0 in full. Nothing ships before this.
**Weeks 3–5** — 1.1, 1.2, 1.6, 2.1, 2.2, 2.3, then deploy for real.
**Weeks 6–8** — 1.4 French, 1.5 saved searches, 2.4 CI/CD, 4.1 bundle split.
**Ongoing** — 1.3 scam prevention alongside Tier 5, growing as you learn how people
actually abuse the platform.
