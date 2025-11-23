package com.basebackend.database.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL优化建议模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlOptimizationSuggestion {

    /**
     * SQL的MD5值
     */
    private String sqlMd5;

    /**
     * 问题严重程度
     */
    private Severity severity;

    /**
     * 问题分类
     */
    private Category category;

    /**
     * 问题描述
     */
    private String issue;

    /**
     * 优化建议
     */
    private String suggestion;

    /**
     * 执行计划（可选）
     */
    private String executionPlan;

    /**
     * 严重程度枚举
     */
    public enum Severity {
        /**
         * 低 - 建议优化，但不紧急
         */
        LOW,

        /**
         * 中 - 应该优化
         */
        MEDIUM,

        /**
         * 高 - 需要立即优化
         */
        HIGH
    }

    /**
     * 问题分类枚举
     */
    public enum Category {
        /**
         * 索引相关
         */
        INDEX,

        /**
         * 查询结构
         */
        QUERY_STRUCTURE,

        /**
         * 性能问题
         */
        PERFORMANCE,

        /**
         * 缓存建议
         */
        CACHING,

        /**
         * 错误处理
         */
        ERROR
    }
}
