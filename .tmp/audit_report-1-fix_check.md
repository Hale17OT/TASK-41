# DispatchOps Follow-Up Static Recheck (2026-04-10)

Scope: static code inspection only (no runtime execution, Docker, or tests run by reviewer).

## Verdicts

1. **High — AES-CBC without integrity protection**: **Fixed**
   - `src/main/java/com/dispatchops/infrastructure/security/AesUtil.java:12` now uses `AES/GCM/NoPadding`.
   - `src/main/java/com/dispatchops/infrastructure/security/AesUtil.java:19` introduces versioned payload (`0x02` for GCM).
   - `src/main/java/com/dispatchops/infrastructure/security/AesUtil.java:52` retains legacy CBC decryption fallback for old ciphertext.
   - `src/test/java/com/dispatchops/infrastructure/security/AesUtilTest.java:42` and `src/test/java/com/dispatchops/infrastructure/security/AesUtilTest.java:43` verify tamper rejection.
   - `src/test/java/com/dispatchops/infrastructure/security/AesUtilTest.java:47` validates legacy CBC compatibility path.

2. **High — Payment ledger test contradictions (creation vs settlement)**: **Fixed**
   - `src/main/java/com/dispatchops/application/service/PaymentService.java:143` and `src/main/java/com/dispatchops/application/service/PaymentService.java:146` document deferred ledger entries until settlement.
   - `src/test/java/com/dispatchops/application/service/PaymentLedgerMethodTest.java:22` and `src/test/java/com/dispatchops/application/service/PaymentLedgerMethodTest.java:26` explicitly test settlement-time behavior.
   - `src/test/java/com/dispatchops/application/service/PaymentLifecycleLedgerTest.java:26` and `src/test/java/com/dispatchops/application/service/PaymentLifecycleLedgerTest.java:77` align lifecycle expectations with deferred posting.

3. **Medium — Filtered delivery job pagination total mismatch**: **Fixed**
   - `src/main/java/com/dispatchops/application/service/DeliveryJobService.java:313` uses `countByStatus(status)` when filtered.
   - `src/main/java/com/dispatchops/infrastructure/persistence/mapper/DeliveryJobMapper.java:47` defines `countByStatus`.
   - `src/main/resources/mappers/DeliveryJobMapper.xml:115` implements `countByStatus` SQL.

4. **Medium — Dashboard metrics inheriting paged list totals**: **Fixed**
   - `src/main/java/com/dispatchops/web/controller/DashboardController.java:49` uses mapper aggregate count for active jobs.
   - `src/main/java/com/dispatchops/web/controller/DashboardController.java:50` uses mapper aggregate count for exception jobs.
   - `src/main/java/com/dispatchops/web/controller/DashboardController.java:60` and `src/main/java/com/dispatchops/web/controller/DashboardController.java:62` use count-based metrics in summary endpoint.

5. **Medium — Notification `read=true` pagination count bug**: **Fixed**
   - `src/main/java/com/dispatchops/application/service/NotificationService.java:99` and `src/main/java/com/dispatchops/application/service/NotificationService.java:100` use paged read-query + matching read-count.
   - `src/main/java/com/dispatchops/application/service/NotificationService.java:102` uses paged unread query.
   - `src/main/java/com/dispatchops/infrastructure/persistence/mapper/NotificationMapper.java:26` and `src/main/java/com/dispatchops/infrastructure/persistence/mapper/NotificationMapper.java:30` add read-page/read-count API.
   - `src/main/resources/mappers/NotificationMapper.xml:50`, `src/main/resources/mappers/NotificationMapper.xml:58`, `src/main/resources/mappers/NotificationMapper.xml:64` implement read/unread paged SQL paths.

6. **Medium — Calendar view restricted to TODO inbox**: **Fixed**
   - `src/main/java/com/dispatchops/web/controller/TaskController.java:131` now calls calendar-specific service path.
   - `src/main/java/com/dispatchops/application/service/TaskService.java:272` and `src/main/java/com/dispatchops/application/service/TaskService.java:274` route to mapper `findCalendarTasks`.
   - `src/main/resources/mappers/InternalTaskMapper.xml:59` and `src/main/resources/mappers/InternalTaskMapper.xml:64` query by due-time window and `show_on_calendar = 1`.

7. **Cannot Confirm (prior) — Customer rating/appeal end-to-end user path**: **Partially Fixed (static), runtime still unverified**
   - New lookup endpoint exists: `src/main/java/com/dispatchops/web/controller/CredibilityController.java:112`.
   - Service-side lookup logic exists: `src/main/java/com/dispatchops/application/service/CredibilityService.java:69`.
   - Public customer page route exists: `src/main/java/com/dispatchops/web/controller/PageController.java:29`.
   - Customer portal UI calls lookup and customer rating APIs: `src/main/webapp/WEB-INF/jsp/customer/portal.jsp:101` and `src/main/webapp/WEB-INF/jsp/customer/portal.jsp:132`.
   - Interceptor exclusions include lookup/rating/appeal customer endpoints: `src/main/java/com/dispatchops/config/WebMvcConfig.java:39`.

## Overall Outcome

- Fixed by static evidence: **6/7**
- Partially fixed by static evidence (runtime flow still not directly validated in this audit): **1/7**
