-- =====================================================================
-- Custom Categories Feature Migration
-- Run this against your db_bookkeeping database
-- =====================================================================

-- 1) Monthly Budget Feature (pre-existing)
ALTER TABLE t_user
    ADD COLUMN monthly_budget DECIMAL(15, 2) DEFAULT NULL COMMENT 'Target pengeluaran maksimal per bulan';

-- 2) Pagination index on t_bill
-- Required for the new GET /api/bills/page endpoint's
--   ORDER BY bill_date DESC LIMIT N
-- Without this index, the query falls back to a full table scan once the
-- user accumulates many rows. The dynamic INFORMATION_SCHEMA check below
-- keeps the migration idempotent on every MySQL 5.7+ / 8.x version (the
-- portable `ALTER TABLE ... ADD INDEX IF NOT EXISTS` form is only available
-- in MySQL 8.0.32+).
SET @ix := (SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE table_schema = DATABASE()
              AND table_name   = 't_bill'
              AND index_name   = 'idx_user_bill_date');
SET @sql := IF(@ix = 0,
    'ALTER TABLE t_bill ADD INDEX idx_user_bill_date (user_id, bill_date)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Recurring bill templates (auto-posted monthly by the @Scheduled job)
-- See the matching comment in src/main/resources/schema.sql for the full
-- rationale. day_of_month is clamped to 1-28 in the service layer.
CREATE TABLE IF NOT EXISTS t_recurring_bill (
    id                  INT          NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id             INT          NOT NULL                COMMENT 'Owner of this template (FK -> t_user.id)',
    amount              DECIMAL(15,2) NOT NULL               COMMENT 'Amount to post every month',
    type                TINYINT      NOT NULL                COMMENT '0 = expense, 1 = income',
    category            VARCHAR(64)  NOT NULL                COMMENT 'Category name (matches t_bill.category)',
    description         VARCHAR(255) DEFAULT NULL            COMMENT 'Optional note copied into the generated bill',
    day_of_month        TINYINT      NOT NULL                COMMENT 'Day of month to post (1-28)',
    start_year_month    CHAR(7)      NOT NULL                COMMENT 'YYYY-MM; the first month the template is eligible to fire',
    last_run_year_month CHAR(7)      DEFAULT NULL            COMMENT 'YYYY-MM of the most recent successful insert; NULL = never run',
    active              TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '0 = paused, 1 = active',
    created_at          DATETIME     DEFAULT NULL            COMMENT 'Creation timestamp',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_active_due (active, last_run_year_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Templates for automatically generated monthly bills';

-- 4) Soft-delete columns (industry-standard "no data loss" pattern).
-- Every "delete" becomes an UPDATE setting is_deleted = 1; every SELECT
-- filters by is_deleted = 0. The DEFAULT 0 means existing rows are
-- treated as live automatically. The idempotent INFORMATION_SCHEMA check
-- below works on every MySQL 5.7+ / 8.x version.
-- See the matching comments in src/main/resources/schema.sql for rationale.

-- t_bill
SET @ix := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
              AND table_name   = 't_bill'
              AND column_name  = 'is_deleted');
SET @sql := IF(@ix = 0,
    'ALTER TABLE t_bill ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_category
SET @ix := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
              AND table_name   = 't_category'
              AND column_name  = 'is_deleted');
SET @sql := IF(@ix = 0,
    'ALTER TABLE t_category ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_recurring_bill
SET @ix := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
              AND table_name   = 't_recurring_bill'
              AND column_name  = 'is_deleted');
SET @sql := IF(@ix = 0,
    'ALTER TABLE t_recurring_bill ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) Custom Categories Table
-- Each row is a user-defined category that the user can freely create,
-- edit, or delete. Linked to the user through `user_id`.
CREATE TABLE IF NOT EXISTS t_category (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id     INT          NOT NULL                COMMENT 'Owner of this category (FK -> t_user.id)',
    type        TINYINT      NOT NULL                COMMENT '0 = expense, 1 = income',
    name        VARCHAR(64)  NOT NULL                COMMENT 'Display name of the category',
    created_at  DATETIME     DEFAULT NULL            COMMENT 'Creation timestamp',
    updated_at  DATETIME     DEFAULT NULL            COMMENT 'Last update timestamp',
    PRIMARY KEY (id),
    -- Prevent duplicate names of the same type for the same user.
    UNIQUE KEY uk_user_type_name (user_id, type, name),
    KEY idx_user_id (user_id),
    KEY idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='User-defined transaction categories';

-- 6) Exchange rates (multi-currency)
-- One row per target currency, expressed as "1 USD = rate * <code>".
-- The CurrencyScheduler in config/ refreshes every supported code
-- once a day at 11:00 Asia/Jakarta. currency_code is the natural unique
-- key — the sync service upserts (insert if missing, update otherwise)
-- so this table is safe to (re)create idempotently.
CREATE TABLE IF NOT EXISTS exchange_rate (
    id            INT           NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    currency_code VARCHAR(8)    NOT NULL                COMMENT 'ISO 4217 code, e.g. IDR, EUR, JPY',
    rate          DECIMAL(20,8) NOT NULL                COMMENT '1 USD = rate * target_currency',
    last_updated  DATETIME      NOT NULL                COMMENT 'Timestamp of the last successful sync',
    PRIMARY KEY (id),
    UNIQUE KEY uk_currency_code (currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='USD-based exchange rates, refreshed daily by CurrencyScheduler';

-- 7) Refresh Tokens (long-lived, revocable)
-- The refresh token is an opaque random string (NOT a JWT) so the server
-- has full authority to revoke it. We never store the raw token — only
-- a SHA-256 hash. The token is sent to the client on /login and
-- exchanged at /refresh; the server looks up by hash, checks the
-- (revoked, expires_at) pair, and issues a new access token + rotated
-- refresh token. Rotating on every refresh (replaced_by column) means a
-- stolen refresh token can only be used once before the legitimate
-- client's next refresh invalidates it.
CREATE TABLE IF NOT EXISTS t_refresh_token (
    id           INT          NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id      INT          NOT NULL                COMMENT 'Owner of this refresh token (FK -> t_user.id)',
    token_hash   CHAR(64)     NOT NULL                COMMENT 'SHA-256 hex digest of the opaque refresh token (64 hex chars)',
    expires_at   DATETIME     NOT NULL                COMMENT 'When this refresh token stops being redeemable',
    revoked      TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '0 = active, 1 = revoked (logout / rotated / anomaly)',
    replaced_by  INT          DEFAULT NULL            COMMENT 'FK -> t_refresh_token.id of the token that replaced this one (rotation chain)',
    created_at   DATETIME     DEFAULT NULL            COMMENT 'Creation timestamp',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token_hash (token_hash),
    KEY idx_user_id (user_id),
    KEY idx_expires_at (expires_at),
    KEY idx_user_active (user_id, revoked, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Opaque, hashed, revocable refresh tokens issued at login';

-- 8) Access-Token Denylist (jti-based, short-lived)
-- Optional safety net for explicit access-token revocation (e.g. anomaly
-- detected, admin force-logout). The denylist is NOT consulted on every
-- request (that would defeat the whole point of stateless JWT); it is
-- only checked at /refresh and /logout. Rows have their own expires_at
-- so the daily cleanup job can drop them once the access token they
-- reference would have naturally expired anyway — the table never grows
-- unbounded.
CREATE TABLE IF NOT EXISTS t_token_denylist (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    jti         VARCHAR(64)  NOT NULL                COMMENT 'JWT ID claim of the revoked access token (UUID)',
    user_id     INT          DEFAULT NULL            COMMENT 'Owner of the revoked token, for diagnostics',
    expires_at  DATETIME     NOT NULL                COMMENT 'When the original access token would have naturally expired; cleanup deletes rows past this',
    reason      VARCHAR(64)  DEFAULT NULL            COMMENT 'Why it was revoked: logout, anomaly, rotation, manual',
    created_at  DATETIME     DEFAULT NULL            COMMENT 'When the row was inserted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_jti (jti),
    KEY idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Explicitly revoked access tokens, checked at /refresh and /logout only';

-- ────────────────────────────────────────────────────────────────────
-- 7) RBAC: User role column
--   t_user.role is an ENUM('USER', 'ADMIN') with a default of 'USER'.
--   Self-registered accounts via /api/auth/register automatically get
--   the 'USER' role. The 'ADMIN' role is granted manually (via a direct
--   database UPDATE) to bootstrap the first administrator; subsequent
--   admin promotion can happen via the admin-only /api/admin/users/:id/role
--   endpoint.
--
--   The Spring Security layer in SecurityConfig.java uses
--   @PreAuthorize("hasRole('ADMIN')") to gate admin-only endpoints.
--   The role is also embedded in the JWT (claim "role") so the
--   JwtAuthenticationFilter can populate the SecurityContext without
--   a DB lookup on every request.
--
--   Adding the column is idempotent: the INFORMATION_SCHEMA check
--   skips the ALTER TABLE on databases that already have it.
-- ────────────────────────────────────────────────────────────────────
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE table_schema = DATABASE()
               AND table_name   = 't_user'
               AND column_name  = 'role');
SET @sql := IF(@col = 0,
    'ALTER TABLE t_user ADD COLUMN role ENUM(''USER'', ''ADMIN'') NOT NULL DEFAULT ''USER'' COMMENT ''RBAC role: USER can manage own data, ADMIN can manage everything''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Index for the admin-promotion flow: when an existing admin lists
-- "all users with the ADMIN role" (or counts them for the bootstrap
-- "is there an admin yet?" check), this index makes the query a fast
-- index-only scan instead of a full table scan.
SET @ix := (SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE table_schema = DATABASE()
              AND table_name   = 't_user'
              AND index_name   = 'idx_user_role');
SET @sql := IF(@ix = 0,
    'ALTER TABLE t_user ADD INDEX idx_user_role (role)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ────────────────────────────────────────────────────────────────────
-- Bootstrap: promote the very first user to ADMIN so the system has
-- an admin out of the box. After this runs once, every subsequent
-- user registered through /api/auth/register gets the default
-- 'USER' role, and existing admins can promote others through the
-- /api/admin/users/:id/role endpoint.
-- ────────────────────────────────────────────────────────────────────
UPDATE t_user
   SET role = 'ADMIN'
 WHERE id = (SELECT id FROM (SELECT MIN(id) AS id FROM t_user) AS _first)
   AND NOT EXISTS (SELECT 1 FROM t_user WHERE role = 'ADMIN');
