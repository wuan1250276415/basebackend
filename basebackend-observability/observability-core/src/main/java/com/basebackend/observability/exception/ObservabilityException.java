package com.basebackend.observability.exception;

import lombok.Getter;

/**
 * 可观测性相关异常基类
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Getter
public class ObservabilityException extends RuntimeException {

    private final ErrorCode errorCode;

    public ObservabilityException(String message) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN;
    }

    public ObservabilityException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ObservabilityException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN;
    }

    public ObservabilityException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        /** 未知错误 */
        UNKNOWN("OBS_0000", "未知错误"),

        /** 指标相关 */
        METRICS_COLLECTION_FAILED("OBS_1001", "指标收集失败"),
        METRICS_EXPORT_FAILED("OBS_1002", "指标导出失败"),

        /** 告警相关 */
        ALERT_RULE_INVALID("OBS_2001", "告警规则无效"),
        ALERT_EVALUATION_FAILED("OBS_2002", "告警评估失败"),
        ALERT_NOTIFICATION_FAILED("OBS_2003", "告警通知发送失败"),

        /** 追踪相关 */
        TRACING_INIT_FAILED("OBS_3001", "追踪初始化失败"),
        SPAN_EXPORT_FAILED("OBS_3002", "Span导出失败"),

        /** SLO相关 */
        SLO_CALCULATION_FAILED("OBS_4001", "SLO计算失败"),
        ERROR_BUDGET_EXCEEDED("OBS_4002", "错误预算超出"),

        /** 配置相关 */
        CONFIG_INVALID("OBS_5001", "配置无效"),
        CONFIG_LOAD_FAILED("OBS_5002", "配置加载失败");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
