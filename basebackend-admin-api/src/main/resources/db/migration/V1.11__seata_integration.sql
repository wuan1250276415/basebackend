-- ================================================================================================
-- Seata AT Mode Client-Side Schema
-- Version: V1.11
-- Description: Add undo_log table for Seata AT mode automatic compensation
-- Author: Claude Code
-- Date: 2025-10-30
-- ================================================================================================

-- ================================================================================================
-- Seata Undo Log Table
-- Purpose: Store before-image and after-image data for automatic rollback in AT mode
-- Scope: Each business database needs this table
-- ================================================================================================

CREATE TABLE IF NOT EXISTS `undo_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `branch_id` BIGINT(20) NOT NULL COMMENT 'Branch transaction ID',
  `xid` VARCHAR(128) NOT NULL COMMENT 'Global transaction ID',
  `context` VARCHAR(128) NOT NULL COMMENT 'Undo log context (serialization type, etc.)',
  `rollback_info` LONGBLOB NOT NULL COMMENT 'Rollback data (before-image and after-image)',
  `log_status` INT(11) NOT NULL COMMENT '0: normal, 1: defense (prevent dirty write)',
  `log_created` DATETIME(6) NOT NULL COMMENT 'Creation timestamp',
  `log_modified` DATETIME(6) NOT NULL COMMENT 'Modification timestamp',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`),
  KEY `idx_log_created` (`log_created`),
  KEY `idx_branch_id` (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT mode undo log table';

-- ================================================================================================
-- Table Design Notes
-- ================================================================================================

-- 1. rollback_info Column:
--    - Type: LONGBLOB (supports up to 4GB data)
--    - Content: Stores before-image (original data) and after-image (modified data)
--    - Serialization: Controlled by 'context' column (default: jackson)
--    - Compression: Enabled for data > 64KB (configurable via seata.client.undo.compress)

-- 2. Unique Index ux_undo_log:
--    - Ensures one undo_log record per branch transaction
--    - Prevents duplicate undo logs for the same branch

-- 3. Index Optimization:
--    - idx_log_created: For cleanup jobs (delete old undo logs)
--    - idx_branch_id: For querying undo logs by branch
--    - idx_xid: For querying all undo logs in a global transaction

-- 4. Data Lifecycle:
--    - Created: When AT mode branch transaction registers (Phase 1)
--    - Deleted: After successful commit or rollback (Phase 2)
--    - Retained: Failed transactions may leave undo_log records (requires manual cleanup)

-- ================================================================================================
-- Automatic Cleanup Configuration
-- ================================================================================================

-- Seata client automatically cleans up undo_log based on configuration:
-- seata.client.undo.log-serialization: jackson (default)
-- seata.client.undo.only-care-update-columns: true (only log updated columns)
-- Cleanup happens after Phase 2 (commit or rollback)

-- For manual cleanup of stale undo_log records (optional):
-- DELETE FROM undo_log WHERE log_created < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- ================================================================================================
-- Storage Optimization for High-Volume Scenarios
-- ================================================================================================

-- Enable compression for large undo_log tables:
-- ALTER TABLE undo_log ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;

-- Partition by date for better performance (optional, for very large deployments):
-- ALTER TABLE undo_log
-- PARTITION BY RANGE (TO_DAYS(log_created)) (
--     PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
--     PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
--     PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
--     PARTITION pmax VALUES LESS THAN MAXVALUE
-- );

-- ================================================================================================
-- Verification Queries
-- ================================================================================================

-- Check undo_log table status:
-- SELECT
--     TABLE_NAME,
--     ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS 'Size (MB)',
--     TABLE_ROWS AS 'Row Count'
-- FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = DATABASE()
-- AND TABLE_NAME = 'undo_log';

-- Check active undo_log records:
-- SELECT xid, branch_id, log_status, log_created FROM undo_log ORDER BY log_created DESC LIMIT 10;

-- Check undo_log growth trend:
-- SELECT
--     DATE(log_created) AS date,
--     COUNT(*) AS undo_log_count,
--     ROUND(SUM(LENGTH(rollback_info)) / 1024 / 1024, 2) AS total_size_mb
-- FROM undo_log
-- GROUP BY DATE(log_created)
-- ORDER BY date DESC
-- LIMIT 7;

-- ================================================================================================
-- Troubleshooting
-- ================================================================================================

-- Issue 1: undo_log records not created
-- Cause: DataSourceProxy not applied
-- Solution: Verify SeataDataSourceConfig is configured correctly

-- Issue 2: undo_log table growing rapidly
-- Cause: High transaction volume or large data modifications
-- Solution:
--   1. Enable compression: seata.client.undo.compress.enable=true
--   2. Only log updated columns: seata.client.undo.only-care-update-columns=true
--   3. Reduce transaction scope

-- Issue 3: Old undo_log records not cleaned up
-- Cause: Incomplete Phase 2 (commit/rollback)
-- Solution: Check Seata Server connectivity and transaction status in global_table

-- ================================================================================================
-- End of Seata Client-Side Schema
-- ================================================================================================
