package com.basebackend.logging.pipeline;

/**
 * 日志管道处理器接口
 *
 * 管道链中的处理环节。每个处理器可以对日志事件进行转换、过滤或增强，
 * 然后传递给链中的下一个处理器。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@FunctionalInterface
public interface LogPipelineProcessor {

    /**
     * 处理日志事件
     *
     * @param event 输入事件
     * @return 处理后的事件，返回 null 表示过滤掉该事件
     */
    LogTransport.LogEvent process(LogTransport.LogEvent event);
}
