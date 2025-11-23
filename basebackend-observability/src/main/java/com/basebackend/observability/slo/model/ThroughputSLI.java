package com.basebackend.observability.slo.model;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 吞吐量 SLI 实现
 * <p>
 * 计算公式：吞吐量 = 总请求数 / 运行时间（秒）
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class ThroughputSLI implements SLI {

    private static final Logger log = LoggerFactory.getLogger(ThroughputSLI.class);

    private final Instant startTime;
    private final Clock clock;

    /**
     * 使用系统时钟创建实例
     */
    public ThroughputSLI() {
        this(Clock.systemUTC());
    }

    /**
     * 使用指定时钟创建实例（用于测试）
     *
     * @param clock 时钟实例
     */
    public ThroughputSLI(Clock clock) {
        this.clock = clock;
        this.startTime = clock.instant();
    }

    @Override
    public double calculate(MeterRegistry registry, String service, String method, String sloName) {
        Counter total = registry.find("sli_requests_total")
                .tags("service", service, "method", method, "slo", sloName, "outcome", "total")
                .counter();

        if (total == null) {
            log.debug("指标未初始化: service={}, method={}, slo={}", service, method, sloName);
            return 0d;
        }

        double count = total.count();
        Duration elapsed = Duration.between(startTime, clock.instant());

        if (elapsed.isZero() || elapsed.isNegative()) {
            return 0d;
        }

        // 返回每秒请求数
        return count / elapsed.toSeconds();
    }
}
