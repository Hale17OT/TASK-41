-- =====================================================
-- DispatchOps Courier Operations & Settlement System
-- MySQL 8.4 Schema -- All times stored as UTC DATETIME
-- Weight stored in POUNDS (lbs) per specification
-- =====================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- =========================
-- GROUP 1: USERS & AUTH
-- =========================

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password_hash   VARCHAR(128) NOT NULL,
    role            ENUM('ADMIN','OPS_MANAGER','DISPATCHER','COURIER','AUDITOR') NOT NULL,
    display_name    VARCHAR(128) NOT NULL,
    email_enc       VARBINARY(512),
    phone_enc       VARBINARY(512),
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    must_change_password TINYINT(1) NOT NULL DEFAULT 0,
    failed_attempts INT          NOT NULL DEFAULT 0,
    lockout_expiry  DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 2: DELIVERY JOBS
-- =========================

CREATE TABLE IF NOT EXISTS delivery_jobs (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tracking_number  VARCHAR(64)  NOT NULL UNIQUE,
    status           ENUM('CREATED','PICKED','IN_TRANSIT','DELIVERED','EXCEPTION','MANUAL_VALIDATION') NOT NULL DEFAULT 'CREATED',
    courier_id       BIGINT       NULL,
    dispatcher_id    BIGINT       NOT NULL,
    sender_name      VARCHAR(128) NOT NULL,
    sender_address   VARCHAR(512) NOT NULL,
    sender_phone_enc VARBINARY(512),
    receiver_name    VARCHAR(128) NOT NULL,
    receiver_address VARCHAR(512) NOT NULL,
    receiver_phone_enc VARBINARY(512),
    receiver_state   VARCHAR(64),
    receiver_zip     VARCHAR(16),
    weight_lbs       DECIMAL(8,2),
    order_amount     DECIMAL(12,2),
    customer_token   VARCHAR(128) NULL,
    admin_override   TINYINT(1)   NOT NULL DEFAULT 0,
    override_comment TEXT         NULL,
    last_event_at    DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version          INT          NOT NULL DEFAULT 1,
    FOREIGN KEY (courier_id)    REFERENCES users(id),
    FOREIGN KEY (dispatcher_id) REFERENCES users(id),
    INDEX idx_jobs_status (status),
    INDEX idx_jobs_courier (courier_id),
    INDEX idx_jobs_last_event (last_event_at),
    INDEX idx_jobs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- APPEND-ONLY: no UPDATE or DELETE permitted
CREATE TABLE IF NOT EXISTS fulfillment_events (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    job_id          BIGINT       NOT NULL,
    event_type      ENUM('STATUS_CHANGE','ADJUSTMENT','NOTE') NOT NULL,
    old_status      ENUM('CREATED','PICKED','IN_TRANSIT','DELIVERED','EXCEPTION','MANUAL_VALIDATION') NULL,
    new_status      ENUM('CREATED','PICKED','IN_TRANSIT','DELIVERED','EXCEPTION','MANUAL_VALIDATION') NULL,
    adjustment_ref  BIGINT       NULL,
    actor_id        BIGINT       NOT NULL,
    comment         TEXT         NULL,
    device_seq_id   BIGINT       NULL,
    device_id       VARCHAR(64)  NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id)   REFERENCES delivery_jobs(id),
    FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_events_job (job_id),
    INDEX idx_events_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 3: INTERNAL TASKS
-- =========================

CREATE TABLE IF NOT EXISTS internal_tasks (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(256) NOT NULL,
    body            TEXT,
    status          ENUM('TODO','IN_PROGRESS','BLOCKED','EXCEPTION','DONE') NOT NULL DEFAULT 'TODO',
    creator_id      BIGINT       NOT NULL,
    assignee_id     BIGINT       NOT NULL,
    due_time        DATETIME     NOT NULL,
    show_on_calendar TINYINT(1)  NOT NULL DEFAULT 1,
    job_id          BIGINT       NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT          NOT NULL DEFAULT 1,
    FOREIGN KEY (creator_id)  REFERENCES users(id),
    FOREIGN KEY (assignee_id) REFERENCES users(id),
    FOREIGN KEY (job_id)      REFERENCES delivery_jobs(id),
    INDEX idx_tasks_assignee (assignee_id),
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_due (due_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_recipients (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    inbox_type      ENUM('TODO','DONE','CC') NOT NULL,
    read_at         DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES internal_tasks(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_task_user_inbox (task_id, user_id, inbox_type),
    INDEX idx_recipients_user_inbox (user_id, inbox_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_comments (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    author_id       BIGINT       NOT NULL,
    body            TEXT         NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id)   REFERENCES internal_tasks(id),
    FOREIGN KEY (author_id) REFERENCES users(id),
    INDEX idx_comments_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 4: CREDIBILITY
-- =========================

CREATE TABLE IF NOT EXISTS credibility_ratings (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    courier_id      BIGINT       NOT NULL,
    job_id          BIGINT       NOT NULL,
    rater_id        BIGINT       NULL,
    rater_type      ENUM('STAFF','CUSTOMER') NOT NULL DEFAULT 'STAFF',
    timeliness      TINYINT      NOT NULL,
    attitude        TINYINT      NOT NULL,
    accuracy        TINYINT      NOT NULL,
    comment         TEXT,
    customer_name   VARCHAR(128) NULL,
    is_excluded     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (courier_id) REFERENCES users(id),
    FOREIGN KEY (job_id)     REFERENCES delivery_jobs(id),
    FOREIGN KEY (rater_id)   REFERENCES users(id),
    UNIQUE KEY uk_rating_job_rater (job_id, rater_id),
    UNIQUE KEY uk_rating_job_customer (job_id, customer_name),
    INDEX idx_ratings_courier (courier_id),
    INDEX idx_ratings_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS credit_levels (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    courier_id      BIGINT       NOT NULL,
    level           ENUM('A','B','C','D') NOT NULL DEFAULT 'D',
    max_concurrent  INT          NOT NULL DEFAULT 1,
    avg_rating_30d  DECIMAL(3,2) NULL,
    violations_active INT        NOT NULL DEFAULT 0,
    calculated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (courier_id) REFERENCES users(id),
    UNIQUE KEY uk_credit_courier (courier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS violations (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    courier_id      BIGINT       NOT NULL,
    job_id          BIGINT       NULL,
    violation_type  VARCHAR(128) NOT NULL,
    description     TEXT,
    penalty_start   DATETIME     NOT NULL,
    penalty_end     DATETIME     NOT NULL,
    issued_by       BIGINT       NOT NULL,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (courier_id) REFERENCES users(id),
    FOREIGN KEY (job_id)     REFERENCES delivery_jobs(id),
    FOREIGN KEY (issued_by)  REFERENCES users(id),
    INDEX idx_violations_courier (courier_id),
    INDEX idx_violations_active (is_active, penalty_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS appeals (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    rating_id       BIGINT       NULL,
    violation_id    BIGINT       NULL,
    courier_id      BIGINT       NOT NULL,
    reason          TEXT         NOT NULL,
    status          ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewer_id     BIGINT       NULL,
    reviewer_comment TEXT,
    resolved_at     DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rating_id)    REFERENCES credibility_ratings(id),
    FOREIGN KEY (violation_id) REFERENCES violations(id),
    FOREIGN KEY (courier_id)   REFERENCES users(id),
    FOREIGN KEY (reviewer_id)  REFERENCES users(id),
    INDEX idx_appeals_status (status),
    INDEX idx_appeals_courier (courier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 5: CONTRACTS
-- =========================

CREATE TABLE IF NOT EXISTS contract_templates (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(256) NOT NULL,
    description     TEXT,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_by      BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contract_template_versions (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    template_id     BIGINT       NOT NULL,
    version_number  INT          NOT NULL,
    body_text       LONGTEXT     NOT NULL,
    placeholder_schema JSON     NOT NULL,
    created_by      BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES contract_templates(id),
    FOREIGN KEY (created_by)  REFERENCES users(id),
    UNIQUE KEY uk_tpl_version (template_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contract_instances (
    id                   BIGINT   AUTO_INCREMENT PRIMARY KEY,
    template_version_id  BIGINT   NOT NULL,
    snapshot_body_text   LONGTEXT NOT NULL,
    rendered_text        LONGTEXT NOT NULL,
    placeholder_values   JSON     NOT NULL,
    job_id               BIGINT   NULL,
    generated_by         BIGINT   NOT NULL,
    status               ENUM('DRAFT','PENDING_SIGNATURE','PARTIALLY_SIGNED','FULLY_SIGNED','VOIDED') NOT NULL DEFAULT 'DRAFT',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_version_id) REFERENCES contract_template_versions(id),
    FOREIGN KEY (job_id)       REFERENCES delivery_jobs(id),
    FOREIGN KEY (generated_by) REFERENCES users(id),
    INDEX idx_contract_inst_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- APPEND-ONLY signing records
CREATE TABLE IF NOT EXISTS signing_records (
    id                   BIGINT   AUTO_INCREMENT PRIMARY KEY,
    contract_instance_id BIGINT   NOT NULL,
    signer_id            BIGINT   NOT NULL,
    signer_order         INT      NOT NULL,
    signature_data       LONGTEXT NOT NULL,
    document_hash        VARCHAR(128) NOT NULL,
    signed_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address           VARCHAR(45),
    FOREIGN KEY (contract_instance_id) REFERENCES contract_instances(id),
    FOREIGN KEY (signer_id) REFERENCES users(id),
    INDEX idx_signing_contract (contract_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 6: PAYMENTS & LEDGER
-- =========================

CREATE TABLE IF NOT EXISTS payments (
    id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    idempotency_key       VARCHAR(64)  NOT NULL UNIQUE,
    job_id                BIGINT       NOT NULL,
    payer_id              BIGINT       NOT NULL,
    amount                DECIMAL(12,2) NOT NULL,
    method                ENUM('CASH','CHECK','INTERNAL_BALANCE') NOT NULL,
    status                ENUM('PENDING_SETTLEMENT','SETTLED','REFUND_PENDING','REFUNDED','CANCELLED') NOT NULL DEFAULT 'PENDING_SETTLEMENT',
    check_number          VARCHAR(64)  NULL,
    settled_by            BIGINT       NULL,
    settled_at            DATETIME     NULL,
    refund_eligible_until DATETIME     NULL,
    device_id             VARCHAR(64)  NULL,
    device_seq_id         BIGINT       NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id)      REFERENCES delivery_jobs(id),
    FOREIGN KEY (payer_id)    REFERENCES users(id),
    FOREIGN KEY (settled_by)  REFERENCES users(id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_job (job_id),
    INDEX idx_payments_settled (settled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- APPEND-ONLY double-entry ledger
CREATE TABLE IF NOT EXISTS ledger_entries (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    payment_id      BIGINT       NULL,
    account_id      BIGINT       NOT NULL,
    entry_type      ENUM('DEBIT','CREDIT') NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    balance_after   DECIMAL(12,2) NOT NULL,
    description     VARCHAR(512) NOT NULL,
    reference_type  VARCHAR(64)  NOT NULL,
    reference_id    BIGINT       NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id),
    FOREIGN KEY (account_id) REFERENCES users(id),
    INDEX idx_ledger_account (account_id),
    INDEX idx_ledger_payment (payment_id),
    INDEX idx_ledger_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_credentials (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    device_id       VARCHAR(64)  NOT NULL UNIQUE,
    device_name     VARCHAR(128) NOT NULL,
    shared_secret   VARCHAR(256) NOT NULL,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reconciliation_queue (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    payment_id_a    BIGINT       NOT NULL,
    payment_id_b    BIGINT       NULL,
    conflict_type   VARCHAR(128) NOT NULL,
    status          ENUM('PENDING','RESOLVED','DISMISSED') NOT NULL DEFAULT 'PENDING',
    resolved_by     BIGINT       NULL,
    resolution_note TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     DATETIME     NULL,
    FOREIGN KEY (payment_id_a) REFERENCES payments(id),
    FOREIGN KEY (resolved_by)  REFERENCES users(id),
    INDEX idx_recon_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- APPEND-ONLY callback event log for device callbacks
CREATE TABLE IF NOT EXISTS callback_events (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    event_id        VARCHAR(128) NOT NULL UNIQUE,
    device_id       VARCHAR(64)  NOT NULL,
    device_seq_id   BIGINT       NULL,
    payload_hash    VARCHAR(128) NOT NULL,
    raw_payload     TEXT         NOT NULL,
    signature       VARCHAR(256) NOT NULL,
    source_ip       VARCHAR(45)  NULL,
    status          ENUM('RECEIVED','VERIFIED','PROCESSED','FAILED') NOT NULL DEFAULT 'RECEIVED',
    verified_at     DATETIME     NULL,
    processed_at    DATETIME     NULL,
    failure_reason  TEXT         NULL,
    received_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_callback_device (device_id),
    INDEX idx_callback_event (event_id),
    INDEX idx_callback_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 7: SHIPPING RULES
-- =========================

CREATE TABLE IF NOT EXISTS shipping_templates (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(256) NOT NULL,
    description     TEXT,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_by      BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS region_rules (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    template_id      BIGINT       NOT NULL,
    state_code       VARCHAR(8)   NULL,
    zip_range_start  VARCHAR(16)  NULL,
    zip_range_end    VARCHAR(16)  NULL,
    min_weight_lbs   DECIMAL(8,2) NULL,
    max_weight_lbs   DECIMAL(8,2) NULL,
    min_order_amount DECIMAL(12,2) NULL,
    max_order_amount DECIMAL(12,2) NULL,
    is_allowed       TINYINT(1)   NOT NULL DEFAULT 1,
    priority         INT          NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES shipping_templates(id),
    INDEX idx_region_rules_tpl (template_id),
    INDEX idx_region_rules_state_zip (state_code, zip_range_start, zip_range_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 8: NOTIFICATIONS
-- =========================

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    recipient_id    BIGINT       NOT NULL,
    type            VARCHAR(64)  NOT NULL,
    title           VARCHAR(256) NOT NULL,
    body            TEXT,
    link_type       VARCHAR(64)  NULL,
    link_id         BIGINT       NULL,
    is_read         TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES users(id),
    INDEX idx_notif_recipient_unread (recipient_id, is_read),
    INDEX idx_notif_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lan_relay_subscriptions (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    topic           VARCHAR(128) NOT NULL,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_relay_user_topic (user_id, topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 9: SEARCH
-- =========================

CREATE TABLE IF NOT EXISTS search_index (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    entity_type     VARCHAR(64)  NOT NULL,
    entity_id       BIGINT       NOT NULL,
    title           VARCHAR(512) NOT NULL,
    description     TEXT,
    tags            VARCHAR(1024),
    author_id       BIGINT       NULL,
    last_indexed_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_search_entity (entity_type, entity_id),
    FULLTEXT idx_search_ft (title, description, tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS synonym_mappings (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    term            VARCHAR(128) NOT NULL,
    synonym         VARCHAR(128) NOT NULL,
    INDEX idx_synonym_term (term)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS search_telemetry (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    query_text      VARCHAR(256) NOT NULL,
    result_count    INT          NOT NULL DEFAULT 0,
    searched_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_telemetry_searched (searched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trending_terms (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    term            VARCHAR(256) NOT NULL,
    search_count    INT          NOT NULL,
    period_start    DATETIME     NOT NULL,
    period_end      DATETIME     NOT NULL,
    calculated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trending_calc (calculated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- GROUP 10: PROFILES & MEDIA
-- =========================

CREATE TABLE IF NOT EXISTS user_profiles (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    bio             TEXT,
    id_number_enc   VARBINARY(512),
    address_enc     VARBINARY(1024),
    emergency_contact_enc VARBINARY(512),
    avatar_path     VARCHAR(512) NULL,
    visibility_level TINYINT     NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Per-field visibility settings (prompt: "tiered visibility for private fields")
CREATE TABLE IF NOT EXISTS profile_field_visibility (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    field_name      VARCHAR(64)  NOT NULL,
    visibility_tier TINYINT      NOT NULL DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_field_vis (user_id, field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS media_attachments (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    entity_type      VARCHAR(64)  NOT NULL,
    entity_id        BIGINT       NOT NULL,
    uploader_id      BIGINT       NOT NULL,
    file_name        VARCHAR(256) NOT NULL,
    file_path        VARCHAR(512) NOT NULL,
    file_size        BIGINT       NOT NULL,
    mime_type        VARCHAR(128) NOT NULL,
    visibility_level TINYINT      NOT NULL DEFAULT 1,
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uploader_id) REFERENCES users(id),
    INDEX idx_media_entity (entity_type, entity_id),
    INDEX idx_media_visibility (visibility_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS profile_change_log (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    profile_id      BIGINT       NOT NULL,
    field_name      VARCHAR(128) NOT NULL,
    old_value_masked VARCHAR(512),
    new_value_masked VARCHAR(512),
    changed_by      BIGINT       NOT NULL,
    changed_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES user_profiles(id),
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_profile_changes (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- APPEND-ONLY TRIGGERS
-- =========================

DELIMITER //

CREATE TRIGGER trg_fulfillment_events_no_update BEFORE UPDATE ON fulfillment_events
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fulfillment_events is append-only: UPDATE prohibited';
END //

CREATE TRIGGER trg_fulfillment_events_no_delete BEFORE DELETE ON fulfillment_events
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fulfillment_events is append-only: DELETE prohibited';
END //

CREATE TRIGGER trg_ledger_no_update BEFORE UPDATE ON ledger_entries
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ledger_entries is append-only: UPDATE prohibited';
END //

CREATE TRIGGER trg_ledger_no_delete BEFORE DELETE ON ledger_entries
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ledger_entries is append-only: DELETE prohibited';
END //

CREATE TRIGGER trg_signing_records_no_update BEFORE UPDATE ON signing_records
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'signing_records is append-only: UPDATE prohibited';
END //

CREATE TRIGGER trg_signing_records_no_delete BEFORE DELETE ON signing_records
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'signing_records is append-only: DELETE prohibited';
END //

DELIMITER ;

-- =========================
-- SEED DATA
-- WARNING: Default passwords below are for initial bootstrap ONLY.
-- Change ALL passwords immediately after first deployment.
-- In production, use a secure bootstrap script instead of these seed inserts.
-- =========================

-- System/org account for ledger double-entry (id=1, used as org-side account in settlement/refund ledger entries)
-- NOTE: email_enc is NULL for seed users. Set via application after startup (email is AES-encrypted at rest).
INSERT INTO users (username, password_hash, role, display_name, is_active)
VALUES ('system', '$2a$12$PolFds0gxycN0zbWKTmhK.BmOEwjP2sowVxaIb9k6Af0B988kU.iG', 'ADMIN', 'System Account', 0);

-- Default admin (password: Admin123! - MUST BE CHANGED on first login)
INSERT INTO users (username, password_hash, role, display_name, is_active, must_change_password)
VALUES ('admin', '$2a$12$PolFds0gxycN0zbWKTmhK.BmOEwjP2sowVxaIb9k6Af0B988kU.iG', 'ADMIN', 'System Administrator', 1, 1);

-- Default test users for each role
-- Test users (all have must_change_password=1 -- CHANGE on first real deployment)
-- email_enc omitted: set via application (AES-encrypted at rest)
INSERT INTO users (username, password_hash, role, display_name, is_active, must_change_password) VALUES
('ops_manager', '$2a$12$PolFds0gxycN0zbWKTmhK.BmOEwjP2sowVxaIb9k6Af0B988kU.iG', 'OPS_MANAGER', 'Operations Manager', 1, 1),
('dispatcher1', '$2a$12$PolFds0gxycN0zbWKTmhK.BmOEwjP2sowVxaIb9k6Af0B988kU.iG', 'DISPATCHER', 'Dispatcher One', 1, 1),
('courier1', '$2a$12$PolFds0gxycN0zbWKTmhK.BmOEwjP2sowVxaIb9k6Af0B988kU.iG', 'COURIER', 'Courier One', 1, 1),
('courier2', '$2a$12$PolFds0gxycN0zbWKTmhK.BmOEwjP2sowVxaIb9k6Af0B988kU.iG', 'COURIER', 'Courier Two', 1, 1),
('auditor1', '$2a$12$PolFds0gxycN0zbWKTmhK.BmOEwjP2sowVxaIb9k6Af0B988kU.iG', 'AUDITOR', 'Auditor One', 1, 1);

-- Default credit levels for couriers
INSERT INTO credit_levels (courier_id, level, max_concurrent, avg_rating_30d, violations_active, calculated_at)
SELECT id, 'B', 5, NULL, 0, NOW() FROM users WHERE role = 'COURIER';

-- Default user profiles
INSERT INTO user_profiles (user_id, bio, visibility_level)
SELECT id, '', 1 FROM users;

-- Sample shipping template
INSERT INTO shipping_templates (name, description, is_active, created_by)
VALUES ('Standard Domestic', 'Default domestic shipping rules', 1, 1);

INSERT INTO region_rules (template_id, state_code, zip_range_start, zip_range_end, min_weight_lbs, max_weight_lbs, min_order_amount, max_order_amount, is_allowed, priority) VALUES
(1, 'CA', '90000', '96199', 0.1, 150.00, 1.00, 10000.00, 1, 10),
(1, 'NY', '10000', '14999', 0.1, 150.00, 1.00, 10000.00, 1, 10),
(1, 'TX', '73000', '79999', 0.1, 150.00, 1.00, 10000.00, 1, 10),
(1, 'FL', '32000', '34999', 0.1, 150.00, 1.00, 10000.00, 1, 10),
(1, 'IL', '60000', '62999', 0.1, 150.00, 1.00, 10000.00, 1, 10);

-- Sample synonym mappings for search
INSERT INTO synonym_mappings (term, synonym) VALUES
('parcel', 'package'),
('package', 'parcel'),
('shipment', 'delivery'),
('delivery', 'shipment'),
('courier', 'driver'),
('driver', 'courier'),
('payment', 'settlement'),
('settlement', 'payment'),
('contract', 'agreement'),
('agreement', 'contract');
