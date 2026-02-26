package com.basebackend.observability.otel.bridge;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.search.Search;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer 到 OpenTelemetry 桥接器
 * <p>
 * 将 Micrometer 指标镜像到 OpenTelemetry，实现双栈指标导出。
 * 使用异步观察者模式，避免影响 Micrometer 的记录性能。
 * </p>
 * <p>
 * 支持的指标类型：
 * <ul>
 *     <li>Counter - 计数器</li>
 *     <li>Gauge - 仪表盘</li>
 *     <li>Timer - 计时器</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class MicrometerToOtelBridge {

    private static final Logger log = LoggerFactory.getLogger(MicrometerToOtelBridge.class);

    private final Meter otelMeter;
    private final ConcurrentHashMap<String, Boolean> registered = new ConcurrentHashMap<>();
    private MeterRegistry meterRegistry;  // 保存 MeterRegistry 引用

    /**
     * 创建桥接器实例
     *
     * @param openTelemetry OpenTelemetry 实例，用于发布镜像指标
     */
    public MicrometerToOtelBridge(OpenTelemetry openTelemetry) {
        Objects.requireNonNull(openTelemetry, "openTelemetry 不能为 null");
        this.otelMeter = openTelemetry.getMeter("com.basebackend.observability.bridge");
    }

    /**
     * 绑定到 Micrometer 注册表
     * <p>
     * 该方法会：
     * <ol>
     *     <li>镜像所有已存在的指标</li>
     *     <li>监听新增指标事件，自动镜像</li>
     * </ol>
     * </p>
     *
     * @param registry Micrometer 注册表
     */
    public void bind(MeterRegistry registry) {
        Objects.requireNonNull(registry, "registry 不能为 null");

        // 保存 MeterRegistry 引用
        this.meterRegistry = registry;

        // 镜像现有指标
        registry.forEachMeter(this::registerIfSupported);

        // 监听新增指标
        registry.config().onMeterAdded(this::registerIfSupported);

        log.info("Micrometer → OpenTelemetry 桥接器已附加到注册表: {}",
                registry.getClass().getSimpleName());
    }

    /**
     * 注册支持的指标类型
     *
     * @param meter Micrometer 指标
     */
    private void registerIfSupported(io.micrometer.core.instrument.Meter meter) {
        try {
            // 使用完整的 Meter.Id 作为 key，包含 name + tags + baseUnit
            // 这样可以正确区分相同名称但不同标签的指标
            String key = buildMeterKey(meter.getId());

            // 避免重复注册
            if (registered.putIfAbsent(key, Boolean.TRUE) != null) {
                return;
            }

            switch (meter.getId().getType()) {
                case COUNTER -> registerCounter(meter);
                case GAUGE -> registerGauge(meter);
                case TIMER -> registerTimer(meter);
                default -> log.debug("跳过不支持的指标类型: {} ({})",
                        meter.getId().getName(), meter.getId().getType());
            }
        } catch (Exception ex) {
            log.warn("镜像 Micrometer 指标失败: {}", meter.getId(), ex);
        }
    }

    /**
     * 构建 Meter 的唯一标识 key
     * <p>
     * 包含 name + tags + baseUnit，确保相同名称但不同标签的指标不会冲突
     * </p>
     *
     * @param meterId Meter.Id
     * @return 唯一标识 key
     */
    private String buildMeterKey(io.micrometer.core.instrument.Meter.Id meterId) {
        StringBuilder key = new StringBuilder(meterId.getName());
        key.append(':').append(meterId.getType());

        // 添加所有标签（按 key 排序保证一致性）
        meterId.getTags().stream()
                .sorted((t1, t2) -> t1.getKey().compareTo(t2.getKey()))
                .forEach(tag -> key.append(':').append(tag.getKey()).append('=').append(tag.getValue()));

        // 添加 baseUnit（如果有）
        if (meterId.getBaseUnit() != null) {
            key.append(':').append(meterId.getBaseUnit());
        }

        return key.toString();
    }

    /**
     * 注册 Counter 指标
     */
    private void registerCounter(io.micrometer.core.instrument.Meter meter) {
        if (meterRegistry == null) {
            log.warn("MeterRegistry 未初始化，无法注册 Counter: {}", meter.getId());
            return;
        }

        // 使用完整的 tags 查找精确匹配的 Counter
        var search = Search.in(meterRegistry).name(meter.getId().getName());
        for (var tag : meter.getId().getTags()) {
            search = search.tag(tag.getKey(), tag.getValue());
        }
        Counter counter = search.counter();

        if (counter == null) {
            log.debug("未找到 Counter: {}", meter.getId());
            return;
        }

        Attributes attributes = toAttributes(meter);
        String description = meter.getId().getDescription() != null
                ? meter.getId().getDescription()
                : "Micrometer counter mirrored to OpenTelemetry";

        otelMeter.gaugeBuilder(meter.getId().getName() + ".count")
                .setDescription(description)
                .buildWithCallback(measurement ->
                        measurement.record(counter.count(), attributes));

        log.debug("已镜像 Counter: {}", meter.getId().getName());
    }

    /**
     * 注册 Gauge 指标
     */
    private void registerGauge(io.micrometer.core.instrument.Meter meter) {
        if (meterRegistry == null) {
            log.warn("MeterRegistry 未初始化，无法注册 Gauge: {}", meter.getId());
            return;
        }

        // 使用完整的 tags 查找精确匹配的 Gauge
        var search = Search.in(meterRegistry).name(meter.getId().getName());
        for (var tag : meter.getId().getTags()) {
            search = search.tag(tag.getKey(), tag.getValue());
        }
        Gauge gauge = search.gauge();

        if (gauge == null) {
            log.debug("未找到 Gauge: {}", meter.getId());
            return;
        }

        Attributes attributes = toAttributes(meter);
        String description = meter.getId().getDescription() != null
                ? meter.getId().getDescription()
                : "Micrometer gauge mirrored to OpenTelemetry";

        otelMeter.gaugeBuilder(meter.getId().getName())
                .setDescription(description)
                .buildWithCallback(measurement ->
                        measurement.record(gauge.value(), attributes));

        log.debug("已镜像 Gauge: {}", meter.getId().getName());
    }

    /**
     * 注册 Timer 指标
     */
    private void registerTimer(io.micrometer.core.instrument.Meter meter) {
        if (meterRegistry == null) {
            log.warn("MeterRegistry 未初始化，无法注册 Timer: {}", meter.getId());
            return;
        }

        // 使用完整的 tags 查找精确匹配的 Timer
        var search = Search.in(meterRegistry).name(meter.getId().getName());
        for (var tag : meter.getId().getTags()) {
            search = search.tag(tag.getKey(), tag.getValue());
        }
        Timer timer = search.timer();

        if (timer == null) {
            log.debug("未找到 Timer: {}", meter.getId());
            return;
        }

        Attributes attributes = toAttributes(meter);

        // 总时间（毫秒）
        otelMeter.gaugeBuilder(meter.getId().getName() + ".totalTimeMs")
                .setDescription("Timer 总耗时 (ms)")
                .buildWithCallback(measurement ->
                        measurement.record(timer.totalTime(TimeUnit.MILLISECONDS), attributes));

        // 调用次数
        otelMeter.gaugeBuilder(meter.getId().getName() + ".count")
                .setDescription("Timer 调用次数")
                .buildWithCallback(measurement ->
                        measurement.record(timer.count(), attributes));

        // 最大值（毫秒）
        otelMeter.gaugeBuilder(meter.getId().getName() + ".maxMs")
                .setDescription("Timer 最大耗时 (ms)")
                .buildWithCallback(measurement ->
                        measurement.record(timer.max(TimeUnit.MILLISECONDS), attributes));

        log.debug("已镜像 Timer: {}", meter.getId().getName());
    }

    /**
     * 将 Micrometer 标签转换为 OpenTelemetry 属性
     *
     * @param meter Micrometer 指标
     * @return OpenTelemetry 属性
     */
    private Attributes toAttributes(io.micrometer.core.instrument.Meter meter) {
        AttributesBuilder builder = Attributes.builder();
        meter.getId().getTags().forEach(tag ->
                builder.put(tag.getKey(), tag.getValue()));
        return builder.build();
    }
}
