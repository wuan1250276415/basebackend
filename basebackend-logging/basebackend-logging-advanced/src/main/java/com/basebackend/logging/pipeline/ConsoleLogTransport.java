package com.basebackend.logging.pipeline;

import lombok.extern.slf4j.Slf4j;

/**
 * 控制台日志传输实现（默认）
 *
 * 将日志事件以结构化格式输出到标准输出。
 * 主要用于开发环境和作为 LogTransport SPI 的参考实现。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class ConsoleLogTransport implements LogTransport {

    @Override
    public void send(LogEvent event) {
        log.debug("[Pipeline] {} [{}] {} - {}",
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
