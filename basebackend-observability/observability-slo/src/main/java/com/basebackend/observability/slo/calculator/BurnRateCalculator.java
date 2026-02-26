package com.basebackend.observability.slo.calculator;

import com.basebackend.observability.slo.model.SLO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Burn Rate 计算器
 * <p>
 * Burn Rate 是错误预算消耗速率的度量，用于预警 SLO 违反。
 * <br>
 * 计算公式：Burn Rate = 实际错误率 / 允许错误率
 * </p>
 * <p>
 * 举例：
 * <ul>
 *     <li>SLO = 99.9%，允许错误率 = 0.1%</li>
 *     <li>实际错误率 = 0.2%</li>
 *     <li>Burn Rate = 0.2% / 0.1% = 2.0（消耗速率是正常的2倍）</li>
 * </ul>
 * </p>
 * <p>
 * Burn Rate > 1 表示错误预算消耗速度超出预期，需要告警。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Component
public class BurnRateCalculator {

    private static final Logger log = LoggerFactory.getLogger(BurnRateCalculator.class);

    /**
     * 计算指定时间窗口的 Burn Rate
     *
     * @param slo       SLO 定义
     * @param errorRate 观测到的错误率（0-1）
     * @param window    评估时间窗口
     * @return Burn Rate 值
     */
    public double calculate(SLO slo, double errorRate, Duration window) {
        if (slo == null || window == null || window.isZero() || window.isNegative()) {
            log.debug("无效参数: slo={}, window={}", slo, window);
            return 0d;
        }

        if (errorRate < 0) {
            log.warn("错误率不能为负: errorRate={}", errorRate);
            return 0d;
        }

        // 计算允许的错误率
        double allowedError = 1 - slo.getTarget();

        if (allowedError <= 0) {
            // SLO 目标为 100%，理论上不允许任何错误
            log.debug("SLO 目标为 100%: slo={}", slo.getName());
            return errorRate > 0 ? Double.MAX_VALUE : 0d;
        }

        // 计算 Burn Rate
        double burnRate = errorRate / allowedError;
        burnRate = Math.max(burnRate, 0d);

        log.debug("计算 Burn Rate: slo={}, window={}, errorRate={}, allowedError={}, burnRate={}",
                slo.getName(), window, errorRate, allowedError, burnRate);

        return burnRate;
    }

    /**
     * 判断 Burn Rate 是否超过阈值
     *
     * @param burnRate  Burn Rate 值
     * @param threshold 阈值（通常设置为 1.0 或更高）
     * @return true 表示超过阈值，需要告警
     */
    public boolean exceedsThreshold(double burnRate, double threshold) {
        return burnRate > threshold;
    }
}
