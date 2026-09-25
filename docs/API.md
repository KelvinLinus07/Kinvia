# Kinvia API Reference

Base URL: `http://localhost:8080` (or the host you deploy to). All request/response bodies are JSON
unless noted. Authenticated endpoints require `Authorization: Bearer <token>`.

Every error response has this shape:

```json
{
  "timestamp": "2026-09-22T10:15:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Origin and destination must be different",
  "path": "/api/journeys/search",
  "fieldErrors": {}
}
```

`fieldErrors` is populated (field name → message) only for Bean Validation failures.

Paginated list endpoints return:

```json
{ "items": [...], "page": 0, "size": 20, "totalItems": 42, "totalPages": 3 }
```

---

## Auth — `/api/auth`

### `POST /api/auth/register`
Public. Creates a PASSENGER account and returns a session.

```json
// request
{ "email": "asha@example.com", "password": "at-least-8-chars", "fullName": "Asha Traveller", "phone": "9000000000" }
// response 201
{ "accessToken": "...", "expiresAt": "...", "user": { "id": 4, "email": "...", "fullName": "...", "role": "PASSENGER", ... } }
```

### `POST /api/auth/login`
Public. `{ "email": "...", "password": "..." }` → same `AuthResponse` shape as register.

---

## Users — `/api/users/me`

All require authentication.

- `GET /` — current profile
- `PUT /` — `{ "fullName": "...", "phone": "..." }`
- `POST /password` — `{ "currentPassword": "...", "newPassword": "..." }` → 204
- `GET /passengers`, `POST /passengers`, `PUT /passengers/{id}`, `DELETE /passengers/{id}` — saved
  travellers (`{ fullName, age, gender: MALE|FEMALE|OTHER, mobile }`)
- `GET /preferences`, `PUT /preferences` — `{ transportPreference: ANY|TRAIN|BUS, ranking: BALANCED|FASTEST|CHEAPEST|FEWEST_TRANSFERS|COMFORTABLE, maxTransfers: 0-2, seatPreference: NO_PREFERENCE|WINDOW|AISLE, journeyReminders: bool }`
- `GET /saved-journeys`, `POST /saved-journeys`, `DELETE /saved-journeys/{id}`

---

## Stations — `/api/stations`

Public. `GET /api/stations?q=patna&limit=10` — partial-text search over station name, city and code,
used by the frontend's autocomplete. Results are ordered by relevance: an exact code first, then names
starting with the text, then cities starting with it, then everything else containing it. `limit` is
1-50 (default 20). Each item has `id`, `code`, `name`, `city`, `state`, `kind`, `latitude`, `longitude`.
Without `q` it returns the first `limit` stations by name.

Station codes for the imported timetable are Kinvia's own short codes, not official railway codes.

---

## Journeys — `/api/journeys`

Public (personalises automatically if a token is supplied).

### `GET /search`
Query params: `originId`, `destinationId`, `date` (ISO, today..+30 days), `passengers` (1-6),
`transport` (`ANY`|`TRAIN`|`BUS`, optional), `ranking` (optional), `maxTransfers` (optional, 0-2), plus
the optional limits below.

| Param | Meaning |
|---|---|
| `timeOfDay` | Departure band: `NIGHT` 00:00-05:59, `MORNING` 06:00-11:59, `AFTERNOON` 12:00-17:59, `EVENING` 18:00-23:59 |
| `departAfter`, `departBefore` | An exact departure range as `HH:mm`, both ends inclusive. Give one or both; a missing end defaults to the start/end of the day. A start later than the end wraps past midnight (`22:00`-`02:00`). Cannot be combined with `timeOfDay` |
| `maxBudget` | Maximum fare **per traveller** in rupees (> 0). Judged against the fare the result shows: the cheapest class that still has room for the whole party, summed over the journey's legs |

The time applies to the departure from `originId`. Both limits are applied on the server before the
result cap (`kinvia.journey.max-results`, default 30), so a limit never hides journeys behind ones that
were cut off. Invalid combinations return 400.

Returns every itinerary the planner found between the two stations for that date that can seat the
requested party and satisfies the limits, ranked by the chosen strategy, each carrying a `key` you use
to proceed to booking. Besides `journeys` the response echoes the applied `filter` and reports
`matchedBeforeFilters` - how many journeys the route, date and party size produced *before* the time and
budget limits - so a client can tell "nothing runs" from "nothing fits your limits".

Point-to-point services from the imported timetable are created in the operational network the first time
their station pair is searched (see the README, "Imported timetable"), so the very first search of a pair
takes a little longer than later ones.

Each item in a journey's `segments` carries its own `classes` array (`coachClass`, `label`, `fare`,
`availableSeats`) — the travel classes actually available for that leg, priced and counted from real seat
inventory. A segment never lists a class it does not offer (for example, a train row with no fare for a
given class simply has no entry for it). Current train classes: `SLEEPER`, `AC_3_TIER`, `AC_2_TIER`,
`AC_1_TIER`. Current bus classes: `SEATER`, `AC_SEATER`, `AC_SLEEPER`. `farePerPassenger` /
`totalFare` on the journey itself are the cheapest class that still fits the whole party, summed over
legs — a "starting from" figure, not the fare for any one class.

### `GET /{key}`
Re-resolves one journey (by the key returned from search) against **current** data — fares, delays and
seat availability may have changed since the search. `?passengers=N` (default 1).

### `POST /interpret`
`{ "text": "cheapest bus from Siliguri to Patna next Friday for 2 people" }` → a best-effort structured
interpretation (`originId`, `destinationId`, `date`, `passengers`, `transport`, `ranking`, plus `notes`
explaining anything it could not confidently parse). Purely rule-based; no external service is called.

---

## Seats & holds

### `GET /api/trips/{tripId}/seats?board={seq}&alight={seq}`
Public. Seat map for one trip between two stop sequences (from a journey's segment), grouped by coach,
each seat tagged `AVAILABLE` / `HELD` / `BOOKED` (and `mine: true` if you currently hold it). Each coach
carries its own `coachClass`, `classLabel` and `fare`, so a client that has already let the traveller
pick a class (from the segment's `classes`) filters this list down to the coach(es) of that class before
rendering seats — the seat's coach still decides the fare charged, so this stays consistent with what
was quoted during class selection.

### `POST /api/trips/{tripId}/seats/{seatId}/hold?board={seq}&alight={seq}`
Authenticated. Places a short-lived hold (default 10 minutes) on the seat for the given section of the
trip. Returns a `HoldDto` with the `id` you pass into booking creation. Concurrency-safe: the trip row
is locked for the duration of the check, so two callers can never be granted the same seat over
overlapping stops.

### `GET /api/holds`, `DELETE /api/holds/{id}`
List your own unattached holds, or release one early.

---

## Bookings — `/api/bookings`

All require authentication.

### `POST /`
```json
{
  "journeyKey": "12.0.4~57.1.3",
  "passengers": [{ "fullName": "Asha Traveller", "age": 29, "gender": "FEMALE", "mobile": "9000000003" }],
  "seats": [{ "segmentIndex": 0, "passengerIndex": 0, "holdId": 501 }]
}
```
Every `(segmentIndex, passengerIndex)` pair must appear exactly once (one seat per passenger per
segment), and every referenced hold must belong to you, be unattached, unexpired, and match the
segment's trip/stops exactly. Returns a `BookingDto` in `HELD` status.

### `GET /?scope=UPCOMING|PREVIOUS|ALL&page=&size=` — paginated booking summaries
### `GET /{id}` — full booking detail (segments, passengers, seats, payment)
### `GET /{id}/cancellation-preview` — `{ cancellable, refundPercent, refundAmount, message }`
### `POST /{id}/cancel` — cancels and (if paid) triggers a refund per the cancellation policy

---

## Payment — `/api/bookings/{id}/payment`

### `POST /`
```json
{ "method": "UPI", "instrument": "asha@upi" }
```
`method` is `UPI`, `CARD`, or `NETBANKING`. This is the **development payment provider** — no real
money moves. An instrument ending in `0002`, or containing the word "fail", is declined (useful for
testing the failure path); everything else is approved. On success the booking is confirmed, seats are
finalised, and a ticket is issued.

---

## Tickets — `/api/bookings/{id}/ticket`

- `GET /` — ticket JSON (passengers, seats, QR payload/SVG)
- `GET /download` — the same ticket as a PDF (`application/pdf`)

---

## Notifications — `/api/notifications`

- `GET /?page=&size=`
- `GET /unread-count`
- `PATCH /{id}/read`, `POST /read-all`

---

## Tracking — `/api/trips/{id}/tracking`

Public. Returns a simulated status (`live: false` always) derived from the schedule and any reported
delay — never presented as real GPS data.

---

## Admin — `/api/admin/*` (role: ADMIN)

`GET /dashboard` (system analytics), `GET /users`, `PUT /users/{id}/role`, `PUT /users/{id}/status`,
`GET|POST|PUT /stations`, `GET|POST|PUT /operators`, `GET /bookings?status=`,
`GET /notifications`, `POST /notifications/broadcast`.

## Operator — `/api/operator/*` (role: OPERATOR or ADMIN)

Scoped automatically to the caller's own operator (an ADMIN caller must not rely on this — operator
endpoints are meant for operator accounts). `GET /dashboard`, `GET|POST|PUT /vehicles`,
`GET|POST|PUT /routes`, `GET|POST|PUT /schedules`, `GET /trips?date=`, `PUT /trips/{id}/status`,
`GET /bookings?status=`.

Setting a trip's status to `CANCELLED` automatically cancels and fully refunds every confirmed booking
on that trip and notifies the affected travellers; setting it to `DELAYED` notifies them without
cancelling anything.
