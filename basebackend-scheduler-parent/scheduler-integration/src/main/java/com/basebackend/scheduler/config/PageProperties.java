package com.basebackend.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分页配置属性
 * <p>
 * 提供分页参数的集中配置管理，支持从配置中心动态调整。
 * 配置前缀：scheduler.page
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Data
@Component
@ConfigurationProperties(prefix = "scheduler.page")
public class PageProperties {

    /**
     * 默认页码（从1开始）
     */
    private Integer defaultPageNo = 1;

    /**
     * 默认每页大小
     */
    private Integer defaultPageSize = 20;

    /**
     * 最大每页大小（防止内存溢出和性能问题）
     */
    private Integer maxPageSize = 200;

    /**
     * 最小每页大小
     */
    private Integer minPageSize = 1;

    /**
     * 是否允许查询所有数据（pageSize=-1表示查询所有）
     * <p>
     * 生产环境建议设置为false，避免大量数据查询导致的性能问题
     * </p>
     */
    private Boolean allowQueryAll = false;

    /**
     * 验证页码是否合法
     *
     * @param pageNo 页码
     * @return true表示合法
     */
    public boolean isValidPageNo(Integer pageNo) {
        return pageNo != null && pageNo >= defaultPageNo;
    }

    /**
     * 验证每页大小是否合法
     *
     * @param pageSize 每页大小
     * @return true表示合法
     */
    public boolean isValidPageSize(Integer pageSize) {
        if (pageSize == null) {
            return false;
        }
        // 允许查询所有时，-1也是合法值
        if (allowQueryAll && pageSize == -1) {
            return true;
        }
        return pageSize >= minPageSize && pageSize <= maxPageSize;
    }

    /**
     * 获取安全的页码（确保在合法范围内）
     *
     * @param pageNo 原始页码
     * @return 安全页码
     */
    public int getSafePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < defaultPageNo) {
            return defaultPageNo;
        }
        return pageNo;
    }

    /**
     * 获取安全的每页大小（确保在合法范围内）
     *
     * @param pageSize 原始每页大小
     * @return 安全每页大小
     */
    public int getSafePageSize(Integer pageSize) {
        if (pageSize == null) {
            return defaultPageSize;
        }
        // 允许查询所有时，返回-1
        if (allowQueryAll && pageSize == -1) {
            return -1;
        }
        // 低于最小值，使用默认值
        if (pageSize < minPageSize) {
            return defaultPageSize;
        }
        // 超过最大值，使用最大值
        if (pageSize > maxPageSize) {
            return maxPageSize;
        }
        return pageSize;
    }
}
