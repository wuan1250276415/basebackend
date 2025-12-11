# SQL Statistics Usage Guide

## Overview

The SQL Statistics module provides comprehensive SQL execution tracking and analysis capabilities. It automatically collects execution statistics for all SQL queries, including execution time, frequency, and failure rates.

## Features

- **Automatic SQL Tracking**: Intercepts all SQL executions and collects statistics
- **Performance Analysis**: Track execution time, identify slow queries
- **Failure Tracking**: Monitor SQL failures and error patterns
- **Query Analysis**: View most executed queries, slowest queries, and failed queries
- **Data Retention**: Automatic cleanup of expired statistics
- **Multi-tenant Support**: Track statistics per tenant
- **Data Source Tracking**: Track statistics per data source

## Configuration

### Enable SQL Statistics

Add the following configuration to your `application.yml`:

```yaml
database:
  enhanced:
    sql-statistics:
      enabled: true                    # Enable SQL statistics collection
      retention-days: 30               # Keep statistics for 30 days
      explain-enabled: false           # Enable SQL execution plan analysis (future feature)
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | boolean | false | Enable/disable SQL statistics collection |
| `retention-days` | int | 30 | Number of days to retain statistics data |
| `explain-enabled` | boolean | false | Enable SQL execution plan analysis |

## How It Works

### 1. SQL Interception

The `SqlStatisticsInterceptor` intercepts all SQL executions through MyBatis:

```java
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
```

### 2. SQL Template Generation

SQL statements are normalized and parameterized to create templates:

- Original: `SELECT * FROM users WHERE id = 123 AND name = 'John'`
- Template: `SELECT * FROM users WHERE id = ? AND name = ?`
- MD5: Calculated from the template for unique identification

### 3. Statistics Collection

For each SQL execution, the following metrics are collected:

- **Execution Count**: Number of times the SQL was executed
- **Execution Time**: Min, max, average, and total execution time
- **Failure Count**: Number of failed executions
- **Last Execution Time**: Timestamp of the last execution
- **Data Source**: Which data source was used
- **Tenant ID**: Which tenant executed the query (if multi-tenancy is enabled)

### 4. Caching Strategy

Statistics are cached in memory (using Caffeine) for 10 minutes to reduce database writes:

- Cache size: 1000 entries
- Expiration: 10 minutes after write
- Automatic flush: Every 10 minutes

## API Usage

### Query SQL Statistics

#### 1. Paginated Query

```http
POST /api/database/sql-statistics/query
Content-Type: application/json

{
  "sqlTemplate": "SELECT",           // Filter by SQL template (optional)
  "dataSourceName": "master",        // Filter by data source (optional)
  "tenantId": "tenant-001",          // Filter by tenant (optional)
  "minExecuteCount": 100,            // Minimum execution count (optional)
  "minAvgTime": 1000,                // Minimum average time in ms (optional)
  "startTime": "2024-01-01T00:00:00", // Start time (optional)
  "endTime": "2024-12-31T23:59:59",   // End time (optional)
  "orderBy": "executeCount",         // Sort field: executeCount, avgTime, maxTime, totalTime, failCount
  "orderDirection": "DESC",          // Sort direction: ASC, DESC
  "pageNum": 1,                      // Page number
  "pageSize": 20                     // Page size
}
```

#### 2. Get Slow Queries

```http
GET /api/database/sql-statistics/slow-queries?minAvgTime=1000&limit=20
```

Returns the top 20 slowest queries with average execution time >= 1000ms.

#### 3. Get Most Executed Queries

```http
GET /api/database/sql-statistics/most-executed?limit=20
```

Returns the top 20 most frequently executed queries.

#### 4. Get Most Failed Queries

```http
GET /api/database/sql-statistics/most-failed?limit=20
```

Returns the top 20 queries with the highest failure count.

#### 5. Get Statistics by ID

```http
GET /api/database/sql-statistics/{id}
```

### Maintenance Operations

#### 1. Clean Expired Statistics

```http
DELETE /api/database/sql-statistics/clean-expired?retentionDays=30
```

Manually trigger cleanup of statistics older than 30 days.

#### 2. Reset All Statistics

```http
DELETE /api/database/sql-statistics/reset
```

Delete all statistics data (use with caution).

#### 3. Delete Statistics by SQL MD5

```http
DELETE /api/database/sql-statistics/by-md5/{sqlMd5}
```

Delete statistics for a specific SQL template.

## Automatic Cleanup

The system automatically performs the following maintenance tasks:

### 1. Statistics Cleanup

- **Schedule**: Daily at 3:00 AM
- **Action**: Removes statistics older than the configured retention period
- **Configuration**: `database.enhanced.sql-statistics.retention-days`

### 2. Cache Flush

- **Schedule**: Every 10 minutes
- **Action**: Flushes in-memory statistics cache to database
- **Purpose**: Ensures data persistence

## Data Model

### SqlStatistics Entity

```java
public class SqlStatistics {
    private Long id;                      // Primary key
    private String sqlMd5;                // MD5 hash of SQL template
    private String sqlTemplate;           // Parameterized SQL statement
    private Long executeCount;            // Number of executions
    private Long totalTime;               // Total execution time (ms)
    private Long avgTime;                 // Average execution time (ms)
    private Long maxTime;                 // Maximum execution time (ms)
    private Long minTime;                 // Minimum execution time (ms)
    private Long failCount;               // Number of failures
    private LocalDateTime lastExecuteTime; // Last execution timestamp
    private String dataSourceName;        // Data source name
    private String tenantId;              // Tenant ID
}
```

## Performance Considerations

### 1. Minimal Overhead

- Asynchronous collection: Statistics are collected asynchronously to avoid blocking SQL execution
- Local caching: Reduces database writes by caching statistics in memory
- Batch updates: Statistics are flushed to database in batches

### 2. Storage Optimization

- SQL templates: Only unique SQL templates are stored (not every execution)
- Automatic cleanup: Old statistics are automatically removed
- Indexed queries: Database indexes on key fields for fast queries

### 3. Scalability

- Per-tenant tracking: Statistics can be tracked separately for each tenant
- Per-data-source tracking: Statistics can be tracked separately for each data source
- Configurable retention: Adjust retention period based on storage capacity

## Use Cases

### 1. Performance Optimization

Identify slow queries that need optimization:

```http
GET /api/database/sql-statistics/slow-queries?minAvgTime=1000&limit=20
```

### 2. Query Frequency Analysis

Find the most frequently executed queries:

```http
GET /api/database/sql-statistics/most-executed?limit=20
```

### 3. Error Monitoring

Monitor queries with high failure rates:

```http
GET /api/database/sql-statistics/most-failed?limit=20
```

### 4. Capacity Planning

Analyze query patterns to plan database capacity:

```http
POST /api/database/sql-statistics/query
{
  "startTime": "2024-01-01T00:00:00",
  "endTime": "2024-01-31T23:59:59",
  "orderBy": "executeCount",
  "orderDirection": "DESC"
}
```

## Integration with Other Features

### 1. Health Monitoring

SQL statistics complement the health monitoring feature:

- Health monitoring tracks real-time slow queries
- SQL statistics provide historical analysis

### 2. Multi-Tenancy

When multi-tenancy is enabled, statistics are tracked per tenant:

```http
POST /api/database/sql-statistics/query
{
  "tenantId": "tenant-001"
}
```

### 3. Dynamic Data Source

Statistics are tracked per data source:

```http
POST /api/database/sql-statistics/query
{
  "dataSourceName": "master"
}
```

## Troubleshooting

### Statistics Not Being Collected

1. Check if SQL statistics is enabled:
   ```yaml
   database.enhanced.sql-statistics.enabled: true
   ```

2. Verify the interceptor is registered:
   ```
   Check logs for: "Registering SQL statistics interceptor"
   ```

3. Check database table exists:
   ```sql
   SHOW TABLES LIKE 'sys_sql_statistics';
   ```

### High Memory Usage

If memory usage is high, consider:

1. Reducing cache size in `SqlStatisticsCollector`
2. Reducing cache expiration time
3. Reducing retention period to clean up old data more frequently

### Slow Query Performance

If querying statistics is slow:

1. Ensure database indexes are created (see migration script)
2. Reduce retention period to have less data
3. Use more specific filters in queries

## Best Practices

1. **Set Appropriate Retention Period**: Balance between historical data and storage capacity
2. **Monitor Storage Growth**: Regularly check the size of `sys_sql_statistics` table
3. **Use Filters**: When querying statistics, use filters to reduce result set size
4. **Regular Cleanup**: Let automatic cleanup run, or manually trigger if needed
5. **Performance Analysis**: Regularly review slow queries and optimize them
6. **Failure Monitoring**: Set up alerts for queries with high failure rates

## Future Enhancements

The following features are planned for future releases:

1. **SQL Execution Plan Analysis**: Automatic EXPLAIN analysis for slow queries
2. **Query Recommendations**: AI-powered query optimization suggestions
3. **Real-time Alerts**: Configurable alerts for slow queries or high failure rates
4. **Dashboard Integration**: Grafana dashboard for visualizing SQL statistics
5. **Export Functionality**: Export statistics to CSV or Excel for offline analysis
