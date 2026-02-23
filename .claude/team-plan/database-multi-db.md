# Team Plan: database-multi-db

## 概述
重构 `basebackend-database` 模块，新增 PostgreSQL 支持，通过方言抽象层实现 MySQL/PostgreSQL 双数据库兼容。

## 技术方案

### 架构设计
1. **DatabaseVendor 枚举**: `MYSQL`, `POSTGRESQL`，从 JDBC URL 模式自动检测（`jdbc:mysql://` → MYSQL, `jdbc:postgresql://` → POSTGRESQL），支持 `database.enhanced.vendor` 配置覆盖
2. **DatabaseDialect 接口**: 抽象 vendor-specific SQL 行为
   - `switchSchema(Connection, String)` — MySQL: `USE \`schema\``, PG: `SET search_path TO schema`
   - `parseExplainResult(List<Map>)` — MySQL: 读 type/rows/Extra 列, PG: 解析 QUERY PLAN 文本
   - `getDbType()` — 返回 MyBatis Plus `DbType`
   - `getDruidDbType()` — 返回 Druid dbType 字符串
3. **Flyway 路由**: 按 vendor 设置 `locations` 为 `classpath:db/migration/mysql` 或 `classpath:db/migration/postgresql`
4. **依赖管理**: PostgreSQL driver + flyway-database-postgresql 设为 `<optional>true</optional>`

### 新增文件清单
- `config/DatabaseVendor.java` — 枚举
- `config/DatabaseVendorDetector.java` — JDBC URL 检测 + 配置覆盖
- `dialect/DatabaseDialect.java` — 接口
- `dialect/MySqlDialect.java` — MySQL 实现
- `dialect/PostgreSqlDialect.java` — PostgreSQL 实现
- `dialect/DatabaseDialectFactory.java` — 工厂，按 vendor 创建 dialect
- `db/migration/mysql/` — 现有 6 个 SQL 文件移入
- `db/migration/postgresql/` — 6 个等效 PostgreSQL SQL 文件

### 关键技术决策
- PostgreSQL 依赖 `<optional>true</optional>`：运行时仅一种 DB 激活，避免 Flyway classpath 冲突
- `DatabaseVendorDetector` 作为 `@Component`，注入 `DataSource`，在 `@PostConstruct` 中从 JDBC URL 检测 vendor
- ShardingSphere 现有 `@ConditionalOnProperty` 已隔离，无需修改
- `application-datasource.yml` 为示例配置，不修改（各部署环境自行覆盖）

## 子任务列表

### Task 1: 添加 PostgreSQL Maven 依赖
- **类型**: 后端
- **文件范围**: `basebackend-database/pom.xml`
- **依赖**: 无
- **实施步骤**:
  1. 在 `mysql-connector-j` 之后添加 `org.postgresql:postgresql`，设 `<optional>true</optional>`
  2. 在 `flyway-mysql` 之后添加 `org.flywaydb:flyway-database-postgresql`，设 `<optional>true</optional>`
  3. 在 test scope 添加 `org.testcontainers:postgresql`
- **验收标准**: `mvn dependency:resolve` 成功，无版本冲突

### Task 2: 创建 DatabaseVendor 枚举与检测器
- **类型**: 后端
- **文件范围**:
  - `basebackend-database/src/main/java/com/basebackend/database/config/DatabaseVendor.java` (新建)
  - `basebackend-database/src/main/java/com/basebackend/database/config/DatabaseVendorDetector.java` (新建)
  - `basebackend-database/src/main/java/com/basebackend/database/config/DatabaseEnhancedProperties.java` (修改：添加 vendor 属性)
  - `basebackend-database/src/main/java/com/basebackend/database/config/DatabaseEnhancedAutoConfiguration.java` (修改：注册 detector bean)
- **依赖**: 无
- **实施步骤**:
  1. 创建 `DatabaseVendor` 枚举，含 `MYSQL`, `POSTGRESQL`，提供 `fromJdbcUrl(String url)` 静态方法
  2. 在 `DatabaseEnhancedProperties` 添加 `private String vendor = "auto"` 字段
  3. 创建 `DatabaseVendorDetector`，注入 `DataSource` + `DatabaseEnhancedProperties`，提供 `detect()` 方法：优先读 `vendor` 属性，若为 `auto` 则从 DataSource 获取 JDBC URL 自动判断
  4. 在 `DatabaseEnhancedAutoConfiguration` 中注册 `DatabaseVendorDetector` bean
- **验收标准**: `DatabaseVendor.fromJdbcUrl("jdbc:mysql://...")` 返回 `MYSQL`，`DatabaseVendor.fromJdbcUrl("jdbc:postgresql://...")` 返回 `POSTGRESQL`

### Task 3: 创建 DatabaseDialect 方言抽象层
- **类型**: 后端
- **文件范围**:
  - `basebackend-database/src/main/java/com/basebackend/database/dialect/DatabaseDialect.java` (新建)
  - `basebackend-database/src/main/java/com/basebackend/database/dialect/MySqlDialect.java` (新建)
  - `basebackend-database/src/main/java/com/basebackend/database/dialect/PostgreSqlDialect.java` (新建)
  - `basebackend-database/src/main/java/com/basebackend/database/dialect/DatabaseDialectFactory.java` (新建)
- **依赖**: Task 2 (需要 DatabaseVendor 枚举)
- **实施步骤**:
  1. 创建 `DatabaseDialect` 接口：
     - `void switchSchema(Connection conn, String schemaName) throws SQLException`
     - `List<SqlOptimizationSuggestion> parseExplainResult(String sqlMd5, List<Map<String, Object>> explainRows)`
     - `DbType getMyBatisPlusDbType()`
     - `String getDruidDbType()`
  2. 实现 `MySqlDialect`：`USE \`schema\``、MySQL EXPLAIN 列解析 (type/rows/Extra)、`DbType.MYSQL`、`"mysql"`
  3. 实现 `PostgreSqlDialect`：`SET search_path TO schema`、PG EXPLAIN 文本解析 (QUERY PLAN)、`DbType.POSTGRE_SQL`、`"postgresql"`
  4. 创建 `DatabaseDialectFactory`，作为 `@Component`，注入 `DatabaseVendorDetector`，提供 `getDialect()` 方法返回对应实现
- **验收标准**: 工厂根据 vendor 返回正确的 dialect 实现

### Task 4: 修复 MyBatisPlusConfig — 动态 DbType
- **类型**: 后端
- **文件范围**: `basebackend-database/src/main/java/com/basebackend/database/config/MyBatisPlusConfig.java`
- **依赖**: Task 3 (需要 DatabaseDialectFactory)
- **实施步骤**:
  1. 注入 `DatabaseDialectFactory` 到 `mybatisPlusInterceptor` 方法参数
  2. 将 `new PaginationInnerInterceptor(DbType.MYSQL)` 替换为 `new PaginationInnerInterceptor(dialectFactory.getDialect().getMyBatisPlusDbType())`
- **验收标准**: PostgreSQL 配置下 PaginationInnerInterceptor 使用 `DbType.POSTGRE_SQL`

### Task 5: 修复 SeataDataSourceConfig — 动态 dbType
- **类型**: 后端
- **文件范围**: `basebackend-database/src/main/java/com/basebackend/database/config/SeataDataSourceConfig.java`
- **依赖**: Task 3 (需要 DatabaseDialectFactory)
- **实施步骤**:
  1. 注入 `DatabaseDialectFactory` (通过构造器或方法参数)
  2. 将 `dataSource.setDbType("mysql")` 替换为 `dataSource.setDbType(dialectFactory.getDialect().getDruidDbType())`
- **验收标准**: PostgreSQL 配置下 Druid dbType 为 `"postgresql"`

### Task 6: 修复 FlywayConfiguration — vendor 路由迁移目录
- **类型**: 后端
- **文件范围**: `basebackend-database/src/main/java/com/basebackend/database/migration/config/FlywayConfiguration.java`
- **依赖**: Task 2 (需要 DatabaseVendorDetector)
- **实施步骤**:
  1. 注入 `DatabaseVendorDetector`
  2. 添加 `@Bean` 方法 `FlywayConfigurationCustomizer`，设置 `locations` 为 `classpath:db/migration/{vendor}` (vendor 小写)
  3. 保持现有 `flywayMigrationStrategy` 不变
- **验收标准**: MySQL 配置读 `db/migration/mysql/`，PostgreSQL 配置读 `db/migration/postgresql/`

### Task 7: 修复 TenantDataSourceRouter — dialect 化 schema 切换
- **类型**: 后端
- **文件范围**: `basebackend-database/src/main/java/com/basebackend/database/tenant/router/TenantDataSourceRouter.java`
- **依赖**: Task 3 (需要 DatabaseDialectFactory + DatabaseDialect)
- **实施步骤**:
  1. 注入 `DatabaseDialectFactory` 到构造器
  2. 将 `switchSchema` 方法中的 `connection.createStatement().execute("USE \`" + schemaName + "\`")` 替换为 `dialectFactory.getDialect().switchSchema(connection, schemaName)`
- **验收标准**: PostgreSQL 下执行 `SET search_path TO schema`

### Task 8: 修复 SqlPerformanceAnalyzer — dialect 化 EXPLAIN 解析
- **类型**: 后端
- **文件范围**: `basebackend-database/src/main/java/com/basebackend/database/statistics/analyzer/SqlPerformanceAnalyzer.java`
- **依赖**: Task 3 (需要 DatabaseDialectFactory + DatabaseDialect)
- **实施步骤**:
  1. 注入 `DatabaseDialectFactory` 到构造器
  2. 将 `analyzeExecutionPlan` 方法中的 MySQL 列解析逻辑委托给 `dialectFactory.getDialect().parseExplainResult(sqlMd5, explainResult)`
  3. 保留 `formatExecutionPlan` 但根据 dialect 选择格式化策略
- **验收标准**: PostgreSQL EXPLAIN 输出能被正确解析

### Task 9: 创建 PostgreSQL 迁移脚本 + 移动 MySQL 脚本
- **类型**: 后端
- **文件范围**:
  - `basebackend-database/src/main/resources/db/migration/mysql/` (新目录，移入现有 6 个 SQL 文件)
  - `basebackend-database/src/main/resources/db/migration/postgresql/` (新目录，6 个等效 SQL 文件)
- **依赖**: 无
- **实施步骤**:
  1. 创建 `db/migration/mysql/` 目录，将现有 6 个 SQL 文件移入（保持文件名不变）
  2. 创建 `db/migration/postgresql/` 目录
  3. 为每个 SQL 文件创建 PostgreSQL 等效版本：
     - `AUTO_INCREMENT` → `GENERATED ALWAYS AS IDENTITY` 或 `SERIAL`/`BIGSERIAL`
     - `BIGINT NOT NULL AUTO_INCREMENT` → `BIGSERIAL`
     - `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` → 删除
     - `DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` → `TIMESTAMP DEFAULT CURRENT_TIMESTAMP`（PG 无 ON UPDATE 语法，需用 trigger 或去掉）
     - `` `反引号` `` → 双引号或无引号
     - `TINYINT` → `SMALLINT`
     - `TEXT COMMENT '...'` → `TEXT`（PG 使用 `COMMENT ON COLUMN` 语法单独添加）
     - `INDEX idx_name (col)` → `CREATE INDEX idx_name ON table (col)` 单独语句
- **验收标准**: PostgreSQL 14+ 上能成功执行所有迁移脚本

## 文件冲突检查
✅ 无冲突 — 所有 Task 的文件范围完全隔离

## 并行分组
- **Layer 1** (并行): Task 1, Task 2, Task 9
- **Layer 2** (依赖 Task 2): Task 3, Task 6
- **Layer 3** (依赖 Task 3，并行): Task 4, Task 5, Task 7, Task 8

## 风险评估
| 风险 | 严重度 | 缓解措施 |
|------|--------|----------|
| Flyway classpath 冲突 (flyway-mysql + flyway-database-postgresql 同时存在) | 中 | 两个依赖均设 `<optional>true</optional>`，运行时仅激活一个 |
| Seata AT 模式 PostgreSQL 兼容性 (undo_log DDL) | 中 | Seata 1.7.1 官方支持 PG；undo_log 需在 PG 迁移脚本中包含 |
| PG `SET search_path` 是 session 级别 vs MySQL `USE` 是 connection 级别 | 低 | 连接归还前重置 search_path 即可 |
| ShardingSphere 意外激活 | 低 | 现有 `@ConditionalOnProperty` 已隔离 |
