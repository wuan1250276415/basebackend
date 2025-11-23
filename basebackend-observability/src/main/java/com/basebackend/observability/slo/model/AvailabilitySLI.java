package com.basebackend.observability.slo.model;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 可用性 SLI 实现
 * <p>
 * 计算公式：可用性 = 成功请求数 / 总请求数
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class AvailabilitySLI implements SLI {

    private static final Logger log = LoggerFactory.getLogger(AvailabilitySLI.class);

    @Override
    public double calculate(MeterRegistry registry, String service, String method, String sloName) {
        Counter total = registry.find("sli_requests_total")
                .tags("service", service, "method", method, "slo", sloName, "outcome", "total")
                .counter();
        Counter success = registry.find("sli_requests_total")
                .tags("service", service, "method", method, "slo", sloName, "outcome", "success")
                .counter();

        if (total == null || success == null) {
            log.debug("指标未初始化: service={}, method={}, slo={}", service, method, sloName);
            return 1.0d;  // 默认100%可用
        }

        double totalCount = total.count();
        if (totalCount == 0) {
            return 1.0d;
        }

        double availability = success.count() / totalCount;
        return Math.min(Math.max(availability, 0d), 1d);
    }
}
