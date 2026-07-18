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

-- 2) Custom Categories table
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
