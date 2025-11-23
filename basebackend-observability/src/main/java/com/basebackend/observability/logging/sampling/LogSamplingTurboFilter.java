package com.basebackend.observability.logging.sampling;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.basebackend.observability.logging.config.LoggingProperties;
import org.slf4j.Marker;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 日志采样 TurboFilter
 * <p>
 * 基于日志级别和包名进行采样过滤，减少日志量同时保留关键信息。
 * </p>
 * <p>
 * <b>功能：</b>
 * <ul>
 *     <li>按日志级别设置不同采样率（ERROR 100%，INFO 10%）</li>
 *     <li>按包名细粒度控制采样率</li>
 *     <li>支持动态配置更新</li>
 * </ul>
 * </p>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * observability:
 *   logging:
 *     sampling:
 *       enabled: true
 *       rules:
 *         - level: ERROR
 *           rate: 1.0          # ERROR 100% 采样
 *         - level: WARN
 *           rate: 1.0          # WARN 100% 采样
 *         - level: INFO
 *           rate: 0.1          # INFO 10% 采样
 *           package-name: com.basebackend.user  # 可选：仅对指定包生效
 *         - level: DEBUG
 *           rate: 0.01         # DEBUG 1% 采样
 * }</pre>
 * </p>
 * <p>
 * <b>使用方式：</b>
 * 通过 LoggingAutoConfiguration 自动注册，或在 logback-spring.xml 中配置：
 * <pre>{@code
 * <turboFilter class="com.basebackend.observability.logging.sampling.LogSamplingTurboFilter" />
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class LogSamplingTurboFilter extends TurboFilter {

    /**
     * 采样规则列表
     */
    private volatile List<LoggingProperties.SamplingRule> rules = Collections.emptyList();

    /**
     * 级别采样率缓存（无包名限制的规则）
     */
    private final Map<Level, Double> levelRates = new ConcurrentHashMap<>();

    /**
     * 包名+级别采样率缓存
     */
    private final Map<String, Double> packageLevelRates = new ConcurrentHashMap<>();

    /**
     * 是否启用采样
     */
    private volatile boolean enabled = true;

    /**
     * 设置采样规则
     *
     * @param rules 采样规则列表
     */
    public void setRules(List<LoggingProperties.SamplingRule> rules) {
        this.rules = (rules == null) ? Collections.emptyList() : rules;
        rebuildCache();
    }

    /**
     * 设置是否启用
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 重建采样率缓存
     */
    private void rebuildCache() {
        levelRates.clear();
        packageLevelRates.clear();

        for (LoggingProperties.SamplingRule rule : rules) {
            Level level = Level.toLevel(rule.getLevel(), null);
            if (level == null) {
                continue;
            }

            String packageName = rule.getPackageName();
            if (packageName == null || packageName.isEmpty()) {
                // 无包名限制，存入级别缓存
                levelRates.put(level, rule.getRate());
            } else {
                // 有包名限制，存入包名+级别缓存
                String key = packageName + ":" + level.toString();
                packageLevelRates.put(key, rule.getRate());
            }
        }
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {
        if (!enabled) {
            return FilterReply.NEUTRAL;
        }

        // 获取采样率
        double rate = getSamplingRate(logger.getName(), level);

        // rate >= 1.0 表示全量采样，直接通过
        if (rate >= 1.0) {
            return FilterReply.NEUTRAL;
        }

        // rate <= 0.0 表示完全过滤
        if (rate <= 0.0) {
            return FilterReply.DENY;
        }

        // 基于采样率随机决定
        if (ThreadLocalRandom.current().nextDouble() < rate) {
            return FilterReply.NEUTRAL;
        }

        return FilterReply.DENY;
    }

    /**
     * 获取采样率
     * <p>
     * 优先级：包名+级别 > 级别 > 默认 1.0
     * </p>
     *
     * @param loggerName logger 名称（通常是类全限定名）
     * @param level      日志级别
     * @return 采样率
     */
    private double getSamplingRate(String loggerName, Level level) {
        // 1. 尝试匹配包名+级别规则
        for (Map.Entry<String, Double> entry : packageLevelRates.entrySet()) {
            String key = entry.getKey();
            int colonIndex = key.lastIndexOf(':');
            if (colonIndex > 0) {
                String packageName = key.substring(0, colonIndex);
                String levelStr = key.substring(colonIndex + 1);
                if (loggerName.startsWith(packageName) && level.toString().equals(levelStr)) {
                    return entry.getValue();
                }
            }
        }

        // 2. 尝试匹配级别规则
        Double levelRate = levelRates.get(level);
        if (levelRate != null) {
            return levelRate;
        }

        // 3. 默认全量采样
        return 1.0;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        levelRates.clear();
        packageLevelRates.clear();
    }
}
