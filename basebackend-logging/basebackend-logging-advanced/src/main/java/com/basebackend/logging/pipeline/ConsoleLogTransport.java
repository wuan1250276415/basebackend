package com.basebackend.logging.pipeline;

import ch.qos.logback.core.status.StatusManager;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;

/**
 * 控制台日志传输实现（默认）
 *
 * 将日志事件以结构化格式输出到标准输出。
 * 主要用于开发环境和作为 LogTransport SPI 的参考实现。
 *
 * <p><b>重要</b>：此类是 Logback 管道的一部分，在 Logback Appender 上下文中被调用。
 * 使用 SLF4J {@code Logger.debug()} 会触发新的日志事件，导致递归调用栈溢出。
 * 改用 {@code System.out} 直接输出，安全地规避该问题。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
public class ConsoleLogTransport implements LogTransport {

    @Override
    public void send(LogEvent event) {
        // 直接写 System.out，避免通过 SLF4J 触发递归日志事件
        System.out.printf("[Pipeline] %s [%s] %s - %s%n",
                event.timestamp(),
                event.level(),
                event.loggerName(),
                event.message());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
