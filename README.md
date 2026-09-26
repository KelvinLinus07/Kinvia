Kinvia 🚆
Kinvia is a multimodal train + bus journey planning and booking platform. It focuses on combining transport search, journey planning, fare selection, availability and booking into a single system.
📊 Project Data
Kinvia currently works with a synthetic demonstration timetable dataset:
Dataset	Records
Stations	90
Train services	80,100
Bus services	40,050
Total transport services	120,150+


The station dataset includes geographical information. Train services contain multiple fare classes, while bus services contain different travel classes.
Train Classes
- Sleeper (SL)
- AC 3 Tier (3AC)
- AC 2 Tier (2AC)
- AC First Class (1AC)
Bus Classes
- Sitting
- Sleeper
The timetable is synthetic development/demo data and does not represent live operator data.
🧭 Core System
Kinvia treats a trip as a journey made up of connected segments, rather than only a single train or bus.
Example:
Bus → Train → Bus
The journey-planning system considers departure and arrival times, transfers, fares, availability and journey preferences.
Journey Ranking
Search results can be ranked using five strategies:
- Fastest
- Cheapest
- Fewest Transfers
- Most Comfortable
- Balanced
🔎 Search & Booking
Natural-Language Search
Kinvia supports rule-based natural-language journey queries and extracts travel intent such as:
- Origin
- Destination
- Date
- Time
- Ranking preference
Train Selection Flow
Search
  ↓
Train
  ↓
Class
  ↓
Fare
  ↓
Availability
  ↓
Seat Selection
  ↓
Booking
The booking system includes seat availability, temporary seat holds, booking references, cancellation and ticket generation.
🏗️ Project Structure
kinvia/
│
├── pom.xml
├── Dockerfile
├── mvnw
├── mvnw.cmd
├── .env.example
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/smarttransit/smart_transit/
│   │   │       ├── admin/
│   │   │       ├── booking/
│   │   │       ├── config/
│   │   │       ├── journey/
│   │   │       ├── network/
│   │   │       ├── notification/
│   │   │       ├── operator/
│   │   │       ├── payment/
│   │   │       ├── recommendation/
│   │   │       ├── schedule/
│   │   │       ├── security/
│   │   │       ├── seed/
│   │   │       ├── smartsearch/
│   │   │       ├── ticket/
│   │   │       ├── timetable/
│   │   │       ├── tracking/
│   │   │       ├── user/
│   │   │       └── ...
│   │   └── resources/
│   │       └── data/
│   │
│   └── test/
│
├── data/
│   └── source/
│
├── tools/
│   └── build_timetable_csv.py
│
├── docs/
│
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/
        ├── components/
        ├── context/
        ├── pages/
        └── styles/
⚙️ Technology Stack
Frontend
- React
- Vite
- JavaScript
- React Router
Backend
- Java 25
- Spring Boot
- Spring Data JPA
- Hibernate
- Maven
- REST APIs
Database
- MySQL 8
Deployment & Tools
- Docker
- Git
- GitHub
- Vercel
Supporting Technologies
- QR code generation
- PDF ticket generation
- PBKDF2 password hashing
- HMAC-signed access tokens
🔌 Backend Modules
The Spring Boot backend is organized into domain-focused modules:
Authentication / Security
        │
        ├── Users
        ├── Stations & Network
        ├── Schedules
        ├── Journey Planning
        ├── Recommendations
        ├── Smart Search
        ├── Booking
        ├── Seat Management
        ├── Payment
        ├── Tickets
        ├── Tracking
        ├── Notifications
        ├── Operator Management
        └── Administration
This structure keeps journey planning, booking, authentication, transport data and operational features separated while allowing them to work together through REST APIs.
🔄 Data Flow
Transport Dataset
       ↓
Timetable Import
       ↓
MySQL Database
       ↓
Spring Boot REST API
       ↓
Journey / Search / Booking Logic
       ↓
React Frontend
       ↓
User
The timetable data is imported into the backend database and used by the journey-planning and search systems.
🎫 Booking Architecture
The booking flow is designed around server-side availability:
Search Journey
      ↓
Select Trip
      ↓
Check Availability
      ↓
Hold Seat
      ↓
Payment
      ↓
Confirm Booking
      ↓
Generate Ticket
Temporary seat holds and server-side locking are used to reduce the possibility of double booking.
🚧 Current Scope
Kinvia is currently a development/prototype platform. Some integrations are simulated, including payment and live tracking.
The project is primarily focused on demonstrating:
Transport Data + Journey Planning + Search + Availability + Booking
as one integrated full-stack system
