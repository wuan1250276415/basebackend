package com.basebackend.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务降级处理控制器
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    public Mono<Map<String, Object>> userFallback() {
        log.warn("User service fallback triggered");
        return Mono.just(createFallbackResponse("用户服务暂时不可用，请稍后重试"));
    }

    @GetMapping("/system")
    public Mono<Map<String, Object>> systemFallback() {
        log.warn("System service fallback triggered");
        return Mono.just(createFallbackResponse("系统服务暂时不可用，请稍后重试"));
    }

    @GetMapping("/auth")
    public Mono<Map<String, Object>> authFallback() {
        log.warn("Auth service fallback triggered");
        return Mono.just(createFallbackResponse("认证服务暂时不可用，请稍后重试"));
    }

    @GetMapping("/notification")
    public Mono<Map<String, Object>> notificationFallback() {
        log.warn("Notification service fallback triggered");
        return Mono.just(createFallbackResponse("通知服务暂时不可用，请稍后重试"));
    }

    @GetMapping("/observability")
    public Mono<Map<String, Object>> observabilityFallback() {
        log.warn("Observability service fallback triggered");
        return Mono.just(createFallbackResponse("可观测性服务暂时不可用，请稍后重试"));
    }

    @GetMapping("/file")
    public Mono<Map<String, Object>> fileFallback() {
        log.warn("File service fallback triggered");
        return Mono.just(createFallbackResponse("文件服务暂时不可用，请稍后重试"));
    }

    @GetMapping("/admin")
    public Mono<Map<String, Object>> adminFallback() {
        log.warn("Admin service fallback triggered");
        return Mono.just(createFallbackResponse("管理服务暂时不可用，请稍后重试"));
    }

    private Map<String, Object> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 503);
        response.put("message", message);
        response.put("data", null);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
