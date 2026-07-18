-- Monthly Budget Feature Migration
-- Run this against your db_bookkeeping database

ALTER TABLE t_user
    ADD COLUMN monthly_budget DECIMAL(15, 2) DEFAULT NULL COMMENT 'Target pengeluaran maksimal per bulan';
