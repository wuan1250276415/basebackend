package com.basebackend.database.statistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.database.statistics.entity.SqlStatistics;
import com.basebackend.database.statistics.model.SqlOptimizationSuggestion;
import com.basebackend.database.statistics.query.SqlStatisticsQuery;
import com.basebackend.database.statistics.service.SqlStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SQL统计控制器
 * 提供SQL统计查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/database/sql-statistics")
public class SqlStatisticsController {

    private final SqlStatisticsService sqlStatisticsService;

    public SqlStatisticsController(SqlStatisticsService sqlStatisticsService) {
        this.sqlStatisticsService = sqlStatisticsService;
    }

    /**
     * 分页查询SQL统计
     */
    @PostMapping("/query")
    public Page<SqlStatistics> query(@RequestBody SqlStatisticsQuery query) {
        return sqlStatisticsService.query(query);
    }

    /**
     * 根据ID查询SQL统计
     */
    @GetMapping("/{id}")
    public SqlStatistics getById(@PathVariable Long id) {
        return sqlStatisticsService.getById(id);
    }

    /**
     * 获取慢查询列表
     */
    @GetMapping("/slow-queries")
    public List<SqlStatistics> getSlowQueries(
            @RequestParam(defaultValue = "1000") Long minAvgTime,
            @RequestParam(defaultValue = "20") Integer limit) {
        return sqlStatisticsService.getSlowQueries(minAvgTime, limit);
    }

    /**
     * 获取执行次数最多的SQL列表
     */
    @GetMapping("/most-executed")
    public List<SqlStatistics> getMostExecuted(
            @RequestParam(defaultValue = "20") Integer limit) {
        return sqlStatisticsService.getMostExecuted(limit);
    }

    /**
     * 获取失败次数最多的SQL列表
     */
    @GetMapping("/most-failed")
    public List<SqlStatistics> getMostFailed(
            @RequestParam(defaultValue = "20") Integer limit) {
        return sqlStatisticsService.getMostFailed(limit);
    }

    /**
     * 清理过期统计数据
     */
    @DeleteMapping("/clean-expired")
    public int cleanExpiredStatistics(
            @RequestParam(defaultValue = "30") int retentionDays) {
        return sqlStatisticsService.cleanExpiredStatistics(retentionDays);
    }

    /**
     * 重置所有统计数据
     */
    @DeleteMapping("/reset")
    public void resetAllStatistics() {
        sqlStatisticsService.resetAllStatistics();
    }

    /**
     * 根据SQL MD5删除统计
     */
    @DeleteMapping("/by-md5/{sqlMd5}")
    public void deleteBySqlMd5(@PathVariable String sqlMd5) {
        sqlStatisticsService.deleteBySqlMd5(sqlMd5);
    }

    /**
     * 分析指定SQL并获取优化建议
     */
    @GetMapping("/analyze/{sqlMd5}")
    public List<SqlOptimizationSuggestion> analyzeSql(@PathVariable String sqlMd5) {
        return sqlStatisticsService.analyzeSql(sqlMd5);
    }

    /**
     * 分析慢查询并获取优化建议
     */
    @GetMapping("/analyze/slow-queries")
    public List<SqlOptimizationSuggestion> analyzeSlowQueries(
            @RequestParam(defaultValue = "1000") Long minAvgTime,
            @RequestParam(defaultValue = "20") Integer limit) {
        return sqlStatisticsService.analyzeSlowQueries(minAvgTime, limit);
    }

    /**
     * 获取所有优化建议
     */
    @GetMapping("/optimization-suggestions")
    public List<SqlOptimizationSuggestion> getAllOptimizationSuggestions() {
        return sqlStatisticsService.getAllOptimizationSuggestions();
    }
}
