# Issue Revalidation (Round 2)

Static re-check of the 5 previously reported issues after your latest changes.

## Verdict
- **4 fixed, 1 fixed as hygiene** -> **All 5 issues are now resolved by static evidence**.

## Detailed Results

1. **High - Customer appeal flow missing in customer portal**
- **Status:** **Fixed (static evidence)**
- **What changed:**
  - Customer portal now has dedicated appeal UI with 48-hour messaging and form state.
  - Appeal form posts to `POST /api/credibility/appeals/customer` and includes `ratingId`.
  - Lookup API now returns `canAppeal` and `appealableRatings`, which portal uses to populate selectable ratings.
- **Evidence:**
  - UI section: `src/main/webapp/WEB-INF/jsp/customer/portal.jsp:75`
  - Appeal endpoint wiring: `src/main/webapp/WEB-INF/jsp/customer/portal.jsp:197`
  - `ratingId` included in payload: `src/main/webapp/WEB-INF/jsp/customer/portal.jsp:205`
  - Rating selector population: `src/main/webapp/WEB-INF/jsp/customer/portal.jsp:137`
  - Backend endpoint exists: `src/main/java/com/dispatchops/web/controller/CredibilityController.java:197`
  - Backend lookup returns appeal metadata: `src/main/java/com/dispatchops/application/service/CredibilityService.java:99`

2. **High - Test runner masks E2E failures**
- **Status:** **Fixed**
- **Evidence:**
  - E2E command no longer has `|| true`: `run_tests.sh:82`
  - Exit captured and enforced: `run_tests.sh:84`, `run_tests.sh:89`, `run_tests.sh:92`

3. **Medium - E2E assertions contradict customer endpoint behavior**
- **Status:** **Fixed**
- **Evidence:**
  - E2E now asserts `422` for non-existent customer rating/appeal requests: `e2e/src/test/java/com/dispatchops/e2e/CustomerCredibilityFlowTest.java:19`, `e2e/src/test/java/com/dispatchops/e2e/CustomerCredibilityFlowTest.java:38`
  - Controller behavior remains `422` for normalized failures: `src/main/java/com/dispatchops/web/controller/CredibilityController.java:106`, `src/main/java/com/dispatchops/web/controller/CredibilityController.java:212`

4. **Medium - Task route authorization intent not explicit**
- **Status:** **Fixed**
- **Evidence:** explicit `@RequireRole` now present on previously flagged routes:
  - `GET /api/tasks/{id}`: `src/main/java/com/dispatchops/web/controller/TaskController.java:52`
  - `PUT /api/tasks/{id}/status`: `src/main/java/com/dispatchops/web/controller/TaskController.java:76`
  - `POST /api/tasks/{id}/comments`: `src/main/java/com/dispatchops/web/controller/TaskController.java:93`
  - `GET /api/tasks/calendar`: `src/main/java/com/dispatchops/web/controller/TaskController.java:124`

5. **Low - Build artifacts present in delivery tree (`target/`)**
- **Status:** **Fixed**
- **Evidence:** root listing no longer contains `target/` (current entries show no `target/`).

## Boundary Note
- This is a **static-only** confirmation. Runtime outcomes (UI behavior in browser, E2E pass/fail execution) remain **Manual Verification Required**.
