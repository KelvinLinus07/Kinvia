# Kinvia

**Kinvia** is a multimodal train + bus journey planning and booking platform. The name is built from
"kin" and the idea of a journey — Kinvia is about people, connections, and the paths that join them.

Unlike a typical booking demo, Kinvia does not assume a trip is a single train or a single bus. A
`Journey` is a first-class concept made up of one or more `JourneySegment`s, so the planner can — and
does — chain a feeder bus into a mainline train into a last-mile bus, and sell the whole thing as one
ticket with one reference number.

This repository contains the complete backend and frontend, seed data for a working local demo, and
the automated tests described below.

---

## Contents

- [Problem statement](#problem-statement)
- [Solution](#solution)
- [Key features](#key-features)
- [Architecture](#architecture)
- [Technology stack](#technology-stack)
- [Project structure](#project-structure)
- [Imported timetable](#imported-timetable)
- [Getting started](#getting-started)
- [Environment variables](#environment-variables)
- [API overview](#api-overview)
- [Testing](#testing)
- [Sample accounts](#sample-accounts)
- [Known limitations & future enhancements](#known-limitations--future-enhancements)

---

## Problem statement

Booking a regional trip that isn't served end-to-end by one operator usually means booking two or
three separate tickets on two or three separate websites, manually working out connection times, and
hoping the schedules actually line up. Existing booking platforms are built around the assumption that
a booking is "one seat on one vehicle."

## Solution

Kinvia's domain model separates the *journey the traveller experiences* from the *vehicles that carry
them*. A `Journey` has an ordered list of `JourneySegment`s; each segment is a ride on one `Trip`
(a dated instance of a `Schedule`) between two stops. The `ItineraryPlanner` searches the live network
of trips for every valid way to chain segments together — respecting each station's minimum transfer
time — and the `WeightedJourneyScorer` ranks the results by what the traveller actually cares about
(fastest, cheapest, fewest transfers, most comfortable, or a balance of all four).

## Key features

- **Multimodal journey planning** — bus → train → bus chains are found and booked as one journey.
- **Search limits** — departure time (morning/afternoon/evening/night or an exact range) and a maximum
  budget per traveller, applied on the server; a large, searchable station list (name, city or code).
- **Smart ranking** — five ranking strategies, implemented as a transparent weighted scorer (no black
  box, no fabricated "AI").
- **Natural-language search** — "from Siliguri to Patna next Friday cheapest" is parsed by a
  deterministic, offline rule-based interpreter (`RuleBasedSearchInterpreter`); no external AI service
  is required for the app to work.
- **Real seat inventory** — seats are held with a server-side, row-locked expiry mechanism that makes
  double-booking structurally impossible, then converted to a real reservation on payment.
- **Digital tickets** — QR codes (ZXing) and PDF tickets (a small dependency-free PDF writer) generated
  per booking.
- **Development payment provider** — a clearly-labelled simulated gateway behind a `PaymentProvider`
  interface; a real provider (Razorpay, Stripe, ...) can be dropped in without touching booking logic.
- **Tiered cancellation & refunds** — refund percentage depends on how far ahead of departure the
  cancellation happens; an operator-cancelled service is always refunded in full.
- **Role-based dashboards** — passenger, operator (scoped to their own fleet/routes/bookings) and admin
  (system-wide) surfaces, all backed by the same REST API.
- **Simulated live tracking** — an honest, clearly-labelled "estimated position" derived from the
  schedule and reported delay, behind a `TrackingProvider` interface a real GPS feed can replace later.

## Architecture

```
Browser (React SPA)
        │  REST / JSON, Bearer token
        ▼
Spring Boot API  ──────────────────────────────────────────────
 │ security     : PBKDF2 password hashing, HMAC-signed stateless tokens, role-based interceptor
 │ network      : stations, operators, vehicles, coaches, seats, routes, fares
 │ schedule     : recurring schedules → dated trips (rolling-window generator, idempotent)
 │ journey      : pure planning domain (Leg/Itinerary/ItineraryPlanner) + persisted Journey/Segment
 │ recommendation: strategy-weighted, min-max normalised scorer behind a JourneyScorer interface
 │ booking      : seat holds (pessimistic row lock, TTL), bookings, cancellation policy
 │ payment      : PaymentProvider interface + development (simulated) implementation
 │ ticket       : QR (ZXing) + PDF generation behind a TicketPdfRenderer interface
 │ notification : NotificationChannel interface + logging (dev) email channel
 │ tracking     : TrackingProvider interface + honest simulated implementation
 │ admin/operator: scoped management + analytics
 └──────────────────────────────────────────────────────────────
        │  JPA / Hibernate
        ▼
MySQL 8 (schema managed by Hibernate `ddl-auto=update`)
```

Every external or unavailable dependency (payment gateway, live tracking, AI search, SMS/email) is
behind a small interface with one production-realistic development implementation, so the whole system
runs with nothing but a MySQL server — no API keys required.

### The multimodal planning algorithm

`LegCatalog` loads every bookable trip in the search window and expands each into every possible
boarding/alighting pair (a `Leg`), pricing it from the route's fare table and counting seats left per
class in a fixed number of queries. `ItineraryPlanner` is a bounded depth-first search over legs
indexed by boarding station: a connection is only allowed if it is at the same station, on a different
trip, with at least the station's configured minimum transfer time between the (delay-adjusted) arrival
and the next departure. `WeightedJourneyScorer` then min-max normalises duration, price, transfers,
comfort and waiting time across the candidate set and combines them using the weights for the chosen
strategy.

A journey result's `key` (e.g. `12.0.4~57.1.3`) is a stateless, URL-safe encoding of its trips and stop
sequences — nothing is cached server-side, so a shared link is always re-resolved against live data.

## Technology stack

**Backend:** Java 25 (compiled and tested here against a Java 21 toolchain — see
[Known limitations](#known-limitations--future-enhancements)), Spring Boot 4.1.1, Spring Data JPA /
Hibernate, MySQL 8, Bean Validation, ZXing (QR codes).

**Frontend:** React 19 (Vite), React Router. No UI kit — a small hand-built design system (see
`frontend/src/styles`) using Google Fonts (Fraunces + Manrope).

## Project structure

```
kinvia/
├── pom.xml
├── src/main/java/com/smarttransit/smart_transit/
│   ├── config/        application properties, CORS, clock
│   ├── security/       tokens, password hashing, @Public / @RequiresRole
│   ├── user/            accounts, passengers, preferences, saved journeys
│   ├── network/         stations, operators, vehicles, coaches, seats, routes, fares
│   ├── schedule/        schedules, trips, trip generation
│   ├── journey/         planner, scorer glue, journey search, persisted Journey/Segment
│   ├── recommendation/  ranking strategies and the weighted scorer
│   ├── booking/         seat holds, bookings, cancellation policy
│   ├── payment/         payment provider abstraction + development provider
│   ├── ticket/          QR + PDF ticket generation
│   ├── notification/    notification channel abstraction
│   ├── tracking/        simulated live tracking
│   ├── smartsearch/     natural-language search interpreter
│   ├── admin/ operator/ scoped management + analytics
│   ├── timetable/       imported reference timetable: entity, CSV import, on-demand activation
│   └── seed/            development seed data
├── src/test/java/...    unit tests (see Testing)
├── data/source/         the original transport workbook (.xlsx)
├── tools/               build_timetable_csv.py: workbook -> the CSVs under src/main/resources/data
└── frontend/
    └── src/
        ├── api/          fetch client
        ├── context/      auth context
        ├── components/   shared UI + dashboard panels
        ├── pages/        routed pages
        └── styles/       design tokens and component styles
```

## Imported timetable

Besides the small development seed, Kinvia ships with a transport workbook
(`data/source/transport_routes_final_separate_ac_2ac_3ac.xlsx`): 90 stations, 80,100 train services and
40,050 bus services - ten trains and five buses for every directed pair of stations, each with a
departure time and per-class fares. The workbook itself says it is synthetic demo data.

- `tools/build_timetable_csv.py` turns the workbook into three CSV files under
  `src/main/resources/data/` (run it again only if the workbook changes; it needs `openpyxl`).
- On start-up `TimetableImporter` loads the stations (reusing any station that already exists by name)
  and stores every service, exactly as supplied, in the `timetable_entries` table. It runs once, only
  while that table is empty, and can be disabled with `KINVIA_TIMETABLE_IMPORT=false`. Expect the
  first start to take a minute or two.
- Creating seat maps for 120,000 services up front would mean millions of unused rows, so
  `TimetableActivator` creates the real operator, vehicle (with seats), route, schedule and dated trips
  for a station pair the first time it is searched. From then on those services behave like any other
  schedule, including the nightly trip generation and the whole booking flow.
- Search limits (departure time, budget) are applied in `JourneySearchService` on every result, so they
  work identically for imported services and the original seed routes.

What the workbook does not contain, and what Kinvia does about it:

| Missing from the workbook | Handling |
|---|---|
| Arrival time, duration, distance | Estimated from approximate station coordinates (`TravelEstimator`: great-circle distance x 1.25, 55 km/h trains, 45 km/h buses, rounded to 5 minutes) |
| Station codes, city, state, coordinates | Supplied in `stations.csv`; the codes are Kinvia's own short codes, the coordinates approximate |
| Days of operation | Every service runs daily |
| Operator | Buses use the brand in the service name (five brands); trains share one "Demo Rail Services" operator |
| Coach layouts | Same layouts as the development seed, plus AC First Class (train: sleeper 48, 3rd AC 48, 2nd AC 24, 1st AC 16 seats; bus: 40 seats) |

Fares are copied unchanged from the workbook and stored as a flat fare per class. All four train fare
columns (Sleeper, AC 3 Tier, AC 2 Tier, AC First Class) map onto a travel class and are offered for
booking; the workbook has no General/2S column, so that class is never offered. Bus Sleeper fares are
still stored in the timetable table but not offered for booking, because the application has no matching
travel class for it.

## Getting started

### Prerequisites

- Java 21+ (Java 25 recommended to match the configured toolchain)
- Maven 3.9+
- MySQL 8, running locally
- Node.js 20+ and npm

### 1. Database

Create the database (Kinvia will also create it automatically on first connect if your MySQL user has
permission, via `createDatabaseIfNotExist=true`):

```sql
CREATE DATABASE smart_transit CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

### 2. Backend

```bash
cp .env.example .env   # then edit .env with your real MySQL credentials
# export the variables from .env into your shell, or configure them in your IDE's run configuration
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`. On an **empty** database it seeds realistic development
data automatically (see [Sample accounts](#sample-accounts)) — set `KINVIA_SEED=false` to disable this.

### 3. Frontend

```bash
cd frontend
cp .env.example .env   # defaults to http://localhost:8080, change if needed
npm install
npm run dev
```

The frontend starts on `http://localhost:5173` (or run `npm run build && npm run preview` for a
production build).

## Environment variables

See [`.env.example`](.env.example) at the project root for the backend and
[`frontend/.env.example`](frontend/.env.example) for the frontend. Nothing in this repository contains
real secrets — every credential is a placeholder or a clearly-marked development value.

| Variable | Required | Purpose |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | Yes | Your local MySQL credentials |
| `DB_URL` | No | Override the JDBC URL (defaults to `smart_transit` on `localhost:3306`) |
| `KINVIA_TOKEN_SECRET` | Recommended | HMAC secret for access tokens; a random one is generated (and logged as a warning) if unset, which invalidates sessions on every restart |
| `KINVIA_SEED` | No | `true` (default) seeds development data into an empty database |
| `KINVIA_SEED_PASSWORD` | No | Password used for the seeded sample accounts (default `Kinvia@Dev1`) |
| `KINVIA_TIMETABLE_IMPORT` | No | `true` (default) loads the bundled stations and train/bus timetable on start-up while the `timetable_entries` table is empty |
| `KINVIA_CORS_ORIGINS` | No | Comma-separated list of allowed frontend origins |

## API overview

All endpoints are under `/api`. Authenticated endpoints expect `Authorization: Bearer <token>`, issued
by `/api/auth/login` or `/api/auth/register`.

| Area | Base path | Notes |
|---|---|---|
| Auth | `/api/auth` | register, login |
| Users | `/api/users/me` | profile, passengers, preferences, saved journeys |
| Stations | `/api/stations` | public partial-text search by name, city or code, used by the frontend's autocomplete |
| Journeys | `/api/journeys` | `/search` (with departure-time and budget limits), `/{key}`, `/interpret` (natural-language search) |
| Seats | `/api/trips/{tripId}/seats`, `/api/holds` | seat map, hold, release |
| Bookings | `/api/bookings` | create, list, detail, cancellation preview, cancel |
| Payment | `/api/bookings/{id}/payment` | development payment |
| Tickets | `/api/bookings/{id}/ticket`, `.../download` | JSON view + PDF download |
| Notifications | `/api/notifications` | list, unread count, mark read |
| Tracking | `/api/trips/{id}/tracking` | simulated live status |
| Admin | `/api/admin/*` | system-wide management (ADMIN role only) |
| Operator | `/api/operator/*` | fleet/route/schedule/trip/booking management scoped to the caller's operator |

Full request/response shapes are in [`docs/API.md`](docs/API.md). Every non-2xx response has the same
shape: `{ timestamp, status, error, message, path, fieldErrors }`.

## Testing

Backend unit tests cover the parts of the system where correctness actually matters most: the
multimodal planner's connection logic, the weighted ranking scorer, the tiered cancellation policy,
journey-key encoding, password hashing, access-token issuance/expiry, booking reference formatting, and
itinerary fare/duration arithmetic.

```bash
mvn test
```

The full booking pipeline (search → seat hold → booking → payment → ticket → PDF download) and the
double-booking guard, payment decline path, and role-based access boundaries were additionally verified
end-to-end against a real MySQL database, both via direct API calls and via a real browser (Playwright)
driving the built frontend — see [`docs/TESTING.md`](docs/TESTING.md) for what was exercised.

## Sample accounts

Seeded automatically on a fresh database (password for all three: `Kinvia@Dev1`, or whatever you set
`KINVIA_SEED_PASSWORD` to):

| Email | Role |
|---|---|
| `admin@kinvia.dev` | ADMIN |
| `operator@kinvia.dev` | OPERATOR (Himalayan Motor Transport) |
| `traveller@kinvia.dev` | PASSENGER |

The seed data includes a direct train, a direct bus, and a bus → train → bus multimodal chain between
Siliguri and the Patna area, at different fares, schedules and seat availability, so all three journey
types can be demonstrated immediately.

## Known limitations & future enhancements

This is a from-scratch build completed end-to-end, but a few things are worth being upfront about:

- **Java 25 toolchain**: the `pom.xml` targets Java 25 / Spring Boot 4.1.1 as specified. This sandbox
  only had a Java 21 JDK available, so all compilation, the 35 unit tests, and the live end-to-end
  verification (including a real browser session) were run against Java 21 bytecode. The codebase uses
  no Java 25–only language features, so this is expected to compile and run unchanged on a Java 25 JDK;
  it has not been physically verified on one.
- **Payments, tracking and notifications are development implementations by design** — swapping in
  Razorpay/Stripe, a real GPS/operator feed, or an email/SMS provider means implementing one interface
  each (`PaymentProvider`, `TrackingProvider`, `NotificationChannel`) with no other code changes.
- **Seat-to-passenger assignment in the frontend** is currently in booking order (first seat selected →
  first passenger), not a drag-and-drop "assign this seat to this named passenger" interaction.
- **The AI/NLP search is intentionally rule-based**, not a language model — it is accurate for
  well-formed input and honest about what it couldn't parse. Swapping in an LLM-backed
  `SearchInterpreter` implementation (with a documented `kinvia.ai.*` config prefix) is a natural next
  step and was designed for from the start.
- **Live map visualisation** (station/route coordinates exist in the data model, and stations carry
  latitude/longitude) is not yet rendered as an actual map in the frontend.
- Presentation-quality touches described in the original brief that were not fully built out include:
  a dedicated skeleton loader per page, comprehensive dark-mode support, and animated seat/booking
  micro-interactions beyond the ones implemented (hero line-draw, stepper transitions, confirmation pop-in).
