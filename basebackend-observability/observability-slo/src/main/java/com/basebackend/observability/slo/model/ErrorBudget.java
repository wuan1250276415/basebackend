package com.basebackend.observability.slo.model;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错误预算模型
 * <p>
 * 错误预算 = (1 - SLO目标) × 总请求数
 * <br>
 * 例如：99.9% 可用性的 SLO，在 100万请求中允许 1000 次失败
 * </p>
 * <p>
 * 该类追踪错误预算的消耗情况和 Burn Rate（消耗速率）
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class ErrorBudget {

    /**
     * 总错误预算
     */
    private double totalBudget;

    /**
     * 已消耗的错误预算
     */
    private double consumedBudget;

    /**
     * 剩余错误预算
     */
    private double remainingBudget;

    /**
     * 各时间窗口的 Burn Rate
     * <p>
     * Burn Rate = 实际错误率 / 允许错误率
     * <br>
     * 值越大表示错误预算消耗越快，> 1 表示超出预算消耗速度
     * </p>
     */
    private final Map<Duration, Double> burnRates = new ConcurrentHashMap<>();

    public ErrorBudget(double totalBudget, double consumedBudget, double remainingBudget) {
        this.totalBudget = totalBudget;
        this.consumedBudget = consumedBudget;
        this.remainingBudget = remainingBudget;
    }

    public double getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(double totalBudget) {
        this.totalBudget = totalBudget;
    }

    public double getConsumedBudget() {
        return consumedBudget;
    }

    public void setConsumedBudget(double consumedBudget) {
        this.consumedBudget = consumedBudget;
    }

    public double getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(double remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public Map<Duration, Double> getBurnRates() {
        return burnRates;
    }

    public void putBurnRate(Duration window, double rate) {
        burnRates.put(window, rate);
    }

    public double getBurnRate(Duration window) {
        return burnRates.getOrDefault(window, 0d);
    }

    @Override
    public String toString() {
        return "ErrorBudget{" +
                "total=" + totalBudget +
                ", consumed=" + consumedBudget +
                ", remaining=" + remainingBudget +
                ", burnRates=" + burnRates +
                '}';
    }
}
