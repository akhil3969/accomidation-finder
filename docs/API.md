# API reference

Base URL: `http://localhost:8080`
Interactive version: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`

All request and response bodies are JSON. Dates are ISO-8601 (`2026-09-01`),
timestamps are ISO-8601 local date-times (`2026-09-01T14:32:05`).

## Authentication

Every protected endpoint expects the JWT returned by register/login:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Tokens are valid for 24 hours by default (`JWT_EXPIRATION_MS`).

## Error format

Every non-2xx response uses the same body:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Those dates are already requested or booked",
  "path": "/api/bookings",
  "timestamp": "2026-08-16T11:04:22"
}
```

Validation failures add a `fieldErrors` map:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": { "email": "Email must be valid", "password": "Password must be between 8 and 72 characters" },
  "timestamp": "2026-08-16T11:04:22"
}
```

| Status | Meaning |
|---|---|
| 400 | Validation failed or the request is semantically invalid |
| 401 | Missing, expired or malformed token |
| 403 | Authenticated, but not allowed to touch this resource |
| 404 | No such id |
| 409 | Conflicts with existing state (duplicate email, overlapping booking) |

## Pagination

List endpoints accept `page` (0-based), `size` and `sort` (`field,asc|desc`) and return:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 12,
  "totalElements": 8,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

## Authentication — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | — | Create an account, returns a token |
| POST | `/api/auth/login` | — | Exchange credentials for a token |
| GET | `/api/auth/me` | ✔ | The signed-in user |

**POST `/api/auth/register`**

```json
{ "name": "Ada Lovelace", "email": "ada@example.com", "password": "password123", "phone": "+33...", "role": "TENANT" }
```

`role` accepts `TENANT` or `LANDLORD`; anything else (including `ADMIN`) becomes `TENANT`.

Response `201`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": { "id": 7, "name": "Ada Lovelace", "email": "ada@example.com", "role": "TENANT", "createdAt": "..." }
}
```

---

## Rooms — `/api/rooms`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/rooms` | — | Search listings |
| GET | `/api/rooms/{id}` | — | One listing |
| GET | `/api/rooms/cities` | — | Distinct cities, for autocomplete |
| GET | `/api/rooms/mine` | LANDLORD | Your own listings |
| GET | `/api/rooms/mine/stats` | LANDLORD | Dashboard counters |
| POST | `/api/rooms` | LANDLORD | Publish → broadcasts `/topic/rooms/new` |
| PUT | `/api/rooms/{id}` | owner | Update |
| PATCH | `/api/rooms/{id}/availability` | owner | Flip availability → broadcasts `/topic/rooms/availability` |
| DELETE | `/api/rooms/{id}` | owner | Delete |

**Search parameters**

| Param | Type | Example |
|---|---|---|
| `query` | string | `studio near EPITA` (matches title, description, city, address) |
| `city` | string | `Paris` |
| `minPrice` / `maxPrice` | decimal | `500` / `900` |
| `roomType` | enum | `STUDIO`, `SHARED`, `APARTMENT`, `HOUSE` |
| `minSize` | int | `20` (m²) |
| `bedrooms` | int | `2` (at least) |
| `availableOnly` | bool | `true` |
| `furnished` / `billsIncluded` | bool | `true` |
| `availableFrom` | date | `2026-09-01` |
| `landlordId` | long | `2` |
| `page` / `size` / `sort` | | `0` / `12` / `price,asc` |

```
GET /api/rooms?city=Paris&maxPrice=900&availableOnly=true&sort=price,asc&page=0&size=12
```

**Room response**

```json
{
  "id": 1,
  "title": "Bright studio 5 min from EPITA",
  "description": "Fully renovated 22 m2 studio ...",
  "city": "Le Kremlin-Bicetre",
  "address": "12 Rue de la Liberte",
  "postalCode": "94270",
  "country": "France",
  "price": 750.00,
  "deposit": 750.00,
  "sizeSqm": 22,
  "bedrooms": 1,
  "bathrooms": 1,
  "maxGuests": 1,
  "available": true,
  "furnished": true,
  "billsIncluded": true,
  "availableFrom": "2026-08-23",
  "minStayMonths": 6,
  "roomType": "STUDIO",
  "latitude": 48.814500,
  "longitude": 2.360700,
  "images": ["https://..."],
  "amenities": ["Wi-Fi", "Desk"],
  "landlord": { "id": 2, "name": "Jean Dupont", "role": "LANDLORD" },
  "averageRating": 4.5,
  "reviewCount": 2,
  "favorite": false,
  "createdAt": "...",
  "updatedAt": "..."
}
```

`favorite` is `null` for anonymous callers and a boolean once signed in.

**PATCH `/api/rooms/{id}/availability`** — body `{ "available": false }`.

---

## Bookings — `/api/bookings`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/bookings` | ✔ | Request a stay |
| GET | `/api/bookings/mine` | ✔ | Requests you sent |
| GET | `/api/bookings/requests` | LANDLORD | Requests on your listings |
| GET | `/api/bookings/{id}` | participant | One booking |
| PATCH | `/api/bookings/{id}/decision` | LANDLORD | Accept / decline / complete |
| PATCH | `/api/bookings/{id}/cancel` | tenant | Withdraw |
| GET | `/api/bookings/room/{roomId}/ranges` | — | Dates already held, for the calendar |

**POST `/api/bookings`**

```json
{ "roomId": 1, "checkIn": "2026-09-01", "checkOut": "2027-06-30", "guests": 1, "message": "Hello!" }
```

Rejected with `409` when the dates overlap a `PENDING` or `CONFIRMED` booking, and with
`400` when you try to book your own listing, exceed `maxGuests`, or set `checkOut` on or
before `checkIn`. `totalPrice` is the monthly rent pro-rated over a 30-day month and is
snapshotted at request time.

**PATCH `/api/bookings/{id}/decision`**

```json
{ "status": "CONFIRMED", "response": "Happy to have you - see you on the 1st." }
```

Accepting also takes the listing off the market and broadcasts the availability change.

---

## Messages — `/api/messages`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/messages` | ✔ | Send (HTTP fallback for the WebSocket) |
| GET | `/api/messages/inbox` | ✔ | One row per conversation |
| GET | `/api/messages/conversation?roomId=&userId=` | ✔ | Full thread; marks it read |
| GET | `/api/messages/unread-count` | ✔ | `{ "unread": 3 }` |

---

## Reviews — `/api/reviews`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/reviews/room/{roomId}` | — | Reviews for a listing |
| POST | `/api/reviews/room/{roomId}` | ✔ | Leave a review (`{ "rating": 5, "comment": "..." }`) |
| DELETE | `/api/reviews/{id}` | author | Delete your review |

Posting requires a `CONFIRMED` or `COMPLETED` booking on that listing, and one review per
tenant per listing.

## Favourites — `/api/favorites`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/favorites` | ✔ | Saved listings |
| GET | `/api/favorites/ids` | ✔ | Just the ids |
| POST | `/api/favorites/{roomId}/toggle` | ✔ | `{ "roomId": 1, "favorite": true }` |

## Users — `/api/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/users/me` | ✔ | Your profile |
| PUT | `/api/users/me` | ✔ | Update name / phone / bio / avatar |
| POST | `/api/users/me/password` | ✔ | Change password |
| POST | `/api/users/me/become-landlord` | ✔ | Upgrade a tenant account |
| GET | `/api/users/{id}` | ✔ | Public profile |

## Admin — `/api/admin`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/admin/users` | ADMIN | All accounts |
| PATCH | `/api/admin/users/{id}/enabled` | ADMIN | Enable / disable an account |
| GET | `/api/admin/stats` | ADMIN | Platform counters |

---

## WebSocket

Endpoint: `ws://localhost:8080/ws` (SockJS) or `/ws-native` (raw WebSocket).
Authenticate by sending `Authorization: Bearer <token>` as a STOMP **CONNECT** header —
required only for the private `/user/queue/**` destinations.

| Destination | Direction | Payload |
|---|---|---|
| `/topic/rooms/new` | server → all | `RoomResponse` for a newly published listing |
| `/topic/rooms/availability` | server → all | `{ "roomId": 1, "available": false, "title": "...", "city": "..." }` |
| `/topic/rooms/{id}` | server → all | `RoomResponse` after an edit |
| `/topic/rooms/{id}/availability` | server → all | Availability event for one listing |
| `/user/queue/messages` | server → user | `MessageResponse` |
| `/user/queue/bookings` | server → user | `BookingResponse` on any status change |
| `/app/chat.send` | client → server | `{ "recipientId": 2, "roomId": 1, "content": "Hi!" }` |

```js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('/ws'),
  connectHeaders: { Authorization: `Bearer ${token}` },
  onConnect: () => {
    client.subscribe('/topic/rooms/availability', (frame) => {
      console.log('availability changed', JSON.parse(frame.body));
    });
  },
});
client.activate();
```
