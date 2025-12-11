package com.basebackend.generator.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.generator.cache.GeneratorCacheManager;
import com.basebackend.generator.constant.GeneratorConstants;
import com.basebackend.generator.core.engine.TemplateEngine;
import com.basebackend.generator.core.engine.TemplateEngineFactory;
import com.basebackend.generator.core.metadata.ColumnMetadata;
import com.basebackend.generator.core.metadata.DatabaseMetadataReader;
import com.basebackend.generator.core.metadata.MetadataReaderFactory;
import com.basebackend.generator.core.metadata.TableMetadata;
import com.basebackend.generator.core.strategy.NamingStrategy;
import com.basebackend.generator.dto.GenerateRequest;
import com.basebackend.generator.dto.GenerateResult;
import com.basebackend.generator.entity.*;
import com.basebackend.generator.mapper.GenDataSourceMapper;
import com.basebackend.generator.mapper.GenTemplateGroupMapper;
import com.basebackend.generator.mapper.GenTemplateMapper;
import com.basebackend.generator.mapper.GenTypeMappingMapper;
import com.basebackend.generator.util.CodeFormatter;
import com.basebackend.generator.util.DataSourceUtils;
import com.basebackend.generator.util.PathSecurityValidator;
import com.basebackend.generator.util.ZipUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成器服务
 * 
 * P2优化内容：
 * 1. 集成MetadataReaderFactory，支持多数据库类型
 * 2. 集成GeneratorCacheManager，提供模板和元数据缓存
 * 3. 集成CodeFormatter，自动格式化生成的代码
 * 4. 保持P0/P1的所有优化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratorService {

    private final GenDataSourceMapper dataSourceMapper;
    private final GenTemplateMapper templateMapper;
    private final GenTemplateGroupMapper templateGroupMapper;
    private final GenTypeMappingMapper typeMappingMapper;
    private final TemplateEngineFactory engineFactory;

    // P2新增组件
    private final MetadataReaderFactory metadataReaderFactory;
    private final GeneratorCacheManager cacheManager;
    private final CodeFormatter codeFormatter;

    /**
     * 生成代码（主入口方法）
     *
     * @param request 生成请求
     * @return 生成结果
     */
    public GenerateResult generate(GenerateRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            // 1. 验证并获取数据源配置
            GenDataSource dsConfig = validateAndGetDataSource(request);
            if (dsConfig == null) {
                return buildErrorResult("数据源不存在");
            }

            // 2. 加载模板列表（带缓存）
            List<GenTemplate> templates = loadTemplatesWithCache(request.getTemplateGroupId());
            if (templates.isEmpty()) {
                return buildErrorResult("未找到可用模板");
            }

            // 3. 加载类型映射
            Map<String, GenTypeMapping> typeMappings = loadTypeMappings(dsConfig.getDbType());

            // 4. 生成代码文件
            GenerationContext context = new GenerationContext(dsConfig, templates, typeMappings, request);
            Map<String, String> generatedFiles = generateFiles(context);

            // 5. 构建并返回结果
            GenerateResult result = buildResult(generatedFiles, context.getFailedTables(), request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("代码生成完成，耗时 {}ms，生成 {} 个文件，缓存统计: {}",
                    duration, generatedFiles.size(), cacheManager.getStats());

            return result;

        } catch (Exception e) {
            log.error("代码生成失败", e);
            return buildErrorResult(e.getMessage());
        }
    }

    // ==================== 第一阶段：数据准备（带缓存） ====================

    /**
     * 验证并获取数据源配置
     */
    private GenDataSource validateAndGetDataSource(GenerateRequest request) {
        if (request.getDatasourceId() == null) {
            log.warn("数据源ID为空");
            return null;
        }
        return dataSourceMapper.selectById(request.getDatasourceId());
    }

    /**
     * 加载启用的模板列表（带缓存）
     */
    private List<GenTemplate> loadTemplatesWithCache(Long templateGroupId) {
        // 先从缓存获取
        List<GenTemplate> cached = cacheManager.getTemplates(templateGroupId);
        if (cached != null) {
            log.debug("从缓存获取模板列表: groupId={}", templateGroupId);
            return cached;
        }

        // 缓存未命中，从数据库加载
        List<GenTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<GenTemplate>()
                        .eq(GenTemplate::getGroupId, templateGroupId)
                        .eq(GenTemplate::getEnabled, GeneratorConstants.STATUS_ENABLED)
                        .orderByAsc(GenTemplate::getSortOrder));

        // 放入缓存
        if (!templates.isEmpty()) {
            cacheManager.putTemplates(templateGroupId, templates);
        }

        return templates;
    }

    /**
     * 加载类型映射
     */
    private Map<String, GenTypeMapping> loadTypeMappings(String dbType) {
        List<GenTypeMapping> mappings = typeMappingMapper.selectList(
                new LambdaQueryWrapper<GenTypeMapping>()
                        .eq(GenTypeMapping::getDbType, dbType));

        return mappings.stream()
                .collect(Collectors.toMap(GenTypeMapping::getColumnType, m -> m));
    }

    // ==================== 第二阶段：代码生成 ====================

    /**
     * 生成所有表的代码文件
     */
    private Map<String, String> generateFiles(GenerationContext context) {
        Map<String, String> generatedFiles = new LinkedHashMap<>();
        DataSource dataSource = DataSourceUtils.getOrCreateDataSource(context.getDsConfig());

        // 使用工厂获取对应数据库类型的元数据读取器
        DatabaseMetadataReader reader = metadataReaderFactory.getReader(context.getDsConfig().getDbType());

        for (String tableName : context.getRequest().getTableNames()) {
            try {
                generateTableFiles(tableName, dataSource, reader, context, generatedFiles);
            } catch (Exception e) {
                log.error("生成表 {} 的代码失败", tableName, e);
                context.addFailedTable(tableName);
            }
        }

        return generatedFiles;
    }

    /**
     * 生成单个表的所有文件
     */
    private void generateTableFiles(String tableName, DataSource dataSource,
            DatabaseMetadataReader reader, GenerationContext context,
            Map<String, String> generatedFiles) {
        // 获取表元数据（带缓存）
        TableMetadata tableMetadata = getTableMetadataWithCache(
                context.getDsConfig().getId(), tableName, dataSource, reader);
        enhanceTableMetadata(tableMetadata, context.getRequest(), context.getTypeMappings());

        // 为每个模板生成代码
        for (GenTemplate template : context.getTemplates()) {
            generateSingleFile(template, tableMetadata, context.getRequest(), generatedFiles);
        }
    }

    /**
     * 获取表元数据（带缓存）
     */
    private TableMetadata getTableMetadataWithCache(Long datasourceId, String tableName,
            DataSource dataSource, DatabaseMetadataReader reader) {
        // 先从缓存获取
        TableMetadata cached = cacheManager.getMetadata(datasourceId, tableName);
        if (cached != null) {
            log.debug("从缓存获取表元数据: {}", tableName);
            // 返回副本以避免缓存被修改
            return cloneTableMetadata(cached);
        }

        // 缓存未命中，从数据库读取
        TableMetadata metadata = reader.getTableMetadata(dataSource, tableName);

        // 放入缓存
        cacheManager.putMetadata(datasourceId, tableName, metadata);

        return metadata;
    }

    /**
     * 克隆表元数据（避免缓存被修改）
     */
    private TableMetadata cloneTableMetadata(TableMetadata original) {
        List<ColumnMetadata> clonedColumns = original.getColumns().stream()
                .map(col -> ColumnMetadata.builder()
                        .columnName(col.getColumnName())
                        .columnType(col.getColumnType())
                        .columnComment(col.getColumnComment())
                        .isPrimaryKey(col.getIsPrimaryKey())
                        .isAutoIncrement(col.getIsAutoIncrement())
                        .nullable(col.getNullable())
                        .isSystemField(col.getIsSystemField())
                        .queryable(col.getQueryable())
                        .maxLength(col.getMaxLength())
                        .defaultValue(col.getDefaultValue())
                        .build())
                .collect(Collectors.toList());

        return TableMetadata.builder()
                .tableName(original.getTableName())
                .tableComment(original.getTableComment())
                .columns(clonedColumns)
                .primaryKey(original.getPrimaryKey())
                .hasDateTime(original.getHasDateTime())
                .hasBigDecimal(original.getHasBigDecimal())
                .importPackages(new ArrayList<>(original.getImportPackages()))
                .build();
    }

    /**
     * 生成单个文件
     */
    private void generateSingleFile(GenTemplate template, TableMetadata table,
            GenerateRequest request, Map<String, String> generatedFiles) {
        // 渲染模板内容
        String content = renderTemplate(template, table, request);

        // 构建并验证文件路径
        String filePath = buildAndValidateFilePath(template, table, request);

        if (filePath != null) {
            // 格式化代码
            String formattedContent = codeFormatter.format(content, filePath);
            generatedFiles.put(filePath, formattedContent);
        }
    }

    // ==================== 第三阶段：模板渲染 ====================

    /**
     * 渲染模板
     */
    private String renderTemplate(GenTemplate template, TableMetadata table, GenerateRequest request) {
        GenTemplateGroup group = templateGroupMapper.selectById(template.getGroupId());
        TemplateEngine engine = engineFactory.getEngine(group.getEngineType());

        Map<String, Object> dataModel = buildDataModel(table, request);
        return engine.render(template.getTemplateContent(), dataModel);
    }

    /**
     * 构建模板数据模型
     */
    private Map<String, Object> buildDataModel(TableMetadata table, GenerateRequest request) {
        Map<String, Object> dataModel = new HashMap<>();

        // 基础信息
        dataModel.put("packageName", request.getPackageName());
        dataModel.put("moduleName", request.getModuleName());
        dataModel.put("author", request.getAuthor());
        dataModel.put("date", DateUtil.today());

        // 表信息
        dataModel.put("tableName", table.getTableName());
        dataModel.put("tableComment", table.getTableComment());
        dataModel.put("className", table.getClassName());
        dataModel.put("variableName", table.getVariableName());
        dataModel.put("urlPath", table.getUrlPath());

        // 列信息
        dataModel.put("columns", table.getColumns());
        dataModel.put("primaryKey", table.getPrimaryKey());
        dataModel.put("hasDateTime", table.getHasDateTime());
        dataModel.put("hasBigDecimal", table.getHasBigDecimal());

        return dataModel;
    }

    // ==================== 第四阶段：路径处理 ====================

    /**
     * 构建并验证文件路径
     * 防止目录遍历攻击
     */
    private String buildAndValidateFilePath(GenTemplate template, TableMetadata table, GenerateRequest request) {
        String path = buildFilePath(template, table, request);

        // 验证路径安全性
        PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
        if (!result.isValid()) {
            log.error("生成的文件路径不安全: {} - {}", path, result.getErrorMessage());
            throw new SecurityException("不安全的文件路径: " + result.getErrorMessage());
        }

        return result.getNormalizedPath();
    }

    /**
     * 构建文件路径
     */
    private String buildFilePath(GenTemplate template, TableMetadata table, GenerateRequest request) {
        String path = template.getOutputPath();
        if (path == null || path.isEmpty()) {
            path = GeneratorConstants.DEFAULT_OUTPUT_PATH + template.getFileSuffix();
        }

        // 替换占位符
        path = path.replace(GeneratorConstants.PLACEHOLDER_PACKAGE_PATH,
                request.getPackageName().replace(".", "/"));
        path = path.replace(GeneratorConstants.PLACEHOLDER_CLASS_NAME, table.getClassName());
        path = path.replace(GeneratorConstants.PLACEHOLDER_VARIABLE_NAME, table.getVariableName());
        path = path.replace(GeneratorConstants.PLACEHOLDER_TEMPLATE_CODE, template.getCode());

        return path;
    }

    // ==================== 第五阶段：元数据增强 ====================

    /**
     * 增强表元数据（添加Java类型、命名等）
     */
    private void enhanceTableMetadata(TableMetadata table, GenerateRequest request,
            Map<String, GenTypeMapping> typeMappings) {
        // 设置命名
        table.setClassName(NamingStrategy.tableToClassName(table.getTableName(), request.getTablePrefix()));
        table.setVariableName(NamingStrategy.tableToVariableName(table.getTableName(), request.getTablePrefix()));
        table.setUrlPath(NamingStrategy.tableToUrlPath(table.getTableName(), request.getTablePrefix()));
        table.setModuleName(request.getModuleName());
        table.setPackageName(request.getPackageName());

        // 处理列信息
        for (ColumnMetadata column : table.getColumns()) {
            enhanceColumnMetadata(column, typeMappings);
        }
    }

    /**
     * 增强列元数据
     */
    private void enhanceColumnMetadata(ColumnMetadata column, Map<String, GenTypeMapping> typeMappings) {
        column.setJavaField(NamingStrategy.columnToJavaField(column.getColumnName()));

        // 类型映射
        GenTypeMapping mapping = typeMappings.get(column.getColumnType());
        if (mapping != null) {
            column.setJavaType(mapping.getJavaType());
            column.setTsType(mapping.getTsType());
            column.setImportPackage(mapping.getImportPackage());
        } else {
            // 使用默认类型
            column.setJavaType(GeneratorConstants.DEFAULT_JAVA_TYPE);
            column.setTsType(GeneratorConstants.DEFAULT_TS_TYPE);
        }
    }

    // ==================== 第六阶段：结果构建 ====================

    /**
     * 构建生成结果
     */
    private GenerateResult buildResult(Map<String, String> generatedFiles,
            List<String> failedTables,
            GenerateRequest request) {
        GenerateResult.GenerateResultBuilder resultBuilder = GenerateResult.builder()
                .files(generatedFiles)
                .fileCount(generatedFiles.size())
                .failedTables(failedTables);

        // 确定状态
        if (failedTables.isEmpty()) {
            resultBuilder.status(GenerateStatus.SUCCESS.name());
        } else if (failedTables.size() == request.getTableNames().size()) {
            resultBuilder.status(GenerateStatus.FAILED.name());
        } else {
            resultBuilder.status(GenerateStatus.PARTIAL.name());
        }

        // 如果是下载模式，打包成ZIP
        if (GeneratorConstants.GENERATE_TYPE_DOWNLOAD.equals(request.getGenerateType())) {
            byte[] zipData = ZipUtils.createZip(generatedFiles);
            resultBuilder.zipData(zipData);
        }

        return resultBuilder.build();
    }

    /**
     * 构建错误结果
     */
    private GenerateResult buildErrorResult(String errorMessage) {
        return GenerateResult.builder()
                .status(GenerateStatus.FAILED.name())
                .errorMessage(errorMessage)
                .build();
    }

    // ==================== 缓存管理接口 ====================

    /**
     * 清空所有缓存
     * 可用于管理员手动刷新缓存
     */
    public void clearAllCaches() {
        cacheManager.clearAll();
        log.info("所有缓存已清空");
    }

    /**
     * 使指定数据源的缓存失效
     * 当数据源结构变更时调用
     */
    public void invalidateDatasourceCache(Long datasourceId) {
        cacheManager.invalidateMetadataByDatasource(datasourceId);
        DataSourceUtils.removeFromCache(datasourceId);
        log.info("数据源缓存已失效: {}", datasourceId);
    }

    /**
     * 使指定模板分组的缓存失效
     * 当模板变更时调用
     */
    public void invalidateTemplateCache(Long templateGroupId) {
        cacheManager.invalidateTemplates(templateGroupId);
        log.info("模板缓存已失效: {}", templateGroupId);
    }

    /**
     * 获取缓存统计信息
     */
    public GeneratorCacheManager.CacheStats getCacheStats() {
        return cacheManager.getStats();
    }

    // ==================== 内部类 ====================

    /**
     * 生成上下文，封装生成过程中需要的所有数据
     */
    private static class GenerationContext {
        private final GenDataSource dsConfig;
        private final List<GenTemplate> templates;
        private final Map<String, GenTypeMapping> typeMappings;
        private final GenerateRequest request;
        private final List<String> failedTables;

        public GenerationContext(GenDataSource dsConfig, List<GenTemplate> templates,
                Map<String, GenTypeMapping> typeMappings, GenerateRequest request) {
            this.dsConfig = dsConfig;
            this.templates = templates;
            this.typeMappings = typeMappings;
            this.request = request;
            this.failedTables = new ArrayList<>();
        }

        public GenDataSource getDsConfig() {
            return dsConfig;
        }

        public List<GenTemplate> getTemplates() {
            return templates;
        }

        public Map<String, GenTypeMapping> getTypeMappings() {
            return typeMappings;
        }

        public GenerateRequest getRequest() {
            return request;
        }

        public List<String> getFailedTables() {
            return failedTables;
        }

        public void addFailedTable(String tableName) {
            failedTables.add(tableName);
        }
    }
}
