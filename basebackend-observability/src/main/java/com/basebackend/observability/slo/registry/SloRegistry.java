package com.basebackend.observability.slo.registry;

import com.basebackend.observability.slo.calculator.ErrorBudgetTracker;
import com.basebackend.observability.slo.calculator.SloCalculator;
import com.basebackend.observability.slo.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SLO 注册表
 * <p>
 * 集中管理所有 SLO 定义和对应的 SLI 实现，提供完整的 SLO 生命周期管理功能：
 * <ul>
 *     <li>SLO 注册/注销</li>
 *     <li>SLI 计算</li>
 *     <li>SLO 合规性评估</li>
 *     <li>错误预算跟踪</li>
 *     <li>Burn Rate 监控</li>
 * </ul>
 * </p>
 * <p>
 * 线程安全：使用 {@link ConcurrentHashMap} 实现，支持高并发场景。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 注册 SLO
 * SLO slo = new SLO();
 * slo.setName("user-api-availability");
 * slo.setType(SloType.AVAILABILITY);
 * slo.setTarget(0.995);
 * sloRegistry.register(slo);
 *
 * // 计算 SLI
 * double sli = sloRegistry.calculateSli("user-api-availability", "user-service", "UserController.getUser");
 *
 * // 检查合规性
 * boolean compliant = sloRegistry.isCompliant("user-api-availability", "user-service", "UserController.getUser");
 *
 * // 更新错误预算
 * ErrorBudget budget = sloRegistry.updateErrorBudget("user-api-availability", 1000, 5);
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class SloRegistry {

    private static final Logger log = LoggerFactory.getLogger(SloRegistry.class);

    private final MeterRegistry meterRegistry;
    private final SloCalculator sloCalculator;
    private final ErrorBudgetTracker errorBudgetTracker;

    /**
     * SLO 定义映射：名称 -> SLO
     */
    private final Map<String, SLO> slos = new ConcurrentHashMap<>();

    /**
     * SLI 实例映射：名称 -> SLI
     */
    private final Map<String, SLI> slis = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param meterRegistry      Micrometer 指标注册表
     * @param sloCalculator      SLO 合规性计算器
     * @param errorBudgetTracker 错误预算跟踪器
     */
    public SloRegistry(MeterRegistry meterRegistry,
                       SloCalculator sloCalculator,
                       ErrorBudgetTracker errorBudgetTracker) {
        this.meterRegistry = meterRegistry;
        this.sloCalculator = sloCalculator;
        this.errorBudgetTracker = errorBudgetTracker;
        log.info("SLO 注册表已初始化");
    }

    /**
     * 注册 SLO
     * <p>
     * 根据 SLO 类型自动创建对应的 SLI 实现：
     * <ul>
     *     <li>AVAILABILITY -> {@link AvailabilitySLI}</li>
     *     <li>LATENCY -> {@link LatencySLI}</li>
     *     <li>ERROR_RATE -> {@link ErrorRateSLI}</li>
     *     <li>THROUGHPUT -> {@link ThroughputSLI}</li>
     * </ul>
     * </p>
     * <p>
     * 如果同名 SLO 已存在，会被新定义覆盖。
     * </p>
     *
     * @param slo SLO 定义
     */
    public void register(SLO slo) {
        if (slo == null || slo.getName() == null || slo.getName().trim().isEmpty()) {
            log.warn("无效 SLO 定义，忽略注册: {}", slo);
            return;
        }

        String name = slo.getName().trim();
        slos.put(name, slo);
        slis.put(name, createSli(slo));

        log.info("注册 SLO: name={}, type={}, target={}, window={}",
                name, slo.getType(), slo.getTarget(), slo.getWindow());
    }

    /**
     * 注销 SLO
     * <p>
     * 同时清除关联的 SLI 实例和错误预算数据。
     * </p>
     *
     * @param name SLO 名称
     */
    public void unregister(String name) {
        if (name == null) {
            log.debug("尝试注销 null 名称的 SLO，忽略");
            return;
        }

        SLO removed = slos.remove(name);
        slis.remove(name);
        errorBudgetTracker.clear(name);

        if (removed != null) {
            log.info("注销 SLO: name={}", name);
        } else {
            log.debug("SLO 不存在，无需注销: name={}", name);
        }
    }

    /**
     * 获取 SLO 定义
     *
     * @param name SLO 名称
     * @return SLO 定义，如果不存在则返回 null
     */
    public SLO get(String name) {
        return name == null ? null : slos.get(name);
    }

    /**
     * 获取所有 SLO 定义
     * <p>
     * 返回的集合是不可修改的视图，避免外部修改内部状态。
     * </p>
     *
     * @return 所有 SLO 定义的集合
     */
    public Collection<SLO> getAll() {
        return Collections.unmodifiableCollection(slos.values());
    }

    /**
     * 计算指定 SLO 的 SLI 值
     * <p>
     * 根据 SLO 类型调用对应的 SLI 实现进行计算：
     * <ul>
     *     <li>AVAILABILITY：计算成功率（0-1）</li>
     *     <li>LATENCY：计算延迟百分位（毫秒）</li>
     *     <li>ERROR_RATE：计算错误率（0-1）</li>
     *     <li>THROUGHPUT：计算吞吐量（请求/秒）</li>
     * </ul>
     * </p>
     *
     * @param name    SLO 名称
     * @param service 服务名
     * @param method  方法名
     * @return SLI 值，如果 SLO 不存在或计算失败则返回 0
     */
    public double calculateSli(String name, String service, String method) {
        SLI sli = slis.get(name);
        if (sli == null) {
            log.debug("SLO 未注册，无法计算 SLI: name={}", name);
            return 0d;
        }

        try {
            double value = sli.calculate(meterRegistry, service, method, name);
            log.trace("计算 SLI: name={}, service={}, method={}, value={}",
                    name, service, method, value);
            return value;
        } catch (Exception ex) {
            log.warn("计算 SLI 失败: name={}, service={}, method={}",
                    name, service, method, ex);
            return 0d;
        }
    }

    /**
     * 计算 SLO 合规性
     * <p>
     * 合规性 = 实际 SLI / 目标 SLI（根据 SLO 类型调整计算逻辑）。
     * <br>
     * 返回值 >= 1.0 表示达标，< 1.0 表示未达标。
     * </p>
     *
     * @param name    SLO 名称
     * @param service 服务名
     * @param method  方法名
     * @return 合规性比率，如果 SLO 不存在则返回 0
     */
    public double calculateCompliance(String name, String service, String method) {
        SLO slo = slos.get(name);
        if (slo == null) {
            log.debug("SLO 未注册，无法计算合规性: name={}", name);
            return 0d;
        }

        double observedSli = calculateSli(name, service, method);
        double compliance = sloCalculator.compliance(slo, observedSli);

        log.debug("计算合规性: name={}, observedSli={}, compliance={}",
                name, observedSli, compliance);

        return compliance;
    }

    /**
     * 判断 SLO 是否达标
     * <p>
     * 基于合规性计算结果，>= 1.0 表示达标。
     * </p>
     *
     * @param name    SLO 名称
     * @param service 服务名
     * @param method  方法名
     * @return true 表示达标，false 表示未达标
     */
    public boolean isCompliant(String name, String service, String method) {
        return calculateCompliance(name, service, method) >= 1.0d;
    }

    /**
     * 获取 SLO 的当前错误预算
     *
     * @param name SLO 名称
     * @return 错误预算对象，如果不存在则返回 null
     */
    public ErrorBudget getErrorBudget(String name) {
        return errorBudgetTracker.get(name);
    }

    /**
     * 更新 SLO 的错误预算
     * <p>
     * 根据总事件数和错误事件数，计算并更新错误预算：
     * <ul>
     *     <li>总预算 = 总事件数 × (1 - 目标)</li>
     *     <li>已消耗 = 错误事件数</li>
     *     <li>剩余预算 = 总预算 - 已消耗</li>
     * </ul>
     * </p>
     * <p>
     * 同时计算所有配置的时间窗口的 Burn Rate。
     * </p>
     *
     * @param name        SLO 名称
     * @param totalEvents 总事件数
     * @param errorEvents 错误事件数
     * @return 更新后的错误预算，如果 SLO 不存在则返回 null
     */
    public ErrorBudget updateErrorBudget(String name, double totalEvents, double errorEvents) {
        SLO slo = slos.get(name);
        if (slo == null) {
            log.warn("SLO 未注册，无法更新错误预算: name={}", name);
            return null;
        }

        ErrorBudget budget = errorBudgetTracker.update(slo, totalEvents, errorEvents);

        log.debug("更新错误预算: name={}, total={}, error={}, remaining={}",
                name, totalEvents, errorEvents,
                budget != null ? budget.getRemainingBudget() : 0);

        return budget;
    }

    /**
     * 获取 SLO 的所有时间窗口 Burn Rate
     * <p>
     * Burn Rate 表示错误预算消耗速率，用于预警 SLO 违反：
     * <ul>
     *     <li>Burn Rate = 实际错误率 / 允许错误率</li>
     *     <li>Burn Rate > 1 表示消耗速度超出预期</li>
     * </ul>
     * </p>
     *
     * @param name SLO 名称
     * @return 时间窗口到 Burn Rate 的映射，如果错误预算不存在则返回空 Map
     */
    public Map<Duration, Double> getBurnRates(String name) {
        ErrorBudget budget = getErrorBudget(name);
        return budget == null ? Collections.emptyMap() : budget.getBurnRates();
    }

    /**
     * 清空所有 SLO
     * <p>
     * 移除所有 SLO 定义、SLI 实例和错误预算数据。
     * </p>
     */
    public void clear() {
        int count = slos.size();
        slos.clear();
        slis.clear();
        errorBudgetTracker.clearAll();
        log.info("已清空所有 SLO 定义与预算: count={}", count);
    }

    /**
     * 获取已注册的 SLO 数量
     *
     * @return SLO 数量
     */
    public int count() {
        return slos.size();
    }

    /**
     * 判断指定名称的 SLO 是否存在
     *
     * @param name SLO 名称
     * @return true 表示存在，false 表示不存在
     */
    public boolean exists(String name) {
        return slos.containsKey(name);
    }

    /**
     * 根据 SLO 定义创建对应的 SLI 实例
     * <p>
     * 创建失败时会回退到默认的 {@link AvailabilitySLI} 实现。
     * </p>
     *
     * @param slo SLO 定义
     * @return SLI 实例
     */
    private SLI createSli(SLO slo) {
        try {
            SloType type = slo.getType() == null ? SloType.AVAILABILITY : slo.getType();
            switch (type) {
                case LATENCY:
                    double percentile = slo.getPercentile() == null ? 0.95d : slo.getPercentile();
                    log.debug("创建 LatencySLI: slo={}, percentile={}", slo.getName(), percentile);
                    return new LatencySLI(percentile);

                case ERROR_RATE:
                    log.debug("创建 ErrorRateSLI: slo={}", slo.getName());
                    return new ErrorRateSLI();

                case THROUGHPUT:
                    log.debug("创建 ThroughputSLI: slo={}", slo.getName());
                    return new ThroughputSLI();

                case AVAILABILITY:
                default:
                    log.debug("创建 AvailabilitySLI: slo={}", slo.getName());
                    return new AvailabilitySLI();
            }
        } catch (Exception ex) {
            log.warn("创建 SLI 实例失败，使用默认可用性实现: slo={}", slo.getName(), ex);
            return new AvailabilitySLI();
        }
    }
}
