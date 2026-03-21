package com.basebackend.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.model.Result;
import com.basebackend.generator.core.metadata.DatabaseMetadataReader;
import com.basebackend.generator.core.metadata.MySQLMetadataReader;
import com.basebackend.generator.dto.GenDataSourceView;
import com.basebackend.generator.entity.GenDataSource;
import com.basebackend.generator.mapper.GenDataSourceMapper;
import com.basebackend.generator.util.DataSourceUtils;
import com.basebackend.security.annotation.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.List;

/**
 * 数据源管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/generator/datasource")
@RequiredArgsConstructor
@Tag(name = "数据源管理")
@RequiresRole(values = {"admin", "sys_admin"}, logical = RequiresRole.Logical.OR)
public class DataSourceController {

    private final GenDataSourceMapper dataSourceMapper;

    @GetMapping
    @Operation(summary = "分页查询数据源")
    public Result<Page<GenDataSourceView>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<GenDataSource> page = new Page<>(current, size);
        Page<GenDataSource> resultPage = dataSourceMapper.selectPage(page, null);

        Page<GenDataSourceView> viewPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        viewPage.setRecords(resultPage.getRecords().stream()
                .map(GenDataSourceView::from)
                .toList());
        return Result.success(viewPage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询数据源")
    public Result<GenDataSourceView> getById(@PathVariable Long id) {
        return Result.success(GenDataSourceView.from(dataSourceMapper.selectById(id)));
    }

    @PostMapping
    @Operation(summary = "创建数据源")
    public Result<String> create(@RequestBody GenDataSource dataSource) {
        dataSourceMapper.insert(dataSource);
        return Result.success("创建成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新数据源")
    public Result<String> update(@PathVariable Long id, @RequestBody GenDataSource dataSource) {
        dataSource.setId(id);
        dataSourceMapper.updateById(dataSource);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据源")
    public Result<String> delete(@PathVariable Long id) {
        dataSourceMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @PostMapping("/test")
    @Operation(summary = "测试数据源连接")
    public Result<String> test(@RequestBody GenDataSource dataSource) {
        try {
            DataSource ds = DataSourceUtils.createDataSource(dataSource);
            boolean success = DataSourceUtils.testConnection(ds);
            DataSourceUtils.closeDataSource(ds);

            if (success) {
                return Result.success("连接成功");
            } else {
                return Result.error("连接失败");
            }
        } catch (Exception e) {
            log.error("测试数据源连接失败", e);
            return Result.error("连接失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/tables")
    @Operation(summary = "获取数据源的表列表")
    public Result<List<String>> getTables(@PathVariable Long id) {
        try {
            GenDataSource dsConfig = dataSourceMapper.selectById(id);
            DataSource dataSource = DataSourceUtils.createDataSource(dsConfig);
            DatabaseMetadataReader reader = new MySQLMetadataReader();

            List<String> tables = reader.getTableNames(dataSource, dsConfig.getDatabaseName());
            DataSourceUtils.closeDataSource(dataSource);

            return Result.success(tables);
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            return Result.error("获取表列表失败: " + e.getMessage());
        }
    }
}
