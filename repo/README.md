# DispatchOps -- Courier Operations & Settlement System

**Project Type:** Fullstack (web backend + JSP/jQuery frontend, served from a single Spring MVC WAR)

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

Nothing else is required on the host — no Java, Maven, MySQL, `npm install`, `pip install`, or manual DB setup. The entire stack (app + database + tests) runs inside Docker.

## Quick Start

```bash
# 1. Clone and configure
cp .env.example .env
# REQUIRED: fill in ALL secrets in .env before starting.
# Generate AES/HMAC keys: openssl rand -hex 32
# Set DB passwords to strong unique values.

# 2. Start the full stack (app + MySQL) in Docker
docker-compose up --build
#   — or, with Docker Compose v2 plugin syntax (equivalent):
#     docker compose up --build
```

> Both `docker-compose up` (Compose v1 / hyphenated binary) and `docker compose up` (Compose v2 plugin) are supported — pick whichever your Docker install provides.

**Access the application:** http://localhost:8080 (redirects to `/login`).

### How to verify it works

1. Open http://localhost:8080/login in a browser.
2. Log in with any demo credential below. The seeded users have `must_change_password=1`, so you'll be redirected to `/change-password` on first login — set a new password and you'll land on the dashboard.
3. Smoke-test the API with curl:
   ```bash
   curl http://localhost:8080/api/health        # → {"status":"UP"}
   curl -i -X POST http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"Admin123!"}'
   ```
4. Exercise flows through the UI: create a delivery job at `/fulfillment`, view credibility at `/credibility`, record a payment at `/payments`, file a customer appeal at `/customer`.

## Demo Credentials

All seeded users share the bootstrap password **`Admin123!`** and have `must_change_password=1` — the UI forces a password change on first login. The helpers in `BaseE2ETest` use `TestPass123!` as the post-change password.

| Username     | Password (initial) | Role         | What they can do                                              |
|--------------|--------------------|--------------|---------------------------------------------------------------|
| `admin`      | `Admin123!`        | ADMIN        | Full system access (users, settings, all business features)   |
| `ops_manager`| `Admin123!`        | OPS_MANAGER  | Jobs, tasks, credibility, contracts, payments                 |
| `dispatcher1`| `Admin123!`        | DISPATCHER   | Create/assign jobs, rate couriers, create tasks               |
| `courier1`   | `Admin123!`        | COURIER      | View/update assigned jobs, view credibility, sign contracts   |
| `courier2`   | `Admin123!`        | COURIER      | Second courier account for RBAC/visibility tests              |
| `auditor1`   | `Admin123!`        | AUDITOR      | Reconciliation, ledger access, settle payments, verify contracts |

> These are bootstrap-only credentials. Change them in production.

## Testing

**Prerequisites:** Docker and Docker Compose only. No local Maven, JDK, or browser dependencies required — everything (including Playwright/Chromium) runs inside the Maven container.

```bash
# Full test suite — runs entirely in Docker:
./run_tests.sh
```

The script has three explicit stages:
1. **Unit + integration tests** — `mvn test` inside `maven:3.9-eclipse-temurin-17` (`run_tests.sh:20-25`).
2. **Application stack up + health check** — `docker compose up -d --build --wait`, then `curl /api/health` loop (`run_tests.sh:43-66`).
3. **Playwright E2E suite** — `mvn -f e2e/pom.xml test` inside the same Maven image on the host Docker network, pointed at `http://localhost:8080` via `E2E_BASE_URL` (`run_tests.sh:77-92`). The script captures the Maven exit code and **exits non-zero if any E2E test fails**.

Afterwards the stack is torn down with `docker compose down -v`. Everything is ephemeral — no state leaks between runs.

Test suite includes:
- **Unit + domain tests**: business rules (credit-level calculation, appeal window, status transitions, contract HMAC integrity, refund window), security utilities (AES/HMAC/BCrypt), interceptor behavior (CSRF, auth, must-change-password)
- **Controller tests** (Spring MockMvc): role authorization matrix, object-level access, admin protection, dashboard authorization, payment callback validation
- **E2E tests** (Playwright, `e2e/` module) — real HTTP against the live stack, no mocking:
  - `AuthFlowTest` — login, logout, lockout, session redirect, health endpoint
  - `RBACFlowTest` — cross-role access checks against every major controller
  - `FulfillmentFlowTest` — job create/assign/status, pick/sort list generation
  - `TaskFlowTest` — inbox tabs, task create, calendar view
  - `PaymentFlowTest` — record/settle/refund, idempotency, reconciliation export
  - `ContractFlowTest` — templates, versions, integrity verification
  - `CredibilityFlowTest` + `CustomerCredibilityFlowTest` — ratings, violations, appeals (internal and customer-facing)
  - `SearchFlowTest` + `SearchIntegrationFlowTest` — full-text search, trending, suggestions; create-then-search integration
  - `EndpointCoverageFlowTest` — exercises every remaining REST endpoint end-to-end via real HTTP

All tests run inside Docker.

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
