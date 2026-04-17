# Unified Test Coverage + README Audit Report (Re-test)

- Repository: `E:\Hale\Coding\Eaglepoint\Task-41\repo`
- Audit mode: static inspection only (no execution)
- Date: 2026-04-17

## 1. Test Coverage Audit

### Backend Endpoint Inventory

Endpoint definition applied: unique `METHOD + fully resolved PATH` using class `@RequestMapping` + method mapping.

Total API endpoints: **91**

| # | Endpoint | Controller Evidence |
|---|---|---|
| 1 | POST `/api/auth/login` | `src/main/java/com/dispatchops/web/controller/AuthController.java:29` |
| 2 | POST `/api/auth/logout` | `src/main/java/com/dispatchops/web/controller/AuthController.java:88` |
| 3 | GET `/api/auth/me` | `src/main/java/com/dispatchops/web/controller/AuthController.java:103` |
| 4 | GET `/api/auth/heartbeat` | `src/main/java/com/dispatchops/web/controller/AuthController.java:125` |
| 5 | GET `/api/tasks` | `src/main/java/com/dispatchops/web/controller/TaskController.java:38` |
| 6 | GET `/api/tasks/{id}` | `src/main/java/com/dispatchops/web/controller/TaskController.java:52` |
| 7 | POST `/api/tasks` | `src/main/java/com/dispatchops/web/controller/TaskController.java:65` |
| 8 | PUT `/api/tasks/{id}/status` | `src/main/java/com/dispatchops/web/controller/TaskController.java:76` |
| 9 | POST `/api/tasks/{id}/comments` | `src/main/java/com/dispatchops/web/controller/TaskController.java:93` |
| 10 | GET `/api/tasks/{id}/comments` | `src/main/java/com/dispatchops/web/controller/TaskController.java:110` |
| 11 | GET `/api/tasks/calendar` | `src/main/java/com/dispatchops/web/controller/TaskController.java:124` |
| 12 | GET `/api/tasks/job/{jobId}` | `src/main/java/com/dispatchops/web/controller/TaskController.java:142` |
| 13 | POST `/api/credibility/ratings` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:37` |
| 14 | POST `/api/credibility/ratings/customer` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:91` |
| 15 | GET `/api/credibility/customer/lookup` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:112` |
| 16 | GET `/api/credibility/ratings/courier/{id}` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:125` |
| 17 | GET `/api/credibility/credit/{courierId}` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:136` |
| 18 | POST `/api/credibility/violations` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:150` |
| 19 | GET `/api/credibility/violations/courier/{id}` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:163` |
| 20 | POST `/api/credibility/appeals` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:181` |
| 21 | POST `/api/credibility/appeals/customer` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:197` |
| 22 | GET `/api/credibility/appeals` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:217` |
| 23 | PUT `/api/credibility/appeals/{id}/resolve` | `src/main/java/com/dispatchops/web/controller/CredibilityController.java:227` |
| 24 | GET `/api/dashboard/metrics` | `src/main/java/com/dispatchops/web/controller/DashboardController.java:40` |
| 25 | GET `/api/dashboard/activity` | `src/main/java/com/dispatchops/web/controller/DashboardController.java:70` |
| 26 | POST `/api/payments` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:40` |
| 27 | GET `/api/payments/{id}` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:51` |
| 28 | GET `/api/payments/job/{jobId}` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:59` |
| 29 | GET `/api/payments/list` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:67` |
| 30 | GET `/api/payments/pending` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:80` |
| 31 | POST `/api/payments/{id}/settle` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:90` |
| 32 | POST `/api/payments/settle-batch` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:101` |
| 33 | POST `/api/payments/{id}/refund` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:112` |
| 34 | POST `/api/payments/callback` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:128` |
| 35 | GET `/api/payments/reconciliation` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:162` |
| 36 | PUT `/api/payments/reconciliation/{id}/resolve` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:173` |
| 37 | GET `/api/payments/reconciliation/export` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:187` |
| 38 | GET `/api/payments/ledger/{accountId}` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:201` |
| 39 | GET `/api/payments/balance/{accountId}` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:212` |
| 40 | POST `/api/payments/sync` | `src/main/java/com/dispatchops/web/controller/PaymentController.java:231` |
| 41 | GET `/api/users` | `src/main/java/com/dispatchops/web/controller/UserController.java:32` |
| 42 | GET `/api/users/{id}` | `src/main/java/com/dispatchops/web/controller/UserController.java:43` |
| 43 | POST `/api/users` | `src/main/java/com/dispatchops/web/controller/UserController.java:52` |
| 44 | PUT `/api/users/{id}` | `src/main/java/com/dispatchops/web/controller/UserController.java:61` |
| 45 | PUT `/api/users/{id}/deactivate` | `src/main/java/com/dispatchops/web/controller/UserController.java:71` |
| 46 | PUT `/api/users/{id}/unlock` | `src/main/java/com/dispatchops/web/controller/UserController.java:79` |
| 47 | PUT `/api/users/{id}/password` | `src/main/java/com/dispatchops/web/controller/UserController.java:87` |
| 48 | GET `/api/search` | `src/main/java/com/dispatchops/web/controller/SearchController.java:30` |
| 49 | GET `/api/search/suggest` | `src/main/java/com/dispatchops/web/controller/SearchController.java:58` |
| 50 | GET `/api/search/trending` | `src/main/java/com/dispatchops/web/controller/SearchController.java:65` |
| 51 | GET `/api/jobs` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:37` |
| 52 | GET `/api/jobs/{id}` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:57` |
| 53 | POST `/api/jobs` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:74` |
| 54 | PUT `/api/jobs/{id}/assign` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:85` |
| 55 | PUT `/api/jobs/{id}/status` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:97` |
| 56 | PUT `/api/jobs/{id}/override` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:120` |
| 57 | GET `/api/jobs/{id}/events` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:133` |
| 58 | GET `/api/jobs/idle` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:152` |
| 59 | POST `/api/jobs/picklist` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:161` |
| 60 | POST `/api/jobs/sortlist` | `src/main/java/com/dispatchops/web/controller/DeliveryJobController.java:170` |
| 61 | GET `/api/contracts/templates` | `src/main/java/com/dispatchops/web/controller/ContractController.java:38` |
| 62 | POST `/api/contracts/templates` | `src/main/java/com/dispatchops/web/controller/ContractController.java:48` |
| 63 | POST `/api/contracts/templates/{id}/versions` | `src/main/java/com/dispatchops/web/controller/ContractController.java:60` |
| 64 | GET `/api/contracts/templates/{id}/versions` | `src/main/java/com/dispatchops/web/controller/ContractController.java:73` |
| 65 | POST `/api/contracts/instances` | `src/main/java/com/dispatchops/web/controller/ContractController.java:81` |
| 66 | GET `/api/contracts/instances/{id}` | `src/main/java/com/dispatchops/web/controller/ContractController.java:95` |
| 67 | POST `/api/contracts/instances/{id}/sign` | `src/main/java/com/dispatchops/web/controller/ContractController.java:114` |
| 68 | GET `/api/contracts/instances/{id}/verify` | `src/main/java/com/dispatchops/web/controller/ContractController.java:129` |
| 69 | PUT `/api/contracts/instances/{id}/void` | `src/main/java/com/dispatchops/web/controller/ContractController.java:137` |
| 70 | GET `/api/contracts/instances/{id}/signatures` | `src/main/java/com/dispatchops/web/controller/ContractController.java:148` |
| 71 | GET `/api/shipping/templates` | `src/main/java/com/dispatchops/web/controller/ShippingRuleController.java:33` |
| 72 | POST `/api/shipping/templates` | `src/main/java/com/dispatchops/web/controller/ShippingRuleController.java:43` |
| 73 | PUT `/api/shipping/templates/{id}` | `src/main/java/com/dispatchops/web/controller/ShippingRuleController.java:55` |
| 74 | GET `/api/shipping/templates/{id}/rules` | `src/main/java/com/dispatchops/web/controller/ShippingRuleController.java:66` |
| 75 | POST `/api/shipping/templates/{id}/rules` | `src/main/java/com/dispatchops/web/controller/ShippingRuleController.java:74` |
| 76 | PUT `/api/shipping/rules/{id}` | `src/main/java/com/dispatchops/web/controller/ShippingRuleController.java:85` |
| 77 | DELETE `/api/shipping/rules/{id}` | `src/main/java/com/dispatchops/web/controller/ShippingRuleController.java:96` |
| 78 | POST `/api/shipping/validate` | `src/main/java/com/dispatchops/web/controller/ShippingRuleController.java:104` |
| 79 | GET `/api/notifications` | `src/main/java/com/dispatchops/web/controller/NotificationController.java:29` |
| 80 | GET `/api/notifications/unread-count` | `src/main/java/com/dispatchops/web/controller/NotificationController.java:42` |
| 81 | GET `/api/notifications/poll` | `src/main/java/com/dispatchops/web/controller/NotificationController.java:50` |
| 82 | PUT `/api/notifications/{id}/read` | `src/main/java/com/dispatchops/web/controller/NotificationController.java:60` |
| 83 | PUT `/api/notifications/read-all` | `src/main/java/com/dispatchops/web/controller/NotificationController.java:70` |
| 84 | GET `/api/profiles/{userId}` | `src/main/java/com/dispatchops/web/controller/ProfileController.java:38` |
| 85 | PUT `/api/profiles/{userId}` | `src/main/java/com/dispatchops/web/controller/ProfileController.java:107` |
| 86 | POST `/api/profiles/{userId}/avatar` | `src/main/java/com/dispatchops/web/controller/ProfileController.java:125` |
| 87 | POST `/api/profiles/{userId}/media` | `src/main/java/com/dispatchops/web/controller/ProfileController.java:152` |
| 88 | GET `/api/profiles/{userId}/media` | `src/main/java/com/dispatchops/web/controller/ProfileController.java:182` |
| 89 | PUT `/api/profiles/{userId}/visibility` | `src/main/java/com/dispatchops/web/controller/ProfileController.java:201` |
| 90 | GET `/api/profiles/{userId}/visibility` | `src/main/java/com/dispatchops/web/controller/ProfileController.java:219` |
| 91 | GET `/api/health` | `src/main/java/com/dispatchops/web/controller/HealthController.java:15` |

### API Test Mapping Table

Test type labels:
- `true no-mock HTTP` = Playwright E2E request through live HTTP stack
- `HTTP with mocking` = MockMvc + mocked dependencies
- `unit-only/indirect` = no direct HTTP route test

| Endpoint | Covered | Test Type | Test Files | Evidence |
|---|---|---|---|---|
| POST `/api/auth/login` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `authLoginReturns200WithValidCredentials`, `authLoginReturns401WithInvalidCredentials` |
| POST `/api/auth/logout` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `authLogoutReturns200` |
| GET `/api/auth/me` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/AuthFlowTest.java` | `apiReturns401WithoutSession` |
| GET `/api/auth/heartbeat` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `authHeartbeatReturns200WhenAuthenticated` |
| GET `/api/tasks` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/TaskFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `todoInboxReturns200`; `dispatcher_tasks_200` |
| GET `/api/tasks/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `getTaskByIdOnMissingTaskReturns404` |
| POST `/api/tasks` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/TaskFlowTest.java` | `createTaskReturns201` |
| PUT `/api/tasks/{id}/status` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `transitionTaskStatusOnMissingTaskReturns404`; `courier_cannotTransitionOtherCouriersJob` |
| POST `/api/tasks/{id}/comments` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `postTaskCommentOnMissingTaskReturns403Or404` |
| GET `/api/tasks/{id}/comments` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `getTaskCommentsOnMissingTaskReturns403Or404` |
| GET `/api/tasks/calendar` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/TaskFlowTest.java` | `calendarEndpointReturns200` |
| GET `/api/tasks/job/{jobId}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `tasksByJobIdHandlerReached` |
| POST `/api/credibility/ratings` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `submitInternalRatingAsDispatcherOnMissingJobReturns404`; `courier_submitRating_403` |
| POST `/api/credibility/ratings/customer` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/CustomerCredibilityFlowTest.java` | `customerRatingOnNonExistentTrackingReturns422` |
| GET `/api/credibility/customer/lookup` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `customerLookupOnNonExistentTrackingReturns422` |
| GET `/api/credibility/ratings/courier/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `listRatingsByCourierHandlerReached` |
| GET `/api/credibility/credit/{courierId}` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/CredibilityFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `getCreditLevelReturns200WithLevel`; `courier_canViewOwnCreditLevel` |
| POST `/api/credibility/violations` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `recordViolationAsOpsManagerReturns201`, `recordViolationAsCourierReturns403` |
| GET `/api/credibility/violations/courier/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `listViolationsByCourierHandlerReached` |
| POST `/api/credibility/appeals` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/CredibilityFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `appealOnNonExistentViolation_returns404`; `courier_cannotAppealOtherCouriersRating` |
| POST `/api/credibility/appeals/customer` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/CustomerCredibilityFlowTest.java` | `customerAppealOnNonExistentTrackingReturns422` |
| GET `/api/credibility/appeals` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/RBACFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `adminCanAccessEverything`; `admin_appeals_200` |
| PUT `/api/credibility/appeals/{id}/resolve` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `resolveAppealHandlerReached`; `dispatcher_resolveAppeal_403` |
| GET `/api/dashboard/metrics` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `dashboardMetricsHandlerReached` |
| GET `/api/dashboard/activity` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/DashboardAuthorizationTest.java` | `dashboardActivityHandlerReached`; `courier_activityFeed_usesListJobsByCourier` |
| POST `/api/payments` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/PaymentFlowTest.java` | `idempotencyPreventsDoublePosting` |
| GET `/api/payments/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `paymentGetByIdHandlerReached` |
| GET `/api/payments/job/{jobId}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `paymentsByJobHandlerReached` |
| GET `/api/payments/list` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `paymentListHandlerReached` |
| GET `/api/payments/pending` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/RBACFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `adminCanAccessEverything`; `auditor_pendingPayments_200` |
| POST `/api/payments/{id}/settle` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/RBACFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `dispatcherCannotSettlePayments`; `dispatcher_settlePayment_403` |
| POST `/api/payments/settle-batch` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `paymentSettleBatchHandlerReached` |
| POST `/api/payments/{id}/refund` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `paymentRefundHandlerReached` |
| POST `/api/payments/callback` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/PaymentCallbackControllerTest.java` | `paymentCallbackHandlerReached`; `successfulProcessingReturns201` |
| GET `/api/payments/reconciliation` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `paymentReconciliationHandlerReached` |
| PUT `/api/payments/reconciliation/{id}/resolve` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `paymentReconciliationResolveHandlerReached` |
| GET `/api/payments/reconciliation/export` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/PaymentFlowTest.java` | `auditorCanExportReconciliation` |
| GET `/api/payments/ledger/{accountId}` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `paymentLedgerHandlerReached`; `auditor_canAccessPaymentLedger` |
| GET `/api/payments/balance/{accountId}` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `paymentBalanceHandlerReached`; `courier_cannotAccessPaymentBalance` |
| POST `/api/payments/sync` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `paymentSyncHandlerReached` |
| GET `/api/users` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/RBACFlowTest.java`, `src/test/java/com/dispatchops/web/controller/AdminEndpointProtectionTest.java` | `adminCanAccessEverything`; `admin_canListUsers` |
| GET `/api/users/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `getUserByIdHandlerReached` |
| POST `/api/users` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/SearchIntegrationFlowTest.java`, `src/test/java/com/dispatchops/web/controller/AdminEndpointProtectionTest.java` | `createdUserAppearsInSearch`; `nonAdmin_cannotCreateUser` |
| PUT `/api/users/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `updateUserHandlerReached` |
| PUT `/api/users/{id}/deactivate` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/SearchIntegrationFlowTest.java` | `deactivatedUserDisappearsFromSearch` |
| PUT `/api/users/{id}/unlock` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `unlockUserHandlerReached` |
| PUT `/api/users/{id}/password` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/RoleAuthorizationMatrixTest.java` | `changePasswordHandlerReached`; `mustChangePasswordAllowsPasswordEndpoint` |
| GET `/api/search` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/SearchFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `searchApiReturnsResults`; `search_noSession_401` |
| GET `/api/search/suggest` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/SearchFlowTest.java` | `suggestEndpointWorks` |
| GET `/api/search/trending` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/SearchFlowTest.java` | `trendingEndpointWorks` |
| GET `/api/jobs` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/FulfillmentFlowTest.java`, `src/test/java/com/dispatchops/web/controller/CourierJobListingTest.java` | `courierCanOnlySeeOwnJobs`; `courier_listJobs_usesQueryLevelCourierFilter` |
| GET `/api/jobs/{id}` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `getJobByIdOnMissingJobReturns404`; `courier_cannotViewOtherCouriersJob` |
| POST `/api/jobs` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/FulfillmentFlowTest.java`, `src/test/java/com/dispatchops/web/controller/RoleAuthorizationMatrixTest.java` | `createJobWithInvalidAddressFails`; `courierCannotCreateJobs` |
| PUT `/api/jobs/{id}/assign` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `assignJobOnMissingJobReturns404` |
| PUT `/api/jobs/{id}/status` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `updateJobStatusOnMissingJobReturns404`; `courier_canTransitionOwnJob` |
| PUT `/api/jobs/{id}/override` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `overrideJobOnMissingJobReturns404` |
| GET `/api/jobs/{id}/events` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `jobEventsOnMissingJobReturns404`; `courier_cannotViewOtherCouriersJobEvents` |
| GET `/api/jobs/idle` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/FulfillmentFlowTest.java` | `apiReturnsIdleJobs` |
| POST `/api/jobs/picklist` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/FulfillmentFlowTest.java` | `pickListGenerationReturns200` |
| POST `/api/jobs/sortlist` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/FulfillmentFlowTest.java` | `sortListGenerationReturns200` |
| GET `/api/contracts/templates` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/ContractFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `listTemplates_returns200WithContent`; `admin_contractTemplates_200` |
| POST `/api/contracts/templates` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/ContractFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `createTemplateAsAdmin_returns201WithId`; `courier_createTemplate_403` |
| POST `/api/contracts/templates/{id}/versions` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `contractTemplateVersionsPostHandlerReached` |
| GET `/api/contracts/templates/{id}/versions` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `contractTemplateVersionsGetHandlerReached` |
| POST `/api/contracts/instances` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `contractInstancesPostHandlerReached` |
| GET `/api/contracts/instances/{id}` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `contractInstanceGetHandlerReached`; `courier_cannotViewNonSignerContract` |
| POST `/api/contracts/instances/{id}/sign` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `contractInstanceSignHandlerReached` |
| GET `/api/contracts/instances/{id}/verify` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/ContractFlowTest.java` | `verifyIntegrityOnNonExistent_returns404WithMessage` |
| PUT `/api/contracts/instances/{id}/void` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `contractInstanceVoidHandlerReached` |
| GET `/api/contracts/instances/{id}/signatures` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `contractInstanceSignaturesHandlerReached`; `courier_cannotAccessContractSignatures` |
| GET `/api/shipping/templates` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `shippingTemplatesListHandlerReached`; `admin_shippingTemplates_200` |
| POST `/api/shipping/templates` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `shippingTemplateCreateHandlerReached` |
| PUT `/api/shipping/templates/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `shippingTemplateUpdateHandlerReached` |
| GET `/api/shipping/templates/{id}/rules` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `shippingTemplateRulesGetHandlerReached` |
| POST `/api/shipping/templates/{id}/rules` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `shippingTemplateRulesPostHandlerReached` |
| PUT `/api/shipping/rules/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `shippingRuleUpdateHandlerReached` |
| DELETE `/api/shipping/rules/{id}` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `shippingRuleDeleteHandlerReached` |
| POST `/api/shipping/validate` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `shippingValidateHandlerReached` |
| GET `/api/notifications` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `notificationListHandlerReached`; `notification_inboxScopedToSelf` |
| GET `/api/notifications/unread-count` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `notificationUnreadCountHandlerReached` |
| GET `/api/notifications/poll` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `notificationPollHandlerReached` |
| PUT `/api/notifications/{id}/read` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `notificationMarkReadHandlerReached`; `notification_cannotMarkOtherUsersAsRead` |
| PUT `/api/notifications/read-all` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `notificationReadAllHandlerReached` |
| GET `/api/profiles/{userId}` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java` | `profileGetHandlerReached`; `profiles_noSession_401` |
| PUT `/api/profiles/{userId}` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/SearchIntegrationFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `updatedProfileAppearsInSearch`; `courier_cannotUpdateOtherProfile` |
| POST `/api/profiles/{userId}/avatar` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `profileAvatarUploadHandlerReached` |
| POST `/api/profiles/{userId}/media` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `profileMediaPostHandlerReached` |
| GET `/api/profiles/{userId}/media` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java` | `profileMediaGetHandlerReached` |
| PUT `/api/profiles/{userId}/visibility` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `profileVisibilityPutHandlerReached`; `courier_canUpdateOwnVisibility` |
| GET `/api/profiles/{userId}/visibility` | yes | true no-mock HTTP (+HTTP with mocking) | `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`, `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java` | `profileVisibilityGetHandlerReached`; `courier_cannotViewOtherVisibilitySettings` |
| GET `/api/health` | yes | true no-mock HTTP | `e2e/src/test/java/com/dispatchops/e2e/AuthFlowTest.java` | `healthEndpointIsPublic` |

### API Test Classification

1. **True No-Mock HTTP**
- `e2e/src/test/java/com/dispatchops/e2e/BaseE2ETest.java` bootstraps real Playwright browser/client and sends real HTTP requests (`page.request()`), with no mocks in E2E module.
- E2E suites: `EndpointCoverageFlowTest`, `AuthFlowTest`, `RBACFlowTest`, `PaymentFlowTest`, `FulfillmentFlowTest`, `TaskFlowTest`, `ContractFlowTest`, `CredibilityFlowTest`, `CustomerCredibilityFlowTest`, `SearchFlowTest`, `SearchIntegrationFlowTest`.

2. **HTTP with Mocking**
- MockMvc controller tests with mocked dependencies:
  - `src/test/java/com/dispatchops/web/controller/PaymentCallbackControllerTest.java` (`@Mock PaymentService`)
  - `src/test/java/com/dispatchops/web/controller/AdminEndpointProtectionTest.java` (`@Mock UserService`)
  - `src/test/java/com/dispatchops/web/controller/CourierJobListingTest.java` (`@Mock DeliveryJobService`)
  - `src/test/java/com/dispatchops/web/controller/DashboardAuthorizationTest.java` (multiple `@Mock` deps)
  - `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java`
  - `src/test/java/com/dispatchops/web/controller/ObjectLevelAuthControllerTest.java`
  - `src/test/java/com/dispatchops/web/controller/RoleAuthorizationMatrixTest.java`

3. **Non-HTTP (unit/integration without HTTP routing)**
- Service/domain/security/interceptor tests in:
  - `src/test/java/com/dispatchops/application/**`
  - `src/test/java/com/dispatchops/domain/**`
  - `src/test/java/com/dispatchops/infrastructure/**`
  - `src/test/java/com/dispatchops/web/interceptor/**`

### Mock Detection Rules Check

Detected mocking/stubbing patterns:
- `@Mock` widespread in `src/test/java` (e.g., `src/test/java/com/dispatchops/web/controller/FullRoleMatrixTest.java:30-40`).
- `Mockito.mock(...)` in service tests (e.g., `src/test/java/com/dispatchops/application/service/PaymentIdempotencyCollisionTest.java:41`).
- `when(...)`/`doThrow(...)` stubbing throughout controller/service tests.
- No `jest.mock`, `vi.mock`, `sinon.stub` detected.

### Coverage Summary

- Total endpoints: **91**
- Endpoints with HTTP tests (any HTTP): **91**
- Endpoints with true no-mock tests: **91**
- HTTP coverage: **100.00%**
- True API coverage: **100.00%**

### Unit Test Summary

Observed test files (non-e2e): **32** under `src/test/java`.

Covered module categories:
- Controllers: authorization matrix/object-level/auth callback mapping.
- Services: payment lifecycle/idempotency/refund/callback, password change, appeal ownership/validation.
- Domain services: transitions, window constraints, contract integrity/signing order, credit calculations.
- Security/infra: AES/HMAC/password utilities, callback security.
- Interceptors: auth/CSRF behaviors.

Important modules without dedicated same-name unit test class (inferred by file inventory):
- `src/main/java/com/dispatchops/application/service/TaskService.java`
- `src/main/java/com/dispatchops/application/service/DeliveryJobService.java`
- `src/main/java/com/dispatchops/application/service/ContractService.java`
- `src/main/java/com/dispatchops/application/service/SearchService.java`
- `src/main/java/com/dispatchops/application/service/ProfileService.java`
- `src/main/java/com/dispatchops/application/service/ShippingRuleService.java`
- `src/main/java/com/dispatchops/application/service/NotificationService.java`

### API Observability Check

Strong examples:
- `e2e/src/test/java/com/dispatchops/e2e/SearchIntegrationFlowTest.java` asserts payload content and post-state effects.
- `e2e/src/test/java/com/dispatchops/e2e/CredibilityFlowTest.java` checks response status + body fields.

Weak examples:
- Some endpoint checks still accept broad status ranges or only `!= 404` (e.g., `paymentCallbackHandlerReached`, `profileAvatarUploadHandlerReached` in `e2e/src/test/java/com/dispatchops/e2e/EndpointCoverageFlowTest.java`).

### Tests Check

- `run_tests.sh` is Docker-based for all stages -> **OK**:
  - Unit/integration in container: `run_tests.sh:20-25`
  - Stack boot via Compose: `run_tests.sh:42`
  - Explicit E2E module execution in container: `run_tests.sh:78-85` (`mvn -f e2e/pom.xml test`)

### Test Quality & Sufficiency

- Success paths: broad across jobs/tasks/payments/contracts/search/profile.
- Failure and security paths: strong RBAC/object-level breadth in both E2E and MockMvc tests.
- Validation/edge cases: present for callback DTO/security, login failure, appeal and role constraints.
- Integration boundaries: fullstack FE↔BE flows exist via Playwright UI + API tests.
- Residual weakness: some endpoint-coverage tests emphasize route reachability over deep behavioral assertions.

### End-to-End Expectations

- Project is fullstack and includes real FE↔BE tests (`AuthFlowTest`, `FulfillmentFlowTest`, `PaymentFlowTest`, `SearchFlowTest`) plus broad API E2E coverage.

### Test Coverage Score (0–100)

**91 / 100**

### Score Rationale

- + Full endpoint coverage with real no-mock HTTP tests.
- + Strong authorization and negative-path coverage.
- + Dockerized test orchestration includes explicit E2E module execution.
- - Non-trivial subset of assertions in `EndpointCoverageFlowTest` remain shallow/permissive.
- - Several core services lack dedicated direct unit test classes despite indirect coverage.

### Key Gaps

1. Increase strict response assertions for remaining permissive route-reach tests (especially callback and multipart endpoints).
2. Add dedicated unit suites for core orchestration services (Task/DeliveryJob/Contract/Search/Profile/ShippingRule/Notification) to improve fault localization.

### Confidence & Assumptions

- Confidence: **High** on endpoint inventory and static mapping.
- Confidence: **Medium-high** on "true no-mock" classification, contingent on E2E tests running against the live app stack as configured in `run_tests.sh` and `BaseE2ETest`.

---

## 2. README Audit

### Project Type Detection

- Declared at top: `Fullstack` (`README.md:3`) -> compliant.

### README Location

- `repo/README.md` exists -> pass.

### Hard Gates

Formatting:
- Structured markdown with readable headings/tables -> pass.

Startup instructions (Fullstack/Backend):
- Required literal `docker-compose up` present (`README.md:45`).
- Equivalent `docker compose up` also documented (`README.md:47`, note at `README.md:50`) -> pass.

Access method:
- URL + port provided (`http://localhost:8080`) at `README.md:52` -> pass.

Verification method:
- Explicit verification steps via UI and `curl` (`README.md:54-65`) -> pass.

Environment rules (strict):
- README explicitly disallows local runtime/manual dependency setup (`README.md:33`) and keeps workflow Docker-contained -> pass.

Demo credentials (auth exists):
- Auth exists and README provides username + password + all roles (`README.md:71-79`) -> pass.

### Engineering Quality

- Tech stack clarity: strong (`README.md:125`).
- Architecture clarity: strong (`README.md:114-123`).
- Testing instructions: now explicit and evidence-aligned with script steps (`README.md:91-95` + `run_tests.sh:78-85`).
- Security/roles/workflow: clearly documented.

### High Priority Issues

- None.

### Medium Priority Issues

1. Quick-start uses `cp` and `openssl` examples only; no Windows-native equivalents are documented.

### Low Priority Issues

1. API endpoint section is grouped by base path only; no method-level endpoint table.

### Hard Gate Failures

- None.

### README Verdict

**PASS**
