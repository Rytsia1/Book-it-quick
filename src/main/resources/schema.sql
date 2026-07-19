-- =====================================================================
-- Auto-initialised schema for the Bookkeeping backend.
-- Spring Boot runs this file on startup when
--   spring.sql.init.mode=always
-- is set in application.yml. Statements are idempotent so re-runs are
-- safe. This mirrors the DDL in /db-migration.sql at the project root
-- (which remains the canonical manual migration reference).
-- =====================================================================

-- 1) Monthly Budget column on t_user
ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS monthly_budget DECIMAL(15, 2) DEFAULT NULL
        COMMENT 'Target pengeluaran maksimal per bulan';

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
-- The t_recurring_bill table is the *template* side: one row per recurring
-- expense/income the user has set up. The actual transactions still live in
-- t_bill — the scheduler inserts a fresh t_bill row on the configured
-- day-of-month for every active template.
--
-- day_of_month is intentionally clamped to 1-28 (validated in the service
-- layer too) so a template can always fire, even in February.
-- last_run_year_month is the deduplication key: it stores 'YYYY-MM' of the
-- most recent successful insertion, so the same template cannot post twice
-- in the same month. The compound index (active, last_run_year_month) makes
-- the daily scan a single cheap range query.
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

-- 4) Soft-delete column on t_bill
-- Soft-delete is the industry-standard "do not lose data" pattern used in
-- financial systems: every "delete" becomes an UPDATE setting is_deleted=1,
-- and every SELECT filters by is_deleted=0. The DEFAULT 0 means existing
-- rows are treated as live automatically. The idempotent
-- INFORMATION_SCHEMA check below works on every MySQL 5.7+ / 8.x version.
SET @ix := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
              AND table_name   = 't_bill'
              AND column_name  = 'is_deleted');
SET @sql := IF(@ix = 0,
    'ALTER TABLE t_bill ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) Soft-delete column on t_category
SET @ix := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
              AND table_name   = 't_category'
              AND column_name  = 'is_deleted');
SET @sql := IF(@ix = 0,
    'ALTER TABLE t_category ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) Soft-delete column on t_recurring_bill
-- The cron job's hot query (findDueTemplates) MUST filter on is_deleted = 0
-- so a deleted template never fires again.
SET @ix := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
              AND table_name   = 't_recurring_bill'
              AND column_name  = 'is_deleted');
SET @sql := IF(@ix = 0,
    'ALTER TABLE t_recurring_bill ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7) Custom Categories table
CREATE TABLE IF NOT EXISTS t_category (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id     INT          NOT NULL                COMMENT 'Owner of this category (FK -> t_user.id)',
    type        TINYINT      NOT NULL                COMMENT '0 = expense, 1 = income',
    name        VARCHAR(64)  NOT NULL                COMMENT 'Display name of the category',
    created_at  DATETIME     DEFAULT NULL            COMMENT 'Creation timestamp',
    updated_at  DATETIME     DEFAULT NULL            COMMENT 'Last update timestamp',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_type_name (user_id, type, name),
    KEY idx_user_id (user_id),
    KEY idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='User-defined transaction categories';

-- 8) Exchange rates (multi-currency)
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
