# Architecture

## The shape of it

```
                 ┌──────────────────────────────┐
   Browser  ───► │  React 18 SPA (Vite)         │
                 │  axios ── REST ──┐           │
                 │  STOMP ── WS ──┐ │           │
                 └────────────────┼─┼───────────┘
                                  │ │
                       ┌──────────▼─▼───────────┐
                       │  Spring Boot 3.2       │
                       │                        │
                       │  Controller  (HTTP)    │
                       │      │                 │
                       │  Service     (rules)   │───► NotificationService ──► STOMP broker
                       │      │                 │
                       │  Repository  (JPA)     │
                       └──────────┬─────────────┘
                                  │
                            ┌─────▼─────┐
                            │  MySQL 8  │   (H2 in the demo profile)
                            └───────────┘
```

## Layering rules

The backend keeps three strict layers, and the rules are what make it readable:

- **Controllers** only translate HTTP to method calls. They never touch a repository and
  never contain a business rule. They validate with `@Valid`, resolve the caller through
  `SecurityUtils`, and hand off.
- **Services** own every rule and every transaction boundary. A service may call other
  services (`BookingService` uses `NotificationService`) but never a controller.
- **Repositories** are Spring Data interfaces. Dynamic filtering lives in
  `RoomSpecifications` rather than in a string query, so filters compose without
  string concatenation and stay type-checked against the entity model.

Entities never leave the service layer. Controllers return records from `dto/`, produced
by the mappers. That is what keeps the password hash out of every JSON response — there
is no field on `UserSummary` that could carry it.

## Why records for DTOs

Java 17 records give immutable, boilerplate-free payloads that Jackson serialises and
deserialises without configuration, and bean validation annotates cleanly on the
components. Entities keep explicit getters and setters because JPA needs mutability and
proxying — mixing the two styles is deliberate, not an oversight.

## Search

`GET /api/rooms` takes eleven optional filters. Building that as query methods would mean
a combinatorial explosion of repository signatures, and as a hand-written JPQL string it
would be a concatenation minefield. `RoomSpecifications.fromCriteria` builds a
`Specification<Room>` by appending only the predicates whose filter is present, then
Spring Data pairs it with the `Pageable` for sorting and paging.

One detail worth pointing out: the specification adds a `fetch` join on the landlord only
when the query is not the count query (`Long.class != query.getResultType()`). Without
that guard, Hibernate would refuse the count query for a query that specifies a fetch.

## The N+1 problem in the listing grid

Each room card shows an average rating, a review count and whether the caller has saved
it. Naively that is three queries per row. `RoomService.decorate` instead:

1. collects the ids on the current page,
2. runs one grouped aggregate (`ReviewRepository.aggregateRatings`) for all of them,
3. runs one query for the caller's favourite ids,
4. zips the results into the DTOs in memory.

A page of twelve rooms costs three queries instead of thirty-seven.

## Real-time

`NotificationService` is the only class that talks to `SimpMessagingTemplate`. Everything
else calls a named method (`roomPublished`, `availabilityChanged`, `chatMessage`,
`bookingEvent`). Two consequences: destinations are declared in one file rather than
scattered as string literals, and a broker failure is caught and logged there instead of
rolling back the HTTP request that triggered it — a dropped notification should never
fail a booking.

Public events go to `/topic/**` and are readable by anonymous visitors, which is what
makes the availability badge live for someone who has not signed in. Private events go to
`/user/queue/**`; Spring routes those by `Principal`, which is why
`WebSocketAuthChannelInterceptor` authenticates the STOMP `CONNECT` frame. A frame without
a token still connects — it simply gets no private queue.

On the client, `RealtimeContext` owns a single `Client` for the whole app. Components call
`useSubscription(destination, handler)`; the provider keeps a `destination → handlers` map
and re-binds every destination in `onConnect`, so a reconnect after a network drop
restores every subscription without the components knowing.

## Booking overlap

Two people must not hold the same dates. The check is one query:

```sql
b.checkIn < :checkOut AND b.checkOut > :checkIn
```

Two half-open ranges overlap exactly when each starts before the other ends. It runs on
request (against `PENDING` and `CONFIRMED`) and again on confirmation (against `CONFIRMED`
only, excluding the booking being confirmed) — because between a request and its
acceptance the landlord may have confirmed somebody else.

`totalPrice` is written at request time rather than computed on read, so a later price
edit does not silently rewrite what a tenant agreed to.

## Security

Stateless JWT: `JwtAuthenticationFilter` reads the bearer token, loads the user, and
populates the `SecurityContext` for that one request. An invalid token clears the context
rather than throwing, so a stale token in localStorage degrades to "anonymous" instead of
a 500.

Authorisation is enforced twice on purpose:

- **URL level** in `SecurityConfig` — coarse, protects everything by default (`anyRequest().authenticated()`).
- **Method level** via `@PreAuthorize`, plus `SecurityUtils.requireOwnerOrAdmin` inside the
  services for ownership. Role alone cannot express "the landlord *of this listing*".

`RestAuthenticationEntryPoint` returns the same JSON `ApiError` shape as every other
failure, so the frontend has exactly one error path to handle.

## Profiles

| Profile | Database | `ddl-auto` | Seeding |
|---|---|---|---|
| `demo` (default) | H2 in-memory | `create-drop` | on |
| `dev` | MySQL | `update` | on (only if empty) |
| `prod` | from `DATABASE_URL` | `validate` | off |

`demo` exists so the project runs on a machine with nothing installed but a JDK — which
matters for a portfolio project someone will try in under five minutes. `prod` uses
`validate` because letting Hibernate alter a production schema is how data goes missing;
`database/schema.sql` is the source of truth there.

## Frontend structure

- `api/` — one axios instance with the JWT interceptor, and one module listing every
  endpoint. No component ever builds a URL.
- `context/` — three providers: `AuthProvider` (session), `ToastProvider` (feedback),
  `RealtimeProvider` (the socket). They nest in that order because the socket needs to
  reconnect when the identity changes.
- `components/` — presentational and reusable; they take props and raise callbacks.
- `pages/` — one per route, owns data fetching and state for that screen.
- `styles/index.css` — a token layer (`--surface`, `--text`, `--radius`, …) then component
  rules that only reference tokens. Dark mode is a single `prefers-color-scheme` block
  that redefines the tokens, so no component has a dark variant.

## Known limits

- The STOMP broker is in-memory, so horizontal scaling needs a relay (RabbitMQ/ActiveMQ).
- Images are URLs, not uploads — an object store and a signed-upload endpoint would be next.
- No refresh-token rotation; the access token simply expires after 24 hours.
- Search is `LIKE`-based. Past a few tens of thousands of listings this wants a real
  full-text index.
