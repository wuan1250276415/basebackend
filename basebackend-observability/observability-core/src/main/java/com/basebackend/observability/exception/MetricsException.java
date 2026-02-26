package com.basebackend.observability.exception;

/**
 * 指标相关异常
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class MetricsException extends ObservabilityException {

    public MetricsException(String message) {
        super(message, ErrorCode.METRICS_COLLECTION_FAILED);
    }

    public MetricsException(String message, Throwable cause) {
        super(message, ErrorCode.METRICS_COLLECTION_FAILED, cause);
    }

    public MetricsException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public static MetricsException collectionFailed(String metricName, Throwable cause) {
        return new MetricsException("Failed to collect metric: " + metricName, cause);
    }

    public static MetricsException exportFailed(String destination, Throwable cause) {
        return new MetricsException("Failed to export metrics to: " + destination,
                ErrorCode.METRICS_EXPORT_FAILED);
    }
}
