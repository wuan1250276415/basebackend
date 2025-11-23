package com.basebackend.database.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basebackend.database.statistics.analyzer.SqlPerformanceAnalyzer;
import com.basebackend.database.statistics.entity.SqlStatistics;
import com.basebackend.database.statistics.mapper.SqlStatisticsMapper;
import com.basebackend.database.statistics.model.SqlOptimizationSuggestion;
import com.basebackend.database.statistics.query.SqlStatisticsQuery;
import com.basebackend.database.statistics.service.SqlStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL统计服务实现
 */
@Slf4j
@Service
public class SqlStatisticsServiceImpl extends ServiceImpl<SqlStatisticsMapper, SqlStatistics>
        implements SqlStatisticsService {

    private final SqlPerformanceAnalyzer performanceAnalyzer;

    public SqlStatisticsServiceImpl(SqlPerformanceAnalyzer performanceAnalyzer) {
        this.performanceAnalyzer = performanceAnalyzer;
    }

    @Override
    public Page<SqlStatistics> query(SqlStatisticsQuery query) {
        Page<SqlStatistics> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<SqlStatistics> wrapper = new LambdaQueryWrapper<>();

        // Apply filters
        if (query.getSqlTemplate() != null && !query.getSqlTemplate().isEmpty()) {
            wrapper.like(SqlStatistics::getSqlTemplate, query.getSqlTemplate());
        }

        if (query.getDataSourceName() != null && !query.getDataSourceName().isEmpty()) {
            wrapper.eq(SqlStatistics::getDataSourceName, query.getDataSourceName());
        }

        if (query.getTenantId() != null && !query.getTenantId().isEmpty()) {
            wrapper.eq(SqlStatistics::getTenantId, query.getTenantId());
        }

        if (query.getMinExecuteCount() != null) {
            wrapper.ge(SqlStatistics::getExecuteCount, query.getMinExecuteCount());
        }

        if (query.getMinAvgTime() != null) {
            wrapper.ge(SqlStatistics::getAvgTime, query.getMinAvgTime());
        }

        if (query.getStartTime() != null) {
            wrapper.ge(SqlStatistics::getLastExecuteTime, query.getStartTime());
        }

        if (query.getEndTime() != null) {
            wrapper.le(SqlStatistics::getLastExecuteTime, query.getEndTime());
        }

        // Apply sorting
        String orderBy = query.getOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(query.getOrderDirection());

        switch (orderBy) {
            case "executeCount":
                wrapper.orderBy(true, isAsc, SqlStatistics::getExecuteCount);
                break;
            case "avgTime":
                wrapper.orderBy(true, isAsc, SqlStatistics::getAvgTime);
                break;
            case "maxTime":
                wrapper.orderBy(true, isAsc, SqlStatistics::getMaxTime);
                break;
            case "totalTime":
                wrapper.orderBy(true, isAsc, SqlStatistics::getTotalTime);
                break;
            case "failCount":
                wrapper.orderBy(true, isAsc, SqlStatistics::getFailCount);
                break;
            default:
                wrapper.orderByDesc(SqlStatistics::getExecuteCount);
        }

        return this.page(page, wrapper);
    }

    @Override
    public SqlStatistics getById(Long id) {
        return this.baseMapper.selectById(id);
    }

    @Override
    public List<SqlStatistics> getSlowQueries(Long minAvgTime, Integer limit) {
        LambdaQueryWrapper<SqlStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SqlStatistics::getAvgTime, minAvgTime)
                .orderByDesc(SqlStatistics::getAvgTime)
                .last("LIMIT " + limit);

        return this.list(wrapper);
    }

    @Override
    public List<SqlStatistics> getMostExecuted(Integer limit) {
        LambdaQueryWrapper<SqlStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SqlStatistics::getExecuteCount)
                .last("LIMIT " + limit);

        return this.list(wrapper);
    }

    @Override
    public List<SqlStatistics> getMostFailed(Integer limit) {
        LambdaQueryWrapper<SqlStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(SqlStatistics::getFailCount, 0)
                .orderByDesc(SqlStatistics::getFailCount)
                .last("LIMIT " + limit);

        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredStatistics(int retentionDays) {
        LocalDateTime expirationDate = LocalDateTime.now().minusDays(retentionDays);

        LambdaQueryWrapper<SqlStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SqlStatistics::getLastExecuteTime, expirationDate);

        long count = this.count(wrapper);
        this.remove(wrapper);

        log.info("Cleaned {} expired SQL statistics records", count);
        return (int) count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetAllStatistics() {
        this.baseMapper.delete(null);
        log.info("Reset all SQL statistics");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBySqlMd5(String sqlMd5) {
        LambdaQueryWrapper<SqlStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SqlStatistics::getSqlMd5, sqlMd5);
        this.remove(wrapper);
        log.info("Deleted SQL statistics for MD5: {}", sqlMd5);
    }

    @Override
    public List<SqlOptimizationSuggestion> analyzeSql(String sqlMd5) {
        LambdaQueryWrapper<SqlStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SqlStatistics::getSqlMd5, sqlMd5);
        SqlStatistics statistics = this.getOne(wrapper);

        if (statistics == null) {
            log.warn("SQL statistics not found for MD5: {}", sqlMd5);
            return new ArrayList<>();
        }

        return performanceAnalyzer.analyze(statistics);
    }

    @Override
    public List<SqlOptimizationSuggestion> analyzeSlowQueries(Long minAvgTime, Integer limit) {
        List<SqlStatistics> slowQueries = getSlowQueries(minAvgTime, limit);
        return performanceAnalyzer.analyzeBatch(slowQueries);
    }

    @Override
    public List<SqlOptimizationSuggestion> getAllOptimizationSuggestions() {
        // Analyze top slow queries
        List<SqlStatistics> slowQueries = getSlowQueries(1000L, 50);
        
        // Analyze most executed queries
        List<SqlStatistics> frequentQueries = getMostExecuted(50);
        
        // Analyze failed queries
        List<SqlStatistics> failedQueries = getMostFailed(20);
        
        // Combine all statistics
        List<SqlStatistics> allStatistics = new ArrayList<>();
        allStatistics.addAll(slowQueries);
        
        // Add frequent queries that are not already in the list
        for (SqlStatistics stats : frequentQueries) {
            if (!containsSqlMd5(allStatistics, stats.getSqlMd5())) {
                allStatistics.add(stats);
            }
        }
        
        // Add failed queries that are not already in the list
        for (SqlStatistics stats : failedQueries) {
            if (!containsSqlMd5(allStatistics, stats.getSqlMd5())) {
                allStatistics.add(stats);
            }
        }
        
        return performanceAnalyzer.analyzeBatch(allStatistics);
    }

    /**
     * Check if list contains SQL with given MD5
     */
    private boolean containsSqlMd5(List<SqlStatistics> list, String sqlMd5) {
        return list.stream().anyMatch(s -> s.getSqlMd5().equals(sqlMd5));
    }
}
