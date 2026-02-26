package com.basebackend.observability.otel.exporter;

import com.basebackend.observability.otel.config.OtelProperties;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * OTLP 指标导出器工厂
 * <p>
 * 创建配置好的 gRPC 指标导出器，用于将指标数据发送到支持 OTLP 协议的后端。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "observability.otel.otlp.metrics", name = "enabled", havingValue = "true")
public class OtlpMetricsExporter {

    private static final Logger log = LoggerFactory.getLogger(OtlpMetricsExporter.class);

    private final OtelProperties properties;

    public OtlpMetricsExporter(OtelProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 OTLP gRPC 指标导出器
     * <p>
     * 配置项包括：
     * <ul>
     *     <li>端点地址：从 observability.otel.otlp.endpoint 读取</li>
     *     <li>超时时间：10秒</li>
     *     <li>压缩：gzip（性能优化）</li>
     * </ul>
     * </p>
     *
     * @return 配置好的指标导出器实例
     */
    public MetricExporter create() {
        String endpoint = resolveEndpoint();

        log.info("正在配置 OTLP 指标导出器，端点: {}", endpoint);

        return OtlpGrpcMetricExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofSeconds(10))
                .setCompression("gzip")
                .build();
    }

    private String resolveEndpoint() {
        String endpoint = properties.getOtlp().getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            endpoint = "http://localhost:4317";
            log.warn("未配置 OTLP 端点，使用默认值: {}", endpoint);
        }
        return endpoint;
    }
}
