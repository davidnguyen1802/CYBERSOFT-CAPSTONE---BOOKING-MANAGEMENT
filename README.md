# Booking Management Platform

A full-stack booking marketplace that helps guests discover short-term stays, allows hosts to publish listings, and gives administrators the tools to govern the inventory. The platform is built with a Spring Boot REST API and an Angular single-page application that share a MySQL data store seeded with realistic travel data.

## Table of contents
- [Architecture overview](#architecture-overview)
- [Key features](#key-features)
  - [Authentication & security](#authentication--security)
  - [Property discovery](#property-discovery)
  - [Bookings & guest services](#bookings--guest-services)
  - [User experience](#user-experience)
  - [Operations & administration](#operations--administration)
- [Technology stack](#technology-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Backend setup](#backend-setup)
  - [Frontend setup](#frontend-setup)
  - [Running the full stack](#running-the-full-stack)
- [Environment configuration](#environment-configuration)
- [Database seed data](#database-seed-data)
- [Testing](#testing)
- [Reference documentation](#reference-documentation)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Architecture overview

```
CYBERSOFT-CAPSTONE---BOOKING-MANAGEMENT/
├── backend/Final_Capstone        # Spring Boot 3 REST API, MySQL persistence, Docker assets
└── frontend/Frontend_FinalCapstone# Angular 16 SPA with Bootstrap UI components
```

The backend exposes REST endpoints for authentication, property discovery, bookings, favorites, promotions, and profile management. The frontend consumes these APIs, handles session renewal via refresh tokens, and presents dedicated flows for guests, hosts, and administrators.

## Key features

### Authentication & security
- Email/username/phone based login with JWT access tokens plus HttpOnly refresh tokens (`/auth/login`, `/auth/refresh`).
- Multi-step registration flow that supports avatar uploads and immediate sign-in on success (`/auth/signup`).
- OAuth 2.0 hooks ready for Google and Facebook sign-in via the configured introspector and WebClient beans.
- Comprehensive token lifecycle management (revocation, rotation, device tracking) backed by the `token` table.
- Fine-grained role enforcement (`GUEST`, `HOST`, `ADMIN`) in user profile, booking, and administration endpoints.
- CORS configuration tailored for cookie-based auth with per-endpoint rules.

### Property discovery
- Unified search endpoints (`GET /property/filter`, `POST /property/search`) that combine type, location, pricing, capacity, amenity, and facility filters.
- Featured property endpoints (`/property/top7`, `/property/top4/type/{type}`) to power home page hero sections.
- Rich domain model with `Property`, `Amenity`, `Facility`, `Location`, `Image`, and `UserReview` entities and DTO mappers.
- Angular property services that implement infinite scroll, search suggestions, and filter forms against the REST API.

### Bookings & guest services
- Booking lifecycle APIs for creation, retrieval, cancellation, and availability checks under `/bookings`.
- Promotion-aware pricing that applies percentage or fixed discounts during booking creation.
- Validation of occupancy limits, conflicting reservations, and booking status transitions.
- Favorites management endpoints (`/user/favorites/**`) that let guests curate personal wishlists.
- Review endpoints that allow guests to submit and hosts to monitor feedback (see `UserReviewController`).

### User experience
- Angular SPA with dedicated modules for home discovery, property detail pages, booking flow, user profile, and admin dashboard.
- Token-aware HTTP interceptor that automatically retries failed requests after refreshing tokens and preserves session state in `localStorage`.
- Route guards that prevent unauthorized access to protected pages (`AuthGuardFn`, `AdminGuardFn`).
- Responsive UI built on Bootstrap 5, Font Awesome icons, and NgBootstrap components.

### Operations & administration
- Admin endpoints for managing properties, promotions, amenities, facilities, users, and system statistics.
- Spring Boot Actuator dependency ready for health checks and operational insights.
- Extensive SQL seed data covering Vietnamese cities, locations, users, properties, amenities, facilities, favorites, and bookings for realistic demos.
- Modular service layer (`service/` + `service/Imp/`) separating business rules from controllers and repositories.

## Technology stack

| Layer      | Technologies |
|------------|--------------|
| Backend    | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, OAuth2 Client, WebFlux, MySQL, Flyway-style migrations, Lombok |
| Frontend   | Angular 16, RxJS 7, TypeScript 5, Bootstrap 5, NgBootstrap, Font Awesome |
| Tooling    | Maven Wrapper, Docker Compose, npm, pnpm (lockfile), Karma & Jasmine for Angular tests |

## Project structure

```
backend/Final_Capstone/
├── src/main/java/com/Cybersoft/Final_Capstone/
│   ├── controller/        # REST controllers (Auth, Property, Booking, User, Admin, ...)
│   ├── service/           # Service interfaces plus Imp/ implementations
│   ├── repository/        # Spring Data repositories
│   ├── dto/ & payload/    # DTOs, request/response contracts
│   ├── components/        # JWT utilities, security helpers
│   ├── config/            # Security, CORS, email, WebClient, RestTemplate configuration
│   └── Entity/, Enum/, util/, mapper/, specification/ # Domain model helpers
├── src/main/resources/
│   ├── db/migration/      # Flyway migration scripts
│   └── logback.xml        # Logging configuration
├── docker/                # MySQL init script with sample data
└── docker-compose.yml     # Local MySQL service definition

frontend/Frontend_FinalCapstone/
├── src/app/
│   ├── components/        # Feature components (home, property detail, order, admin, ...)
│   ├── services/          # API integrations (auth, property, booking, favorites, ...)
│   ├── guards/ & interceptors/ # Route protection and token handling
│   ├── dtos/, models/, responses/ # Typed client contracts
│   └── utils/, styles/    # Shared helpers and styling
├── angular.json, tsconfig.* # Angular workspace configuration
├── package.json           # Scripts & dependencies
└── README.md              # Additional frontend notes
```

## Getting started

### Prerequisites

| Requirement | Version / Notes |
|-------------|-----------------|
| Java        | 21 (JDK 21 for Spring Boot 3.5) |
| Maven       | Maven Wrapper included (`./mvnw`) |
| Node.js     | ≥ 18.x (matches Angular CLI 16 requirements) |
| npm / pnpm  | npm 9+ or pnpm 8+ (lockfiles for both are present) |
| Docker      | Docker Engine & Compose plugin for the MySQL container |

### Backend setup

1. **Start MySQL** (optional but recommended for local dev):
   ```bash
   cd backend/Final_Capstone
   docker compose up -d
   ```
   This launches a MySQL 8 container named `final-capstone-database` with database `hotels_management` and seeds it with demo data from `docker/init.sql`.

2. **Configure application properties** (see [Environment configuration](#environment-configuration)). The project expects an `application.yml` or `application.properties` under `src/main/resources/`.

3. **Run the API**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The service listens on `http://localhost:8080` by default.

### Frontend setup

1. Install dependencies:
   ```bash
   cd frontend/Frontend_FinalCapstone
   npm install
   ```
2. Run the dev server:
   ```bash
   npm run start
   ```
   The Angular CLI serves the SPA on `http://localhost:4200`. The default `environment.ts` already points to `http://localhost:8080/` for API calls.

### Running the full stack

1. Start the backend (see above). Ensure it can connect to MySQL.
2. Start the Angular dev server.
3. Visit `http://localhost:4200` and log in or browse as a guest. Authentication requests use HttpOnly cookies, so keep both apps on the same origin set (configure CORS if needed).

## Environment configuration

Create `backend/Final_Capstone/src/main/resources/application.yml` (or `.properties`) with values similar to:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hotels_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: admin123
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
    show-sql: true

app:
  security:
    allowed-origins: http://localhost:4200,http://127.0.0.1:4200

jwt:
  issuer: booking-platform
  # Generate a base64 secret: `openssl rand -base64 64`
  secret: REPLACE_WITH_BASE64_SECRET
  access-expiration-ms: 900000      # 15 minutes
  refresh-expiration-ms: 1209600000 # 14 days

mail:
  host: smtp.example.com
  port: 587
  username: noreply@example.com
  password: change-me
```

Environment overrides (e.g., `SPRING_DATASOURCE_URL`, `JWT_SECRET`) also work thanks to Spring Boot’s configuration precedence.

## Database seed data

The `docker/init.sql` script provisions:
- Roles, statuses, and baseline statistics records.
- 60+ Vietnamese cities and signature locations.
- 100+ amenities and facilities tied to properties.
- Sample users for admin, host, and guest personas with BCrypt-hashed passwords.
- 40+ properties, images, favorites, promotions, and bookings to explore immediately.

Feel free to adjust or extend this data before first launch. Rerun `docker compose down -v` followed by `docker compose up -d` to reset the dataset.

## Testing

| Area     | Command |
|----------|---------|
| Backend  | `./mvnw test` |
| Frontend | `npm test` |

## Reference documentation

Additional deep-dive notes live alongside the codebase:
- `backend/Final_Capstone/ALL_ENDPOINTS_LIST.txt` – consolidated API endpoint inventory.
- `backend/Final_Capstone/SECURITY_CONFIGURATION_SUMMARY.txt` – detailed security architecture.
- `frontend/Frontend_FinalCapstone/README.md` – Angular-specific developer guide.
- Frontend `*.md` files (e.g., `LOGIN_TEST_CHECKLIST.md`, `AUTH_IMPLEMENTATION_SUMMARY.md`) – focused runbooks for specific features.

## Troubleshooting

- **Login succeeds but API calls fail with 401**: confirm the frontend runs on an origin listed in `app.security.allowed-origins` and that both apps share the same protocol/host for cookie delivery.
- **JWT creation errors**: ensure `jwt.secret` is a valid base64-encoded string and that system time is correct.
- **Database migrations fail**: verify the MySQL container is running and your datasource credentials match the container configuration.
- **Angular cannot reach the API**: update `frontend/Frontend_FinalCapstone/src/environments/environment.ts` if the backend runs on a non-default host/port.

## Contributing

1. Fork and clone the repository.
2. Create a feature branch.
3. Follow the existing code style and naming conventions.
4. Run unit tests for the layers you touched.
5. Submit a pull request with a concise summary of your changes.

This project currently does not specify a license; contact the maintainers before using it in production.