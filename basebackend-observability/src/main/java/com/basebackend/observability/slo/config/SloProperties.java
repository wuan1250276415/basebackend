package com.basebackend.observability.slo.config;

import com.basebackend.observability.slo.model.SloType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SLO 配置属性
 * <p>
 * 从配置文件中读取 SLO 定义，支持多种 SLO 类型和灵活的配置选项。
 * </p>
 * <p>
 * 配置示例：
 * <pre>{@code
 * observability:
 *   slo:
 *     enabled: true
 *     slos:
 *       - name: user-api-availability
 *         type: AVAILABILITY
 *         target: 0.995          # 99.5%
 *         window: 30d
 *         burn-rate-windows:
 *           - 1h
 *           - 6h
 *           - 24h
 *       - name: payment-latency-p95
 *         type: LATENCY
 *         target: 100.0          # 100ms
 *         percentile: 0.95
 *         window: 30d
 *       - name: order-error-rate
 *         type: ERROR_RATE
 *         target: 0.01           # 1%
 *         window: 7d
 *       - name: api-throughput
 *         type: THROUGHPUT
 *         target: 100.0          # 100 req/s
 *         window: 1d
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "observability.slo")
public class SloProperties {

    /**
     * 是否启用 SLO 功能
     * <p>
     * 默认为 false。启用后，会激活 SLO 监控切面和指标导出。
     * </p>
     */
    private boolean enabled = false;

    /**
     * SLO 定义列表
     * <p>
     * 每个 SLO 定义包含名称、类型、目标值、评估窗口等配置。
     * </p>
     */
    private List<SloDefinition> slos = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<SloDefinition> getSlos() {
        return slos;
    }

    public void setSlos(List<SloDefinition> slos) {
        this.slos = slos;
    }

    /**
     * 单个 SLO 定义
     * <p>
     * 表示一个服务级别目标（Service Level Objective），包含目标值、评估窗口、
     * Burn Rate 窗口等配置。
     * </p>
     */
    public static final class SloDefinition {

        /**
         * SLO 名称
         * <p>
         * 唯一标识符，用于在监控系统中引用该 SLO。
         * </p>
         * <p>
         * 示例：user-api-availability, payment-latency-p95, order-error-rate
         * </p>
         */
        private String name;

        /**
         * SLO 类型
         * <p>
         * 支持的类型：
         * <ul>
         *     <li>AVAILABILITY：可用性（成功率）</li>
         *     <li>LATENCY：延迟（响应时间）</li>
         *     <li>ERROR_RATE：错误率</li>
         *     <li>THROUGHPUT：吞吐量（请求/秒）</li>
         * </ul>
         * </p>
         */
        private SloType type = SloType.AVAILABILITY;

        /**
         * 目标值
         * <p>
         * 根据不同的 SLO 类型，目标值的含义不同：
         * <ul>
         *     <li>AVAILABILITY：成功率（0-1），如 0.995 表示 99.5%</li>
         *     <li>LATENCY：延迟阈值（毫秒），如 100.0 表示 100ms</li>
         *     <li>ERROR_RATE：错误率（0-1），如 0.01 表示 1%</li>
         *     <li>THROUGHPUT：吞吐量（请求/秒），如 100.0 表示 100 req/s</li>
         * </ul>
         * </p>
         */
        private double target;

        /**
         * 评估窗口
         * <p>
         * SLO 的统计周期，默认为 30 天。
         * </p>
         * <p>
         * 示例：30d, 7d, 24h, 1h
         * </p>
         */
        private Duration window = Duration.ofDays(30);

        /**
         * 延迟百分位（仅用于 LATENCY 类型）
         * <p>
         * 指定计算延迟时使用的百分位，如 0.95 表示 P95（95th percentile）。
         * </p>
         * <p>
         * 常用值：
         * <ul>
         *     <li>0.50：P50（中位数）</li>
         *     <li>0.95：P95</li>
         *     <li>0.99：P99</li>
         * </ul>
         * </p>
         */
        private Double percentile;

        /**
         * Burn Rate 评估窗口列表
         * <p>
         * 用于多窗口 Burn Rate 监控。建议配置短期、中期、长期窗口，
         * 以便及时发现不同时间尺度的 SLO 违反风险。
         * </p>
         * <p>
         * 推荐配置：
         * <ul>
         *     <li>短期窗口：1h（快速检测突发问题）</li>
         *     <li>中期窗口：6h（检测持续性问题）</li>
         *     <li>长期窗口：24h（趋势分析）</li>
         * </ul>
         * </p>
         */
        private List<Duration> burnRateWindows = new ArrayList<>();

        /**
         * 服务名（用于 SLI 计算）
         * <p>
         * 指定该 SLO 监控的服务名称。如果为空，则使用应用名称。
         * 必须与 @SloMonitored 注解中的 service 参数匹配。
         * </p>
         * <p>
         * 示例：user-service, order-service
         * </p>
         */
        private String service;

        /**
         * 方法名（用于 SLI 计算）
         * <p>
         * 指定该 SLO 监控的方法名称。如果为空，则使用 SLO 名称作为方法名。
         * 必须与实际监控的方法名格式匹配（格式：类名.方法名）。
         * </p>
         * <p>
         * 示例：UserController.getUser, OrderService.createOrder
         * </p>
         * <p>
         * <b>注意：</b>如果一个 SLO 监控多个方法，建议为每个方法创建单独的 SLO 定义，
         * 或者使用 SLO 名称作为统一的聚合维度。
         * </p>
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
}
