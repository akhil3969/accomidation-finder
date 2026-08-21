# 🏠 Accommodation Finder

> Real-time accommodation search, booking and messaging — built as a portfolio project at **EPITA Paris**.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-18-61dafb?style=flat-square&logo=react)
![Vite](https://img.shields.io/badge/Vite-5-646cff?style=flat-square&logo=vite)
![Framer Motion](https://img.shields.io/badge/Framer%20Motion-11-0055ff?style=flat-square&logo=framer)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1?style=flat-square&logo=mysql)
![WebSocket](https://img.shields.io/badge/STOMP-WebSocket-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-black?style=flat-square)

---

## What it does

Finding a room as a student is a race: the good listings go within hours and you only
find out they are gone after you have written the email. This app closes that gap.
Availability, new listings, booking decisions and chat messages are all pushed over a
STOMP WebSocket, so every open browser sees the truth at the same moment — no polling,
no refresh button.

| | |
|---|---|
| 🔍 **Search & filter** | Full-text search plus city, price range, type, size, furnishing, bills and availability filters, with sorting and pagination |
| 📡 **Real-time** | New listings, availability flips, booking decisions and chat all stream over `/ws` |
| 📅 **Booking flow** | Request → landlord accepts/declines → confirmed stay, with overlap detection so two people cannot hold the same dates |
| 💬 **Live chat** | Per-listing threads between tenant and landlord, delivered on a private user queue |
| 🗺️ **Map** | Leaflet map that follows the result set, with colour-coded availability pins |
| ⭐ **Reviews** | Star ratings, restricted to tenants who actually stayed |
| ❤️ **Favourites** | One-click save, batched into the listing grid in a single request |
| 👤 **Roles** | `TENANT`, `LANDLORD` and `ADMIN`, enforced both at the URL and the method level |
| 🔐 **Auth** | Stateless JWT, BCrypt hashing, JSON error bodies, WebSocket handshake authentication |

---

## What makes this different from a listings site

Most rental sites answer "what is available?". Someone arriving in France from
another country has four other questions first, and nobody answers them.

### What will this flat actually cost me?

The French state pays part of most students' rent through a benefit called APL.
Almost no international student knows it exists, and it is never paid
automatically - you apply, and it is not backdated, so applying late is money
simply lost.

Every listing shows the real figure. A EUR 750 studio commonly costs around
EUR 550 once housing benefit is counted, and that changes which flats someone
even considers. The estimator shows its full working - the rent ceiling for that
zone, the bills allowance, the income the state assumes for a student - because a
number with no explanation teaches nobody, and this one is worth understanding.

There is a standalone calculator at `/cost` that needs no account, so somebody
still deciding whether they can afford to come can get an answer first.

### Can I even apply without a French guarantor?

Nearly every landlord demands a *garant*: someone living in France, earning a
French salary, who signs to cover your rent. If your family is abroad you cannot
provide one, and this is the single most common reason international students are
refused a flat.

Visale is the French state doing that job, free, for anyone aged 18 to 30 of any
nationality. The app checks eligibility against the current ceilings and explains
how to apply - before you start viewing, which is when it actually helps.

### What paperwork do they expect?

French landlords all want the same bundle of documents, and the list is unwritten
because everyone who grew up here already knows it. `/checklist` turns it into a
tickable list, filtered to your situation, with a "if you cannot get this, do
that instead" note on every item.

### Is this listing real?

Rental fraud targets newcomers specifically - they cannot visit before arriving,
they do not know what a normal price looks like, and they are short of time. The
pattern barely varies: below-market rent, an owner conveniently abroad, and a
request to wire a deposit before anyone has seen a key.

Every listing is scored against those signals and the findings are shown in plain
words, alongside what is *reassuring* about it. Flagged listings go to a
moderation queue rather than being hidden automatically - quietly removing honest
landlords would be its own kind of harm.

### Where the official numbers come from

Rent ceilings, the bills allowance, the assumed student income and the Visale
limits are all set by decree and change at least yearly. They live in a database
table with their source and the date they were last checked, editable from the
admin panel. Hard-coding them would mean the estimator quietly starts lying twelve
months from now and nobody notices.

Figures currently loaded: CAF ceilings in force from 1 October 2025, Visale
ceilings raised 6 January 2026.

---

## Real APIs, no keys required

| What | Service | Key needed |
|---|---|---|
| Address to coordinates, INSEE code, postcode | Base Adresse Nationale (`api-adresse.data.gouv.fr`) | No |
| Nearby transport, shops, pharmacies | OpenStreetMap via Overpass | No |
| Map tiles | OpenStreetMap | No |

Landlords were previously asked to type a latitude and longitude, which nobody
knows - so listings never appeared on the map. Addresses are now geocoded on save,
which also yields the postcode the housing-aid zone depends on.

Both services degrade to a clear message rather than an error if they are
unreachable, and neighbourhood lookups are cached for twelve hours because
Overpass is free and community-funded.

There is no public CAF API. The estimate is computed here from the published
figures and links out to the official simulator.

---

## The admin side

`/admin`, four tabs:

- **Overview** - signups, listings and bookings over time on one chart, plus where
  users are arriving from, listings by city, and the booking funnel. Every chart
  has a "show numbers" table view.
- **Moderation** - the report queue, landlord verification, the listings the scam
  scorer flagged, and an audit log of every moderator decision. Moderation without
  a trail is just an unaccountable delete button.
- **People & listings** - browse, search, edit roles, enable, disable and delete,
  without touching the database.
- **Content & figures** - edit every guide, every checklist item, and every
  official figure behind the aid estimates.

---

## Tech stack

| Layer | Choice | Why |
|---|---|---|
| Backend | Java 17, Spring Boot 3.2 | Records for DTOs, pattern matching, long-term support |
| Persistence | Spring Data JPA, MySQL 8 | JPA Specifications give the dynamic search filter without string SQL |
| Real-time | STOMP over SockJS | Named destinations and a per-user queue, with an automatic fallback transport |
| Security | Spring Security 6 + JJWT 0.12 | Stateless, no server-side session to scale |
| Frontend | React 18 + Vite 5 | Fast dev server, tiny production bundle |
| Routing / HTTP | React Router 6, axios | Interceptors attach the JWT and normalise error messages |
| Map | Leaflet + react-leaflet | No API key, OpenStreetMap tiles |
| Motion | Framer Motion 11 | Route, list and dialog transitions; honours `prefers-reduced-motion` through `MotionConfig` |
| Docs | springdoc-openapi | Swagger UI with a working Authorize button |

---

## Run it

### Option A — zero setup (recommended for a first look)

The backend defaults to the `demo` profile: an in-memory H2 database that seeds itself
with eight listings, six accounts, bookings, reviews and a chat thread on startup.

```bash
# terminal 1 - API on :8080
cd backend
mvn spring-boot:run

# terminal 2 - UI on :5173
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. Sign in with any of the demo accounts:

| Role | Email | Password |
|---|---|---|
| Tenant | `akhil@epita.fr` | `password123` |
| Landlord | `jean@landlord.fr` | `password123` |
| Admin | `admin@accomfinder.app` | `password123` |

> The login page has one-click buttons that fill these in for you.

### Option B — MySQL

```bash
# create the schema and load the demo data
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql

cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Connection details come from environment variables (`DB_HOST`, `DB_USER`, `DB_PASSWORD`, …);
see `backend/.env.example`. The defaults are `localhost:3306`, `root` / `root`.

### Option C — Docker

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | <http://localhost:3000> |
| API | <http://localhost:8080> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| Adminer (DB browser) | <http://localhost:8081> |

### Prerequisites

- Java 17+ and Maven 3.8+
- Node.js 18+ (20 recommended)
- MySQL 8 — only for Option B; Options A and C do not need it installed

---

## Seeing the real-time part work

The feature is easiest to appreciate with two windows side by side:

1. Open <http://localhost:5173> in a normal window and sign in as **akhil@epita.fr**.
2. Open a second window (private/incognito) and sign in as **jean@landlord.fr**.
3. In Jean's window go to **Dashboard** and hit *Mark taken* on "Bright studio 5 min from EPITA".
4. Akhil's search results update the badge and flash the card — with no refresh.
5. Now request a booking as Akhil: the request appears in Jean's dashboard instantly, and
   Jean's accept/decline lands on Akhil's **My bookings** page the moment he clicks.
6. Open a listing and use **Message the landlord** — the chat is live in both directions.

---

## Project layout

```
accommodation-finder/
├── backend/                          Spring Boot API
│   ├── src/main/java/com/accommodationfinder/
│   │   ├── config/                   WebSocket, OpenAPI, Jackson, data seeding
│   │   ├── controller/               REST controllers + the STOMP chat controller
│   │   ├── dto/                      Java records: requests, responses, error body
│   │   ├── exception/                Typed exceptions + @RestControllerAdvice
│   │   ├── mapper/                   Entity ↔ DTO translation
│   │   ├── model/                    JPA entities and enums
│   │   ├── repository/               Spring Data repositories + search Specifications
│   │   ├── security/                 JWT service, filter, SecurityConfig, principal
│   │   └── service/                  Business rules
│   ├── src/main/resources/           application.yml + demo / dev / prod profiles
│   ├── src/test/java/                Unit and integration tests
│   └── Dockerfile
├── frontend/                         React single-page app
│   ├── src/api/                      axios instance + typed endpoint map
│   ├── src/components/               Reusable UI (cards, map, filters, modal, …)
│   ├── src/context/                  Auth, toasts, confirm dialogs, inbox, STOMP
│   ├── src/hooks/                    Debounce, form validation, media query, title
│   ├── src/motion/tokens.js          Shared durations, easings and variants
│   ├── src/pages/                    One file per route
│   ├── src/styles/index.css          Design tokens, light + dark, responsive
│   └── Dockerfile / nginx.conf
├── database/
│   ├── schema.sql                    MySQL 8 DDL (authoritative)
│   └── seed.sql                      Demo data with real BCrypt hashes
├── docs/
│   ├── API.md                        Every endpoint, with examples
│   └── ARCHITECTURE.md               How the pieces fit and why
└── docker-compose.yml
```

---

## Tests

```bash
cd backend
mvn test
```

Covers the JWT service, the booking rules (overlaps, own-listing, guest limits, date
validation) and an end-to-end auth flow through `MockMvc` against an in-memory database.

---

## API

Full reference in [`docs/API.md`](docs/API.md), or browse it interactively at
`/swagger-ui.html` once the backend is running. A taste:

```bash
# register
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada Lovelace","email":"ada@example.com","password":"password123","role":"TENANT"}'

# search
curl 'http://localhost:8080/api/rooms?city=Paris&maxPrice=900&availableOnly=true&sort=price,asc'

# anything protected
curl http://localhost:8080/api/bookings/mine -H "Authorization: Bearer $TOKEN"
```

---

## Configuration

Every setting has a working default; override with environment variables.

| Variable | Default | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `demo` | `demo` (H2), `dev` (MySQL), `prod` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `accommodation_finder` | `dev` profile |
| `DB_USER` / `DB_PASSWORD` | `root` / `root` | `dev` profile |
| `JWT_SECRET` | development value | **Change in production.** Minimum 32 characters |
| `JWT_EXPIRATION_MS` | `86400000` | 24 hours |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma separated |
| `VITE_API_BASE_URL` | empty | Frontend: leave empty to use the Vite dev proxy |

---

## Deployment notes

- Run with `SPRING_PROFILES_ACTIVE=prod`. That sets `ddl-auto=validate` and disables
  seeding, so the schema must already exist — apply `database/schema.sql` first.
- Supply a real `JWT_SECRET`; the app refuses to start on a secret shorter than 32 characters.
- `npm run build` emits a static `dist/` — serve it from any CDN or the bundled nginx image.
- The in-memory STOMP broker is fine for a single instance. To run more than one, swap
  `enableSimpleBroker` for `enableStompBrokerRelay` and put RabbitMQ or ActiveMQ behind it.

---

## Roadmap

- [x] Project setup and repository structure
- [x] Room CRUD API with dynamic search filters
- [x] WebSocket real-time availability
- [x] React search UI with map integration
- [x] Booking system with overlap detection
- [x] Live chat between tenant and landlord
- [x] JWT authentication with tenant / landlord / admin roles
- [x] Favourites and reviews
- [x] Seed data, Docker Compose and API documentation
- [ ] Image upload to object storage (currently URLs)
- [ ] Email notifications
- [ ] Deployment on Railway / Render

---

## Author

**Kona Sai Akhil** — MSc student, EPITA Paris
[![GitHub](https://img.shields.io/badge/GitHub-akhil3969-black?style=flat-square&logo=github)](https://github.com/akhil3969)

## License

MIT — free to use and modify.
