package com.basebackend.database.statistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.database.statistics.entity.SqlStatistics;
import com.basebackend.database.statistics.model.SqlOptimizationSuggestion;
import com.basebackend.database.statistics.query.SqlStatisticsQuery;

import java.util.List;

/**
 * SQL统计服务接口
 */
public interface SqlStatisticsService {

    /**
     * 分页查询SQL统计
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<SqlStatistics> query(SqlStatisticsQuery query);

    /**
     * 根据ID查询SQL统计
     *
     * @param id 统计ID
     * @return SQL统计
     */
    SqlStatistics getById(Long id);

    /**
     * 获取慢查询列表（按平均执行时间排序）
     *
     * @param minAvgTime 最小平均执行时间（毫秒）
     * @param limit 返回数量限制
     * @return 慢查询列表
     */
    List<SqlStatistics> getSlowQueries(Long minAvgTime, Integer limit);

    /**
     * 获取执行次数最多的SQL列表
     *
     * @param limit 返回数量限制
     * @return SQL列表
     */
    List<SqlStatistics> getMostExecuted(Integer limit);

    /**
     * 获取失败次数最多的SQL列表
     *
     * @param limit 返回数量限制
     * @return SQL列表
     */
    List<SqlStatistics> getMostFailed(Integer limit);

    /**
     * 清理过期统计数据
     *
     * @param retentionDays 保留天数
     * @return 清理的记录数
     */
    int cleanExpiredStatistics(int retentionDays);

    /**
     * 重置所有统计数据
     */
    void resetAllStatistics();

    /**
     * 根据SQL MD5删除统计
     *
     * @param sqlMd5 SQL MD5
     */
    void deleteBySqlMd5(String sqlMd5);

    /**
     * 分析SQL性能并获取优化建议
     *
     * @param sqlMd5 SQL MD5
     * @return 优化建议列表
     */
    List<SqlOptimizationSuggestion> analyzeSql(String sqlMd5);

    /**
     * 批量分析慢查询并获取优化建议
     *
     * @param minAvgTime 最小平均执行时间（毫秒）
     * @param limit 分析数量限制
     * @return 优化建议列表
     */
    List<SqlOptimizationSuggestion> analyzeSlowQueries(Long minAvgTime, Integer limit);

    /**
     * 获取所有需要优化的SQL建议
     *
     * @return 优化建议列表
     */
    List<SqlOptimizationSuggestion> getAllOptimizationSuggestions();
}
