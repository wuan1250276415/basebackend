package com.basebackend.observability.slo.model;

import java.time.Duration;
import java.util.List;

/**
 * 服务级别目标 (SLO) 定义
 * <p>
 * SLO 定义了服务应该达到的目标水平，包括：
 * <ul>
 *     <li>目标值（如 99.9% 可用性）</li>
 *     <li>评估时间窗口（如 30天）</li>
 *     <li>关联的 SLI 类型</li>
 *     <li>Burn Rate 告警窗口</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class SLO {

    /**
     * SLO 名称（唯一标识）
     */
    private String name;

    /**
     * SLO 类型
     */
    private SloType type = SloType.AVAILABILITY;

    /**
     * 目标值
     * <ul>
     *     <li>可用性/错误率：0-1 之间的小数（如 0.999 表示 99.9%）</li>
     *     <li>延迟：毫秒数（如 100.0 表示 100ms）</li>
     *     <li>吞吐量：请求数/秒（如 1000.0 表示 1000 qps）</li>
     * </ul>
     */
    private double target;

    /**
     * 评估时间窗口（如 30天）
     */
    private Duration window = Duration.ofDays(30);

    /**
     * 延迟百分位数（仅用于 LATENCY 类型）
     * <p>如 0.95 表示 P95，0.99 表示 P99</p>
     */
    private Double percentile;

    /**
     * Burn Rate 告警窗口列表
     * <p>用于计算多个时间窗口的错误预算消耗速率</p>
     */
    private List<Duration> burnRateWindows;

    /**
     * 服务名（用于 SLI 计算）
     * <p>如果为空，则使用默认应用名称</p>
     */
    private String service;

    /**
     * 方法名（用于 SLI 计算）
     * <p>如果为空，则使用 SLO 名称作为方法名</p>
     */
    private String method;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SloType getType() {
        return type;
    }

    public void setType(SloType type) {
        this.type = type;
    }

    public double getTarget() {
        return target;
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public Double getPercentile() {
        return percentile;
    }

    public void setPercentile(Double percentile) {
        this.percentile = percentile;
    }

    public List<Duration> getBurnRateWindows() {
        return burnRateWindows;
    }

    public void setBurnRateWindows(List<Duration> burnRateWindows) {
        this.burnRateWindows = burnRateWindows;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
