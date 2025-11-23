package com.basebackend.observability.slo.calculator;

import com.basebackend.observability.slo.model.SLO;
import com.basebackend.observability.slo.model.SloType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SLO 合规性计算器
 * <p>
 * 根据 SLO 目标和观测到的 SLI 值，计算合规性得分。
 * 合规性 >= 1.0 表示达标，< 1.0 表示未达标。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Component
public class SloCalculator {

    private static final Logger log = LoggerFactory.getLogger(SloCalculator.class);

    /**
     * 计算 SLO 合规性
     *
     * @param slo         SLO 定义
     * @param observedSli 观测到的 SLI 值
     * @return 合规性比率（>= 0）
     */
    public double compliance(SLO slo, double observedSli) {
        if (slo == null) {
            log.warn("SLO 为 null，返回 0 合规性");
            return 0d;
        }

        double compliance;

        // 根据不同类型的 SLO 计算合规性
        switch (slo.getType()) {
            case LATENCY:
                // 延迟：观测值越小越好，合规性 = 目标 / 观测值
                if (observedSli <= 0) {
                    compliance = 1d;  // 无延迟，完全合规
                } else {
                    compliance = slo.getTarget() / observedSli;
                }
                break;

            case THROUGHPUT:
                // 吞吐量：观测值越大越好，合规性 = 观测值 / 目标
                compliance = observedSli >= slo.getTarget() ? 1d : observedSli / slo.getTarget();
                break;

            case ERROR_RATE:
                // 错误率：观测值越小越好
                // 合规性 = (1 - 观测值) / (1 - 目标)
                double allowedErrorRate = slo.getTarget();
                if (observedSli <= allowedErrorRate) {
                    compliance = 1d;  // 低于目标错误率，完全合规
                } else {
                    compliance = allowedErrorRate / observedSli;
                }
                break;

            case AVAILABILITY:
            default:
                // 可用性：观测值越大越好，合规性 = 观测值 / 目标
                compliance = observedSli / slo.getTarget();
                break;
        }

        // 确保合规性非负
        compliance = Math.max(compliance, 0d);

        log.debug("计算合规性: slo={}, type={}, target={}, observed={}, compliance={}",
                slo.getName(), slo.getType(), slo.getTarget(), observedSli, compliance);

        return compliance;
    }

    /**
     * 判断 SLO 是否达标
     *
     * @param slo         SLO 定义
     * @param observedSli 观测到的 SLI 值
     * @return true 表示达标，false 表示未达标
     */
    public boolean isCompliant(SLO slo, double observedSli) {
        return compliance(slo, observedSli) >= 1.0d;
    }
}
