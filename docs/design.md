# DispatchOps Design Notes

## System overview

DispatchOps is a monolithic Spring MVC application for LAN-only courier operations. It combines delivery fulfillment, internal tasking, credibility scoring, contracts, payments, search, notifications, and profile management in one deployable WAR.

The code follows a layered/hexagonal style:

- `web`: REST controllers, JSP page routes, DTOs, interceptors, exception mapping
- `application`: transactional use-case services and scheduled jobs
- `domain`: entities, enums, validators, and domain exceptions
- `infrastructure`: MyBatis mappers, crypto utilities, storage, relay adapters

## Runtime and composition

- Java 17 + Spring MVC + MyBatis + MySQL
- HikariCP-backed datasource from environment variables
- Scheduler enabled for daily/hourly batch jobs
- Async executor used for deferred workflows (long-poll support)
- JSP views served from `/WEB-INF/jsp`, static assets from `/static/**`

## Request lifecycle

For `/api/**` routes, request handling is interceptor-driven:

1. `AuthInterceptor`
   - Enforces session auth except public endpoints.
   - Enforces `mustChangePassword` lock-down (only password/auth helper endpoints allowed).
2. `CsrfInterceptor`
   - Enforces `X-CSRF-TOKEN` on mutating session-authenticated requests.
   - Exempts GET/HEAD/OPTIONS and the explicitly public API routes.
3. `RoleInterceptor`
   - Enforces `@RequireRole` annotations.

Controller methods then apply object-level checks where needed (for example courier-only self access) and delegate to services.

## Security model

- Session-based authentication (`currentUser` in session), with session fixation prevention on login.
- CSRF token generated at login and returned in login response.
- Role-based authorization via `@RequireRole` + runtime role interceptor.
- Object-level authorization implemented inside controllers/services for task/job/payment/profile ownership paths.
- Password hashing with BCrypt (`PasswordService`).
- Account lockout after failed attempts with timed unlock.
- PII field encryption via AES (`FieldEncryptionService`).
- HMAC integrity/authentication for:
  - contract signature tamper evidence
  - payment device callbacks
  - customer action tokens for delivered jobs

## Core business modules

### Fulfillment (`DeliveryJobService`)

- Creates jobs after shipping rule validation.
- Applies optimistic locking for status transitions.
- Emits immutable fulfillment events.
- Enforces credit-level concurrency limits on courier assignment.
- Generates per-delivery customer token when a job reaches `DELIVERED`.
- Sends notifications on assignment/status updates.

### Internal tasks (`TaskService`)

- Supports TODO/DONE/CC inbox semantics via recipient rows.
- Handles optimistic status transitions with comment rules.
- Supports task comments and `@mention` notifications.
- Supports calendar view and optional job linkage.

### Credibility (`CredibilityService`)

- Staff and customer ratings for delivered jobs.
- 48-hour appeal window for ratings and violations.
- Appeal resolution can exclude ratings/deactivate violations.
- Credit levels (`A-D`) recalculated from ratings + active violations.
- Daily scheduler recomputes all courier levels.
- Public customer flows validate receiver + HMAC token and normalize failures to reduce enumeration risk.

### Contracts (`ContractService`)

- Template + version management with extracted placeholder schema.
- Contract instance generation with rendered placeholders.
- Ordered multi-signer workflow with signer plan enforcement.
- Signing records include HMAC document hash for verification.
- Integrity verification recomputes HMAC and reports per-signature validity.

### Payments and settlement (`PaymentService`)

- Idempotent payment creation via `idempotencyKey`.
- Settlement phase posts ledger entries (double-entry style).
- Refund flow enforces eligibility window and posts compensating ledger entries.
- Reconciliation item management and CSV export.
- Device callback pipeline: timestamp freshness -> dedupe -> persist -> HMAC verify -> business processing.
- Supports offline sync batch submission for dispatcher-captured payments.

### Search (`SearchService`)

- Unified index over multiple entities (`JOB`, `TASK`, `CONTRACT`, `PAYMENT`, `PROFILE`, `USER`).
- Filtered/full-text search with telemetry capture.
- Courier-scoped search path applies query-level restrictions.
- Suggest endpoint combines synonyms, related categories, and trending terms.
- Hourly scheduler materializes trending terms from telemetry.

### Profiles (`ProfileService`)

- Per-user profile storage + encrypted sensitive fields.
- Per-field visibility tiers with overrides.
- Avatar/media upload with magic-byte and size validation.
- Change logging with masked before/after values.

### Notifications (`NotificationService`)

- Stores in-app notifications, supports unread counts and read marks.
- Supports long-poll (`DeferredResult`) fanout for near-real-time inbox updates.
- Optionally relays over LAN channel without failing primary flow.

## Data and consistency patterns

- Most write services are transactional (`@Transactional`).
- Optimistic locking used for mutable workflow entities (tasks/jobs).
- Append-oriented/event-style records for fulfillment events, ledger entries, signing records, callback events.
- MyBatis mappers are the persistence ports; services orchestrate cross-aggregate behavior.

## Public and integration-facing surfaces

Public (no session required) endpoints are intentionally narrow:

- `POST /api/auth/login`
- `GET /api/health`
- `POST /api/payments/callback`
- `POST /api/credibility/ratings/customer`
- `POST /api/credibility/appeals/customer`
- `GET /api/credibility/customer/lookup`

Everything else is session-authenticated and, for mutating calls, CSRF-protected.

## Notable implementation tradeoffs

- Customer rate limiting is in-memory per app node (no distributed limiter).
- Domain models are often returned directly by controllers, so API shape is tightly coupled to model fields.
- Some object-level checks are controller-local instead of centralized policy middleware.
- Search indexing is best-effort (failures logged but do not fail core transaction).
