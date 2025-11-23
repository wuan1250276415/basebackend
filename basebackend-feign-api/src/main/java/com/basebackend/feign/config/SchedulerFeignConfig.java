package com.basebackend.feign.config;

import feign.Request;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 调度器 Feign 客户端配置
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Configuration
public class SchedulerFeignConfig {

    /**
     * 配置默认的连接和读取超时
     */
    @Bean
    public Request.Options options() {
        // 连接超时 5 秒，读取超时 10 秒
        return new Request.Options(
                5 * 1000,  // connect-timeout
                10 * 1000, // read-timeout
                true
        );
    }

    /**
     * 自定义错误解码器
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("[Feign错误] 服务调用失败: methodKey={}, status={}, reason={}",
                    methodKey, response.status(), response.reason());

            // 这里可以处理特定的错误响应
            // 例如，根据状态码返回不同的异常
            if (response.status() >= 500) {
                return new RuntimeException("调度器服务内部错误: " + response.status());
            } else if (response.status() == 404) {
                return new IllegalArgumentException("调度器服务资源不存在: " + response.status());
            } else if (response.status() == 403) {
                return new IllegalStateException("调度器服务访问被拒绝: " + response.status());
            }

            return new RuntimeException("调度器服务调用失败: " + response.status());
        };
    }
}
