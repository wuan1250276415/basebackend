package com.basebackend.observability.slo.model;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 错误率 SLI 实现
 * <p>
 * 计算公式：错误率 = 错误请求数 / 总请求数
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class ErrorRateSLI implements SLI {

    private static final Logger log = LoggerFactory.getLogger(ErrorRateSLI.class);

    @Override
    public double calculate(MeterRegistry registry, String service, String method, String sloName) {
        Counter total = registry.find("sli_requests_total")
                .tags("service", service, "method", method, "slo", sloName, "outcome", "total")
                .counter();
        Counter errors = registry.find("sli_requests_total")
                .tags("service", service, "method", method, "slo", sloName, "outcome", "error")
                .counter();

        if (total == null || errors == null) {
            log.debug("指标未初始化: service={}, method={}, slo={}", service, method, sloName);
            return 0d;  // 默认无错误
        }

        double totalCount = total.count();
        if (totalCount == 0) {
            return 0d;
        }

        double errorRate = errors.count() / totalCount;
        return Math.min(Math.max(errorRate, 0d), 1d);
    }
}
