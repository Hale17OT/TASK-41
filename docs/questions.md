# DispatchOps Technical Ambiguities & Gap Analysis

As a senior developer, I have identified the following 15 system-affecting ambiguities within the project requirements. These must be resolved to ensure the integrity of the settlement, state transitions, and offline-first persistence.

---

1) **Offline Settlement "Source of Truth"**
**Question:** How does the system handle a "double-spend" or conflicting payment recorded on two different offline devices once they reconnect to the organization's server?
**My Understanding:** Settlement usually requires a central ledger. In a local-only, multi-device environment, two dispatchers might record a payment against the same reference number simultaneously.
**Solution:** Implement a **Logical Clock/Sequence ID** per terminal. Upon synchronization, the Server Ledger acts as the final arbiter. Any timestamp/reference collisions will be moved to a "Pending Reconciliation" queue for manual Auditor intervention.

2) **Internal Account Balance Logic**
**Question:** Is the "Internal Account Balance" a prepaid wallet, a credit-based system, or a simple tally of owed funds?
**My Understanding:** Without a banking backend, "balance" can be prone to manipulation if stored as a simple integer.
**Solution:** Implement **Double-Entry Bookkeeping**. Every change to a balance must have a corresponding Debit and Credit entry in a `ledger_entries` table. The balance is calculated as the sum of these immutable rows rather than a mutable field.

3) **Credit Level (A–D) Decay & Promotion**
**Question:** What are the specific quantitative triggers for a courier to move between credit levels?
**My Understanding:** The prompt mentions the limits (8 vs 1 jobs) but not the "Economy of Credit" (how points are earned or lost).
**Solution:** Define a **Credit Scoring Engine**. Level A requires a 30-day rolling average rating of >4.8 and 0 violations. Level D is triggered automatically by >3 violations in 7 days or an appeal loss. Calculations run via a Spring Batch job at 00:00 daily.

4) **"Idle for 20 Minutes" Status Detection**
**Question:** Does "Idle" refer to a lack of physical movement (GPS) or a lack of status updates in the UI?
**My Understanding:** Since the system is "local/LAN-based," constant GPS tracking may not be feasible or requested.
**Solution:** "Idle" will be defined as **Time-since-last-status-transition**. The jQuery frontend will run a background worker comparing `current_time` against the `last_updated` timestamp of the delivery job, applying a `.warning-flash` CSS class when the delta exceeds 20 minutes.

5) **E-Signature Tamper-Evidence (Offline)**
**Question:** How is a signature proven "tamper-evident" without a Public Certificate Authority (CA) in a strictly local environment?
**My Understanding:** A standard image upload of a signature is not legally or technically tamper-evident.
**Solution:** Implement **Content-Hash Binding**. When a signature is captured, the system generates an HMAC-SHA256 hash using the document's text, the signature's Base64 string, and a system-level secret. This hash is stored in the MySQL database; if the document text is edited post-signing, the hash will no longer match.

6) **The "Audit-Only" Ledger Constraints**
**Question:** If a fulfillment event is "append-only," how do staff correct a data-entry error (e.g., marking a package "Delivered" by mistake)?
**My Understanding:** Hard deletes are forbidden, but business operations require corrections.
**Solution:** Use a **Journaling Pattern**. No `UPDATE` or `DELETE` commands are permitted on the `event_log` table. To correct an error, a new "Adjustment" event is appended with a reference to the original ID, effectively nullifying the previous state for reporting.

7) **Search "Trending Terms" Calculation**
**Question:** In a high-traffic courier environment, how is "Trending" calculated without killing MySQL performance?
**My Understanding:** Real-time aggregation of thousands of search rows on every page load is inefficient.
**Solution:** Create a `search_telemetry` table. A Spring-scheduled task will aggregate the top 10 most frequent queries from the last 24 hours into a **Global Cached Variable** every hour. The jQuery UI fetches this static list rather than performing a heavy `GROUP BY` query.

8) **Conflict in Collaboration (The CC Inbox)**
**Question:** If two Operations Managers respond to the same @mention or CC task while offline, whose comments take priority?
**My Understanding:** Data loss occurs if the last person to sync overwrites the previous person.
**Solution:** Implement **Optimistic Locking** via a `version` column in the MyBatis mapping. If a user tries to save a "Done" status on a version that has already been incremented on the server, they receive a "Merge Conflict" prompt to review the other manager's comments.

9) **Media Upload Visibility Tiers**
**Question:** The prompt mentions "tiered visibility" for private fields but doesn't define the tiers for file attachments.
**My Understanding:** Couriers should not see internal Auditor notes or Dispatcher-uploaded contract drafts.
**Solution:** Assign a **Visibility Level (1-4)** to every row in the `media_attachments` table. The MyBatis XML mapper will dynamically append `AND visibility_level <= #{userRoleLevel}` to all `SELECT` queries based on the authenticated session.

10) **Refund Idempotency vs. Physical Devices**
**Question:** How do we prevent a refund from being triggered twice if the LAN connection drops during the "callback" from an on-prem device?
**My Understanding:** Unique payment references are mentioned, but the "Refund" state needs a strict lock.
**Solution:** Implement a **State Machine** for payments. A refund can only be initiated if the transaction status is exactly `SETTLED`. Upon the refund request, the status is immediately moved to `REFUND_PENDING` to block any concurrent retry attempts.

11) **Address Validation "False Negatives"**
**Question:** If the shipping template blocks a dispatch due to a ZIP code error, can an Administrator override it for a "Special Case"?
**My Understanding:** Hard blocks can stop business if the local ZIP database is slightly outdated.
**Solution:** The system will block the "Auto-Dispatch" but allow an "Admin Override." The job will be tagged as `MANUAL_VALIDATION`, requiring a mandatory comment from the Admin to bypass the shipping template rules.

12) **LAN-Only Notification Relay**
**Question:** How are "In-app" notifications delivered in real-time if there is no external network/Push service?
**My Architecture:** WebSockets can be unstable in some local server environments.
**Solution:** Implement **Long Polling** via jQuery. The frontend will hit a `/poll` endpoint every 15–30 seconds. The server will keep the request open (using Spring's `DeferredResult`) until a new notification is inserted into the MySQL `notifications` table for that user's role.

13) **Contract Template Versioning**
**Question:** Does updating a "Contract Template" affect contracts that are currently out for signature?
**My Understanding:** Changing a placeholder in a template could invalidate the legal terms of a pending agreement.
**Solution:** Use **Template Snapshotting**. When a contract is "Generated," the full text of the template is copied into a `contract_instances` table. Subsequent changes to the master template only affect *new* generations.

14) **Account Lockout Recovery**
**Question:** After the 15-minute lockout for 5 failed logins, how does the user regain access?
**My Understanding:** Manual Admin resets are tedious for a local-only system.
**Solution:** Store a `lockout_expiry` timestamp in the `users` table. The Spring Security login provider will check if `current_time > lockout_expiry`. If true, it resets the `failed_attempts` counter to 0 and allows the login attempt automatically.

15) **"Settled" Transaction Definition**
**Question:** When does a "Cash" or "Check" payment transition from "Recorded" to "Settled" for refund eligibility?
**My Understanding:** Refunds are only allowed on settled transactions, but "Cash" has no external gateway to confirm settlement.
**Solution:** All local payments (Cash/Check) enter a `PENDING_SETTLEMENT` state. They are only moved to `SETTLED` once an Auditor performs a "Daily Drawer Close" or "Reconciliation Export," confirming the physical funds were accounted for.