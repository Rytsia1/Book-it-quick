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

-- 3) Custom Categories Table
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
