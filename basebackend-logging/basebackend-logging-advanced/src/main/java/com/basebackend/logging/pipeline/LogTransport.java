package com.basebackend.logging.pipeline;

import java.util.Map;

/**
 * 日志传输 SPI 接口
 *
 * 可插拔的日志传输后端。实现方负责将日志事件发送到目标系统（如 Kafka、Pulsar、Loki 等）。
 * 通过 Spring @ConditionalOnMissingBean 机制替换默认实现即可接入新传输后端。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
public interface LogTransport extends AutoCloseable {

    /**
     * 发送单条日志事件
     *
     * @param event 日志事件
     * @throws TransportException 传输失败时抛出
     */
    void send(LogEvent event) throws TransportException;

    /**
     * 批量发送日志事件
     *
     * @param events 日志事件列表
     * @throws TransportException 传输失败时抛出
     */
    default void sendBatch(java.util.List<LogEvent> events) throws TransportException {
        for (LogEvent event : events) {
            send(event);
        }
    }

    /**
     * 检查传输是否可用
     */
    boolean isAvailable();

    /**
     * 关闭传输连接
     */
    @Override
    default void close() {
        // default no-op
    }

    /**
     * 日志事件数据模型
     */
    record LogEvent(
            String timestamp,
            String level,
            String loggerName,
            String message,
            String threadName,
            String traceId,
            Map<String, String> mdc,
            String throwable
    ) {
    }

    /**
     * 传输异常
     */
    class TransportException extends Exception {
        public TransportException(String message) {
            super(message);
        }

        public TransportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
