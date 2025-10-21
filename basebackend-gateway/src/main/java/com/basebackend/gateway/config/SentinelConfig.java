package com.basebackend.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sentinel配置
 */
@Slf4j
@Configuration
public class SentinelConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                          ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(java.util.Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * 配置Sentinel异常处理器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 初始化限流降级回调
     */
    @PostConstruct
    public void initBlockHandler() {
        BlockRequestHandler blockRequestHandler = (exchange, t) -> {
            Map<String, Object> result = new HashMap<>();

            if (t instanceof FlowException) {
                result.put("code", 429);
                result.put("message", "请求过于频繁，请稍后再试");
                log.warn("触发限流: {}", exchange.getRequest().getPath());
            } else if (t instanceof DegradeException) {
                result.put("code", 503);
                result.put("message", "服务暂时不可用，请稍后再试");
                log.warn("触发熔断: {}", exchange.getRequest().getPath());
            } else if (t instanceof AuthorityException) {
                result.put("code", 403);
                result.put("message", "没有权限访问");
                log.warn("触发权限控制: {}", exchange.getRequest().getPath());
            } else {
                result.put("code", 500);
                result.put("message", "系统繁忙");
                log.error("未知的Sentinel异常", t);
            }

            result.put("success", false);
            result.put("timestamp", System.currentTimeMillis());

            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(result));
        };

        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
