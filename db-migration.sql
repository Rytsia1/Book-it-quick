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
