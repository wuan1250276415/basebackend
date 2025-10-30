-- ================================================================================================
-- Seata Server Database Schema
-- Database: seata_server
-- Version: 1.7.1
-- Description: Seata TC (Transaction Coordinator) storage tables for AT mode
-- ================================================================================================

-- Create database (run manually before starting Seata Server)
-- CREATE DATABASE IF NOT EXISTS seata_server CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE seata_server;

-- ================================================================================================
-- 1. Global Transaction Table
-- Stores global transaction metadata for distributed transactions
-- ================================================================================================
CREATE TABLE IF NOT EXISTS `global_table` (
  `xid` VARCHAR(128) NOT NULL COMMENT 'Global transaction ID',
  `transaction_id` BIGINT COMMENT 'Transaction sequence ID',
  `status` TINYINT NOT NULL COMMENT 'Transaction status: 1=Begin, 2=Committing, 3=Committed, 4=Rollbacking, 5=RolledBack, 6=TimeoutRollbacking, 7=TimeoutRolledBack, 8=CommitFailed, 9=RollbackFailed, 10=Finished, 11=CommitRetrying, 12=RollbackRetrying, 13=AsyncCommitting',
  `application_id` VARCHAR(32) COMMENT 'Application ID (service name)',
  `transaction_service_group` VARCHAR(32) COMMENT 'Transaction service group',
  `transaction_name` VARCHAR(128) COMMENT 'Transaction name',
  `timeout` INT COMMENT 'Transaction timeout (milliseconds)',
  `begin_time` BIGINT COMMENT 'Transaction begin timestamp',
  `application_data` VARCHAR(2000) COMMENT 'Application context data',
  `gmt_create` DATETIME COMMENT 'Record creation time',
  `gmt_modified` DATETIME COMMENT 'Record modification time',
  PRIMARY KEY (`xid`),
  KEY `idx_status_gmt_modified` (`status`, `gmt_modified`),
  KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata global transaction table';

-- ================================================================================================
-- 2. Branch Transaction Table
-- Stores branch transaction metadata (one global transaction can have multiple branches)
-- ================================================================================================
CREATE TABLE IF NOT EXISTS `branch_table` (
  `branch_id` BIGINT NOT NULL COMMENT 'Branch transaction ID',
  `xid` VARCHAR(128) NOT NULL COMMENT 'Global transaction ID',
  `transaction_id` BIGINT COMMENT 'Transaction sequence ID',
  `resource_group_id` VARCHAR(32) COMMENT 'Resource group ID',
  `resource_id` VARCHAR(256) COMMENT 'Resource ID (datasource identifier)',
  `branch_type` VARCHAR(8) COMMENT 'Branch type: AT, TCC, SAGA, XA',
  `status` TINYINT COMMENT 'Branch status: 1=Registered, 2=PhaseOneDone, 3=PhaseOneFailed, 4=PhaseTwoCommitted, 5=PhaseTwoCommitFailed_Retryable, 6=PhaseTwoCommitFailed_Unretryable, 7=PhaseTwoRolledBack, 8=PhaseTwoRollbackFailed_Retryable, 9=PhaseTwoRollbackFailed_Unretryable',
  `client_id` VARCHAR(64) COMMENT 'Client ID (application instance)',
  `application_data` VARCHAR(2000) COMMENT 'Application context data',
  `gmt_create` DATETIME(6) COMMENT 'Record creation time',
  `gmt_modified` DATETIME(6) COMMENT 'Record modification time',
  PRIMARY KEY (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata branch transaction table';

-- ================================================================================================
-- 3. Global Lock Table
-- Stores global locks for AT mode pessimistic locking
-- ================================================================================================
CREATE TABLE IF NOT EXISTS `lock_table` (
  `row_key` VARCHAR(128) NOT NULL COMMENT 'Lock key: resourceId^^tableName^^pk',
  `xid` VARCHAR(128) COMMENT 'Global transaction ID holding the lock',
  `transaction_id` BIGINT COMMENT 'Transaction sequence ID',
  `branch_id` BIGINT NOT NULL COMMENT 'Branch transaction ID',
  `resource_id` VARCHAR(256) COMMENT 'Resource ID (datasource identifier)',
  `table_name` VARCHAR(32) COMMENT 'Locked table name',
  `pk` VARCHAR(36) COMMENT 'Primary key value',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT 'Lock status: 0=locked, 1=rollbacking',
  `gmt_create` DATETIME COMMENT 'Record creation time',
  `gmt_modified` DATETIME COMMENT 'Record modification time',
  PRIMARY KEY (`row_key`),
  KEY `idx_status` (`status`),
  KEY `idx_branch_id` (`branch_id`),
  KEY `idx_xid_and_branch_id` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata global lock table for AT mode';

-- ================================================================================================
-- 4. Distributed Lock Table
-- Supports Seata Server cluster mode high availability
-- ================================================================================================
CREATE TABLE IF NOT EXISTS `distributed_lock` (
  `lock_key` VARCHAR(20) NOT NULL COMMENT 'Lock key identifier',
  `lock_value` VARCHAR(20) NOT NULL COMMENT 'Lock value (holder identifier)',
  `expire` BIGINT COMMENT 'Lock expiration timestamp',
  PRIMARY KEY (`lock_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata distributed lock table for TC cluster';

-- ================================================================================================
-- Index Optimization (for high-throughput scenarios)
-- ================================================================================================

-- Additional indexes for global_table query optimization
ALTER TABLE `global_table` ADD INDEX `idx_application_id` (`application_id`);
ALTER TABLE `global_table` ADD INDEX `idx_gmt_modified_status` (`gmt_modified`, `status`);

-- Additional indexes for branch_table query optimization
ALTER TABLE `branch_table` ADD INDEX `idx_transaction_id` (`transaction_id`);
ALTER TABLE `branch_table` ADD INDEX `idx_gmt_create` (`gmt_create`);

-- Additional indexes for lock_table query optimization
ALTER TABLE `lock_table` ADD INDEX `idx_xid` (`xid`);
ALTER TABLE `lock_table` ADD INDEX `idx_gmt_modified` (`gmt_modified`);

-- ================================================================================================
-- Data Retention and Cleanup
-- ================================================================================================

-- Seata Server automatically cleans up completed transactions based on configuration:
-- - server.undo.logSaveDays: How many days to keep undo logs (default: 7)
-- - server.undo.logDeletePeriod: Cleanup interval in milliseconds (default: 86400000 = 24 hours)

-- Manual cleanup query (if needed):
-- DELETE FROM global_table WHERE status IN (3, 5, 10) AND gmt_modified < DATE_SUB(NOW(), INTERVAL 7 DAY);
-- DELETE FROM branch_table WHERE gmt_modified < DATE_SUB(NOW(), INTERVAL 7 DAY);
-- DELETE FROM lock_table WHERE gmt_modified < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- ================================================================================================
-- Verification Queries
-- ================================================================================================

-- Check active global transactions
-- SELECT xid, status, application_id, transaction_name, timeout, begin_time FROM global_table WHERE status NOT IN (3, 5, 10);

-- Check transaction statistics
-- SELECT status, COUNT(*) as count FROM global_table GROUP BY status;

-- Check branch transaction distribution
-- SELECT branch_type, status, COUNT(*) as count FROM branch_table GROUP BY branch_type, status;

-- Check global lock contention
-- SELECT table_name, COUNT(*) as lock_count FROM lock_table GROUP BY table_name ORDER BY lock_count DESC LIMIT 10;

-- ================================================================================================
-- End of Seata Server Schema
-- ================================================================================================
