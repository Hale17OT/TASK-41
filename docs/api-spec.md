# DispatchOps API Spec (Implementation-Aligned)

This spec is derived from the current Spring MVC controller implementation.

## Conventions

- Base API prefix: `/api`
- Primary response envelope:

```json
{
  "code": 200,
  "message": "OK",
  "data": {},
  "errors": [],
  "timestamp": "2026-04-10T12:34:56"
}
```

- Paginated payload shape (`data` when paged):

```json
{
  "content": [],
  "page": 0,
  "size": 25,
  "totalElements": 0,
  "totalPages": 0
}
```

- Validation and business-rule failures commonly return `422`.
- Optimistic/idempotency conflicts return `409`.
- Auth/authorization failures return `401` or `403`.

## Authentication, session, and CSRF

- Auth model: session cookie (`currentUser` stored server-side).
- Login returns `csrfToken`; mutating authenticated requests require `X-CSRF-TOKEN` header.
- Public endpoints (no session required):
  - `POST /api/auth/login`
  - `GET /api/health`
  - `POST /api/payments/callback`
  - `POST /api/credibility/ratings/customer`
  - `POST /api/credibility/appeals/customer`
  - `GET /api/credibility/customer/lookup`

## Roles

- `ADMIN`
- `OPS_MANAGER`
- `DISPATCHER`
- `COURIER`
- `AUDITOR`

Role requirements below reflect `@RequireRole` plus notable object-level checks.

## Auth endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Login, rotates session, returns `user`, `csrfToken`, `mustChangePassword` |
| POST | `/api/auth/logout` | Session | Logout and invalidate session |
| GET | `/api/auth/me` | Session | Return current session user (sensitive fields scrubbed) |
| GET | `/api/auth/heartbeat` | Session | Health-style session check and CSRF token echo |

### Login request body

```json
{
  "username": "string",
  "password": "string"
}
```

### Login special statuses

- `423` account locked (`remainingSeconds`, `lockoutExpiry`)
- `401` invalid credentials (may include `remainingAttempts`)

## Health

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/health` | Public | Returns `"UP"` |

## Users

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/users` | ADMIN, OPS_MANAGER | List users (`page`, `size`, optional `role`) |
| GET | `/api/users/{id}` | ADMIN, OPS_MANAGER | Get user by id |
| POST | `/api/users` | ADMIN | Create user |
| PUT | `/api/users/{id}` | ADMIN | Update user |
| PUT | `/api/users/{id}/deactivate` | ADMIN | Deactivate user |
| PUT | `/api/users/{id}/unlock` | ADMIN | Clear lockout/failed attempts |
| PUT | `/api/users/{id}/password` | Session (self or ADMIN) | Change password |

### User create/update body

```json
{
  "username": "string",
  "password": "string (min 8)",
  "role": "ADMIN|OPS_MANAGER|DISPATCHER|COURIER|AUDITOR",
  "displayName": "string",
  "email": "string",
  "phone": "string"
}
```

## Delivery jobs

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/jobs` | Session | List jobs (courier sees own assignments only) |
| GET | `/api/jobs/{id}` | Session | Get job (courier must be assigned) |
| POST | `/api/jobs` | DISPATCHER, OPS_MANAGER, ADMIN | Create job |
| PUT | `/api/jobs/{id}/assign?courierId=` | DISPATCHER, OPS_MANAGER, ADMIN | Assign courier |
| PUT | `/api/jobs/{id}/status` | COURIER, DISPATCHER, OPS_MANAGER, ADMIN | Transition job status |
| PUT | `/api/jobs/{id}/override` | ADMIN | Force manual validation override |
| GET | `/api/jobs/{id}/events` | Session | Fulfillment event timeline |
| GET | `/api/jobs/idle?minutes=20` | OPS_MANAGER, ADMIN | Jobs idle beyond threshold |
| POST | `/api/jobs/picklist?runDate=YYYY-MM-DD` | DISPATCHER, OPS_MANAGER | Generate pick list |
| POST | `/api/jobs/sortlist?runDate=YYYY-MM-DD` | DISPATCHER, OPS_MANAGER | Generate sort list |

### Create job body

```json
{
  "senderName": "string",
  "senderAddress": "string",
  "senderPhone": "string",
  "receiverName": "string",
  "receiverAddress": "string",
  "receiverPhone": "string",
  "receiverState": "string",
  "receiverZip": "string",
  "weightLbs": 1.0,
  "orderAmount": 100.0
}
```

### Status transition body (jobs/tasks shared DTO)

```json
{
  "status": "string",
  "comment": "string",
  "version": 1
}
```

Job statuses: `CREATED`, `PICKED`, `IN_TRANSIT`, `DELIVERED`, `EXCEPTION`, `MANUAL_VALIDATION`.

## Internal tasks

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/tasks` | ADMIN, OPS_MANAGER, DISPATCHER, AUDITOR | Inbox by type (`TODO`, `DONE`, `CC`) |
| GET | `/api/tasks/{id}` | ADMIN, OPS_MANAGER, DISPATCHER, AUDITOR, COURIER | Get task (participant or elevated role) |
| POST | `/api/tasks` | ADMIN, OPS_MANAGER, DISPATCHER, AUDITOR | Create task |
| PUT | `/api/tasks/{id}/status` | ADMIN, OPS_MANAGER, DISPATCHER, AUDITOR, COURIER | Transition task status |
| POST | `/api/tasks/{id}/comments` | ADMIN, OPS_MANAGER, DISPATCHER, AUDITOR, COURIER | Add comment |
| GET | `/api/tasks/{id}/comments` | ADMIN, OPS_MANAGER, DISPATCHER, AUDITOR, COURIER | List comments |
| GET | `/api/tasks/calendar?from=&to=` | ADMIN, OPS_MANAGER, DISPATCHER, AUDITOR | Calendar view |
| GET | `/api/tasks/job/{jobId}` | ADMIN, OPS_MANAGER, DISPATCHER, AUDITOR, COURIER | Tasks linked to job |

### Create task body

```json
{
  "title": "string",
  "body": "string",
  "assigneeId": 1,
  "ccUserIds": [2, 3],
  "dueTime": "2026-04-10T12:00:00",
  "showOnCalendar": true,
  "jobId": 10
}
```

Task statuses: `TODO`, `IN_PROGRESS`, `BLOCKED`, `EXCEPTION`, `DONE`.

## Credibility

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/credibility/ratings` | DISPATCHER, OPS_MANAGER, ADMIN, AUDITOR | Staff rating for delivered job |
| POST | `/api/credibility/ratings/customer` | Public | Customer rating by tracking + token |
| GET | `/api/credibility/customer/lookup` | Public | Customer pre-check for rating/appeal |
| GET | `/api/credibility/ratings/courier/{id}` | OPS_MANAGER, ADMIN, AUDITOR | Paged ratings for courier |
| GET | `/api/credibility/credit/{courierId}` | Session | Credit level (courier self-only) |
| POST | `/api/credibility/violations` | OPS_MANAGER, ADMIN | Record violation |
| GET | `/api/credibility/violations/courier/{id}` | OPS_MANAGER, ADMIN, AUDITOR, COURIER | List violations (courier self-only) |
| POST | `/api/credibility/appeals` | COURIER | File courier appeal |
| POST | `/api/credibility/appeals/customer` | Public | File customer appeal by tracking + token |
| GET | `/api/credibility/appeals` | OPS_MANAGER, ADMIN | List pending appeals |
| PUT | `/api/credibility/appeals/{id}/resolve` | OPS_MANAGER, ADMIN | Resolve appeal |

### Rating body

```json
{
  "jobId": 1,
  "timeliness": 1,
  "attitude": 1,
  "accuracy": 1,
  "comment": "string"
}
```

### Customer rating body

```json
{
  "trackingNumber": "DO-...",
  "receiverName": "string",
  "customerToken": "string",
  "timeliness": 1,
  "attitude": 1,
  "accuracy": 1,
  "comment": "string"
}
```

### Appeal bodies

Courier:

```json
{
  "ratingId": 1,
  "violationId": null,
  "reason": "string"
}
```

Customer:

```json
{
  "trackingNumber": "DO-...",
  "receiverName": "string",
  "customerToken": "string",
  "reason": "string",
  "ratingId": 1
}
```

Resolve:

```json
{
  "status": "APPROVED|REJECTED",
  "comment": "string"
}
```

Notes:

- Public customer rating/appeal endpoints are rate-limited per source IP.
- Not-found/permission failures are normalized to `422` on public customer flows.

## Contracts

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/contracts/templates` | ADMIN, OPS_MANAGER | List templates |
| POST | `/api/contracts/templates` | ADMIN | Create template + initial version |
| POST | `/api/contracts/templates/{id}/versions` | ADMIN | Create template version |
| GET | `/api/contracts/templates/{id}/versions` | ADMIN, OPS_MANAGER, AUDITOR | List versions |
| POST | `/api/contracts/instances` | OPS_MANAGER, ADMIN | Generate contract instance |
| GET | `/api/contracts/instances/{id}` | Session | Get instance (courier must be designated signer) |
| POST | `/api/contracts/instances/{id}/sign` | COURIER, OPS_MANAGER | Submit signature |
| GET | `/api/contracts/instances/{id}/verify` | AUDITOR, ADMIN | Verify HMAC integrity |
| PUT | `/api/contracts/instances/{id}/void` | ADMIN | Void contract |
| GET | `/api/contracts/instances/{id}/signatures` | ADMIN, OPS_MANAGER, AUDITOR | List sanitized signing records |

### Create template body

```json
{
  "name": "string",
  "description": "string",
  "body": "Template with {{placeholders}}"
}
```

### Generate instance body

```json
{
  "templateVersionId": 1,
  "placeholderValues": {
    "courier_name": "Alex"
  },
  "signerIds": [12, 34],
  "jobId": 100
}
```

### Sign body

```json
{
  "signatureData": "base64-or-signature-string"
}
```

## Payments and settlement

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/payments` | DISPATCHER, OPS_MANAGER | Create payment intent (idempotent) |
| GET | `/api/payments/{id}` | DISPATCHER, OPS_MANAGER, ADMIN, AUDITOR | Get payment |
| GET | `/api/payments/job/{jobId}` | DISPATCHER, OPS_MANAGER, ADMIN, AUDITOR | Payments for job |
| GET | `/api/payments/list` | DISPATCHER, OPS_MANAGER, ADMIN, AUDITOR | Filtered payment list |
| GET | `/api/payments/pending` | AUDITOR, ADMIN | Pending settlements |
| POST | `/api/payments/{id}/settle` | AUDITOR | Settle payment |
| POST | `/api/payments/settle-batch` | AUDITOR | Settle payment ids in batch |
| POST | `/api/payments/{id}/refund` | OPS_MANAGER, ADMIN | Refund settled payment |
| POST | `/api/payments/callback` | Public | Device callback pipeline |
| GET | `/api/payments/reconciliation` | AUDITOR, ADMIN | Reconciliation items |
| PUT | `/api/payments/reconciliation/{id}/resolve` | AUDITOR | Resolve reconciliation item |
| GET | `/api/payments/reconciliation/export` | AUDITOR, ADMIN | CSV export (non-envelope binary response) |
| GET | `/api/payments/ledger/{accountId}` | AUDITOR, ADMIN | Ledger entries by account |
| GET | `/api/payments/balance/{accountId}` | AUDITOR, ADMIN, COURIER, DISPATCHER, OPS_MANAGER | Balance (self-only for non-privileged roles) |
| POST | `/api/payments/sync` | DISPATCHER | Offline payment sync batch |

### Create payment body

```json
{
  "idempotencyKey": "string",
  "jobId": 1,
  "amount": 99.99,
  "method": "CASH|CHECK|INTERNAL_BALANCE",
  "checkNumber": "string",
  "deviceId": "string",
  "deviceSeqId": 1
}
```

### Refund body

```json
{
  "amount": 10.00,
  "reason": "string"
}
```

### Callback body

```json
{
  "deviceId": "string",
  "eventId": "string",
  "payload": "json-string-or-opaque",
  "signature": "hmac-sha256",
  "timestamp": "2026-04-10T12:34:56Z",
  "deviceSeqId": "123"
}
```

Callback response behavior:

- `201`: processed
- `200`: duplicate/idempotent
- `202`: verified but business processing error
- `401`: invalid signature
- `422`: timestamp/validation/business rejection

## Shipping rules

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/shipping/templates` | ADMIN, OPS_MANAGER | List shipping templates |
| POST | `/api/shipping/templates` | ADMIN | Create template |
| PUT | `/api/shipping/templates/{id}` | ADMIN | Update template |
| GET | `/api/shipping/templates/{id}/rules` | ADMIN, OPS_MANAGER | List rules for template |
| POST | `/api/shipping/templates/{id}/rules` | ADMIN | Create region rule |
| PUT | `/api/shipping/rules/{id}` | ADMIN | Update region rule |
| DELETE | `/api/shipping/rules/{id}` | ADMIN | Delete region rule |
| POST | `/api/shipping/validate` | DISPATCHER, OPS_MANAGER, ADMIN | Validate address/weight/order |

### Validate body

```json
{
  "state": "CA",
  "zip": "90001",
  "weightLbs": 2.5,
  "orderAmount": 150.0
}
```

## Notifications

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/notifications` | Session | Inbox (`page`,`size`, optional `read`) |
| GET | `/api/notifications/unread-count` | Session | Unread count |
| GET | `/api/notifications/poll` | Session | Long-poll (up to 30s) |
| PUT | `/api/notifications/{id}/read` | Session | Mark one read |
| PUT | `/api/notifications/read-all` | Session | Mark all read |

## Search

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/search` | Session | Full-text search with filters |
| GET | `/api/search/suggest` | Session | Synonyms/related/trending suggestions |
| GET | `/api/search/trending` | Session | Top trending terms |

Search query params:

- `q` (required)
- `type`, `sort`, `author`, `dateFrom`, `dateTo`, `status`
- `page` (default `0`), `size` (default `25`)

Courier sessions use a courier-scoped search path with stricter result filtering.

## Profiles

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/profiles/{userId}` | Session | Composite profile view with visibility rules |
| PUT | `/api/profiles/{userId}` | Session (self or ADMIN) | Update profile fields |
| POST | `/api/profiles/{userId}/avatar` | Session (self) | Upload avatar |
| POST | `/api/profiles/{userId}/media` | Session (self or OPS_MANAGER/ADMIN) | Upload media |
| GET | `/api/profiles/{userId}/media` | Session | List media filtered by visibility tier |
| PUT | `/api/profiles/{userId}/visibility` | Session (self) | Update per-field visibility tier |
| GET | `/api/profiles/{userId}/visibility` | Session (self) | Get per-field visibility settings |

### Visibility update body

```json
{
  "field": "email|phone|bio|address|emergencyContact|idNumber",
  "tier": 1
}
```

### Media upload notes

- Multipart form key: `file`
- Magic-byte validation: JPEG, PNG, PDF
- Size limits: image 5MB, PDF 10MB

## Dashboard

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/dashboard/metrics` | Session | Role-tailored KPI summary |
| GET | `/api/dashboard/activity` | Session | Recent operational activity |

## Enumerations used by API

- Roles: `ADMIN`, `OPS_MANAGER`, `DISPATCHER`, `COURIER`, `AUDITOR`
- Job status: `CREATED`, `PICKED`, `IN_TRANSIT`, `DELIVERED`, `EXCEPTION`, `MANUAL_VALIDATION`
- Task status: `TODO`, `IN_PROGRESS`, `BLOCKED`, `EXCEPTION`, `DONE`
- Payment method: `CASH`, `CHECK`, `INTERNAL_BALANCE`
- Payment status: `PENDING_SETTLEMENT`, `SETTLED`, `REFUND_PENDING`, `REFUNDED`, `CANCELLED`
- Appeal status: `PENDING`, `APPROVED`, `REJECTED`
- Reconciliation status: `PENDING`, `RESOLVED`, `DISMISSED`

## Common error mappings

- `401`: authentication failure
- `403`: role/object-level denial, CSRF failures, must-change-password lock
- `404`: resource not found
- `409`: optimistic lock / idempotency conflict
- `422`: validation/domain rule failures
- `423`: account locked
- `429`: customer endpoint rate limit
- `500`: unexpected server error
