package com.basebackend.logging.cost;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志成本治理配置属性
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Data
@ConfigurationProperties(prefix = "basebackend.logging.cost")
public class LogCostProperties {

    /**
     * 是否启用日志成本治理
     */
    private boolean enabled = false;

    /**
     * 统计窗口大小（秒）
     */
    private int windowSeconds = 60;

    /**
     * 每服务每窗口的事件数阈值，超过后触发采样
     */
    private long eventThreshold = 10000;

    /**
     * 每服务每窗口的字节数阈值（默认 10MB），超过后触发采样
     */
    private long byteThreshold = 10485760L;

    /**
     * 超阈值后的采样率（0.0 ~ 1.0），如 0.1 表示仅保留 10%
     */
    private double samplingRate = 0.1;

    /**
     * 是否对 WARN/ERROR 级别豁免采样
     */
    private boolean exemptHighSeverity = true;
}
