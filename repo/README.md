# DispatchOps -- Courier Operations & Settlement System

An offline-ready, LAN-based courier operations and settlement platform. Runs entirely on a local organizational server with no external internet dependencies.

## Features

- **Courier Fulfillment Control** -- Dispatch jobs, validate service areas, track delivery status with color-coded idle warnings
- **Internal Task Collaboration** -- To-do/Done/CC inboxes with @mentions, calendar view, and optimistic locking
- **Credibility Management** -- 1-5 ratings, credit levels A-D, 48-hour appeals, daily batch recalculation
- **Contract Templating** -- Versioned templates with placeholders, local e-signatures, HMAC tamper-evidence
- **Payments & Settlement** -- Cash/check/internal balance, double-entry ledger, offline-ready with idempotency, reconciliation exports
- **Notifications** -- In-app inbox with long-polling, optional LAN relay
- **Search** -- Full-text search with filters, trending terms, synonym suggestions
- **Profile Management** -- Tiered visibility, AES-encrypted PII, media uploads

## Roles

| Role | Access |
|------|--------|
| Administrator | Full system access |
| Operations Manager | Jobs, tasks, credibility, contracts, payments |
| Dispatcher | Create/assign jobs, rate couriers, create tasks |
| Courier | View/update assigned jobs, view credibility, sign contracts |
| Auditor | Reconciliation, ledger access, settle payments, verify contracts |

## Prerequisites

- Docker Desktop 4.x+
- Docker Compose v2+
- (Optional for local dev) Java 17, Maven 3.9+, MySQL 8.4

## Quick Start

```bash
# Clone and configure
cp .env.example .env
# REQUIRED: Fill in ALL secrets in .env before starting.
# Generate AES/HMAC keys: openssl rand -hex 32
# Set DB passwords to strong unique values.
docker compose up --build

# App available at http://localhost:8080
# Bootstrap admin credentials are in seed data - CHANGE IMMEDIATELY after first login.
```

## Testing

**Prerequisites:** Docker and Docker Compose only. No local Maven or JDK required.

```bash
# Full test suite — runs entirely in Docker:
./run_tests.sh
```

This script:
1. Generates ephemeral secrets at runtime (no `.env` file needed)
2. Runs unit + integration tests inside a `maven:3.9-eclipse-temurin-17` container
3. Starts the full app stack via `docker compose up` (secrets passed via environment)
4. Runs Playwright E2E tests against the live stack
5. Tears down the stack and all volumes (fully ephemeral)

```bash
# Unit tests only (in Docker, no app stack needed):
docker run --rm -v "$(pwd):/app" -w /app maven:3.9-eclipse-temurin-17 mvn test -B

# Manual E2E (requires running stack):
docker compose up -d --build --wait
docker run --rm --network host -v "$(pwd)/e2e:/app" -w /app -e E2E_BASE_URL=http://localhost:8080 maven:3.9-eclipse-temurin-17 mvn test -B
docker compose down -v
```

Test suite includes:
- **Unit tests** (200+): domain logic, security utilities, interceptor behavior (CSRF/auth/must-change-password), role matrix, object-level auth, callback pipeline, payment refund, ledger lifecycle
- **Controller tests** (MockMvc): role authorization matrix, object-level access, callback endpoint, admin protection, dashboard authorization
- **E2E tests** (Playwright, in `e2e/` module): auth flows, RBAC, fulfillment, payments, contracts, credibility, search, customer ratings
- **All tests run inside Docker** — no host-level Maven/JDK installation required

### Local (non-Docker) development

```bash
# Required environment variables:
export JDBC_URL=jdbc:mysql://localhost:3306/dispatchops?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
export DB_USER=dispatchops_app
export DB_PASSWORD=your_password
export AES_SECRET_KEY=$(openssl rand -hex 32)
export HMAC_SECRET_KEY=$(openssl rand -hex 32)

# Build and run tests:
mvn clean test

# Package WAR and deploy to Tomcat:
mvn package -DskipTests
# Deploy target/dispatchops.war to Tomcat webapps/
```

## Architecture

Hexagonal (Ports & Adapters) architecture with clear separation of concerns:

```
web/          -- Controllers, interceptors, DTOs (Spring MVC)
application/  -- Service orchestration, scheduled jobs
domain/       -- Entities, enums, business rules, exceptions
infrastructure/ -- MyBatis mappers, file storage, encryption, notifications
```

**Tech Stack:** Java 17, Spring MVC 6.1, MyBatis 3.5, MySQL 8.4, jQuery 3.7, Fluent UI CSS

## API Endpoints

| Group | Base Path | Description |
|-------|-----------|-------------|
| Auth | `/api/auth` | Login, logout, session |
| Users | `/api/users` | User CRUD (Admin) |
| Jobs | `/api/jobs` | Delivery job management |
| Tasks | `/api/tasks` | Task collaboration |
| Credibility | `/api/credibility` | Ratings, violations, appeals |
| Contracts | `/api/contracts` | Templates, instances, signing |
| Payments | `/api/payments` | Payments, settlement, reconciliation |
| Shipping | `/api/shipping` | Shipping rules and validation |
| Notifications | `/api/notifications` | Inbox, long-polling |
| Search | `/api/search` | Full-text search, trending |
| Profiles | `/api/profiles` | User profiles, media |

## Configuration

All configuration via environment variables (see `.env.example`):

| Variable | Description | Required |
|----------|-------------|----------|
| `JDBC_URL` | MySQL JDBC connection URL. Docker: `jdbc:mysql://db:3306/dispatchops?...` Local: `jdbc:mysql://localhost:3306/dispatchops?...` | Yes |
| `DB_USER` | MySQL username | Yes |
| `DB_PASSWORD` | MySQL password | Yes |
| `AES_SECRET_KEY` | 64-char hex key for AES-256 field encryption (`openssl rand -hex 32`) | Yes |
| `HMAC_SECRET_KEY` | 64-char hex key for HMAC-SHA256 signatures (`openssl rand -hex 32`) | Yes |
| `UPLOAD_DIR` | Local directory for file uploads (default: `./uploads`) | No |

## Security

- BCrypt password hashing (12 rounds)
- AES-256/CBC encryption for sensitive profile fields
- HMAC-SHA256 tamper-evident contract signatures
- Role-based access control with object-level authorization
- Account lockout after 5 failed attempts (15-minute auto-unlock)
- All data stored on local server disk

## Operational Notes

- All times displayed in 24-hour format (UTC internally)
- Weight measured in pounds (lbs)
- All CSS/JS/fonts served locally (no CDN dependencies)
- File uploads stored in the configured `UPLOAD_DIR`
- Append-only tables: `fulfillment_events`, `ledger_entries`, `signing_records`
