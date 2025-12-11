package com.basebackend.observability.constants;

/**
 * 可观测性服务常量
 * <p>
 * 统一管理所有常量值，避免魔法数字。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class ObservabilityConstants {

    private ObservabilityConstants() {
        throw new UnsupportedOperationException("常量类不能实例化");
    }

    // ========== 时间常量 ==========

    /** 1分钟（毫秒） */
    public static final long ONE_MINUTE_MS = 60_000L;

    /** 1小时（毫秒） */
    public static final long ONE_HOUR_MS = 3_600_000L;

    /** 1天（毫秒） */
    public static final long ONE_DAY_MS = 86_400_000L;

    // ========== 追踪格式 ==========

    /** Zipkin格式 */
    public static final String TRACE_FORMAT_ZIPKIN = "zipkin";

    /** Tempo格式 */
    public static final String TRACE_FORMAT_TEMPO = "tempo";

    // ========== API路径 ==========

    /** Zipkin API路径 */
    public static final String ZIPKIN_API_V2 = "/api/v2";

    /** Zipkin追踪查询路径 */
    public static final String ZIPKIN_TRACES_PATH = "/traces";

    /** Zipkin服务列表路径 */
    public static final String ZIPKIN_SERVICES_PATH = "/services";

    /** Zipkin Span名称路径 */
    public static final String ZIPKIN_SPANS_PATH = "/spans";

    /** Prometheus查询路径 */
    public static final String PROMETHEUS_QUERY_PATH = "/api/v1/query";

    /** Prometheus范围查询路径 */
    public static final String PROMETHEUS_QUERY_RANGE_PATH = "/api/v1/query_range";

    /** Loki查询路径 */
    public static final String LOKI_QUERY_PATH = "/loki/api/v1/query";

    /** Loki范围查询路径 */
    public static final String LOKI_QUERY_RANGE_PATH = "/loki/api/v1/query_range";

    // ========== 默认值 ==========

    /** 默认查询限制 */
    public static final int DEFAULT_LIMIT = 100;

    /** 默认日志查询限制 */
    public static final int DEFAULT_LOG_LIMIT = 1000;

    /** 最大查询限制 */
    public static final int MAX_LIMIT = 10000;

    /** 默认查询步长（秒） */
    public static final int DEFAULT_STEP_SECONDS = 60;

    // ========== 缓存名称 ==========

    /** 服务列表缓存 */
    public static final String CACHE_SERVICES = "obs-services";

    /** 追踪数据缓存 */
    public static final String CACHE_TRACES = "obs-traces";

    /** 指标数据缓存 */
    public static final String CACHE_METRICS = "obs-metrics";

    /** 日志数据缓存 */
    public static final String CACHE_LOGS = "obs-logs";

    // ========== 错误消息 ==========

    /** 服务不可用消息 */
    public static final String ERROR_SERVICE_UNAVAILABLE = "外部服务暂不可用，请稍后重试";

    /** 查询超时消息 */
    public static final String ERROR_QUERY_TIMEOUT = "查询超时，请缩小查询范围";

    /** 参数错误消息 */
    public static final String ERROR_INVALID_PARAMS = "请求参数无效";
}
