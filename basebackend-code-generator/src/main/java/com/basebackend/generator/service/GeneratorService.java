package com.basebackend.generator.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.generator.core.engine.TemplateEngine;
import com.basebackend.generator.core.engine.TemplateEngineFactory;
import com.basebackend.generator.core.metadata.ColumnMetadata;
import com.basebackend.generator.core.metadata.DatabaseMetadataReader;
import com.basebackend.generator.core.metadata.MySQLMetadataReader;
import com.basebackend.generator.core.metadata.TableMetadata;
import com.basebackend.generator.core.strategy.NamingStrategy;
import com.basebackend.generator.dto.GenerateRequest;
import com.basebackend.generator.dto.GenerateResult;
import com.basebackend.generator.entity.*;
import com.basebackend.generator.mapper.GenDataSourceMapper;
import com.basebackend.generator.mapper.GenTemplateGroupMapper;
import com.basebackend.generator.mapper.GenTemplateMapper;
import com.basebackend.generator.mapper.GenTypeMappingMapper;
import com.basebackend.generator.util.DataSourceUtils;
import com.basebackend.generator.util.ZipUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成器服务
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

    /**
     * 生成代码
     */
    public GenerateResult generate(GenerateRequest request) {
        try {
            // 1. 获取数据源
            GenDataSource dsConfig = dataSourceMapper.selectById(request.getDatasourceId());
            if (dsConfig == null) {
                return GenerateResult.builder()
                        .status(GenerateStatus.FAILED.name())
                        .errorMessage("数据源不存在")
                        .build();
            }

            // 2. 创建数据源连接
            DataSource dataSource = DataSourceUtils.createDataSource(dsConfig);
            DatabaseMetadataReader reader = new MySQLMetadataReader();

            // 3. 获取模板列表
            List<GenTemplate> templates = templateMapper.selectList(
                    new LambdaQueryWrapper<GenTemplate>()
                            .eq(GenTemplate::getGroupId, request.getTemplateGroupId())
                            .eq(GenTemplate::getEnabled, 1)
                            .orderByAsc(GenTemplate::getSortOrder)
            );

            if (templates.isEmpty()) {
                return GenerateResult.builder()
                        .status(GenerateStatus.FAILED.name())
                        .errorMessage("未找到可用模板")
                        .build();
            }

            // 4. 获取类型映射
            Map<String, GenTypeMapping> typeMappings = loadTypeMappings(dsConfig.getDbType());

            // 5. 生成代码
            Map<String, String> generatedFiles = new HashMap<>();
            List<String> failedTables = new ArrayList<>();

            for (String tableName : request.getTableNames()) {
                try {
                    TableMetadata tableMetadata = reader.getTableMetadata(dataSource, tableName);
                    enhanceTableMetadata(tableMetadata, request, typeMappings);

                    for (GenTemplate template : templates) {
                        String content = renderTemplate(template, tableMetadata, request);
                        String filePath = buildFilePath(template, tableMetadata, request);
                        generatedFiles.put(filePath, content);
                    }
                } catch (Exception e) {
                    log.error("生成表 {} 的代码失败", tableName, e);
                    failedTables.add(tableName);
                }
            }

            // 6. 处理生成结果
            GenerateResult.GenerateResultBuilder resultBuilder = GenerateResult.builder()
                    .files(generatedFiles)
                    .fileCount(generatedFiles.size())
                    .failedTables(failedTables);

            if (failedTables.isEmpty()) {
                resultBuilder.status(GenerateStatus.SUCCESS.name());
            } else if (failedTables.size() == request.getTableNames().size()) {
                resultBuilder.status(GenerateStatus.FAILED.name());
            } else {
                resultBuilder.status(GenerateStatus.PARTIAL.name());
            }

            // 7. 如果是下载模式，打包成ZIP
            if (GenerateType.DOWNLOAD.name().equals(request.getGenerateType())) {
                byte[] zipData = ZipUtils.createZip(generatedFiles);
                resultBuilder.zipData(zipData);
            }

            return resultBuilder.build();

        } catch (Exception e) {
            log.error("代码生成失败", e);
            return GenerateResult.builder()
                    .status(GenerateStatus.FAILED.name())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 加载类型映射
     */
    private Map<String, GenTypeMapping> loadTypeMappings(String dbType) {
        List<GenTypeMapping> mappings = typeMappingMapper.selectList(
                new LambdaQueryWrapper<GenTypeMapping>()
                        .eq(GenTypeMapping::getDbType, dbType)
        );

        return mappings.stream()
                .collect(Collectors.toMap(GenTypeMapping::getColumnType, m -> m));
    }

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
            column.setJavaField(NamingStrategy.columnToJavaField(column.getColumnName()));

            // 类型映射
            GenTypeMapping mapping = typeMappings.get(column.getColumnType());
            if (mapping != null) {
                column.setJavaType(mapping.getJavaType());
                column.setTsType(mapping.getTsType());
                column.setImportPackage(mapping.getImportPackage());
            } else {
                column.setJavaType("String");
                column.setTsType("string");
            }
        }
    }

    /**
     * 渲染模板
     */
    private String renderTemplate(GenTemplate template, TableMetadata table, GenerateRequest request) {
        GenTemplateGroup group = templateGroupMapper.selectById(template.getGroupId());
        TemplateEngine engine = engineFactory.getEngine(group.getEngineType());

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", request.getPackageName());
        dataModel.put("moduleName", request.getModuleName());
        dataModel.put("author", request.getAuthor());
        dataModel.put("date", DateUtil.today());
        dataModel.put("tableName", table.getTableName());
        dataModel.put("tableComment", table.getTableComment());
        dataModel.put("className", table.getClassName());
        dataModel.put("variableName", table.getVariableName());
        dataModel.put("urlPath", table.getUrlPath());
        dataModel.put("columns", table.getColumns());
        dataModel.put("primaryKey", table.getPrimaryKey());
        dataModel.put("hasDateTime", table.getHasDateTime());
        dataModel.put("hasBigDecimal", table.getHasBigDecimal());

        return engine.render(template.getTemplateContent(), dataModel);
    }

    /**
     * 构建文件路径
     */
    private String buildFilePath(GenTemplate template, TableMetadata table, GenerateRequest request) {
        String path = template.getOutputPath();
        if (path == null) {
            path = "${packagePath}/${templateCode}/${className}" + template.getFileSuffix();
        }

        path = path.replace("${packagePath}", request.getPackageName().replace(".", "/"));
        path = path.replace("${className}", table.getClassName());
        path = path.replace("${variableName}", table.getVariableName());
        path = path.replace("${templateCode}", template.getCode());

        return path;
    }
}
