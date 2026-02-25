package com.basebackend.observability.slo.config;

import com.basebackend.observability.slo.calculator.ErrorBudgetTracker;
import com.basebackend.observability.slo.calculator.SloCalculator;
import com.basebackend.observability.slo.model.ErrorBudget;
import com.basebackend.observability.slo.model.SLO;
import com.basebackend.observability.slo.registry.SloRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Duration;
import java.util.Map;

/**
 * SLO 自动配置
 * <p>
 * 负责：
 * <ul>
 *     <li>启用 AOP 切面（用于 @SloMonitored 注解）</li>
 *     <li>加载 SLO 配置属性</li>
 *     <li>创建和初始化 SloRegistry</li>
 *     <li>绑定 SLO 指标到 Prometheus</li>
 * </ul>
 * </p>
 * <p>
 * 仅在配置 {@code observability.slo.enabled=true} 时生效。
 * </p>
 * <p>
 * 导出的 Prometheus 指标：
 * <ul>
 *     <li><b>slo_compliance</b>：SLO 合规性（>= 1 表示达标）</li>
 *     <li><b>slo_error_budget_remaining</b>：剩余错误预算</li>
 *     <li><b>slo_burn_rate</b>：Burn Rate（按时间窗口）</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see SloProperties
 * @see SloRegistry
 */
@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(SloProperties.class)
@ConditionalOnProperty(prefix = "observability.slo", name = "enabled", havingValue = "true")
public class SloConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SloConfiguration.class);

    /**
     * 创建 SLO 注册表
     * <p>
     * 从配置文件加载 SLO 定义并注册到注册表中。
     * </p>
     *
     * @param meterRegistry      Micrometer 指标注册表
     * @param sloCalculator      SLO 合规性计算器
     * @param errorBudgetTracker 错误预算跟踪器
     * @param sloProperties      SLO 配置属性
     * @return SloRegistry 实例
     */
    @Bean
    public SloRegistry sloRegistry(MeterRegistry meterRegistry,
                                   SloCalculator sloCalculator,
                                   ErrorBudgetTracker errorBudgetTracker,
                                   SloProperties sloProperties) {
        SloRegistry registry = new SloRegistry(meterRegistry, sloCalculator, errorBudgetTracker);

        // 从配置文件加载并注册 SLO
        if (sloProperties.getSlos() != null && !sloProperties.getSlos().isEmpty()) {
            log.info("开始加载 SLO 定义: count={}", sloProperties.getSlos().size());

            sloProperties.getSlos().forEach(def -> {
                try {
                    SLO slo = toSlo(def);
                    registry.register(slo);
                } catch (Exception ex) {
                    log.error("加载 SLO 定义失败: name={}", def.getName(), ex);
                }
            });

            log.info("SLO 定义加载完成: registered={}", registry.count());
        } else {
            log.warn("未配置任何 SLO 定义，SLO 监控将不会生效");
        }

        return registry;
    }

    /**
     * 创建 SLO 指标绑定器
     * <p>
     * 将 SLO 相关指标绑定到 Micrometer，使其可以通过 Prometheus 导出。
     * </p>
     *
     * @param sloRegistry     SLO 注册表
     * @param applicationName 应用名称（从 spring.application.name 读取）
     * @return MeterBinder 实例
     */
    @Bean
    public MeterBinder sloMetricsBinder(SloRegistry sloRegistry,
                                        @Value("${spring.application.name:application}") String applicationName) {
        log.info("创建 SLO 指标绑定器: applicationName={}", applicationName);
        return new SloMetricsBinder(sloRegistry, applicationName);
    }

    /**
     * 将 SloProperties.SloDefinition 转换为 SLO 模型
     *
     * @param def SLO 定义
     * @return SLO 模型对象
     */
    private SLO toSlo(SloProperties.SloDefinition def) {
        SLO slo = new SLO();
        slo.setName(def.getName());
        slo.setType(def.getType());
        slo.setTarget(def.getTarget());
        slo.setWindow(def.getWindow());
        slo.setPercentile(def.getPercentile());
        slo.setBurnRateWindows(def.getBurnRateWindows());
        slo.setService(def.getService());
        slo.setMethod(def.getMethod());
        return slo;
    }

    /**
     * SLO 指标绑定器
     * <p>
     * 实现 {@link MeterBinder} 接口，将 SLO 指标注册到 Micrometer。
     * 为每个 SLO 创建以下 Gauge 指标：
     * <ul>
     *     <li>slo_compliance：合规性（>= 1 表示达标）</li>
     *     <li>slo_error_budget_remaining：剩余错误预算</li>
     *     <li>slo_burn_rate：Burn Rate（每个时间窗口一个）</li>
     * </ul>
     * </p>
     */
    static class SloMetricsBinder implements MeterBinder {

        private static final Logger log = LoggerFactory.getLogger(SloMetricsBinder.class);

        private final SloRegistry sloRegistry;
        private final String defaultService;

        /**
         * 构造函数
         *
         * @param sloRegistry    SLO 注册表
         * @param defaultService 默认服务名（用于 SLI 计算）
         */
        SloMetricsBinder(SloRegistry sloRegistry, String defaultService) {
            this.sloRegistry = sloRegistry;
            this.defaultService = defaultService;
        }

        /**
         * 绑定 SLO 指标到 MeterRegistry
         * <p>
         * 遍历所有已注册的 SLO，为每个 SLO 创建合规性、错误预算和 Burn Rate 指标。
         * </p>
         *
         * @param meterRegistry Micrometer 指标注册表
         */
        @Override
        public void bindTo(MeterRegistry meterRegistry) {
            log.info("开始绑定 SLO 指标: count={}", sloRegistry.count());

            sloRegistry.getAll().forEach(slo -> {
                try {
                    bindSloMetrics(meterRegistry, slo);
                } catch (Exception ex) {
                    log.error("绑定 SLO 指标失败: slo={}", slo.getName(), ex);
                }
            });

            log.info("SLO 指标绑定完成");
        }

        /**
         * 为单个 SLO 绑定指标
         *
         * @param meterRegistry Micrometer 指标注册表
         * @param slo           SLO 定义
         */
        private void bindSloMetrics(MeterRegistry meterRegistry, SLO slo) {
            // 基础标签：所有指标共用
            Tags baseTags = Tags.of(
                    "slo", slo.getName(),
                    "type", slo.getType().name(),
                    "target", Double.toString(slo.getTarget())
            );

            // 确定用于计算的 service 和 method
            // 如果 SLO 定义中指定了 service/method，则使用指定值
            // 否则使用默认值（service=应用名，method=SLO名）
            String service = slo.getService() != null && !slo.getService().trim().isEmpty()
                    ? slo.getService().trim()
                    : defaultService;
            String method = slo.getMethod() != null && !slo.getMethod().trim().isEmpty()
                    ? slo.getMethod().trim()
                    : slo.getName();

            // 1. 绑定合规性指标
            Gauge.builder("slo_compliance",
                            () -> sloRegistry.calculateCompliance(slo.getName(), service, method))
                    .description("SLO 合规性指标（>= 1.0 表示达标）")
                    .tags(baseTags)
                    .register(meterRegistry);

            // 2. 绑定剩余错误预算指标
            Gauge.builder("slo_error_budget_remaining",
                            () -> {
                                ErrorBudget budget = sloRegistry.getErrorBudget(slo.getName());
                                return budget == null ? 0d : budget.getRemainingBudget();
                            })
                    .description("SLO 剩余错误预算")
                    .tags(baseTags)
                    .register(meterRegistry);

            // 3. 绑定 Burn Rate 指标（每个时间窗口一个）
            bindBurnRateMetrics(meterRegistry, slo, baseTags);

            log.debug("SLO 指标绑定成功: slo={}, service={}, method={}", slo.getName(), service, method);
        }

        /**
         * 为 SLO 的所有 Burn Rate 窗口绑定指标
         * <p>
         * 每个时间窗口对应一个独立的 Gauge 指标，通过 window 标签区分。
         * </p>
         *
         * @param meterRegistry Micrometer 指标注册表
         * @param slo           SLO 定义
         * @param baseTags      基础标签
         */
        private void bindBurnRateMetrics(MeterRegistry meterRegistry, SLO slo, Tags baseTags) {
            // 获取当前错误预算（可能为空，首次启动时尚未更新）
            ErrorBudget budget = sloRegistry.getErrorBudget(slo.getName());
            if (budget == null || budget.getBurnRates().isEmpty()) {
                // 如果错误预算尚未初始化，根据配置的窗口创建占位符指标
                if (slo.getBurnRateWindows() != null && !slo.getBurnRateWindows().isEmpty()) {
                    for (Duration window : slo.getBurnRateWindows()) {
                        Tags windowTags = Tags.concat(baseTags, "window", formatDuration(window));
                        Gauge.builder("slo_burn_rate",
                                        () -> {
                                            ErrorBudget currentBudget = sloRegistry.getErrorBudget(slo.getName());
                                            if (currentBudget == null) {
                                                return 0d;
                                            }
                                            return currentBudget.getBurnRates().getOrDefault(window, 0d);
                                        })
                                .description("SLO Burn Rate（错误预算消耗速率）")
                                .tags(windowTags)
                                .register(meterRegistry);
                    }
                }
                return;
            }

            // 为每个 Burn Rate 窗口创建 Gauge
            for (Map.Entry<Duration, Double> entry : budget.getBurnRates().entrySet()) {
                Duration window = entry.getKey();
                Tags windowTags = Tags.concat(baseTags, "window", formatDuration(window));

                Gauge.builder("slo_burn_rate",
                                () -> {
                                    ErrorBudget currentBudget = sloRegistry.getErrorBudget(slo.getName());
                                    if (currentBudget == null) {
                                        return 0d;
                                    }
                                    return currentBudget.getBurnRates().getOrDefault(window, 0d);
                                })
                        .description("SLO Burn Rate（错误预算消耗速率）")
                        .tags(windowTags)
                        .register(meterRegistry);

                log.trace("绑定 Burn Rate 指标: slo={}, window={}", slo.getName(), window);
            }
        }

        /**
         * 格式化 Duration 为易读字符串
         * <p>
         * 示例：PT1H -> 1h, PT6H -> 6h, P1D -> 1d
         * </p>
         *
         * @param duration 时间段
         * @return 格式化后的字符串
         */
        private String formatDuration(Duration duration) {
            if (duration == null) {
                return "0s";
            }

            long hours = duration.toHours();
            if (hours > 0 && hours % 24 == 0) {
                return (hours / 24) + "d";
            } else if (hours > 0) {
                return hours + "h";
            }

            long minutes = duration.toMinutes();
            if (minutes > 0) {
                return minutes + "m";
            }

            return duration.toSeconds() + "s";
        }
    }
}
