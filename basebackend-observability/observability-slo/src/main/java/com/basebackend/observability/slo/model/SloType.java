package com.basebackend.observability.slo.model;

/**
 * SLO 类型枚举
 * <p>
 * 定义支持的服务级别指标类型
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public enum SloType {
    /**
     * 可用性 SLO - 成功请求数 / 总请求数
     */
    AVAILABILITY,

    /**
     * 延迟 SLO - 基于百分位数（P50/P95/P99）
     */
    LATENCY,

    /**
     * 错误率 SLO - 错误请求数 / 总请求数
     */
    ERROR_RATE,

    /**
     * 吞吐量 SLO - 请求数/秒
     */
    THROUGHPUT
}
