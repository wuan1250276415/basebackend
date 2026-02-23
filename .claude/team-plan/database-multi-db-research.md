# Team Research: database-multi-db

## Enhanced Requirements

**Goal**: Refactor `basebackend-database` module to support MySQL + PostgreSQL as target databases.

**Scope**:
- Add PostgreSQL driver + Flyway PostgreSQL support to `pom.xml`
- Introduce database dialect abstraction for vendor-specific SQL
- Split Flyway migration directories by database type (`db/migration/mysql/`, `db/migration/postgresql/`)
- Auto-detect database vendor from JDBC URL and configure accordingly
- Keep ShardingSphere MySQL-only
- Keep Druid as connection pool
- Preserve all existing MySQL functionality unchanged

**Out of Scope**: Oracle, SQL Server, other databases; ShardingSphere multi-DB; connection pool replacement.

## Constraint Sets

### Hard Constraints

- [HC-1] `MyBatisPlusConfig.java:71` — `PaginationInnerInterceptor(DbType.MYSQL)` hardcoded. Must detect DbType from active DataSource. — Source: Manual Analysis
- [HC-2] `SeataDataSourceConfig.java:97` — `dataSource.setDbType("mysql")` hardcoded. Must detect from JDBC URL or configuration property. — Source: Manual Analysis
- [HC-3] `TenantDataSourceRouter.java:159` — `USE \`schemaName\`` is MySQL-specific. PostgreSQL equivalent: `SET search_path TO schemaName`. Must branch by dialect. — Source: Manual Analysis
- [HC-4] `SqlPerformanceAnalyzer.java:69` — `EXPLAIN <sql>` output format differs between MySQL and PostgreSQL. MySQL returns `type/rows/Extra` columns; PostgreSQL returns plan text. Must implement dialect-specific parsing. — Source: Manual Analysis
- [HC-5] `pom.xml` — Only `mysql-connector-j` and `flyway-mysql` present. Must add `postgresql` driver (optional) and `flyway-database-postgresql` (optional). Use Maven profiles or `<optional>true</optional>`. — Source: Manual Analysis
- [HC-6] Migration SQL files (6 files) — All use MySQL DDL syntax (`ENGINE=InnoDB`, `AUTO_INCREMENT`, `DEFAULT CHARSET=utf8mb4`). PostgreSQL equivalents: `SERIAL`/`BIGSERIAL`, no ENGINE clause, `DEFAULT` charset not applicable. — Source: Manual Analysis
- [HC-7] `application-datasource.yml` — Hardcoded `com.mysql.cj.jdbc.Driver` and `jdbc:mysql://` URLs. Must be configurable per environment. — Source: Manual Analysis
- [HC-8] `DataSourceManager.java` — Creates `HikariDataSource` without explicit `driverClassName`. HikariCP auto-detects from URL for both MySQL/PostgreSQL, but Druid requires explicit `driverClassName`. Must handle both pool types. — Source: Manual Analysis
- [HC-9] ShardingSphere 5.4.1 — Must remain MySQL-only. Any dialect abstraction must not break ShardingSphere configuration. Conditional activation (`@ConditionalOnProperty`) already in place. — Source: User Confirmation
- [HC-10] Flyway migration directory structure — Must support `db/migration/mysql/` and `db/migration/postgresql/` with automatic vendor-based location selection. — Source: User Confirmation

### Soft Constraints

- [SC-1] Database vendor detection should prefer JDBC URL pattern matching (`jdbc:mysql://` vs `jdbc:postgresql://`) over explicit configuration, with configuration override as fallback. — Source: Best Practice
- [SC-2] MyBatis Plus `DbType` enum already supports `POSTGRE_SQL`. Mapping from detected vendor to `DbType` is straightforward. — Source: Framework Analysis
- [SC-3] Druid `dbType` property accepts `"postgresql"` natively. No custom integration needed. — Source: Framework Analysis
- [SC-4] Existing `DatabaseEnhancedProperties` has no `vendor`/`dialect` property. A new property `database.enhanced.vendor` (auto/mysql/postgresql) should be added. — Source: Analysis
- [SC-5] All 6 migration SQL files need PostgreSQL equivalents. DDL translation: `AUTO_INCREMENT` → `SERIAL`, `ENGINE=InnoDB ...` → removed, `BIGINT NOT NULL AUTO_INCREMENT` → `BIGSERIAL`, backtick quoting → double-quote or unquoted. — Source: Analysis
- [SC-6] `DatabaseMetricsExporter.java` casts to `DruidDataSource` — this is Druid-specific (not MySQL-specific), acceptable since Druid is retained. — Source: Analysis

### Dependencies

- [DEP-1] `MyBatisPlusConfig` → `DatabaseEnhancedProperties` (new vendor property): Must add vendor property before updating PaginationInnerInterceptor.
- [DEP-2] `FlywayConfiguration` → vendor detection: Must implement vendor detection before routing migration directories.
- [DEP-3] `TenantDataSourceRouter` → dialect abstraction: Schema-switching logic depends on dialect.
- [DEP-4] `SqlPerformanceAnalyzer` → dialect abstraction: EXPLAIN parsing depends on dialect.
- [DEP-5] Migration SQL files → Flyway directory routing: PostgreSQL SQL files must exist before Flyway can route to them.

### Risks

- [RISK-1] **Flyway version collision** — `flyway-mysql` and `flyway-database-postgresql` may have classpath conflicts if both are present simultaneously. Mitigation: Use Maven `<optional>true</optional>` or profiles; only one should be active at runtime. — Severity: Medium
- [RISK-2] **Seata AT mode PostgreSQL compatibility** — Seata AT mode generates `undo_log` DDL that may be MySQL-specific. Need to verify Seata 1.7.1 PostgreSQL support. — Severity: Medium
- [RISK-3] **Multi-tenant schema switching** — `USE \`schema\`` (MySQL) vs `SET search_path TO schema` (PostgreSQL) have different transaction semantics. PostgreSQL's `search_path` is session-scoped; MySQL's `USE` is connection-scoped. — Severity: Low
- [RISK-4] **ShardingSphere isolation** — ShardingSphere must not be activated when PostgreSQL is the primary database. Existing `@ConditionalOnProperty` should suffice, but must verify no transitive activation. — Severity: Low

## MySQL Hardcoding Points (Complete Inventory)

| # | File | Line | Hardcoded Content | Fix Strategy |
|---|------|------|-------------------|--------------|
| 1 | `config/MyBatisPlusConfig.java` | 71 | `DbType.MYSQL` | Detect from vendor property → `DbType.POSTGRE_SQL` |
| 2 | `config/SeataDataSourceConfig.java` | 97 | `setDbType("mysql")` | Detect from JDBC URL or vendor property |
| 3 | `tenant/router/TenantDataSourceRouter.java` | 159 | `USE \`schemaName\`` | Dialect-based: MySQL=`USE`, PG=`SET search_path TO` |
| 4 | `statistics/analyzer/SqlPerformanceAnalyzer.java` | 69 | `EXPLAIN` result parsing | Dialect-specific EXPLAIN parser |
| 5 | `pom.xml` | - | mysql-connector-j, flyway-mysql | Add optional postgresql deps |
| 6 | `application-datasource.yml` | 13,29,43 | `com.mysql.cj.jdbc.Driver`, `jdbc:mysql://` | Configuration-per-environment |
| 7 | `db/migration/*.sql` (6 files) | all | `ENGINE=InnoDB`, `AUTO_INCREMENT` | Create parallel `db/migration/postgresql/` |

## Success Criteria

- [OK-1] Application starts successfully with `jdbc:postgresql://` URL and PostgreSQL 14+ database
- [OK-2] Flyway executes PostgreSQL-specific migration scripts from `db/migration/postgresql/`
- [OK-3] MyBatis Plus pagination works correctly with PostgreSQL (`LIMIT/OFFSET` instead of MySQL's `LIMIT`)
- [OK-4] Multi-tenant schema switching works with PostgreSQL (`SET search_path TO`)
- [OK-5] SQL performance analysis (`EXPLAIN`) works with PostgreSQL output format
- [OK-6] All existing MySQL functionality remains unchanged (zero regression)
- [OK-7] Existing unit tests pass without modification
- [OK-8] ShardingSphere remains MySQL-only and does not activate for PostgreSQL configurations

## Open Questions (Resolved)

- Q1: Target databases? → A: MySQL + PostgreSQL → Constraint: [HC-5], [HC-6]
- Q2: ShardingSphere scope? → A: MySQL-only → Constraint: [HC-9]
- Q3: Flyway strategy? → A: Separate directories per database type → Constraint: [HC-10]
- Q4: Connection pool? → A: Keep Druid → Constraint: [SC-6]

## Recommended Implementation Sequence

1. **Foundation**: Add vendor detection property + utility class (`DatabaseVendor` enum, URL-based auto-detection)
2. **Dependencies**: Add optional PostgreSQL driver + flyway-database-postgresql to pom.xml
3. **Core Config**: Fix `MyBatisPlusConfig` (DbType), `SeataDataSourceConfig` (dbType), `FlywayConfiguration` (migration locations)
4. **Dialect Abstraction**: Create `DatabaseDialect` interface + MySQL/PostgreSQL implementations for schema switching + EXPLAIN parsing
5. **Tenant Router**: Refactor `TenantDataSourceRouter.switchSchema()` to use dialect
6. **SQL Analyzer**: Refactor `SqlPerformanceAnalyzer.analyzeExecutionPlan()` to use dialect
7. **Migration Scripts**: Create `db/migration/postgresql/` with 6 equivalent SQL files
8. **Testing**: Add PostgreSQL-specific test configurations
