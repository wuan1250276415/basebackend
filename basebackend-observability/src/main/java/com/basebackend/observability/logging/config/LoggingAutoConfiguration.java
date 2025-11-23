package com.basebackend.observability.logging.config;

import com.basebackend.observability.logging.format.LogAttributeEnricher;
import com.basebackend.observability.logging.masking.MaskingConverter;
import com.basebackend.observability.logging.routing.LogRoutingAppender;
import com.basebackend.observability.logging.sampling.LogSamplingTurboFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 日志系统自动配置
 * <p>
 * 集成所有日志增强组件，包括：
 * <ul>
 *     <li>结构化日志格式（JSON）</li>
 *     <li>敏感信息脱敏</li>
 *     <li>日志采样（基于级别和包名）</li>
 *     <li>多出口路由（Console、Loki、File）</li>
 * </ul>
 * </p>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * observability:
 *   logging:
 *     enabled: true
 *     format:
 *       type: json
 *       include-trace-context: true
 *       include-business-context: true
 *     masking:
 *       enabled: true
 *       rules:
 *         - field-pattern: "mobile|phone"
 *           strategy: PARTIAL
 *         - field-pattern: "password|pwd"
 *           strategy: HIDE
 *     sampling:
 *       enabled: true
 *       rules:
 *         - level: ERROR
 *           rate: 1.0
 *         - level: INFO
 *           rate: 0.1
 *     routing:
 *       enabled: true
 *       destinations:
 *         - name: console
 *           enabled: true
 *           level: INFO
 *         - name: loki
 *           enabled: true
 *           url: http://localhost:3100
 * }</pre>
 * </p>
 * <p>
 * <b>使用说明：</b>
 * <ol>
 *     <li>LogAttributeEnricher Bean 自动注入，可在 Filter/Interceptor 中使用</li>
 *     <li>MaskingConverter 需要在 logback-spring.xml 中配置为自定义转换器</li>
 *     <li>采样和路由功能通过 Logback TurboFilter 和 Appender 实现</li>
 * </ol>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see LoggingProperties 日志配置属性
 * @see LogAttributeEnricher 日志属性填充器
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(
        prefix = "observability.logging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@AutoConfigureAfter(name = {
        "com.basebackend.observability.otel.config.OtelAutoConfiguration",
        "com.basebackend.observability.tracing.config.TracingAutoConfiguration"
})
public class LoggingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LoggingAutoConfiguration.class);

    public LoggingAutoConfiguration(LoggingProperties properties) {
        log.info("日志系统增强已启用: enabled={}", properties.isEnabled());
        log.info("日志配置概览: format={}, masking={}, sampling={}, routing={}",
                properties.getFormat().getType(),
                properties.getMasking().isEnabled(),
                properties.getSampling().isEnabled(),
                properties.getRouting().isEnabled());

        // 初始化脱敏转换器的配置规则
        if (properties.getMasking().isEnabled() && !properties.getMasking().getRules().isEmpty()) {
            MaskingConverter.setConfiguredRules(properties.getMasking().getRules());
            log.info("已加载 {} 条自定义脱敏规则", properties.getMasking().getRules().size());
        }
    }

    /**
     * 创建日志属性填充器 Bean
     * <p>
     * 用于自动填充追踪上下文和业务上下文到 MDC。
     * </p>
     *
     * @return LogAttributeEnricher 实例
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "observability.logging.format",
            name = "include-trace-context",
            havingValue = "true",
            matchIfMissing = true
    )
    public LogAttributeEnricher logAttributeEnricher() {
        log.info("日志属性填充器已创建");
        return new LogAttributeEnricher();
    }

    /**
     * 创建日志采样过滤器 Bean
     * <p>
     * 基于日志级别和包名进行采样，减少日志量。
     * </p>
     *
     * @param properties 日志配置属性
     * @return LogSamplingTurboFilter 实例
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "observability.logging.sampling",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public LogSamplingTurboFilter logSamplingTurboFilter(LoggingProperties properties) {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(properties.getSampling().isEnabled());
        filter.setRules(properties.getSampling().getRules());
        filter.start();
        log.info("日志采样过滤器已创建，规则数: {}", properties.getSampling().getRules().size());
        return filter;
    }

    /**
     * 创建日志路由器 Bean
     * <p>
     * 根据配置将日志路由到不同目标。
     * </p>
     *
     * @param properties 日志配置属性
     * @return LogRoutingAppender 实例
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "observability.logging.routing",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public LogRoutingAppender logRoutingAppender(LoggingProperties properties) {
        LogRoutingAppender appender = new LogRoutingAppender();
        appender.setRoutingEnabled(properties.getRouting().isEnabled());
        appender.setDestinations(properties.getRouting().getDestinations());
        appender.start();
        log.info("日志路由器已创建，目标数: {}", properties.getRouting().getDestinations().size());
        return appender;
    }
}
