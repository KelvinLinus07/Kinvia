# Testing

## Automated unit tests

Run with `mvn test`. 59 tests (35 original + 24 for the search improvements), covering the parts of the system where a subtle bug would
actually matter:

| Class under test | What is verified |
|---|---|
| `ItineraryPlannerTest` | Chains a bus and a train across a shared station; rejects a connection that violates the station's minimum transfer time; respects the configured maximum wait between legs; excludes a leg (including the *first* leg — a real bug caught during development, see below) that cannot seat the whole party; a direct leg reports zero transfers |
| `WeightedJourneyScorerTest` | CHEAPEST/FASTEST strategies rank correctly; an itinerary that cannot seat the party always sorts after ones that can, regardless of strategy; an empty candidate list produces no results |
| `ItineraryFareAndDurationTest` | Multi-leg fare is the sum of each leg's cheapest bookable class; duration and inter-leg wait time arithmetic; single-mode vs. multimodal detection |
| `CancellationPolicyTest` | The three refund tiers (90% / 50% / 0%) and their boundary; operator-initiated cancellation always refunds 100% |
| `JourneyKeyTest` | Format/parse round-trip for one and multiple segments; rejects blank, malformed, self-contradicting (alight ≤ board), and oversized keys |
| `PasswordHasherTest` | A password matches its own hash; a different password does not; two hashes of the same password differ (fresh salt); malformed stored hashes are rejected rather than throwing |
| `AccessTokenServiceTest` | A freshly issued token verifies; an expired token does not; a tampered token does not; garbage input never throws |
| `DepartureWindowTest` | Both ends of a range are inclusive; seconds are ignored; a range that starts after it ends wraps past midnight; every minute of the day belongs to exactly one named band (night/morning/afternoon/evening) |
| `JourneyFilterTest` | Departure time must fall inside the window; the budget is compared with the fare per traveller shown on the result, following the cheapest class the party can actually book; both limits must hold |
| `TravelEstimatorTest` | Great-circle distance is plausible (Howrah-Patna); trains are estimated faster than buses; very short hops still take a few minutes; durations round to 5 minutes |
| `TimetableCsvTest` | CSV quoting; the bundled files are internally consistent (90 unique stations and codes, every service refers to a known station and has a unique number, exactly 10 trains and 5 buses for each of the 8,010 directed pairs) |
| `BookingReferencesTest` | Format matches `KV-XXXXXXXX`; no ambiguous characters (`I`, `O`, `0`, `1`); 500 generated references are all distinct |

### A real bug this suite caught

While writing `ItineraryPlannerTest`, a test asserting that a party of 3 could not be offered a direct
leg with only 1 free seat failed. The planner was checking seat capacity for every *connecting* leg but
not for the very first leg of a journey. This was a genuine bug (a search could show an unbookable
single-leg direct journey with no warning) and was fixed in `ItineraryPlanner.plan()` before this README
was written — not a hypothetical example.

## Manual / integration verification

The parts of the system that need a real database, a real HTTP server, and (for the last item) a real
browser were exercised directly rather than only unit-tested, against a local MySQL 8 instance with the
development seed data loaded:

- **Full booking pipeline**, via direct API calls: search (confirmed the multimodal bus→train chain is
  actually found and correctly priced) → seat map → seat hold → booking creation → payment → ticket
  JSON → ticket PDF download (downloaded file verified as a valid PDF).
- **Double-booking prevention**: after a seat was booked, attempting to hold it again (including by the
  same trip after a server restart, proving the guarantee is durable, not in-memory) correctly returned
  a conflict.
- **Payment decline path**: an instrument crafted to trigger the development gateway's failure mode
  correctly returned `402 Payment Required` and left the booking in `HELD` rather than `CONFIRMED`.
- **Cancellation and refund**: cancelling a confirmed booking correctly computed the refund tier from
  time-to-departure and updated the payment's refund status.
- **Role-based access control**: an OPERATOR token was confirmed to receive `403 Forbidden` from an
  ADMIN-only endpoint, and the operator dashboard was confirmed to return only that operator's own
  scoped data (vehicle/route/trip/booking counts).
- **Admin analytics**: confirmed the dashboard reflects real counts (users, operators, bookings,
  transport distribution) rather than placeholder values.
- **Full frontend flow, in an actual headless browser** (Playwright driving the production Vite build
  against the real backend): landing page search → results → sign in → journey selection → seat
  selection → passenger details → payment → confirmation → ticket view, plus a separate pass through
  the admin dashboard. Screenshots were captured at each step and visually reviewed.

This browser pass caught a second real bug: the backend's journey search response did not expose a
segment's boarding/alighting station directly (only a raw stop list and index), which crashed the
frontend's route visualization component. This was fixed by adding `board`/`alight` fields to
`SegmentDto` (see `JourneyDtos` and `JourneyMapper`), and the full flow was re-verified afterwards.

## Search improvements: how they were verified

The new logic that has no Spring/JPA dependency (departure windows, journey filter, travel estimator, CSV
reader and the bundled data) is covered by the unit tests above.

The parts that need Spring and a database - the `/api/stations` and `/api/journeys/search` endpoints,
the JPQL in `TripRepository`/`StationRepository`, the timetable import and the on-demand activation -
need a real run. Suggested manual pass (with MySQL running and the app started):

1. First start: the log shows `[TIMETABLE] Imported 80100 train and 40050 bus services.`
2. `GET /api/stations?q=howr`, `?q=HWH`, `?q=jn` - partial name, code and city matches, best matches first.
3. Pick two station ids, then `GET /api/journeys/search?originId=..&destinationId=..&date=<tomorrow>`
   - expect 10 trains + 5 buses, each with class fares; repeat to confirm the second call is faster.
4. Add `timeOfDay=MORNING`, then `departAfter=18:00&departBefore=23:59` - every result departs inside the window.
5. Add `maxBudget=300`, then `maxBudget=100` - only journeys whose shown fare is at or under the limit remain;
   `matchedBeforeFilters` still reports the unfiltered count.
6. `transport=BUS` / `transport=TRAIN`; an unknown `originId` (404); `originId` = `destinationId` (400);
   `timeOfDay` together with `departAfter` (400); `maxBudget=0` (400).
7. Select a result in the UI and complete seat selection, passenger details, payment, ticket and My Trips.

## What was not covered

Given the scope of the project, the following were exercised through the manual pass above but do not
yet have dedicated automated (integration or component) tests: the Spring MVC controllers themselves
(request mapping, validation wiring), the seat-hold row-locking behaviour under genuine concurrent load
(only sequential/logical correctness was verified), and the React components. A natural next step would
be `@SpringBootTest` / `MockMvc` integration tests for the controller layer and a concurrency test that
fires many simultaneous hold requests at the same seat from a thread pool.
