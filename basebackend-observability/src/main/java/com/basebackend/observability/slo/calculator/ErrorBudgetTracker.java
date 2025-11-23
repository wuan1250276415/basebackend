package com.basebackend.observability.slo.calculator;

import com.basebackend.observability.slo.model.ErrorBudget;
import com.basebackend.observability.slo.model.SLO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错误预算跟踪器
 * <p>
 * 维护每个 SLO 的错误预算消耗情况，并计算各时间窗口的 Burn Rate。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Component
public class ErrorBudgetTracker {

    private static final Logger log = LoggerFactory.getLogger(ErrorBudgetTracker.class);

    private final BurnRateCalculator burnRateCalculator;
    private final Map<String, ErrorBudget> budgets = new ConcurrentHashMap<>();

    public ErrorBudgetTracker(BurnRateCalculator burnRateCalculator) {
        this.burnRateCalculator = burnRateCalculator;
    }

    /**
     * 更新 SLO 的错误预算
     *
     * @param slo         SLO 定义
     * @param totalEvents 总事件数
     * @param errorEvents 错误事件数
     * @return 更新后的错误预算
     */
    public ErrorBudget update(SLO slo, double totalEvents, double errorEvents) {
        if (slo == null) {
            log.warn("SLO 为 null，无法更新错误预算");
            return new ErrorBudget(0, 0, 0);
        }

        if (totalEvents <= 0) {
            // 无事件，返回初始预算
            return budgets.computeIfAbsent(slo.getName(), k -> new ErrorBudget(0, 0, 0));
        }

        // 计算错误预算
        double totalBudget = totalEvents * (1 - slo.getTarget());
        double consumed = Math.min(errorEvents, totalBudget);
        double remaining = Math.max(totalBudget - consumed, 0d);

        // 获取或创建错误预算对象
        ErrorBudget budget = budgets.computeIfAbsent(
                slo.getName(),
                k -> new ErrorBudget(totalBudget, consumed, remaining)
        );

        // 更新预算值
        budget.setTotalBudget(totalBudget);
        budget.setConsumedBudget(consumed);
        budget.setRemainingBudget(remaining);

        // 计算各时间窗口的 Burn Rate
        if (slo.getBurnRateWindows() != null && !slo.getBurnRateWindows().isEmpty()) {
            double errorRate = errorEvents / totalEvents;

            for (Duration window : slo.getBurnRateWindows()) {
                double burnRate = burnRateCalculator.calculate(slo, errorRate, window);
                budget.putBurnRate(window, burnRate);
            }
        }

        log.debug("更新错误预算: slo={}, total={}, error={}, budget={}",
                slo.getName(), totalEvents, errorEvents, budget);

        return budget;
    }

    /**
     * 获取 SLO 的当前错误预算
     *
     * @param sloName SLO 名称
     * @return 错误预算，如果不存在则返回 null
     */
    public ErrorBudget get(String sloName) {
        return budgets.get(sloName);
    }

    /**
     * 获取所有错误预算
     *
     * @return SLO 名称到错误预算的映射
     */
    public Map<String, ErrorBudget> getAllBudgets() {
        return new ConcurrentHashMap<>(budgets);
    }

    /**
     * 清除指定 SLO 的错误预算
     *
     * @param sloName SLO 名称
     */
    public void clear(String sloName) {
        budgets.remove(sloName);
        log.info("清除错误预算: slo={}", sloName);
    }

    /**
     * 清除所有错误预算
     */
    public void clearAll() {
        budgets.clear();
        log.info("清除所有错误预算");
    }
}
