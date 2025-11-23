package com.basebackend.observability.logging.routing;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.Appender;
import com.basebackend.observability.logging.config.LoggingProperties;
import org.slf4j.MDC;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志路由 Appender
 * <p>
 * 根据配置将日志路由到不同的目标（Console、File、Loki 等）。
 * </p>
 * <p>
 * <b>功能：</b>
 * <ul>
 *     <li>按日志级别路由到不同目标</li>
 *     <li>按业务类别（category）路由</li>
 *     <li>支持多目标同时输出</li>
 * </ul>
 * </p>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * observability:
 *   logging:
 *     routing:
 *       enabled: true
 *       destinations:
 *         - name: console
 *           type: console
 *           enabled: true
 *           level: INFO
 *         - name: error-file
 *           type: file
 *           enabled: true
 *           level: ERROR
 *           path: /var/log/app/error.log
 *         - name: audit
 *           type: file
 *           enabled: true
 *           level: INFO
 *           category: AUDIT     # 仅路由审计日志
 *           path: /var/log/app/audit.log
 * }</pre>
 * </p>
 * <p>
 * <b>使用方式：</b>
 * 在 logback-spring.xml 中配置：
 * <pre>{@code
 * <appender name="ROUTING" class="com.basebackend.observability.logging.routing.LogRoutingAppender">
 *     <appender-ref ref="CONSOLE"/>
 *     <appender-ref ref="FILE"/>
 * </appender>
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class LogRoutingAppender extends AppenderBase<ILoggingEvent>
        implements AppenderAttachable<ILoggingEvent> {

    /**
     * MDC key for log category
     */
    public static final String CATEGORY_KEY = "logCategory";

    /**
     * 子 Appender 管理器
     */
    private final AppenderAttachableImpl<ILoggingEvent> appenderAttachable =
            new AppenderAttachableImpl<>();

    /**
     * 路由目标配置
     */
    private volatile List<LoggingProperties.Destination> destinations;

    /**
     * 目标名称到 Appender 的映射
     */
    private final Map<String, Appender<ILoggingEvent>> namedAppenders = new ConcurrentHashMap<>();

    /**
     * 是否启用路由
     */
    private volatile boolean routingEnabled = true;

    /**
     * 设置路由目标配置
     *
     * @param destinations 目标配置列表
     */
    public void setDestinations(List<LoggingProperties.Destination> destinations) {
        this.destinations = destinations;
    }

    /**
     * 设置是否启用路由
     *
     * @param enabled 是否启用
     */
    public void setRoutingEnabled(boolean enabled) {
        this.routingEnabled = enabled;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!routingEnabled || destinations == null || destinations.isEmpty()) {
            // 路由未启用或无配置，转发到所有子 Appender
            appenderAttachable.appendLoopOnAppenders(event);
            return;
        }

        // 根据配置路由到匹配的目标
        for (LoggingProperties.Destination dest : destinations) {
            if (!dest.isEnabled()) {
                continue;
            }

            if (shouldRoute(event, dest)) {
                Appender<ILoggingEvent> appender = namedAppenders.get(dest.getName());
                if (appender != null) {
                    appender.doAppend(event);
                }
            }
        }
    }

    /**
     * 判断日志事件是否应路由到指定目标
     *
     * @param event 日志事件
     * @param dest  目标配置
     * @return 是否路由
     */
    private boolean shouldRoute(ILoggingEvent event, LoggingProperties.Destination dest) {
        // 检查日志级别
        Level destLevel = Level.toLevel(dest.getLevel(), Level.INFO);
        if (event.getLevel().toInt() < destLevel.toInt()) {
            return false;
        }

        // 检查业务类别
        String category = dest.getCategory();
        if (category != null && !category.isEmpty()) {
            String eventCategory = MDC.get(CATEGORY_KEY);
            if (eventCategory == null || !eventCategory.equals(category)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 注册命名 Appender
     * <p>
     * 用于将配置中的目标名称映射到实际的 Appender 实例。
     * </p>
     *
     * @param name     目标名称
     * @param appender Appender 实例
     */
    public void registerNamedAppender(String name, Appender<ILoggingEvent> appender) {
        if (name != null && appender != null) {
            namedAppenders.put(name, appender);
        }
    }

    // ========== AppenderAttachable 接口实现 ==========

    @Override
    public void addAppender(Appender<ILoggingEvent> appender) {
        appenderAttachable.addAppender(appender);
        // 同时注册到命名映射
        if (appender.getName() != null) {
            namedAppenders.put(appender.getName(), appender);
        }
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return appenderAttachable.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return appenderAttachable.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return appenderAttachable.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenderAttachable.detachAndStopAllAppenders();
        namedAppenders.clear();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        if (appender.getName() != null) {
            namedAppenders.remove(appender.getName());
        }
        return appenderAttachable.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        namedAppenders.remove(name);
        return appenderAttachable.detachAppender(name);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        detachAndStopAllAppenders();
    }

    /**
     * 设置日志类别到 MDC
     * <p>
     * 便捷方法，用于在业务代码中标记日志类别。
     * </p>
     *
     * @param category 日志类别（如 AUDIT、SECURITY）
     */
    public static void setCategory(String category) {
        if (category != null) {
            MDC.put(CATEGORY_KEY, category);
        }
    }

    /**
     * 清除日志类别
     */
    public static void clearCategory() {
        MDC.remove(CATEGORY_KEY);
    }
}
