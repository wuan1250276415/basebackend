package com.basebackend.logging.pipeline;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 日志管道链执行器
 *
 * 将日志事件依次通过注册的处理器链，最终发送到传输后端。
 * 任一处理器返回 null 则中止该事件的传递（过滤）。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class LogPipelineChain {

    private final List<LogPipelineProcessor> processors;
    private final LogTransport transport;

    public LogPipelineChain(List<LogPipelineProcessor> processors, LogTransport transport) {
        this.processors = processors != null ? processors : Collections.emptyList();
        this.transport = transport;
    }

    /**
     * 将事件通过处理器链后发送到传输后端
     *
     * @param event 原始日志事件
     */
    public void execute(LogTransport.LogEvent event) {
        LogTransport.LogEvent current = event;
        for (LogPipelineProcessor processor : processors) {
            current = processor.process(current);
            if (current == null) {
                log.trace("日志事件被处理器 {} 过滤", processor.getClass().getSimpleName());
                return;
            }
        }
        try {
            transport.send(current);
        } catch (LogTransport.TransportException e) {
            log.warn("日志管道传输失败: {}", e.getMessage());
        }
    }

    /**
     * 获取处理器链长度
     */
    public int getProcessorCount() {
        return processors.size();
    }

    /**
     * 获取传输后端
     */
    public LogTransport getTransport() {
        return transport;
    }
}
