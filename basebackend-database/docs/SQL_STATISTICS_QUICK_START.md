# SQL Statistics Quick Start Guide

## Quick Setup (5 minutes)

### Step 1: Enable SQL Statistics

Add to your `application.yml`:

```yaml
database:
  enhanced:
    sql-statistics:
      enabled: true
      retention-days: 30
```

### Step 2: Run Database Migration

The SQL statistics table will be created automatically by Flyway on application startup.

If you need to create it manually:

```sql
CREATE TABLE IF NOT EXISTS sys_sql_statistics (
    id BIGINT NOT NULL PRIMARY KEY,
    sql_md5 VARCHAR(32) NOT NULL,
    sql_template TEXT NOT NULL,
    execute_count BIGINT NOT NULL DEFAULT 0,
    total_time BIGINT NOT NULL DEFAULT 0,
    avg_time BIGINT NOT NULL DEFAULT 0,
    max_time BIGINT NOT NULL DEFAULT 0,
    min_time BIGINT NOT NULL DEFAULT 0,
    fail_count BIGINT NOT NULL DEFAULT 0,
    last_execute_time DATETIME,
    data_source_name VARCHAR(100),
    tenant_id VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_sql_md5 (sql_md5),
    INDEX idx_execute_count (execute_count),
    INDEX idx_avg_time (avg_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Step 3: Start Your Application

```bash
mvn spring-boot:run
```

Check logs for:
```
Registering SQL statistics interceptor
Registering SQL statistics collector
```

### Step 4: Execute Some Queries

Run your application and execute some database operations. Statistics will be collected automatically.

### Step 5: View Statistics

#### Get Slow Queries

```bash
curl http://localhost:8080/api/database/sql-statistics/slow-queries?minAvgTime=100&limit=10
```

#### Get Most Executed Queries

```bash
curl http://localhost:8080/api/database/sql-statistics/most-executed?limit=10
```

#### Query with Filters

```bash
curl -X POST http://localhost:8080/api/database/sql-statistics/query \
  -H "Content-Type: application/json" \
  -d '{
    "orderBy": "avgTime",
    "orderDirection": "DESC",
    "pageNum": 1,
    "pageSize": 20
  }'
```

## Common Use Cases

### 1. Find Slow Queries

```bash
# Get queries with average execution time > 1 second
curl http://localhost:8080/api/database/sql-statistics/slow-queries?minAvgTime=1000&limit=20
```

### 2. Find Most Frequent Queries

```bash
# Get top 20 most executed queries
curl http://localhost:8080/api/database/sql-statistics/most-executed?limit=20
```

### 3. Find Failed Queries

```bash
# Get queries with failures
curl http://localhost:8080/api/database/sql-statistics/most-failed?limit=20
```

### 4. Search by SQL Pattern

```bash
curl -X POST http://localhost:8080/api/database/sql-statistics/query \
  -H "Content-Type: application/json" \
  -d '{
    "sqlTemplate": "SELECT * FROM users",
    "orderBy": "executeCount",
    "orderDirection": "DESC"
  }'
```

## Configuration Options

### Basic Configuration

```yaml
database:
  enhanced:
    sql-statistics:
      enabled: true              # Enable/disable feature
      retention-days: 30         # Keep data for 30 days
      explain-enabled: false     # SQL execution plan analysis (future)
```

### Advanced Configuration

For production environments, consider:

```yaml
database:
  enhanced:
    sql-statistics:
      enabled: true
      retention-days: 90         # Keep more historical data
      explain-enabled: false
```

## Monitoring

### Check Statistics Collection

```sql
-- Check if statistics are being collected
SELECT COUNT(*) FROM sys_sql_statistics;

-- View recent statistics
SELECT 
    sql_template,
    execute_count,
    avg_time,
    max_time,
    fail_count,
    last_execute_time
FROM sys_sql_statistics
ORDER BY last_execute_time DESC
LIMIT 10;
```

### Check Slow Queries

```sql
-- Find slow queries (avg time > 1 second)
SELECT 
    sql_template,
    execute_count,
    avg_time,
    max_time
FROM sys_sql_statistics
WHERE avg_time > 1000
ORDER BY avg_time DESC
LIMIT 20;
```

### Check Failed Queries

```sql
-- Find queries with failures
SELECT 
    sql_template,
    execute_count,
    fail_count,
    ROUND(fail_count * 100.0 / execute_count, 2) as failure_rate
FROM sys_sql_statistics
WHERE fail_count > 0
ORDER BY fail_count DESC
LIMIT 20;
```

## Maintenance

### Manual Cleanup

```bash
# Clean statistics older than 30 days
curl -X DELETE http://localhost:8080/api/database/sql-statistics/clean-expired?retentionDays=30
```

### Reset All Statistics

```bash
# WARNING: This deletes all statistics data
curl -X DELETE http://localhost:8080/api/database/sql-statistics/reset
```

## Troubleshooting

### No Statistics Being Collected

1. **Check Configuration**
   ```yaml
   database.enhanced.sql-statistics.enabled: true
   ```

2. **Check Logs**
   ```
   Look for: "Registering SQL statistics interceptor"
   ```

3. **Verify Table Exists**
   ```sql
   SHOW TABLES LIKE 'sys_sql_statistics';
   ```

### Statistics Not Updating

1. **Check Cache Flush**
   - Cache is flushed every 10 minutes
   - Wait a few minutes and check again

2. **Manual Flush** (if needed)
   - Restart the application to flush cache

### High Storage Usage

1. **Reduce Retention Period**
   ```yaml
   database.enhanced.sql-statistics.retention-days: 7
   ```

2. **Manual Cleanup**
   ```bash
   curl -X DELETE http://localhost:8080/api/database/sql-statistics/clean-expired?retentionDays=7
   ```

## Next Steps

1. **Review Documentation**: See `SQL_STATISTICS_USAGE.md` for detailed information
2. **Set Up Monitoring**: Create dashboards to visualize SQL statistics
3. **Optimize Queries**: Use statistics to identify and optimize slow queries
4. **Configure Alerts**: Set up alerts for slow queries or high failure rates

## Example Response

### Slow Queries Response

```json
[
  {
    "id": 1,
    "sqlMd5": "a1b2c3d4e5f6...",
    "sqlTemplate": "SELECT * FROM users WHERE id = ?",
    "executeCount": 1523,
    "totalTime": 45690,
    "avgTime": 30,
    "maxTime": 250,
    "minTime": 15,
    "failCount": 0,
    "lastExecuteTime": "2024-01-15T10:30:45",
    "dataSourceName": "master",
    "tenantId": null
  }
]
```

### Query Statistics Response

```json
{
  "records": [
    {
      "id": 1,
      "sqlMd5": "a1b2c3d4e5f6...",
      "sqlTemplate": "SELECT * FROM users WHERE id = ?",
      "executeCount": 1523,
      "avgTime": 30,
      "maxTime": 250,
      "failCount": 0
    }
  ],
  "total": 1,
  "size": 20,
  "current": 1,
  "pages": 1
}
```

## Support

For more information:
- Full Documentation: `SQL_STATISTICS_USAGE.md`
- Design Document: `.kiro/specs/database-enhancement/design.md`
- Requirements: `.kiro/specs/database-enhancement/requirements.md`
