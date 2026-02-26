package com.basebackend.observability.slo.model;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 延迟 SLI 实现
 * <p>
 * 基于百分位数计算延迟指标（P50/P95/P99）
 * </p>
 * <p>
 * <b>重要：</b>需要精确匹配配置的百分位数，不支持最近匹配。
 * 如果指标未配置所需的百分位数，将记录警告并返回 0。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class LatencySLI implements SLI {

    private static final Logger log = LoggerFactory.getLogger(LatencySLI.class);

    /**
     * 百分位数精确匹配的容差
     * <p>
     * 由于浮点数精度问题，允许 0.001 的误差
     * 例如：请求 0.95 时，0.9499 ~ 0.9501 都被认为是匹配的
     * </p>
     */
    private static final double PERCENTILE_TOLERANCE = 0.001;

    private final double percentile;

    /**
     * 创建延迟 SLI 实例
     *
     * @param percentile 百分位数（0.5=P50, 0.95=P95, 0.99=P99）
     */
    public LatencySLI(double percentile) {
        if (percentile <= 0 || percentile >= 1) {
            throw new IllegalArgumentException("百分位数必须在 0 到 1 之间");
        }
        this.percentile = percentile;
    }

    @Override
    public double calculate(MeterRegistry registry, String service, String method, String sloName) {
        Timer timer = registry.find("sli_latency")
                .tags("service", service, "method", method, "slo", sloName)
                .timer();

        if (timer == null) {
            log.debug("Timer 未初始化: service={}, method={}, slo={}", service, method, sloName);
            return 0d;
        }

        if (timer.count() == 0) {
            return 0d;
        }

        // 获取指定百分位数的延迟值（纳秒转毫秒）
        // 注意：新版 Micrometer 中使用 percentileValues() 代替 percentileValue()
        var snapshot = timer.takeSnapshot();
        var percentileValues = snapshot.percentileValues();

        // 要求精确匹配目标百分位（允许小的浮点数误差）
        double valueNanos = 0d;
        boolean found = false;

        if (percentileValues != null && percentileValues.length > 0) {
            for (var pv : percentileValues) {
                double diff = Math.abs(pv.percentile() - percentile);
                if (diff <= PERCENTILE_TOLERANCE) {
                    // 精确匹配（在容差范围内）
                    valueNanos = pv.value(TimeUnit.NANOSECONDS);
                    found = true;
                    break;
                }
            }

            if (!found) {
                // 未找到匹配的百分位数，记录警告
                log.warn("未找到配置的百分位数 P{}: service={}, method={}, slo={}, 可用百分位: {}",
                        (int) (percentile * 100), service, method, sloName,
                        formatAvailablePercentiles(percentileValues));
                return 0d;
            }
        } else {
            // 百分位数未配置
            log.warn("Timer 未配置百分位数: service={}, method={}, slo={}, 需要启用 percentile histogram",
                    service, method, sloName);
            return 0d;
        }

        if (Double.isNaN(valueNanos) || valueNanos == 0d) {
            return 0d;
        }

        return TimeUnit.NANOSECONDS.toMillis((long) valueNanos);
    }

    /**
     * 格式化可用的百分位数列表（用于日志输出）
     */
    private String formatAvailablePercentiles(io.micrometer.core.instrument.distribution.ValueAtPercentile[] percentileValues) {
        if (percentileValues == null || percentileValues.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < percentileValues.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("P").append((int) (percentileValues[i].percentile() * 100));
        }
        sb.append("]");
        return sb.toString();
    }
}
