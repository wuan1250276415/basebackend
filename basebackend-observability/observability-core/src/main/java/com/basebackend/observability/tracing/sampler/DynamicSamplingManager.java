package com.basebackend.observability.tracing.sampler;

import com.basebackend.observability.tracing.config.TracingProperties;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 动态采样管理器
 * <p>
 * 根据实际 Span 生成速率自动调整采样率，以达到目标 Spans/分钟。
 * 使用后台调度线程定期计算和更新采样率。
 * </p>
 * <p>
 * 核心功能：
 * <ul>
 *     <li><b>自适应采样率</b>：根据观察到的 Span 速率自动调整</li>
 *     <li><b>速率限制</b>：限制采样率在 [minRate, maxRate] 范围内</li>
 *     <li><b>平滑调整</b>：使用比例调整算法，避免剧烈波动</li>
 *     <li><b>线程安全</b>：使用 AtomicReference 实现无锁采样器切换</li>
 *     <li><b>资源管理</b>：支持优雅关闭，释放后台线程</li>
 * </ul>
 * </p>
 * <p>
 * 调整算法：
 * <pre>
 * observedPerMinute = spansSinceLast / (intervalMs / 60000)
 * newRate = clamp(currentRate * targetPerMinute / observedPerMinute, minRate, maxRate)
 * </pre>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 配置
 * observability:
 *   tracing:
 *     sampler:
 *       dynamic:
 *         enabled: true
 *         min-rate: 0.01        # 最小 1%
 *         max-rate: 1.0         # 最大 100%
 *         target-spans-per-minute: 1000  # 目标 1000 spans/分钟
 *         adjust-interval: 30s  # 每 30 秒调整一次
 *
 * // 使用
 * DynamicSamplingManager manager = new DynamicSamplingManager(config);
 * manager.start();
 * Sampler sampler = manager.getCurrentSampler();  // 获取当前采样器
 * manager.recordSpan();  // 记录已采样的 Span
 * manager.shutdown();  // 优雅关闭
 * }</pre>
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>调整间隔不应过短（建议 ≥ 30s），避免频繁波动</li>
 *     <li>目标速率应基于后端存储能力和成本考虑</li>
 *     <li>动态采样率仅影响根 Span，子 Span 继承父级决策</li>
 *     <li>必须调用 {@link #start()} 启动后台线程</li>
 *     <li>应在应用关闭时调用 {@link #shutdown()} 释放资源</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/otel/trace/sdk/#sampler">OpenTelemetry Sampler Specification</a>
 */
public final class DynamicSamplingManager {

    private static final Logger log = LoggerFactory.getLogger(DynamicSamplingManager.class);

    private final double minRate;
    private final double maxRate;
    private final long targetSpansPerMinute;
    private final long adjustIntervalMs;

    private final AtomicReference<Sampler> currentSampler;
    private final AtomicLong spanCount = new AtomicLong(0);
    private volatile long lastAdjustmentTime = System.currentTimeMillis();

    private ScheduledExecutorService scheduler;
    private volatile boolean started = false;

    /**
     * 构造函数
     *
     * @param dynamicConfig 动态采样配置
     */
    public DynamicSamplingManager(TracingProperties.Sampler.Dynamic dynamicConfig) {
        this.minRate = Math.max(0.0, Math.min(1.0, dynamicConfig.getMinRate()));
        this.maxRate = Math.max(this.minRate, Math.min(1.0, dynamicConfig.getMaxRate()));
        this.targetSpansPerMinute = Math.max(1, dynamicConfig.getTargetSpansPerMinute());

        // 解析调整间隔
        Duration interval = dynamicConfig.getAdjustInterval();
        this.adjustIntervalMs = (interval != null) ? interval.toMillis() : 30_000L;

        // 初始采样率：先从配置获取，再 clamp 到 [minRate, maxRate] 区间
        double initialRate = dynamicConfig.getInitialRate();
        // 将 initialRate 限制在 [minRate, maxRate] 区间内
        initialRate = Math.max(this.minRate, Math.min(this.maxRate, initialRate));
        // 如果 clamp 后仍然无效（可能配置全为 0 或负数），回退到 minRate
        if (initialRate <= 0 || initialRate > 1.0) {
            initialRate = this.minRate;
        }
        double finalInitialRate = initialRate;
        this.currentSampler = new AtomicReference<>(Sampler.traceIdRatioBased(finalInitialRate));

        log.info("动态采样管理器已初始化: minRate={}, maxRate={}, targetSpansPerMinute={}, adjustInterval={}ms, initialRate={} (clamped from config={})",
                minRate, maxRate, targetSpansPerMinute, adjustIntervalMs, initialRate, dynamicConfig.getInitialRate());
    }

    /**
     * 启动后台调整线程
     * <p>
     * 创建一个单线程的 ScheduledExecutorService，定期调整采样率。
     * </p>
     */
    public synchronized void start() {
        if (started) {
            log.warn("动态采样管理器已启动，跳过重复启动");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "otel-dynamic-sampling");
            t.setDaemon(true);  // 守护线程，不阻止 JVM 关闭
            return t;
        });

        scheduler.scheduleAtFixedRate(
                this::adjustSamplingRate,
                adjustIntervalMs,
                adjustIntervalMs,
                TimeUnit.MILLISECONDS
        );

        started = true;
        log.info("动态采样管理器后台线程已启动");
    }

    /**
     * 优雅关闭
     * <p>
     * 停止后台调整线程并等待其完成。
     * </p>
     */
    public synchronized void shutdown() {
        if (!started || scheduler == null) {
            return;
        }

        log.info("正在关闭动态采样管理器...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info("动态采样管理器已关闭");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            log.warn("关闭动态采样管理器时被中断", ex);
        } finally {
            started = false;
        }
    }

    /**
     * 获取当前采样器
     * <p>
     * 线程安全，可以在多线程环境下调用。
     * </p>
     *
     * @return 当前采样器
     */
    public Sampler getCurrentSampler() {
        return currentSampler.get();
    }

    /**
     * 记录一个已采样的 Span
     * <p>
     * 调用此方法以通知管理器有新的 Span 被采样。
     * 通常由采样器或 SpanProcessor 调用。
     * </p>
     */
    public void recordSpan() {
        spanCount.incrementAndGet();
    }

    /**
     * 调整采样率（后台线程调用）
     * <p>
     * 计算观察到的 Span 速率，并根据目标速率调整采样率。
     * </p>
     */
    private void adjustSamplingRate() {
        try {
            long now = System.currentTimeMillis();
            long elapsedMs = now - lastAdjustmentTime;

            // 避免除零
            if (elapsedMs <= 0) {
                return;
            }

            long spansSinceLast = spanCount.getAndSet(0);
            lastAdjustmentTime = now;

            // 计算观察到的 Span 速率（每分钟）
            double observedPerMinute = (double) spansSinceLast / elapsedMs * 60_000.0;

            // 获取当前采样率（从 Sampler 描述中提取）
            double currentRate = extractCurrentRate();

            // 计算新采样率（比例调整）
            double newRate;
            if (observedPerMinute > 0) {
                newRate = currentRate * targetSpansPerMinute / observedPerMinute;
            } else {
                // 没有观察到 Span，增加采样率
                newRate = Math.min(currentRate * 1.5, maxRate);
            }

            // 限制在 [minRate, maxRate] 范围内
            newRate = Math.max(minRate, Math.min(maxRate, newRate));

            // 如果变化显著（> 5%），更新采样器
            // 避免除零：确保 currentRate > 0
            boolean shouldUpdate = false;
            if (currentRate > 0) {
                shouldUpdate = Math.abs(newRate - currentRate) / currentRate > 0.05;
            } else {
                // currentRate 为 0，任何非零的 newRate 都应该更新
                shouldUpdate = newRate > 0;
            }

            if (shouldUpdate) {
                Sampler newSampler = Sampler.traceIdRatioBased(newRate);
                currentSampler.set(newSampler);

                log.info("动态调整采样率: observedSpansPerMinute={}, currentRate={}, newRate={}",
                        String.format("%.2f", observedPerMinute),
                        String.format("%.4f", currentRate),
                        String.format("%.4f", newRate));
            } else {
                log.debug("采样率变化不显著，跳过更新: observedSpansPerMinute={}, currentRate={}",
                        String.format("%.2f", observedPerMinute),
                        String.format("%.4f", currentRate));
            }

        } catch (Exception ex) {
            log.error("调整采样率失败", ex);
        }
    }

    /**
     * 从当前 Sampler 描述中提取采样率
     * <p>
     * TraceIdRatioBasedSampler 的描述格式为 "TraceIdRatioBased{ratio}"。
     * </p>
     *
     * @return 当前采样率，解析失败则返回 minRate
     */
    private double extractCurrentRate() {
        try {
            Sampler sampler = currentSampler.get();
            String description = sampler.getDescription();

            // 解析 "TraceIdRatioBased{0.1234}" 格式
            if (description.startsWith("TraceIdRatioBased{")) {
                int start = description.indexOf('{') + 1;
                int end = description.indexOf('}', start);
                if (start > 0 && end > start) {
                    String rateStr = description.substring(start, end);
                    return Double.parseDouble(rateStr);
                }
            }

            log.warn("无法从 Sampler 描述中提取采样率: description={}, 使用 minRate", description);
            return minRate;

        } catch (Exception ex) {
            log.warn("提取当前采样率失败，使用 minRate", ex);
            return minRate;
        }
    }
}
