# Phase 13.1: 数据治理平台实施指南

## 📋 概述

本指南介绍如何构建企业级数据治理平台，实现元数据管理、数据质量管理、数据血缘追踪和敏感数据保护，构建可信赖的数据资产管理体系。

---

## 🏗️ 数据治理平台架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      数据治理平台架构                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   元数据管理   │  │   数据质量    │  │   数据血缘    │           │
│  │              │  │              │  │              │           │
│  │ • 数据资产     │  │ • 质量规则     │  │ • 血缘分析     │           │
│  │ • 业务术语     │  │ • 质量检测     │  │ • 影响分析     │           │
│  │ • 技术元数据   │  │ • 质量报告     │  │ • 变更传播     │           │
│  │ • 数据标签     │  │ • 异常告警     │  │ • 血缘可视化   │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   数据安全     │  │   数据标准   │  │   数据服务   │           │
│  │              │  │              │  │              │           │
│  │ • 数据分级     │  │ • 标准规范     │  │ • 数据API    │           │
│  │ • 敏感数据     │  │ • 元数据标准   │  │ • 数据目录     │           │
│  │ • 访问控制     │  │ • 数据模型     │  │ • 数据检索     │           │
│  │ • 审计日志     │  │ • 命名规范     │  │ • 自助分析     │           │
│  └──────────────┘  └─────────────┘  └─────────────┘           │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    数据存储层                                 │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • PostgreSQL (元数据存储)                                     │ │
│  │ • Elasticsearch (搜索引擎)                                    │ │
│  │ • Neo4j (血缘图数据库)                                        │ │
│  │ • MinIO (文档存储)                                            │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    采集与处理层                                │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • DataX (数据同步)                                            │ │
│  │ • Flink CDC (变更捕获)                                        │ │
│  │ •自定义扫描器 (元数据抽取)                                     │ │
│  │ • 定时任务调度 (XXL-Job)                                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 核心技术栈

| 组件 | 用途 | 版本 |
|------|------|------|
| **Apache Atlas** | 元数据管理 | 2.3.0 |
| **DataHub** | 元数据平台 | 0.10.0 |
| **Apache Griffin** | 数据质量 | 0.7.0 |
| **Elasticsearch** | 搜索引擎 | 8.11.0 |
| **Neo4j** | 图数据库 | 5.15.0 |
| **Apache Flink** | 实时处理 | 1.18.0 |
| **DataX** | 数据同步 | 3.0 |
| **XXL-Job** | 任务调度 | 2.4.0 |

---

## 📦 元数据管理

### 1. 元数据模型设计

```java
/**
 * 元数据实体定义
 */

/**
 * 数据表元数据
 */
@Entity
@Table(name = "dg_table_metadata")
@Data
@EqualsAndHashCode(callSuper = true)
public class TableMetadata extends BaseEntity {

    /**
     * 表名称
     */
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    /**
     * 表注释
     */
    @Column(name = "table_comment", length = 500)
    private String tableComment;

    /**
     * 数据库名称
     */
    @Column(name = "database_name", nullable = false, length = 100)
    private String databaseName;

    /**
     * 数据源ID
     */
    @Column(name = "datasource_id")
    private Long datasourceId;

    /**
     * 表类型 (TABLE/VIEW)
     */
    @Column(name = "table_type", length = 20)
    private String tableType;

    /**
     * 行数
     */
    @Column(name = "row_count")
    private Long rowCount;

    /**
     * 表大小 (字节)
     */
    @Column(name = "table_size")
    private Long tableSize;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 最后更新时间
     */
    @Column(name = "last_update_time")
    private Date lastUpdateTime;

    /**
     * 数据所有者
     */
    @Column(name = "owner", length = 100)
    private String owner;

    /**
     * 业务Owner
     */
    @Column(name = "business_owner", length = 100)
    private String businessOwner;

    /**
     * 技术Owner
     */
    @Column(name = "technical_owner", length = 100)
    private String technicalOwner;

    /**
     * 数据分级 (PUBLIC/INTERNAL/CONFIDENTIAL/SECRET)
     */
    @Column(name = "data_classification", length = 20)
    private String dataClassification;

    /**
     * 状态 (ACTIVE/INACTIVE/DEPRECATED)
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 表字段列表
     */
    @OneToMany(mappedBy = "tableMetadata", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ColumnMetadata> columns;

    /**
     * 标签
     */
    @ManyToMany
    @JoinTable(name = "dg_table_tags",
        joinColumns = @JoinColumn(name = "table_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags;
}

/**
 * 字段元数据
 */
@Entity
@Table(name = "dg_column_metadata")
@Data
@EqualsAndHashCode(callSuper = true)
public class ColumnMetadata extends BaseEntity {

    /**
     * 所属表
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private TableMetadata tableMetadata;

    /**
     * 字段名称
     */
    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    /**
     * 字段注释
     */
    @Column(name = "column_comment", length = 500)
    private String columnComment;

    /**
     * 数据类型
     */
    @Column(name = "data_type", nullable = false, length = 100)
    private String dataType;

    /**
     * 字段长度
     */
    @Column(name = "column_size")
    private Integer columnSize;

    /**
     * 小数位
     */
    @Column(name = "decimal_digits")
    private Integer decimalDigits;

    /**
     * 是否可空
     */
    @Column(name = "nullable")
    private Boolean nullable;

    /**
     * 默认值
     */
    @Column(name = "default_value", length = 500)
    private String defaultValue;

    /**
     * 是否主键
     */
    @Column(name = "is_primary_key")
    private Boolean isPrimaryKey;

    /**
     * 是否外键
     */
    @Column(name = "is_foreign_key")
    private Boolean isForeignKey;

    /**
     * 字段序号
     */
    @Column(name = "ordinal_position")
    private Integer ordinalPosition;

    /**
     * 统计信息
     */
    @Embedded
    private ColumnStatistics statistics;
}

/**
 * 数据源元数据
 */
@Entity
@Table(name = "dg_datasource")
@Data
@EqualsAndHashCode(callSuper = true)
public class DataSource extends BaseEntity {

    /**
     * 数据源名称
     */
    @Column(name = "datasource_name", nullable = false, length = 100)
    private String datasourceName;

    /**
     * 数据源类型 (MySQL/PostgreSQL/Oracle/SQLServer/MongoDB/Elasticsearch等)
     */
    @Column(name = "datasource_type", length = 50)
    private String datasourceType;

    /**
     * 连接地址
     */
    @Column(name = "connection_url", length = 500)
    private String connectionUrl;

    /**
     * 用户名
     */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * 密码(加密)
     */
    @Column(name = "password", length = 500)
    private String password;

    /**
     * 连接参数(JSON)
     */
    @Column(name = "connection_params", columnDefinition = "TEXT")
    private String connectionParams;

    /**
     * 描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 状态
     */
    @Column(name = "status", length = 20)
    private String status;
}

/**
 * 标签
 */
@Entity
@Table(name = "dg_tag")
@Data
@EqualsAndHashCode(callSuper = true)
public class Tag extends BaseEntity {

    /**
     * 标签名称
     */
    @Column(name = "tag_name", nullable = false, length = 50)
    private String tagName;

    /**
     * 标签描述
     */
    @Column(name = "tag_description", length = 500)
    private String tagDescription;

    /**
     * 标签颜色
     */
    @Column(name = "color", length = 20)
    private String color;

    /**
     * 标签分类
     */
    @Column(name = "category", length = 50)
    private String category;
}

/**
 * 业务术语
 */
@Entity
@Table(name = "dg_business_term")
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessTerm extends BaseEntity {

    /**
     * 术语名称
     */
    @Column(name = "term_name", nullable = false, length = 100)
    private String termName;

    /**
     * 术语定义
     */
    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    /**
     * 业务口径
     */
    @Column(name = "business_rules", columnDefinition = "TEXT")
    private String businessRules;

    /**
     * 相关数据项
     */
    @ManyToMany
    @JoinTable(name = "dg_term_columns",
        joinColumns = @JoinColumn(name = "term_id"),
        inverseJoinColumns = @JoinColumn(name = "column_id"))
    private Set<ColumnMetadata> relatedColumns;

    /**
     * 状态
     */
    @Column(name = "status", length = 20)
    private String status;
}
```

### 2. 元数据扫描服务

```java
/**
 * 元数据扫描服务
 */
@Service
public class MetadataScannerService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataScannerService.class);

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private TableMetadataMapper tableMetadataMapper;

    @Autowired
    private ColumnMetadataMapper columnMetadataMapper;

    /**
     * 扫描数据源的所有表
     */
    @Transactional
    public void scanDataSource(Long datasourceId) {
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        if (dataSource == null) {
            throw new BusinessException("数据源不存在");
        }

        logger.info("开始扫描数据源: {}", dataSource.getDatasourceName());

        try (Connection conn = getConnection(dataSource)) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取所有表
            ResultSet tables = metaData.getTables(
                conn.getCatalog(),
                null,
                null,
                new String[]{"TABLE", "VIEW"}
            );

            while (tables.next()) {
                scanTable(conn, metaData, dataSource, tables);
            }

        } catch (Exception e) {
            logger.error("扫描数据源失败", e);
            throw new BusinessException("扫描数据源失败: " + e.getMessage());
        }

        logger.info("数据源扫描完成: {}", dataSource.getDatasourceName());
    }

    /**
     * 扫描单个表
     */
    private void scanTable(Connection conn, DatabaseMetaData metaData,
                          DataSource dataSource, ResultSet tables) throws SQLException {
        String tableName = tables.getString("TABLE_NAME");
        String tableComment = tables.getString("REMARKS");
        String tableType = tables.getString("TABLE_TYPE");

        // 检查表是否已存在
        TableMetadata tableMetadata = tableMetadataMapper.findByName(
            dataSource.getDatabaseName(), tableName);

        if (tableMetadata == null) {
            tableMetadata = new TableMetadata();
            tableMetadata.setDatabaseName(dataSource.getDatabaseName());
            tableMetadata.setTableName(tableName);
            tableMetadata.setTableComment(tableComment);
            tableMetadata.setTableType(tableType);
            tableMetadata.setDatasourceId(dataSource.getId());
            tableMetadata.setStatus("ACTIVE");
            tableMetadata.setCreateTime(new Date());
            tableMetadataMapper.insert(tableMetadata);
        }

        // 更新表统计信息
        updateTableStatistics(conn, tableMetadata);

        // 扫描字段
        scanColumns(metaData, tableMetadata);
    }

    /**
     * 扫描表字段
     */
    private void scanColumns(DatabaseMetaData metaData, TableMetadata tableMetadata)
            throws SQLException {

        // 删除旧字段
        columnMetadataMapper.deleteByTableId(tableMetadata.getId());

        // 获取字段信息
        ResultSet columns = metaData.getColumns(
            null,
            null,
            tableMetadata.getTableName(),
            null
        );

        while (columns.next()) {
            ColumnMetadata columnMetadata = new ColumnMetadata();
            columnMetadata.setTableMetadata(tableMetadata);
            columnMetadata.setColumnName(columns.getString("COLUMN_NAME"));
            columnMetadata.setColumnComment(columns.getString("REMARKS"));
            columnMetadata.setDataType(columns.getString("TYPE_NAME"));
            columnMetadata.setColumnSize(columns.getInt("COLUMN_SIZE"));
            columnMetadata.setDecimalDigits(columns.getInt("DECIMAL_DIGITS"));
            columnMetadata.setNullable("YES".equals(columns.getString("IS_NULLABLE")));
            columnMetadata.setDefaultValue(columns.getString("COLUMN_DEF"));
            columnMetadata.setOrdinalPosition(columns.getInt("ORDINAL_POSITION"));

            columnMetadataMapper.insert(columnMetadata);
        }
    }

    /**
     * 更新表统计信息
     */
    private void updateTableStatistics(Connection conn, TableMetadata tableMetadata)
            throws SQLException {

        String sql = "SELECT COUNT(*) FROM " + tableMetadata.getDatabaseName() + "." +
                    tableMetadata.getTableName();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                tableMetadata.setRowCount(rs.getLong(1));
            }
        }

        tableMetadata.setLastUpdateTime(new Date());
        tableMetadataMapper.updateById(tableMetadata);
    }

    /**
     * 获取数据库连接
     */
    private Connection getConnection(DataSource dataSource) throws Exception {
        Properties props = new Properties();
        props.put("driver", dataSource.getDatasourceType());

        if (StringUtils.hasText(dataSource.getUsername())) {
            props.put("user", dataSource.getUsername());
        }
        if (StringUtils.hasText(dataSource.getPassword())) {
            props.put("password", dataSource.getPassword());
        }

        if (StringUtils.hasText(dataSource.getConnectionParams())) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> params = mapper.readValue(
                dataSource.getConnectionParams(), Map.class);
            props.putAll(params);
        }

        return DriverManager.getConnection(dataSource.getConnectionUrl(), props);
    }
}

/**
 * 元数据采集任务
 */
@Component
public class MetadataScanJob {

    private static final Logger logger = LoggerFactory.getLogger(MetadataScanJob.class);

    @Autowired
    private MetadataScannerService scannerService;

    @Autowired
    private DataSourceMapper dataSourceMapper;

    /**
     * 扫描所有数据源
     */
    @XxlJob("scanAllDataSources")
    public void scanAllDataSources() {
        logger.info("开始执行全量元数据扫描任务");

        try {
            List<DataSource> dataSources = dataSourceMapper.selectList(
                new QueryWrapper<DataSource>().eq("status", "ACTIVE"));

            for (DataSource dataSource : dataSources) {
                try {
                    scannerService.scanDataSource(dataSource.getId());
                    logger.info("数据源扫描完成: {}", dataSource.getDatasourceName());
                } catch (Exception e) {
                    logger.error("数据源扫描失败: {}", dataSource.getDatasourceName(), e);
                }
            }

            logger.info("全量元数据扫描任务完成");
        } catch (Exception e) {
            logger.error("全量元数据扫描任务失败", e);
        }
    }

    /**
     * 增量扫描(基于时间戳)
     */
    @XxlJob("incrementalScan")
    public void incrementalScan() {
        logger.info("开始执行增量元数据扫描任务");

        // 实现基于时间戳的增量扫描逻辑
        // ...
    }
}
```

### 3. 元数据API服务

```java
/**
 * 元数据管理API
 */
@RestController
@RequestMapping("/api/metadata")
@Api(tags = "元数据管理")
public class MetadataController {

    @Autowired
    private MetadataScannerService scannerService;

    @Autowired
    private TableMetadataMapper tableMetadataMapper;

    @Autowired
    private ColumnMetadataMapper columnMetadataMapper;

    /**
     * 获取表列表
     */
    @GetMapping("/tables")
    @ApiOperation("获取表列表")
    public Result<Page<TableMetadata>> getTables(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String databaseName,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String owner) {

        Page<TableMetadata> pageInfo = new Page<>(page, size);

        QueryWrapper<TableMetadata> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(databaseName)) {
            wrapper.like("database_name", databaseName);
        }
        if (StringUtils.hasText(tableName)) {
            wrapper.like("table_name", tableName);
        }
        if (StringUtils.hasText(owner)) {
            wrapper.like("owner", owner);
        }

        wrapper.orderByDesc("last_update_time");

        pageInfo = tableMetadataMapper.selectPage(pageInfo, wrapper);

        return Result.success(pageInfo);
    }

    /**
     * 获取表详情
     */
    @GetMapping("/tables/{tableId}")
    @ApiOperation("获取表详情")
    public Result<TableMetadata> getTableDetail(@PathVariable Long tableId) {
        TableMetadata table = tableMetadataMapper.selectById(tableId);
        if (table == null) {
            return Result.error("表不存在");
        }

        // 加载字段信息
        List<ColumnMetadata> columns = columnMetadataMapper.selectList(
            new QueryWrapper<ColumnMetadata>().eq("table_id", tableId)
                .orderByAsc("ordinal_position"));
        table.setColumns(columns);

        return Result.success(table);
    }

    /**
     * 搜索表
     */
    @GetMapping("/tables/search")
    @ApiOperation("搜索表")
    public Result<List<TableMetadata>> searchTables(@RequestParam String keyword) {

        QueryWrapper<TableMetadata> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.like("table_name", keyword)
            .or().like("table_comment", keyword)
            .or().like("owner", keyword));

        List<TableMetadata> tables = tableMetadataMapper.selectList(wrapper);

        return Result.success(tables);
    }

    /**
     * 手动触发扫描
     */
    @PostMapping("/scan/{datasourceId}")
    @ApiOperation("手动触发扫描")
    public Result<Void> triggerScan(@PathVariable Long datasourceId) {
        scannerService.scanDataSource(datasourceId);
        return Result.success();
    }

    /**
     * 获取数据血缘关系
     */
    @GetMapping("/lineage/{tableId}")
    @ApiOperation("获取数据血缘关系")
    public Result<Map<String, Object>> getDataLineage(@PathVariable Long tableId) {
        // 查询上游表
        List<TableMetadata> upstreamTables = tableMetadataMapper.selectUpstreamTables(tableId);

        // 查询下游表
        List<TableMetadata> downstreamTables = tableMetadataMapper.selectDownstreamTables(tableId);

        Map<String, Object> result = new HashMap<>();
        result.put("upstream", upstreamTables);
        result.put("downstream", downstreamTables);

        return Result.success(result);
    }

    /**
     * 更新表标签
     */
    @PutMapping("/tables/{tableId}/tags")
    @ApiOperation("更新表标签")
    public Result<Void> updateTableTags(@PathVariable Long tableId,
                                       @RequestBody Set<Tag> tags) {
        TableMetadata table = tableMetadataMapper.selectById(tableId);
        if (table == null) {
            return Result.error("表不存在");
        }

        table.setTags(tags);
        tableMetadataMapper.updateById(table);

        return Result.success();
    }

    /**
     * 获取数据概览
     */
    @GetMapping("/overview")
    @ApiOperation("获取数据概览")
    public Result<Map<String, Object>> getDataOverview() {

        Map<String, Object> overview = new HashMap<>();

        // 总表数
        Integer totalTables = tableMetadataMapper.selectCount(new QueryWrapper<>());
        overview.put("totalTables", totalTables);

        // 数据库数
        Integer totalDatabases = tableMetadataMapper.selectObjs(
            new QueryWrapper<TableMetadata>().select("DISTINCT database_name"))
            .size();
        overview.put("totalDatabases", totalDatabases);

        // 今日更新
        Integer todayUpdated = tableMetadataMapper.selectCount(
            new QueryWrapper<TableMetadata>()
                .ge("last_update_time", DateUtil.beginOfDay(new Date()))
                .le("last_update_time", DateUtil.endOfDay(new Date())));
        overview.put("todayUpdated", todayUpdated);

        // 数据分类统计
        Map<String, Long> classificationStats = tableMetadataMapper.selectMaps(
            new QueryWrapper<TableMetadata>()
                .select("data_classification, COUNT(*) as count")
                .groupBy("data_classification"))
            .stream()
            .collect(Collectors.toMap(
                m -> (String) m.get("data_classification"),
                m -> (Long) m.get("count")
            ));
        overview.put("classificationStats", classificationStats);

        return Result.success(overview);
    }
}
```

---

## 🔍 数据质量管理

### 1. 质量规则引擎

```java
/**
 * 数据质量规则
 */
@Entity
@Table(name = "dg_quality_rule")
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityRule extends BaseEntity {

    /**
     * 规则名称
     */
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    /**
     * 规则描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 规则类型 (COMPLETENESS/ACCURACY/CONSISTENCY/TIMELINESS/UNIQUENESS/VALIDITY)
     */
    @Column(name = "rule_type", length = 50)
    private String ruleType;

    /**
     * 规则模板
     */
    @Column(name = "rule_template", columnDefinition = "TEXT")
    private String ruleTemplate;

    /**
     * 规则参数(JSON)
     */
    @Column(name = "rule_params", columnDefinition = "TEXT")
    private String ruleParams;

    /**
     * 严重程度 (FATAL/ERROR/WARNING/INFO)
     */
    @Column(name = "severity", length = 20)
    private String severity;

    /**
     * 目标表ID
     */
    @Column(name = "table_id")
    private Long tableId;

    /**
     * 目标字段
     */
    @Column(name = "target_column", length = 100)
    private String targetColumn;

    /**
     * 状态
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 执行频率
     */
    @Column(name = "execution_frequency", length = 50)
    private String executionFrequency;
}

/**
 * 数据质量检查结果
 */
@Entity
@Table(name = "dg_quality_check_result")
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityCheckResult extends BaseEntity {

    /**
     * 规则ID
     */
    @Column(name = "rule_id")
    private Long ruleId;

    /**
     * 执行时间
     */
    @Column(name = "execution_time")
    private Date executionTime;

    /**
     * 执行状态 (SUCCESS/FAILED/SKIPPED)
     */
    @Column(name = "execution_status", length = 20)
    private String executionStatus;

    /**
     * 检查总数
     */
    @Column(name = "total_count")
    private Long totalCount;

    /**
     * 通过数
     */
    @Column(name = "pass_count")
    private Long passCount;

    /**
     * 失败数
     */
    @Column(name = "fail_count")
    private Long failCount;

    /**
     * 通过率
     */
    @Column(name = "pass_rate")
    private Double passRate;

    /**
     * 错误详情
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    /**
     * 执行耗时(ms)
     */
    @Column(name = "execution_duration")
    private Long executionDuration;
}

/**
 * 数据质量规则引擎
 */
@Service
public class QualityRuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(QualityRuleEngine.class);

    @Autowired
    private QualityRuleMapper ruleMapper;

    @Autowired
    private QualityCheckResultMapper resultMapper;

    @Autowired
    private TableMetadataMapper tableMetadataMapper;

    /**
     * 执行质量检查
     */
    @Transactional
    public QualityCheckResult executeCheck(Long ruleId) {
        QualityRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException("规则不存在");
        }

        QualityCheckResult result = new QualityCheckResult();
        result.setRuleId(ruleId);
        result.setExecutionTime(new Date());

        long startTime = System.currentTimeMillis();

        try {
            switch (rule.getRuleType()) {
                case "COMPLETENESS":
                    result = checkCompleteness(rule, result);
                    break;
                case "ACCURACY":
                    result = checkAccuracy(rule, result);
                    break;
                case "CONSISTENCY":
                    result = checkConsistency(rule, result);
                    break;
                case "TIMELINESS":
                    result = checkTimeliness(rule, result);
                    break;
                case "UNIQUENESS":
                    result = checkUniqueness(rule, result);
                    break;
                case "VALIDITY":
                    result = checkValidity(rule, result);
                    break;
                default:
                    throw new BusinessException("不支持的规则类型: " + rule.getRuleType());
            }

            result.setExecutionStatus("SUCCESS");
            result.setExecutionDuration(System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("质量检查失败", e);
            result.setExecutionStatus("FAILED");
            result.setExecutionDuration(System.currentTimeMillis() - startTime);
            result.setErrorDetails(ExceptionUtils.getStackTrace(e));
        }

        resultMapper.insert(result);
        return result;
    }

    /**
     * 完整性检查
     */
    private QualityCheckResult checkCompleteness(QualityRule rule, QualityCheckResult result) {
        TableMetadata table = tableMetadataMapper.selectById(rule.getTableId());
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) as total_count, ");
        sql.append("COUNT(").append(rule.getTargetColumn()).append(") as non_null_count ");
        sql.append("FROM ").append(table.getDatabaseName()).append(".").append(table.getTableName());

        // 执行SQL查询
        Map<String, Object> queryResult = executeQuery(sql.toString());

        Long totalCount = ((Number) queryResult.get("total_count")).longValue();
        Long nonNullCount = ((Number) queryResult.get("non_null_count")).longValue();
        Long nullCount = totalCount - nonNullCount;

        result.setTotalCount(totalCount);
        result.setPassCount(nonNullCount);
        result.setFailCount(nullCount);
        result.setPassRate(totalCount > 0 ? (double) nonNullCount / totalCount : 1.0);

        return result;
    }

    /**
     * 准确性检查
     */
    private QualityCheckResult checkAccuracy(QualityRule rule, QualityCheckResult result) {
        TableMetadata table = tableMetadataMapper.selectById(rule.getTableId());

        // 根据规则模板生成检查SQL
        String checkSql = generateAccuracyCheckSQL(rule, table);

        Map<String, Object> queryResult = executeQuery(checkSql);

        Long totalCount = ((Number) queryResult.get("total_count")).longValue();
        Long validCount = ((Number) queryResult.get("valid_count")).longValue();
        Long invalidCount = totalCount - validCount;

        result.setTotalCount(totalCount);
        result.setPassCount(validCount);
        result.setFailCount(invalidCount);
        result.setPassRate(totalCount > 0 ? (double) validCount / totalCount : 1.0);

        return result;
    }

    /**
     * 一致性检查
     */
    private QualityCheckResult checkConsistency(QualityRule rule, QualityCheckResult result) {
        TableMetadata table = tableMetadataMapper.selectById(rule.getTableId());

        // 检查跨表一致性
        String checkSql = generateConsistencyCheckSQL(rule, table);

        Map<String, Object> queryResult = executeQuery(checkSql);

        Long totalCount = ((Number) queryResult.get("total_count")).longValue();
        Long consistentCount = ((Number) queryResult.get("consistent_count")).longValue();
        Long inconsistentCount = totalCount - consistentCount;

        result.setTotalCount(totalCount);
        result.setPassCount(consistentCount);
        result.setFailCount(inconsistentCount);
        result.setPassRate(totalCount > 0 ? (double) consistentCount / totalCount : 1.0);

        return result;
    }

    /**
     * 唯一性检查
     */
    private QualityCheckResult checkUniqueness(QualityRule rule, QualityCheckResult result) {
        TableMetadata table = tableMetadataMapper.selectById(rule.getTableId());

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) as total_count FROM ")
            .append(table.getDatabaseName()).append(".").append(table.getTableName());

        Map<String, Object> totalResult = executeQuery(sql.toString());
        Long totalCount = ((Number) totalResult.get("total_count")).longValue();

        StringBuilder duplicateSql = new StringBuilder();
        duplicateSql.append("SELECT COUNT(*) as duplicate_count FROM (")
            .append("SELECT ").append(rule.getTargetColumn())
            .append(" FROM ").append(table.getDatabaseName()).append(".").append(table.getTableName())
            .append(" GROUP BY ").append(rule.getTargetColumn())
            .append(" HAVING COUNT(*) > 1) t");

        Map<String, Object> duplicateResult = executeQuery(duplicateSql.toString());
        Long duplicateCount = ((Number) duplicateResult.get("duplicate_count")).longValue();

        Long uniqueCount = totalCount - duplicateCount;

        result.setTotalCount(totalCount);
        result.setPassCount(uniqueCount);
        result.setFailCount(duplicateCount);
        result.setPassRate(totalCount > 0 ? (double) uniqueCount / totalCount : 1.0);

        return result;
    }

    /**
     * 及时性检查
     */
    private QualityCheckResult checkTimeliness(QualityRule rule, QualityCheckResult result) {
        TableMetadata table = tableMetadataMapper.selectById(rule.getTableId());

        // 从规则参数中获取时间阈值
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> params = mapper.readValue(rule.getRuleParams(), Map.class);
        String timeColumn = (String) params.get("timeColumn");
        Long hoursThreshold = ((Number) params.get("hoursThreshold")).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) as total_count, ")
            .append("SUM(CASE WHEN TIMESTAMPDIFF(HOUR, ")
            .append(timeColumn).append(", NOW()) <= ").append(hoursThreshold)
            .append(" THEN 1 ELSE 0 END) as timely_count ")
            .append("FROM ").append(table.getDatabaseName()).append(".").append(table.getTableName());

        Map<String, Object> queryResult = executeQuery(sql.toString());

        Long totalCount = ((Number) queryResult.get("total_count")).longValue();
        Long timelyCount = ((Number) queryResult.get("timely_count")).longValue();
        Long untimelyCount = totalCount - timelyCount;

        result.setTotalCount(totalCount);
        result.setPassCount(timelyCount);
        result.setFailCount(untimelyCount);
        result.setPassRate(totalCount > 0 ? (double) timelyCount / totalCount : 1.0);

        return result;
    }

    /**
     * 有效性检查
     */
    private QualityCheckResult checkValidity(QualityRule rule, QualityCheckResult result) {
        TableMetadata table = tableMetadataMapper.selectById(rule.getTableId());

        // 根据规则参数生成验证SQL
        String checkSql = generateValidityCheckSQL(rule, table);

        Map<String, Object> queryResult = executeQuery(checkSql);

        Long totalCount = ((Number) queryResult.get("total_count")).longValue();
        Long validCount = ((Number) queryResult.get("valid_count")).longValue();
        Long invalidCount = totalCount - validCount;

        result.setTotalCount(totalCount);
        result.setPassCount(validCount);
        result.setFailCount(invalidCount);
        result.setPassRate(totalCount > 0 ? (double) validCount / totalCount : 1.0);

        return result;
    }

    /**
     * 执行SQL查询
     */
    private Map<String, Object> executeQuery(String sql) {
        // 这里需要根据实际数据源配置执行SQL
        // 返回查询结果
        // ...
        return new HashMap<>();
    }

    /**
     * 生成准确性检查SQL
     */
    private String generateAccuracyCheckSQL(QualityRule rule, TableMetadata table) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> params = mapper.readValue(rule.getRuleParams(), Map.class);

        String validationRule = (String) params.get("validationRule");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) as total_count, ")
            .append("SUM(CASE WHEN ").append(validationRule).append(" THEN 1 ELSE 0 END) as valid_count ")
            .append("FROM ").append(table.getDatabaseName()).append(".").append(table.getTableName());

        return sql.toString();
    }

    /**
     * 生成一致性检查SQL
     */
    private String generateConsistencyCheckSQL(QualityRule rule, TableMetadata table) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> params = mapper.readValue(rule.getRuleParams(), Map.class);

        String referenceTable = (String) params.get("referenceTable");
        String joinCondition = (String) params.get("joinCondition");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) as total_count, ")
            .append("COUNT(t1.id) as consistent_count ")
            .append("FROM ").append(table.getDatabaseName()).append(".").append(table.getTableName())
            .append(" t1 LEFT JOIN ").append(referenceTable).append(" t2 ON ").append(joinCondition);

        return sql.toString();
    }

    /**
     * 生成有效性检查SQL
     */
    private String generateValidityCheckSQL(QualityRule rule, TableMetadata table) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> params = mapper.readValue(rule.getRuleParams(), Map.class);

        String validationRule = (String) params.get("validationRule");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) as total_count, ")
            .append("SUM(CASE WHEN ").append(validationRule).append(" THEN 1 ELSE 0 END) as valid_count ")
            .append("FROM ").append(table.getDatabaseName()).append(".").append(table.getTableName());

        return sql.toString();
    }
}

/**
 * 数据质量监控Job
 */
@Component
public class QualityMonitoringJob {

    private static final Logger logger = LoggerFactory.getLogger(QualityMonitoringJob.class);

    @Autowired
    private QualityRuleEngine qualityEngine;

    @Autowired
    private QualityRuleMapper ruleMapper;

    /**
     * 执行所有活跃规则
     */
    @XxlJob("executeQualityChecks")
    public void executeQualityChecks() {
        logger.info("开始执行数据质量检查任务");

        try {
            List<QualityRule> rules = ruleMapper.selectList(
                new QueryWrapper<QualityRule>().eq("status", "ACTIVE"));

            for (QualityRule rule : rules) {
                try {
                    qualityEngine.executeCheck(rule.getId());
                    logger.info("规则执行完成: {}", rule.getRuleName());
                } catch (Exception e) {
                    logger.error("规则执行失败: {}", rule.getRuleName(), e);
                }
            }

            logger.info("数据质量检查任务完成");
        } catch (Exception e) {
            logger.error("数据质量检查任务失败", e);
        }
    }
}
```

### 2. 质量监控看板

```java
/**
 * 数据质量监控API
 */
@RestController
@RequestMapping("/api/quality")
@Api(tags = "数据质量")
public class QualityController {

    @Autowired
    private QualityRuleMapper ruleMapper;

    @Autowired
    private QualityCheckResultMapper resultMapper;

    /**
     * 获取质量概览
     */
    @GetMapping("/overview")
    @ApiOperation("获取质量概览")
    public Result<Map<String, Object>> getQualityOverview(
            @RequestParam(defaultValue = "7") Integer days) {

        Map<String, Object> overview = new HashMap<>();

        // 规则总数
        Integer totalRules = ruleMapper.selectCount(new QueryWrapper<>());
        overview.put("totalRules", totalRules);

        // 活跃规则数
        Integer activeRules = ruleMapper.selectCount(
            new QueryWrapper<QualityRule>().eq("status", "ACTIVE"));
        overview.put("activeRules", activeRules);

        // 质量分数
        Double qualityScore = calculateQualityScore(days);
        overview.put("qualityScore", qualityScore);

        // 趋势数据
        List<Map<String, Object>> trendData = getQualityTrend(days);
        overview.put("trendData", trendData);

        // 规则类型分布
        Map<String, Long> typeDistribution = ruleMapper.selectMaps(
            new QueryWrapper<QualityRule>()
                .select("rule_type, COUNT(*) as count")
                .groupBy("rule_type"))
            .stream()
            .collect(Collectors.toMap(
                m -> (String) m.get("rule_type"),
                m -> (Long) m.get("count")
            ));
        overview.put("typeDistribution", typeDistribution);

        return Result.success(overview);
    }

    /**
     * 获取质量趋势
     */
    @GetMapping("/trend")
    @ApiOperation("获取质量趋势")
    public Result<List<Map<String, Object>>> getQualityTrend(
            @RequestParam(defaultValue = "30") Integer days) {

        List<Map<String, Object>> trendData = new ArrayList<>();

        Date startDate = DateUtil.offsetDay(new Date(), -days);

        for (int i = 0; i < days; i++) {
            Date date = DateUtil.offsetDay(startDate, i);

            QueryWrapper<QualityCheckResult> wrapper = new QueryWrapper<>();
            wrapper.between("execution_time",
                DateUtil.beginOfDay(date),
                DateUtil.endOfDay(date));

            List<QualityCheckResult> results = resultMapper.selectList(wrapper);

            if (!results.isEmpty()) {
                double avgPassRate = results.stream()
                    .mapToDouble(QualityCheckResult::getPassRate)
                    .average()
                    .orElse(0.0);

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", DateUtil.formatDate(date));
                dayData.put("passRate", avgPassRate);
                dayData.put("checkCount", results.size());

                trendData.add(dayData);
            }
        }

        return Result.success(trendData);
    }

    /**
     * 获取规则执行历史
     */
    @GetMapping("/rules/{ruleId}/history")
    @ApiOperation("获取规则执行历史")
    public Result<Page<QualityCheckResult>> getRuleHistory(
            @PathVariable Long ruleId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        Page<QualityCheckResult> pageInfo = new Page<>(page, size);

        QueryWrapper<QualityCheckResult> wrapper = new QueryWrapper<>();
        wrapper.eq("rule_id", ruleId)
            .orderByDesc("execution_time");

        pageInfo = resultMapper.selectPage(pageInfo, wrapper);

        return Result.success(pageInfo);
    }

    /**
     * 计算质量分数
     */
    private Double calculateQualityScore(Integer days) {
        Date startDate = DateUtil.offsetDay(new Date(), -days);

        QueryWrapper<QualityCheckResult> wrapper = new QueryWrapper<>();
        wrapper.between("execution_time", startDate, new Date());

        List<QualityCheckResult> results = resultMapper.selectList(wrapper);

        if (results.isEmpty()) {
            return 100.0;
        }

        // 加权计算质量分数
        double totalScore = 0;
        double totalWeight = 0;

        for (QualityCheckResult result : results) {
            double score = result.getPassRate() * 100;
            double weight = result.getSeverity().equals("FATAL") ? 5 :
                          result.getSeverity().equals("ERROR") ? 3 :
                          result.getSeverity().equals("WARNING") ? 2 : 1;

            totalScore += score * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? totalScore / totalWeight : 100.0;
    }
}
```

---

## 🔗 数据血缘管理

### 1. 血缘关系模型

```java
/**
 * 数据血缘节点
 */
@Entity
@Table(name = "dg_lineage_node")
@Data
@EqualsAndHashCode(callSuper = true)
public class LineageNode extends BaseEntity {

    /**
     * 节点类型 (TABLE/COLUMN/TRANSFORMATION)
     */
    @Column(name = "node_type", length = 20)
    private String nodeType;

    /**
     * 节点名称
     */
    @Column(name = "node_name", length = 200)
    private String nodeName;

    /**
     * 节点描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 表ID(如果是表节点)
     */
    @Column(name = "table_id")
    private Long tableId;

    /**
     * 字段ID(如果是字段节点)
     */
    @Column(name = "column_id")
    private Long columnId;

    /**
     * 所属数据源
     */
    @Column(name = "datasource_id")
    private Long datasourceId;

    /**
     * 节点属性(JSON)
     */
    @Column(name = "node_properties", columnDefinition = "TEXT")
    private String nodeProperties;
}

/**
 * 数据血缘关系
 */
@Entity
@Table(name = "dg_lineage_edge")
@Data
@EqualsAndHashCode(callSuper = true)
public class LineageEdge extends BaseEntity {

    /**
     * 源节点ID
     */
    @Column(name = "source_node_id")
    private Long sourceNodeId;

    /**
     * 目标节点ID
     */
    @Column(name = "target_node_id")
    private Long targetNodeId;

    /**
     * 关系类型 (DERIVED_FROM/TRANSFORMS/AGGREGATES/LOOKS_UP)
     */
    @Column(name = "edge_type", length = 50)
    private String edgeType;

    /**
     * 转换逻辑
     */
    @Column(name = "transformation_logic", columnDefinition = "TEXT")
    private String transformationLogic;

    /**
     * 执行顺序
     */
    @Column(name = "execution_order")
    private Integer executionOrder;

    /**
     * 任务ID(ETL任务)
     */
    @Column(name = "job_id")
    private Long jobId;
}

/**
 * ETL作业
 */
@Entity
@Table(name = "dg_etl_job")
@Data
@EqualsAndHashCode(callSuper = true)
public class EtlJob extends BaseEntity {

    /**
     * 作业名称
     */
    @Column(name = "job_name", length = 100)
    private String jobName;

    /**
     * 作业类型 (BATCH/STREAMING)
     */
    @Column(name = "job_type", length = 20)
    private String jobType;

    /**
     * 作业描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 源系统
     */
    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    /**
     * 目标系统
     */
    @Column(name = "target_system", length = 100)
    private String targetSystem;

    /**
     * 作业配置(JSON)
     */
    @Column(name = "job_config", columnDefinition = "TEXT")
    private String jobConfig;

    /**
     * 调度表达式
     */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    /**
     * 状态
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 最后执行时间
     */
    @Column(name = "last_execution_time")
    private Date lastExecutionTime;
}

/**
 * 数据血缘服务
 */
@Service
public class LineageService {

    private static final Logger logger = LoggerFactory.getLogger(LineageService.class);

    @Autowired
    private LineageNodeMapper nodeMapper;

    @Autowired
    private LineageEdgeMapper edgeMapper;

    @Autowired
    private EtlJobMapper jobMapper;

    @Autowired
    private TableMetadataMapper tableMetadataMapper;

    /**
     * 构建数据血缘关系
     */
    @Transactional
    public void buildLineage(Long tableId, String etlJobName) {
        TableMetadata table = tableMetadataMapper.selectById(tableId);
        if (table == null) {
            throw new BusinessException("表不存在");
        }

        // 获取ETL作业
        EtlJob job = jobMapper.selectOne(
            new QueryWrapper<EtlJob>().eq("job_name", etlJobName));

        // 构建血缘节点
        LineageNode targetNode = createTableNode(table);

        // 从作业配置中解析源表
        List<TableMetadata> sourceTables = parseSourceTables(job);

        for (TableMetadata sourceTable : sourceTables) {
            LineageNode sourceNode = createTableNode(sourceTable);

            // 创建血缘边
            LineageEdge edge = new LineageEdge();
            edge.setSourceNodeId(sourceNode.getId());
            edge.setTargetNodeId(targetNode.getId());
            edge.setEdgeType("DERIVED_FROM");
            edge.setJobId(job.getId());
            edge.setExecutionOrder(1);

            edgeMapper.insert(edge);
        }
    }

    /**
     * 获取完整血缘链路
     */
    public Map<String, Object> getLineageGraph(Long tableId) {
        TableMetadata table = tableMetadataMapper.selectById(tableId);

        Map<String, Object> result = new HashMap<>();

        // 获取上游血缘
        List<Map<String, Object>> upstream = getUpstreamLineage(tableId);

        // 获取下游血缘
        List<Map<String, Object>> downstream = getDownstreamLineage(tableId);

        result.put("table", table);
        result.put("upstream", upstream);
        result.put("downstream", downstream);

        return result;
    }

    /**
     * 获取上游血缘
     */
    public List<Map<String, Object>> getUpstreamLineage(Long tableId) {
        List<Map<String, Object>> upstream = new ArrayList<>();

        // 递归获取上游血缘
        collectUpstream(tableId, 0, upstream);

        return upstream;
    }

    /**
     * 获取下游血缘
     */
    public List<Map<String, Object>> getDownstreamLineage(Long tableId) {
        List<Map<String, Object>> downstream = new ArrayList<>();

        // 递归获取下游血缘
        collectDownstream(tableId, 0, downstream);

        return downstream;
    }

    /**
     * 递归收集上游血缘
     */
    private void collectUpstream(Long tableId, int depth, List<Map<String, Object>> result) {
        if (depth > 5) { // 防止循环引用，限制深度
            return;
        }

        QueryWrapper<LineageEdge> wrapper = new QueryWrapper<>();
        wrapper.eq("target_node_id", getTableNodeId(tableId));

        List<LineageEdge> edges = edgeMapper.selectList(wrapper);

        for (LineageEdge edge : edges) {
            LineageNode sourceNode = nodeMapper.selectById(edge.getSourceNodeId());

            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("node", sourceNode);
            nodeInfo.put("depth", depth);
            nodeInfo.put("edgeType", edge.getEdgeType());
            nodeInfo.put("transformation", edge.getTransformationLogic());

            result.add(nodeInfo);

            // 递归收集上游
            if (sourceNode.getTableId() != null) {
                collectUpstream(sourceNode.getTableId(), depth + 1, result);
            }
        }
    }

    /**
     * 递归收集下游血缘
     */
    private void collectDownstream(Long tableId, int depth, List<Map<String, Object>> result) {
        if (depth > 5) { // 防止循环引用，限制深度
            return;
        }

        QueryWrapper<LineageEdge> wrapper = new QueryWrapper<>();
        wrapper.eq("source_node_id", getTableNodeId(tableId));

        List<LineageEdge> edges = edgeMapper.selectList(wrapper);

        for (LineageEdge edge : edges) {
            LineageNode targetNode = nodeMapper.selectById(edge.getTargetNodeId());

            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("node", targetNode);
            nodeInfo.put("depth", depth);
            nodeInfo.put("edgeType", edge.getEdgeType());
            nodeInfo.put("transformation", edge.getTransformationLogic());

            result.add(nodeInfo);

            // 递归收集下游
            if (targetNode.getTableId() != null) {
                collectDownstream(targetNode.getTableId(), depth + 1, result);
            }
        }
    }

    /**
     * 影响分析
     */
    public List<Map<String, Object>> impactAnalysis(Long tableId) {
        List<Map<String, Object>> impactedObjects = new ArrayList<>();

        // 获取下游影响
        List<Map<String, Object>> downstream = getDownstreamLineage(tableId);

        for (Map<String, Object> downstreamItem : downstream) {
            LineageNode node = (LineageNode) downstreamItem.get("node");

            Map<String, Object> impact = new HashMap<>();
            impact.put("type", "DOWNSTREAM");
            impact.put("object", node);
            impact.put("description", "表 " + node.getNodeName() + " 受当前表变更影响");

            impactedObjects.add(impact);
        }

        return impactedObjects;
    }

    /**
     * 创建表节点
     */
    private LineageNode createTableNode(TableMetadata table) {
        QueryWrapper<LineageNode> wrapper = new QueryWrapper<>();
        wrapper.eq("node_type", "TABLE")
            .eq("table_id", table.getId());

        LineageNode existingNode = nodeMapper.selectOne(wrapper);

        if (existingNode != null) {
            return existingNode;
        }

        LineageNode node = new LineageNode();
        node.setNodeType("TABLE");
        node.setNodeName(table.getDatabaseName() + "." + table.getTableName());
        node.setTableId(table.getId());
        node.setDatasourceId(table.getDatasourceId());

        nodeMapper.insert(node);
        return node;
    }

    /**
     * 获取表的节点ID
     */
    private Long getTableNodeId(Long tableId) {
        QueryWrapper<LineageNode> wrapper = new QueryWrapper<>();
        wrapper.eq("node_type", "TABLE")
            .eq("table_id", tableId);

        LineageNode node = nodeMapper.selectOne(wrapper);
        return node != null ? node.getId() : null;
    }

    /**
     * 解析源表
     */
    private List<TableMetadata> parseSourceTables(EtlJob job) {
        // 从作业配置中解析源表
        // 这里需要根据实际的作业配置格式进行解析
        // 示例格式：{"sourceTables": ["db1.table1", "db1.table2"]}

        List<TableMetadata> sourceTables = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = mapper.readValue(job.getJobConfig(), Map.class);

            List<String> sourceTableNames = (List<String>) config.get("sourceTables");

            for (String tableName : sourceTableNames) {
                String[] parts = tableName.split("\\.");
                if (parts.length == 2) {
                    TableMetadata table = tableMetadataMapper.findByName(parts[0], parts[1]);
                    if (table != null) {
                        sourceTables.add(table);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析源表失败", e);
        }

        return sourceTables;
    }
}

/**
 * 数据血缘API
 */
@RestController
@RequestMapping("/api/lineage")
@Api(tags = "数据血缘")
public class LineageController {

    @Autowired
    private LineageService lineageService;

    /**
     * 获取血缘关系图
     */
    @GetMapping("/graph/{tableId}")
    @ApiOperation("获取血缘关系图")
    public Result<Map<String, Object>> getLineageGraph(@PathVariable Long tableId) {
        Map<String, Object> graph = lineageService.getLineageGraph(tableId);
        return Result.success(graph);
    }

    /**
     * 获取上游血缘
     */
    @GetMapping("/upstream/{tableId}")
    @ApiOperation("获取上游血缘")
    public Result<List<Map<String, Object>>> getUpstream(@PathVariable Long tableId) {
        List<Map<String, Object>> upstream = lineageService.getUpstreamLineage(tableId);
        return Result.success(upstream);
    }

    /**
     * 获取下游血缘
     */
    @GetMapping("/downstream/{tableId}")
    @ApiOperation("获取下游血缘")
    public Result<List<Map<String, Object>>> getDownstream(@PathVariable Long tableId) {
        List<Map<String, Object>> downstream = lineageService.getDownstreamLineage(tableId);
        return Result.success(downstream);
    }

    /**
     * 影响分析
     */
    @GetMapping("/impact/{tableId}")
    @ApiOperation("影响分析")
    public Result<List<Map<String, Object>>> impactAnalysis(@PathVariable Long tableId) {
        List<Map<String, Object>> impacted = lineageService.impactAnalysis(tableId);
        return Result.success(impacted);
    }

    /**
     * 构建血缘关系
     */
    @PostMapping("/build")
    @ApiOperation("构建血缘关系")
    public Result<Void> buildLineage(@RequestParam Long tableId,
                                     @RequestParam String etlJobName) {
        lineageService.buildLineage(tableId, etlJobName);
        return Result.success();
    }
}
```

---

## 🔒 数据安全与合规

### 1. 敏感数据发现

```java
/**
 * 敏感数据规则
 */
@Entity
@Table(name = "dg_sensitive_rule")
@Data
@EqualsAndHashCode(callSuper = true)
public class SensitiveRule extends BaseEntity {

    /**
     * 规则名称
     */
    @Column(name = "rule_name", length = 100)
    private String ruleName;

    /**
     * 规则类型 (PATTERN_REGEX/DICTIONARY/ML_BASED)
     */
    @Column(name = "rule_type", length = 50)
    private String ruleType;

    /**
     * 数据分类 (PII/PCI/PHI/FINANCIAL/SECRET)
     */
    @Column(name = "data_category", length = 50)
    private String dataCategory;

    /**
     * 检测规则
     */
    @Column(name = "detection_rule", columnDefinition = "TEXT")
    private String detectionRule;

    /**
     * 严重程度
     */
    @Column(name = "severity", length = 20)
    private String severity;

    /**
     * 状态
     */
    @Column(name = "status", length = 20)
    private String status;
}

/**
 * 敏感数据扫描结果
 */
@Entity
@Table(name = "dg_sensitive_scan_result")
@Data
@EqualsAndHashCode(callSuper = true)
public class SensitiveScanResult extends BaseEntity {

    /**
     * 表ID
     */
    @Column(name = "table_id")
    private Long tableId;

    /**
     * 字段ID
     */
    @Column(name = "column_id")
    private Long columnId;

    /**
     * 敏感类型
     */
    @Column(name = "sensitive_type", length = 100)
    private String sensitiveType;

    /**
     * 置信度
     */
    @Column(name = "confidence")
    private Double confidence;

    /**
     * 敏感数据数量
     */
    @Column(name = "sensitive_count")
    private Long sensitiveCount;

    /**
     * 总数量
     */
    @Column(name = "total_count")
    private Long totalCount;

    /**
     * 敏感比例
     */
    @Column(name = "sensitive_rate")
    private Double sensitiveRate;

    /**
     * 扫描时间
     */
    @Column(name = "scan_time")
    private Date scanTime;
}

/**
 * 敏感数据发现服务
 */
@Service
public class SensitiveDataDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataDiscoveryService.class);

    @Autowired
    private SensitiveRuleMapper ruleMapper;

    @Autowired
    private SensitiveScanResultMapper resultMapper;

    @Autowired
    private TableMetadataMapper tableMetadataMapper;

    @Autowired
    private ColumnMetadataMapper columnMetadataMapper;

    /**
     * 扫描表中的敏感数据
     */
    @Transactional
    public void scanSensitiveData(Long tableId) {
        TableMetadata table = tableMetadataMapper.selectById(tableId);
        if (table == null) {
            throw new BusinessException("表不存在");
        }

        logger.info("开始扫描敏感数据: {}", table.getTableName());

        // 获取所有规则
        List<SensitiveRule> rules = ruleMapper.selectList(
            new QueryWrapper<SensitiveRule>().eq("status", "ACTIVE"));

        // 获取所有字段
        List<ColumnMetadata> columns = columnMetadataMapper.selectList(
            new QueryWrapper<ColumnMetadata>().eq("table_id", tableId));

        for (ColumnMetadata column : columns) {
            for (SensitiveRule rule : rules) {
                try {
                    scanColumn(table, column, rule);
                } catch (Exception e) {
                    logger.error("字段扫描失败: {}.{}",
                        table.getTableName(), column.getColumnName(), e);
                }
            }
        }

        logger.info("敏感数据扫描完成: {}", table.getTableName());
    }

    /**
     * 扫描单个字段
     */
    private void scanColumn(TableMetadata table, ColumnMetadata column, SensitiveRule rule) {
        try {
            String detectionResult = executeDetection(table, column, rule);

            if (detectionResult != null && !detectionResult.isEmpty()) {
                SensitiveScanResult result = new SensitiveScanResult();
                result.setTableId(table.getId());
                result.setColumnId(column.getId());
                result.setSensitiveType(rule.getDataCategory());
                result.setConfidence(0.8); // 默认置信度
                result.setScanTime(new Date());

                // 计算敏感数据比例
                Map<String, Object> stats = calculateSensitiveStats(table, column, rule);
                result.setTotalCount((Long) stats.get("totalCount"));
                result.setSensitiveCount((Long) stats.get("sensitiveCount"));
                result.setSensitiveRate((Double) stats.get("sensitiveRate"));

                resultMapper.insert(result);
            }
        } catch (Exception e) {
            logger.error("字段检测失败", e);
        }
    }

    /**
     * 执行敏感数据检测
     */
    private String executeDetection(TableMetadata table, ColumnMetadata column, SensitiveRule rule) {
        StringBuilder sql = new StringBuilder();

        switch (rule.getRuleType()) {
            case "PATTERN_REGEX":
                sql.append("SELECT COUNT(*) FROM ")
                    .append(table.getDatabaseName()).append(".").append(table.getTableName())
                    .append(" WHERE ").append(column.getColumnName())
                    .append(" REGEXP '").append(rule.getDetectionRule()).append("'");
                break;

            case "DICTIONARY":
                sql.append("SELECT COUNT(*) FROM ")
                    .append(table.getDatabaseName()).append(".").append(table.getTableName())
                    .append(" WHERE ").append(column.getColumnName())
                    .append(" IN (").append(rule.getDetectionRule()).append(")");
                break;

            default:
                logger.warn("不支持的检测规则类型: {}", rule.getRuleType());
                return null;
        }

        // 执行SQL查询
        try {
            Long count = executeCountQuery(sql.toString());
            return count > 0 ? "FOUND" : null;
        } catch (Exception e) {
            logger.error("执行检测查询失败", e);
            return null;
        }
    }

    /**
     * 计算敏感数据统计
     */
    private Map<String, Object> calculateSensitiveStats(TableMetadata table,
                                                       ColumnMetadata column,
                                                       SensitiveRule rule) {
        Map<String, Object> stats = new HashMap<>();

        String totalSql = "SELECT COUNT(*) FROM " +
            table.getDatabaseName() + "." + table.getTableName();

        String sensitiveSql = "SELECT COUNT(*) FROM " +
            table.getDatabaseName() + "." + table.getTableName() +
            " WHERE " + column.getColumnName() + " " + getWhereClause(rule);

        try {
            Long totalCount = executeCountQuery(totalSql);
            Long sensitiveCount = executeCountQuery(sensitiveSql);

            stats.put("totalCount", totalCount);
            stats.put("sensitiveCount", sensitiveCount);
            stats.put("sensitiveRate", totalCount > 0 ?
                (double) sensitiveCount / totalCount : 0.0);

        } catch (Exception e) {
            logger.error("计算统计信息失败", e);
            stats.put("totalCount", 0L);
            stats.put("sensitiveCount", 0L);
            stats.put("sensitiveRate", 0.0);
        }

        return stats;
    }

    /**
     * 获取WHERE子句
     */
    private String getWhereClause(SensitiveRule rule) {
        switch (rule.getRuleType()) {
            case "PATTERN_REGEX":
                return "REGEXP '" + rule.getDetectionRule() + "'";
            case "DICTIONARY":
                return "IN (" + rule.getDetectionRule() + ")";
            default:
                return "IS NOT NULL";
        }
    }

    /**
     * 执行计数查询
     */
    private Long executeCountQuery(String sql) {
        // 这里需要根据实际数据源执行查询
        // 返回查询结果
        return 0L;
    }
}

/**
 * 数据脱敏服务
 */
@Service
public class DataMaskingService {

    /**
     * 手机号脱敏
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (StringUtils.isEmpty(phoneNumber) || phoneNumber.length() != 11) {
            return phoneNumber;
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
    }

    /**
     * 身份证号脱敏
     */
    public String maskIdCard(String idCard) {
        if (StringUtils.isEmpty(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 邮箱脱敏
     */
    public String maskEmail(String email) {
        if (StringUtils.isEmpty(email) || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf("@");
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return "*" + domain;
        }

        return username.substring(0, 2) + "***" + domain;
    }

    /**
     * 银行卡号脱敏
     */
    public String maskBankCard(String bankCard) {
        if (StringUtils.isEmpty(bankCard) || bankCard.length() < 8) {
            return bankCard;
        }

        return "**** **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 姓名脱敏
     */
    public String maskName(String name) {
        if (StringUtils.isEmpty(name)) {
            return name;
        }

        if (name.length() == 1) {
            return name;
        }

        return name.substring(0, 1) + "*".repeat(name.length() - 1);
    }

    /**
     * 地址脱敏
     */
    public String maskAddress(String address) {
        if (StringUtils.isEmpty(address)) {
            return address;
        }

        int length = address.length();
        if (length <= 6) {
            return address.substring(0, length / 2) + "*".repeat(length / 2);
        }

        return address.substring(0, 3) + "*".repeat(length - 6) + address.substring(length - 3);
    }

    /**
     * 自定义脱敏
     */
    public String maskCustom(String value, int keepStart, int keepEnd, char maskChar) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        int length = value.length();
        if (keepStart + keepEnd >= length) {
            return maskChar + "*".repeat(length - 1);
        }

        StringBuilder masked = new StringBuilder();
        masked.append(value.substring(0, keepStart));
        masked.append(maskChar, 0, length - keepStart - keepEnd);
        masked.append(value.substring(length - keepEnd));

        return masked.toString();
    }
}

/**
 * 数据安全API
 */
@RestController
@RequestMapping("/api/security")
@Api(tags = "数据安全")
public class DataSecurityController {

    @Autowired
    private SensitiveDataDiscoveryService discoveryService;

    @Autowired
    private DataMaskingService maskingService;

    /**
     * 扫描敏感数据
     */
    @PostMapping("/scan/{tableId}")
    @ApiOperation("扫描敏感数据")
    public Result<Void> scanSensitiveData(@PathVariable Long tableId) {
        discoveryService.scanSensitiveData(tableId);
        return Result.success();
    }

    /**
     * 获取敏感数据概览
     */
    @GetMapping("/overview")
    @ApiOperation("获取敏感数据概览")
    public Result<Map<String, Object>> getSensitiveOverview() {
        Map<String, Object> overview = new HashMap<>();

        // TODO: 从数据库查询敏感数据统计信息

        overview.put("totalSensitiveTables", 0);
        overview.put("totalSensitiveColumns", 0);
        overview.put("highRiskDataCount", 0);
        overview.put("mediumRiskDataCount", 0);
        overview.put("lowRiskDataCount", 0);

        return Result.success(overview);
    }

    /**
     * 数据脱敏
     */
    @PostMapping("/mask")
    @ApiOperation("数据脱敏")
    public Result<Map<String, String>> maskData(@RequestBody Map<String, String> data) {
        Map<String, String> maskedData = new HashMap<>();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 根据字段类型应用不同的脱敏策略
            String maskedValue = maskByFieldType(key, value);
            maskedData.put(key, maskedValue);
        }

        return Result.success(maskedData);
    }

    /**
     * 根据字段类型脱敏
     */
    private String maskByFieldType(String fieldName, String value) {
        String lowerFieldName = fieldName.toLowerCase();

        if (lowerFieldName.contains("phone") || lowerFieldName.contains("mobile")) {
            return maskingService.maskPhoneNumber(value);
        } else if (lowerFieldName.contains("idcard") || lowerFieldName.contains("id_card")) {
            return maskingService.maskIdCard(value);
        } else if (lowerFieldName.contains("email")) {
            return maskingService.maskEmail(value);
        } else if (lowerFieldName.contains("bank") || lowerFieldName.contains("card")) {
            return maskingService.maskBankCard(value);
        } else if (lowerFieldName.contains("name")) {
            return maskingService.maskName(value);
        } else if (lowerFieldName.contains("address")) {
            return maskingService.maskAddress(value);
        } else {
            // 默认保留前2位和后2位
            return maskingService.maskCustom(value, 2, 2, '*');
        }
    }
}
```

---

## 📊 数据治理Dashboard

### 1. 管理界面

```vue
<template>
  <div class="data-governance-dashboard">
    <el-row :gutter="20">
      <!-- 数据概览卡片 -->
      <el-col :span="6">
        <el-card class="dashboard-card">
          <div class="card-content">
            <i class="icon-database"></i>
            <div class="card-info">
              <h3>{{ overview.totalTables }}</h3>
              <p>数据表总数</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="dashboard-card">
          <div class="card-content">
            <i class="icon-quality"></i>
            <div class="card-info">
              <h3>{{ overview.qualityScore }}%</h3>
              <p>数据质量分数</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="dashboard-card">
          <div class="card-content">
            <i class="icon-lineage"></i>
            <div class="card-info">
              <h3>{{ overview.totalLineages }}</h3>
              <p>血缘关系数</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="dashboard-card">
          <div class="card-content">
            <i class="icon-security"></i>
            <div class="card-info">
              <h3>{{ overview.sensitiveCount }}</h3>
              <p>敏感数据发现</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 质量趋势图表 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <h3>数据质量趋势</h3>
          <div id="qualityChart" style="height: 300px;"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <h3>数据分类分布</h3>
          <div id="classificationChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表列表 -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <div slot="header">
            <span>数据表概览</span>
            <el-button type="primary" size="small" @click="refreshData">
              刷新数据
            </el-button>
          </div>

          <el-table :data="tableList" stripe style="width: 100%">
            <el-table-column prop="tableName" label="表名"></el-table-column>
            <el-table-column prop="databaseName" label="数据库"></el-table-column>
            <el-table-column prop="rowCount" label="数据量"></el-table-column>
            <el-table-column prop="dataClassification" label="数据分级"></el-table-column>
            <el-table-column label="质量分数">
              <template slot-scope="scope">
                <el-tag :type="getQualityTagType(scope.row.qualityScore)">
                  {{ scope.row.qualityScore }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template slot-scope="scope">
                <el-button type="text" @click="viewLineage(scope.row.id)">
                  查看血缘
                </el-button>
                <el-button type="text" @click="viewQuality(scope.row.id)">
                  质量报告
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="currentPage"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total">
          </el-pagination>
        </el-card>
      </el-col>
    </el-row>

    <!-- 血缘关系弹窗 -->
    <el-dialog :visible.sync="lineageDialogVisible" title="数据血缘关系" width="80%">
      <div id="lineageGraph" style="height: 500px;"></div>
    </el-dialog>

    <!-- 质量报告弹窗 -->
    <el-dialog :visible.sync="qualityDialogVisible" title="数据质量报告" width="60%">
      <div v-if="qualityReport">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="表名">{{ qualityReport.tableName }}</el-descriptions-item>
          <el-descriptions-item label="数据库">{{ qualityReport.databaseName }}</el-descriptions-item>
          <el-descriptions-item label="总数据量">{{ qualityReport.totalRows }}</el-descriptions-item>
          <el-descriptions-item label="质量分数">{{ qualityReport.qualityScore }}%</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px;">质量规则检查结果</h4>
        <el-table :data="qualityReport.ruleResults" stripe>
          <el-table-column prop="ruleName" label="规则名称"></el-table-column>
          <el-table-column prop="ruleType" label="规则类型"></el-table-column>
          <el-table-column prop="passRate" label="通过率">
            <template slot-scope="scope">
              {{ (scope.row.passRate * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'DataGovernanceDashboard',

  data() {
    return {
      overview: {
        totalTables: 0,
        qualityScore: 0,
        totalLineages: 0,
        sensitiveCount: 0
      },
      tableList: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      lineageDialogVisible: false,
      qualityDialogVisible: false,
      qualityReport: null,
      qualityChart: null,
      classificationChart: null,
      lineageGraph: null
    }
  },

  mounted() {
    this.loadOverview();
    this.loadTableList();
    this.initCharts();
  },

  methods: {
    // 加载概览数据
    async loadOverview() {
      try {
        const response = await this.$http.get('/api/metadata/overview');
        this.overview = response.data;
      } catch (error) {
        console.error('加载概览数据失败', error);
      }
    },

    // 加载表列表
    async loadTableList() {
      try {
        const params = {
          page: this.currentPage,
          size: this.pageSize
        };
        const response = await this.$http.get('/api/metadata/tables', { params });
        this.tableList = response.data.records;
        this.total = response.data.total;
      } catch (error) {
        console.error('加载表列表失败', error);
      }
    },

    // 初始化图表
    initCharts() {
      // 质量趋势图表
      this.qualityChart = echarts.init(document.getElementById('qualityChart'));

      // 数据分类分布图表
      this.classificationChart = echarts.init(document.getElementById('classificationChart'));

      this.loadQualityTrend();
      this.loadClassificationData();
    },

    // 加载质量趋势数据
    async loadQualityTrend() {
      try {
        const response = await this.$http.get('/api/quality/trend', {
          params: { days: 30 }
        });

        const option = {
          tooltip: {
            trigger: 'axis'
          },
          xAxis: {
            type: 'category',
            data: response.data.map(item => item.date)
          },
          yAxis: {
            type: 'value',
            min: 0,
            max: 100
          },
          series: [{
            name: '质量分数',
            type: 'line',
            data: response.data.map(item => (item.passRate * 100).toFixed(2)),
            smooth: true,
            lineStyle: {
              color: '#409EFF'
            }
          }]
        };

        this.qualityChart.setOption(option);
      } catch (error) {
        console.error('加载质量趋势数据失败', error);
      }
    },

    // 加载分类数据
    async loadClassificationData() {
      try {
        const response = await this.$http.get('/api/metadata/overview');
        const classificationData = response.data.classificationStats;

        const option = {
          tooltip: {
            trigger: 'item'
          },
          legend: {
            orient: 'vertical',
            left: 'left'
          },
          series: [{
            name: '数据分类',
            type: 'pie',
            radius: '50%',
            data: Object.entries(classificationData).map(([key, value]) => ({
              name: key,
              value: value
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }]
        };

        this.classificationChart.setOption(option);
      } catch (error) {
        console.error('加载分类数据失败', error);
      }
    },

    // 查看血缘关系
    async viewLineage(tableId) {
      this.lineageDialogVisible = true;

      // 延迟初始化图表
      this.$nextTick(() => {
        this.initLineageGraph(tableId);
      });
    },

    // 初始化血缘图表
    async initLineageGraph(tableId) {
      try {
        const response = await this.$http.get(`/api/lineage/graph/${tableId}`);

        this.lineageGraph = echarts.init(document.getElementById('lineageGraph'));

        // 处理血缘数据
        const graphData = this.processLineageData(response.data);

        const option = {
          tooltip: {},
          series: [{
            type: 'graph',
            layout: 'force',
            symbolSize: 50,
            roam: true,
            label: {
              show: true
            },
            edgeSymbol: ['none', 'arrow'],
            edgeSymbolSize: [4, 10],
            data: graphData.nodes,
            links: graphData.links,
            lineStyle: {
              opacity: 0.9,
              width: 2,
              curveness: 0.1
            },
            force: {
              repulsion: 1000,
              edgeLength: 200
            }
          }]
        };

        this.lineageGraph.setOption(option);
      } catch (error) {
        console.error('加载血缘关系失败', error);
      }
    },

    // 处理血缘数据
    processLineageData(data) {
      const nodes = [];
      const links = [];

      // 添加当前表节点
      nodes.push({
        id: data.table.id.toString(),
        name: data.table.tableName,
        category: 0,
        symbolSize: 80,
        itemStyle: {
          color: '#FF6B6B'
        }
      });

      // 添加上游节点
      data.upstream.forEach((item, index) => {
        const nodeId = `upstream_${index}`;
        nodes.push({
          id: nodeId,
          name: item.node.nodeName,
          category: 1,
          itemStyle: {
            color: '#4ECDC4'
          }
        });

        links.push({
          source: nodeId,
          target: data.table.id.toString(),
          value: item.edgeType
        });
      });

      // 添加下游节点
      data.downstream.forEach((item, index) => {
        const nodeId = `downstream_${index}`;
        nodes.push({
          id: nodeId,
          name: item.node.nodeName,
          category: 2,
          itemStyle: {
            color: '#45B7D1'
          }
        });

        links.push({
          source: data.table.id.toString(),
          target: nodeId,
          value: item.edgeType
        });
      });

      return { nodes, links };
    },

    // 查看质量报告
    async viewQuality(tableId) {
      this.qualityDialogVisible = true;

      try {
        const response = await this.$http.get(`/api/quality/table/${tableId}/report`);
        this.qualityReport = response.data;
      } catch (error) {
        console.error('加载质量报告失败', error);
      }
    },

    // 获取质量标签类型
    getQualityTagType(score) {
      if (score >= 90) return 'success';
      if (score >= 70) return 'warning';
      return 'danger';
    },

    // 刷新数据
    refreshData() {
      this.loadOverview();
      this.loadTableList();
      this.loadQualityTrend();
      this.loadClassificationData();
    },

    // 分页处理
    handleSizeChange(val) {
      this.pageSize = val;
      this.loadTableList();
    },

    handleCurrentChange(val) {
      this.currentPage = val;
      this.loadTableList();
    }
  }
}
</script>

<style scoped>
.data-governance-dashboard {
  padding: 20px;
}

.dashboard-card {
  text-align: center;
}

.card-content {
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-content .icon-database,
.card-content .icon-quality,
.card-content .icon-lineage,
.card-content .icon-security {
  font-size: 48px;
  margin-right: 20px;
}

.card-info h3 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.card-info p {
  margin: 5px 0 0 0;
  font-size: 14px;
  color: #909399;
}
</style>
```

---

## 📚 参考资料

1. [Apache Atlas 官方文档](https://atlas.apache.org/)
2. [DataHub 官方文档](https://datahubproject.io/)
3. [Apache Griffin 官方文档](https://griffin.apache.org/)
4. [数据治理最佳实践](https://www.dama.org/)
5. [GDPR 数据保护指南](https://gdpr.eu/)

---

## 📋 实施检查清单

### 元数据管理
- [ ] 数据源配置完成
- [ ] 元数据扫描任务配置
- [ ] 业务术语表建立
- [ ] 数据标签体系建立

### 数据质量管理
- [ ] 质量规则配置完成
- [ ] 质量检查任务调度
- [ ] 质量报告自动生成
- [ ] 质量告警配置

### 数据血缘管理
- [ ] ETL作业血缘记录
- [ ] 血缘关系图可视化
- [ ] 影响分析功能
- [ ] 变更传播追踪

### 数据安全
- [ ] 敏感数据规则配置
- [ ] 敏感数据自动发现
- [ ] 数据脱敏策略
- [ ] 访问权限控制

### 平台建设
- [ ] 管理界面开发
- [ ] API服务完善
- [ ] 监控告警配置
- [ ] 文档编写完成

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-15
**状态：** 📋 指南完成，准备实施

**加油喵～ 数据治理平台即将完成！** ฅ'ω'ฅ
